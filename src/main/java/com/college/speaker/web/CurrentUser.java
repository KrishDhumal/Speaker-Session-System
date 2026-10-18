package com.college.speaker.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

/** Small helpers so controllers do not repeat Spring Security plumbing. */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** Authority "ROLE_SPEAKER" becomes the plain role name "SPEAKER". */
    public static Set<String> roles(Authentication authentication) {
        if (authentication == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toSet());
    }

    public static String username(Authentication authentication) {
        return authentication == null ? "anonymous" : authentication.getName();
    }
}
