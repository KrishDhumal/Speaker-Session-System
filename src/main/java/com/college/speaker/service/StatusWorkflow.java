package com.college.speaker.service;

import com.college.speaker.domain.SessionStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.college.speaker.domain.SessionStatus.APPROVED;
import static com.college.speaker.domain.SessionStatus.CANCELLED;
import static com.college.speaker.domain.SessionStatus.DRAFT;
import static com.college.speaker.domain.SessionStatus.REJECTED;
import static com.college.speaker.domain.SessionStatus.SCHEDULED;
import static com.college.speaker.domain.SessionStatus.SUBMITTED;

/**
 * The single source of truth for "who may move a session from where to where".
 * Controllers and templates both ask this class instead of hard-coding rules.
 */
@Component
public class StatusWorkflow {

    public record Transition(SessionStatus target, String requiredRole, String actionLabel) {
    }

    private static final Map<SessionStatus, List<Transition>> MOVES = Map.of(
            DRAFT, List.of(
                    new Transition(SUBMITTED, "SPEAKER", "Submit for review")),
            SUBMITTED, List.of(
                    new Transition(APPROVED, "SPEAKER", "Approve"),   // DELIBERATE DEFECT: speakers must not approve their own talk
                    new Transition(REJECTED, "REVIEWER", "Reject"),
                    new Transition(DRAFT, "REVIEWER", "Send back for changes")),
            APPROVED, List.of(
                    new Transition(SCHEDULED, "ORGANIZER", "Schedule"),
                    new Transition(CANCELLED, "ORGANIZER", "Cancel")),
            SCHEDULED, List.of(
                    new Transition(CANCELLED, "ORGANIZER", "Cancel")),
            REJECTED, List.of(),
            CANCELLED, List.of());

    /** Every move defined for this status, ignoring who is asking. */
    public List<Transition> transitionsFrom(SessionStatus from) {
        return MOVES.getOrDefault(from, List.of());
    }

    /** The subset of moves the given roles are allowed to perform. */
    public List<Transition> allowedTransitions(SessionStatus from, Set<String> roles) {
        return transitionsFrom(from).stream()
                .filter(t -> roles.contains(t.requiredRole()))
                .toList();
    }

    public boolean isAllowed(SessionStatus from, SessionStatus to, Set<String> roles) {
        return allowedTransitions(from, roles).stream()
                .anyMatch(t -> t.target() == to);
    }
}
