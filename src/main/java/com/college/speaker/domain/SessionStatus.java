package com.college.speaker.domain;

/**
 * Lifecycle of a talk proposal. Allowed moves between these values live in
 * {@link com.college.speaker.service.StatusWorkflow}.
 */
public enum SessionStatus {

    DRAFT("Draft"),
    SUBMITTED("Submitted for review"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    SCHEDULED("Scheduled"),
    CANCELLED("Cancelled");

    private final String label;

    SessionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
