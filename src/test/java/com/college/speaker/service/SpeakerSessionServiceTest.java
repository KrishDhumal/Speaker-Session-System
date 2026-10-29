package com.college.speaker.service;

import com.college.speaker.domain.SessionStatus;
import com.college.speaker.domain.SpeakerSession;
import com.college.speaker.domain.Track;
import com.college.speaker.repo.SpeakerSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the service against a real (in-memory) database, so the search
 * query itself is covered.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:servicetest;DB_CLOSE_DELAY=-1",
        "app.seed-demo-data=false"
})
class SpeakerSessionServiceTest {

    @Autowired
    private SpeakerSessionService service;

    @Autowired
    private SpeakerSessionRepository repository;

    @BeforeEach
    void reset() {
        repository.deleteAll();
    }

    private SpeakerSession sample(String title, String speaker, Track track) {
        SpeakerSession s = new SpeakerSession();
        s.setTitle(title);
        s.setSpeakerName(speaker);
        s.setSpeakerEmail("someone@example.com");
        s.setTrack(track);
        s.setStartTime(LocalDateTime.now().plusDays(10));
        s.setDurationMinutes(45);
        return s;
    }

    @Test
    @DisplayName("a new session always starts life as a draft")
    void createStartsAsDraft() {
        SpeakerSession saved = service.create(sample("Pipelines 101", "Asha", Track.DEVOPS), "speaker");

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(SessionStatus.DRAFT);
        assertThat(saved.getCreatedBy()).isEqualTo("speaker");
    }

    @Test
    @DisplayName("search matches title or speaker name, case-insensitively")
    void searchByKeyword() {
        service.create(sample("Pipelines 101", "Asha", Track.DEVOPS), "speaker");
        service.create(sample("Cloud Costs", "Rahul", Track.CLOUD), "speaker");

        assertThat(service.search("pipelines", null, null)).hasSize(1);
        assertThat(service.search("RAHUL", null, null)).hasSize(1);
        assertThat(service.search("nothing-here", null, null)).isEmpty();
    }

    @Test
    @DisplayName("empty filters return every session")
    void searchWithoutFilters() {
        service.create(sample("Pipelines 101", "Asha", Track.DEVOPS), "speaker");
        service.create(sample("Cloud Costs", "Rahul", Track.CLOUD), "speaker");

        assertThat(service.search(null, null, null)).hasSize(2);
        assertThat(service.search("  ", null, null)).hasSize(2);
    }

    @Test
    @DisplayName("status and track filters combine with the keyword")
    void searchByStatusAndTrack() {
        SpeakerSession devops = service.create(sample("Pipelines 101", "Asha", Track.DEVOPS), "speaker");
        service.create(sample("Cloud Costs", "Rahul", Track.CLOUD), "speaker");
        service.changeStatus(devops.getId(), SessionStatus.SUBMITTED, Set.of("SPEAKER"), null);

        assertThat(service.search(null, SessionStatus.SUBMITTED, null)).hasSize(1);
        assertThat(service.search(null, null, Track.CLOUD)).hasSize(1);
        assertThat(service.search("pipelines", SessionStatus.SUBMITTED, Track.DEVOPS)).hasSize(1);
        assertThat(service.search("pipelines", SessionStatus.DRAFT, Track.DEVOPS)).isEmpty();
    }

    @Test
    @DisplayName("update changes the details but never the status")
    void updateKeepsStatus() {
        SpeakerSession saved = service.create(sample("Old title", "Asha", Track.DEVOPS), "speaker");
        service.changeStatus(saved.getId(), SessionStatus.SUBMITTED, Set.of("SPEAKER"), null);

        SpeakerSession edit = sample("New title", "Asha Menon", Track.CLOUD);
        edit.setStatus(SessionStatus.APPROVED);
        SpeakerSession updated = service.update(saved.getId(), edit);

        assertThat(updated.getTitle()).isEqualTo("New title");
        assertThat(updated.getTrack()).isEqualTo(Track.CLOUD);
        assertThat(updated.getStatus()).isEqualTo(SessionStatus.SUBMITTED);
    }

