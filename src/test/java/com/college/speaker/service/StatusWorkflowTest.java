package com.college.speaker.service;

import com.college.speaker.domain.SessionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests — no Spring context, so they run in milliseconds and keep the
 * Jenkins build fast.
 */
class StatusWorkflowTest {

    private final StatusWorkflow workflow = new StatusWorkflow();

    private static final Set<String> SPEAKER = Set.of("SPEAKER");
    private static final Set<String> REVIEWER = Set.of("REVIEWER");
    private static final Set<String> ORGANIZER = Set.of("ORGANIZER");

    @Test
    @DisplayName("a speaker can submit a draft")
    void speakerSubmitsDraft() {
        assertThat(workflow.isAllowed(SessionStatus.DRAFT, SessionStatus.SUBMITTED, SPEAKER)).isTrue();
    }

    @Test
    @DisplayName("a speaker cannot approve their own submission")
    void speakerCannotApprove() {
        assertThat(workflow.isAllowed(SessionStatus.SUBMITTED, SessionStatus.APPROVED, SPEAKER)).isFalse();
    }

    @Test
    @DisplayName("a reviewer approves, rejects or sends back a submission")
    void reviewerDecides() {
        assertThat(workflow.allowedTransitions(SessionStatus.SUBMITTED, REVIEWER))
                .extracting(StatusWorkflow.Transition::target)
                .containsExactlyInAnyOrder(SessionStatus.APPROVED, SessionStatus.REJECTED, SessionStatus.DRAFT);
    }

    @Test
    @DisplayName("only an organizer schedules an approved session")
    void organizerSchedules() {
        assertThat(workflow.isAllowed(SessionStatus.APPROVED, SessionStatus.SCHEDULED, ORGANIZER)).isTrue();
        assertThat(workflow.isAllowed(SessionStatus.APPROVED, SessionStatus.SCHEDULED, REVIEWER)).isFalse();
    }

    @Test
    @DisplayName("a draft cannot jump straight to scheduled")
    void noSkippingAhead() {
        assertThat(workflow.isAllowed(SessionStatus.DRAFT, SessionStatus.SCHEDULED, ORGANIZER)).isFalse();
    }

    @Test
    @DisplayName("rejected and cancelled are dead ends")
    void terminalStates() {
        assertThat(workflow.transitionsFrom(SessionStatus.REJECTED)).isEmpty();
        assertThat(workflow.transitionsFrom(SessionStatus.CANCELLED)).isEmpty();
    }
}
