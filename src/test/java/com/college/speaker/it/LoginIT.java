package com.college.speaker.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Journey 1 — signing in, and seeing only what your role is allowed to see.
 */
class LoginIT extends SeleniumTestBase {

    @Test
    @DisplayName("a valid sign-in lands on the dashboard")
    void signInSucceeds() {
        signInAs(SPEAKER_USER);

        assertThat(browser().getCurrentUrl()).endsWith("/dashboard");
        assertThat(browser().findElement(By.id("page-title")).getText()).isEqualTo("Dashboard");
        assertThat(browser().findElement(By.id("current-user")).getText()).isEqualTo(SPEAKER_USER);
    }

    @Test
    @DisplayName("a wrong password shows an error and does not sign you in")
    void signInFails() {
        signIn(SPEAKER_USER, "definitely-not-the-password");

        assertThat(browser().getCurrentUrl()).contains("/login");
        assertThat(browser().findElement(By.id("login-error")).getText())
                .contains("Wrong username or password");
    }

    @Test
    @DisplayName("an unauthenticated visitor is sent to the sign-in page")
    void protectedPagesRedirect() {
        open("/sessions");

        assertThat(browser().getCurrentUrl()).contains("/login");
    }

    @Test
    @DisplayName("only a speaker is offered the New session link")
    void newSessionLinkIsRoleBased() {
        signInAs(SPEAKER_USER);
        assertThat(isPresent(By.id("nav-new"))).isTrue();

        switchTo(REVIEWER_USER);
        assertThat(isPresent(By.id("nav-new"))).isFalse();
    }

    @Test
    @DisplayName("signing out ends the session")
    void signOutWorks() {
        signInAs(ORGANIZER_USER);
        signOut();

        assertThat(browser().findElement(By.id("logout-message")).getText())
                .contains("signed out");
    }
}