    @Test
    @DisplayName("a role without permission cannot move the status")
    void rejectsForbiddenTransition() {
        SpeakerSession saved = service.create(sample("Pipelines 101", "Asha", Track.DEVOPS), "speaker");
        service.changeStatus(saved.getId(), SessionStatus.SUBMITTED, Set.of("SPEAKER"), null);

        assertThatThrownBy(() ->
                service.changeStatus(saved.getId(), SessionStatus.APPROVED, Set.of("SPEAKER"), null))
                .isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    @DisplayName("a review note is stored with the status change")
    void storesReviewNote() {
        SpeakerSession saved = service.create(sample("Pipelines 101", "Asha", Track.DEVOPS), "speaker");
        service.changeStatus(saved.getId(), SessionStatus.SUBMITTED, Set.of("SPEAKER"), null);
        SpeakerSession reviewed = service.changeStatus(
                saved.getId(), SessionStatus.REJECTED, Set.of("REVIEWER"), "Too similar to another talk");

        assertThat(reviewed.getReviewNote()).isEqualTo("Too similar to another talk");
    }

    @Test
    @DisplayName("asking for a missing session is a 404, not a crash")
    void missingSession() {
        assertThatThrownBy(() -> service.get(9999L)).isInstanceOf(SessionNotFoundException.class);
    }

    /** Books a room end to end: create, submit, approve, schedule. */
    private SpeakerSession bookRoom(String title, String room, java.time.LocalDateTime start) {
        SpeakerSession session = sample(title, "Asha", Track.DEVOPS);
        session.setRoom(room);
        session.setStartTime(start);
        SpeakerSession saved = service.create(session, "speaker");
        service.changeStatus(saved.getId(), SessionStatus.SUBMITTED, Set.of("SPEAKER"), null);
        service.changeStatus(saved.getId(), SessionStatus.APPROVED, Set.of("REVIEWER"), null);
        return service.changeStatus(saved.getId(), SessionStatus.SCHEDULED, Set.of("ORGANIZER"), null);
    }

    @Test
    @DisplayName("a room already scheduled for an overlapping time cannot be requested again")
    void roomClashIsRefusedImmediately() {
        SpeakerSession first = bookRoom("Pipelines 101", "Hall A", LocalDateTime.now().plusDays(10));

        SpeakerSession clashing = sample("Cloud Costs", "Rahul", Track.CLOUD);
        clashing.setRoom("hall a"); // case-insensitive on purpose
        clashing.setStartTime(first.getStartTime().plusMinutes(10)); // overlaps

        assertThatThrownBy(() -> service.create(clashing, "speaker"))
                .isInstanceOf(RoomConflictException.class)
                .hasMessageContaining("hall a")
                .hasMessageContaining("Pipelines 101");
    }

    @Test
    @DisplayName("the same room is fine once the two time slots no longer overlap")
    void roomReusedAtADifferentTimeIsAllowed() {
        SpeakerSession first = bookRoom("Pipelines 101", "Hall A", LocalDateTime.now().plusDays(10));

        SpeakerSession second = sample("Cloud Costs", "Rahul", Track.CLOUD);
        second.setRoom("Hall A");
        second.setStartTime(first.getEndTime().plusMinutes(5)); // starts after the first ends
        SpeakerSession savedSecond = service.create(second, "speaker");

        assertThat(savedSecond.getRoom()).isEqualTo("Hall A");
    }

    @Test
    @DisplayName("editing into a clashing room is refused; keeping a session's own room is fine")
    void roomClashOnUpdate() {
        SpeakerSession first = bookRoom("Pipelines 101", "Hall A", LocalDateTime.now().plusDays(10));

        // A second session, parked in a different room, with no conflict yet.
        SpeakerSession second = sample("Cloud Costs", "Rahul", Track.CLOUD);
        second.setRoom("Hall B");
        second.setStartTime(first.getStartTime());
        SpeakerSession savedSecond = service.create(second, "speaker");

        // Editing it to ask for Hall A at an overlapping time is refused.
        SpeakerSession edit = sample("Cloud Costs", "Rahul", Track.CLOUD);
        edit.setRoom("Hall A");
        edit.setStartTime(first.getStartTime());
        assertThatThrownBy(() -> service.update(savedSecond.getId(), edit))
                .isInstanceOf(RoomConflictException.class);

        // Re-saving the first session's own form, unchanged, must not clash with itself.
        SpeakerSession selfEdit = sample("Pipelines 101", "Asha", Track.DEVOPS);
        selfEdit.setRoom("Hall A");
        selfEdit.setStartTime(first.getStartTime());
        SpeakerSession updated = service.update(first.getId(), selfEdit);
        assertThat(updated.getRoom()).isEqualTo("Hall A");
    }

    @Test
    @DisplayName("the dashboard counts every status and track")
    void dashboardCounts() {
        service.create(sample("Pipelines 101", "Asha", Track.DEVOPS), "speaker");
        service.create(sample("Cloud Costs", "Rahul", Track.CLOUD), "speaker");

        assertThat(service.total()).isEqualTo(2);
        assertThat(service.countsByStatus().get(SessionStatus.DRAFT)).isEqualTo(2);
        assertThat(service.countsByTrack().get(Track.DEVOPS)).isEqualTo(1);
        assertThat(service.countsByTrack()).containsKey(Track.OTHER);
    }
}
