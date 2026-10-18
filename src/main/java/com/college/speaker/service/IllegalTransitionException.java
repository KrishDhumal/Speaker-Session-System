package com.college.speaker.service;

import com.college.speaker.domain.SessionStatus;

public class IllegalTransitionException extends RuntimeException {

    public IllegalTransitionException(SessionStatus from, SessionStatus to) {
        super("Your role cannot move a session from " + from + " to " + to);
    }
}
