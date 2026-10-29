package com.college.speaker.service;

import com.college.speaker.domain.SessionStatus;
import com.college.speaker.domain.SpeakerSession;
import com.college.speaker.domain.Track;
import com.college.speaker.repo.SpeakerSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class SpeakerSessionService {

    private final SpeakerSessionRepository repository;
    private final StatusWorkflow workflow;

    public SpeakerSessionService(SpeakerSessionRepository repository, StatusWorkflow workflow) {
        this.repository = repository;
        this.workflow = workflow;
    }

    @Transactional(readOnly = true)
    public List<SpeakerSession> search(String keyword, SessionStatus status, Track track) {
        String trimmed = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return repository.search(trimmed, status, track);
    }

    @Transactional(readOnly = true)
    public SpeakerSession get(Long id) {
        return repository.findById(id).orElseThrow(() -> new SessionNotFoundException(id));
    }

    public SpeakerSession create(SpeakerSession session, String username) {
        session.setId(null);
        session.setStatus(SessionStatus.DRAFT);
        session.setCreatedBy(username);
        checkRoomAvailable(session, null);
        return repository.save(session);
    }

    /** Copies the editable fields only — status is moved through {@link #changeStatus}. */
    public SpeakerSession update(Long id, SpeakerSession edited) {
        SpeakerSession existing = get(id);
        checkRoomAvailable(edited, id);
        existing.setTitle(edited.getTitle());
        existing.setSpeakerName(edited.getSpeakerName());
        existing.setSpeakerEmail(edited.getSpeakerEmail());
        existing.setTrack(edited.getTrack());
        existing.setRoom(edited.getRoom());
        existing.setStartTime(edited.getStartTime());
        existing.setDurationMinutes(edited.getDurationMinutes());
        existing.setSummary(edited.getSummary());
        return repository.save(existing);
    }

    public SpeakerSession changeStatus(Long id, SessionStatus target, Set<String> roles, String note) {
        SpeakerSession session = get(id);
        if (!workflow.isAllowed(session.getStatus(), target, roles)) {
            throw new IllegalTransitionException(session.getStatus(), target);
        }
        if (target == SessionStatus.SCHEDULED) {
            checkRoomAvailable(session, session.getId());
        }
        session.setStatus(target);
        if (StringUtils.hasText(note)) {
            session.setReviewNote(note.trim());
        }
        return repository.save(session);
    }

    /**
     * Refuses a room that is already scheduled for an overlapping time slot.
     * A speaker hits this while drafting a proposal; an organizer hits it
     * again when actually scheduling two approved talks into the same room.
     * {@code excludeId} leaves a session's own prior booking out of the check
     * (editing a session, or re-confirming the one already being scheduled).
     */
    private void checkRoomAvailable(SpeakerSession candidate, Long excludeId) {
        if (!StringUtils.hasText(candidate.getRoom()) || candidate.getStartTime() == null) {
            return;
        }
        for (SpeakerSession existing : repository.findByRoomIgnoreCaseAndStatus(
                candidate.getRoom(), SessionStatus.SCHEDULED)) {
            if (existing.getId().equals(excludeId)) {
                continue;
            }
            if (overlaps(candidate, existing)) {
                throw new RoomConflictException(candidate.getRoom(), existing);
            }
        }
    }

    private boolean overlaps(SpeakerSession a, SpeakerSession b) {
        return a.getStartTime().isBefore(b.getEndTime()) && b.getStartTime().isBefore(a.getEndTime());
    }

    public void delete(Long id) {
        repository.delete(get(id));
    }

    @Transactional(readOnly = true)
    public Map<SessionStatus, Long> countsByStatus() {
        Map<SessionStatus, Long> counts = new LinkedHashMap<>();
        for (SessionStatus status : SessionStatus.values()) {
            counts.put(status, repository.countByStatus(status));
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public Map<Track, Long> countsByTrack() {
        Map<Track, Long> counts = new LinkedHashMap<>();
        for (Track track : Track.values()) {
            counts.put(track, repository.countByTrack(track));
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public List<SpeakerSession> upcomingScheduled() {
        return repository.findByStatusAndStartTimeAfterOrderByStartTimeAsc(
                SessionStatus.SCHEDULED, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public long total() {
        return repository.count();
    }
}
