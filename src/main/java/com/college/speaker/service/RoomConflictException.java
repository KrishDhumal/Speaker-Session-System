package com.college.speaker.service;

import com.college.speaker.domain.SpeakerSession;

/**
 * Thrown when a requested room is already scheduled for an overlapping time
 * slot. Carries the clashing session so the caller can name it in the error.
 */
public class RoomConflictException extends RuntimeException {

    private final SpeakerSession conflictingSession;

    public RoomConflictException(String room, SpeakerSession conflictingSession) {
        super("Room " + room + " is already scheduled for \"" + conflictingSession.getTitle()
                + "\" at " + conflictingSession.getStartTimeDisplay());
        this.conflictingSession = conflictingSession;
    }

    public SpeakerSession getConflictingSession() {
        return conflictingSession;
    }
}
