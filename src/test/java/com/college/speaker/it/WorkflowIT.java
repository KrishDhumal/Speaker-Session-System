package com.college.speaker.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Journeys 4 and 5 — the review workflow end to end, and the guard that stops a
 * role performing someone else's action.
 */
class WorkflowIT extends SeleniumTestBase {

    @Test
    @DisplayName("draft to scheduled, passing through all three roles")
    void fullApprovalJourney() {
        signInAs(SPEAKER_USER);
        long id = createSession(uniqueTitle("Full Journey Talk"), "Asha Menon", "DEVOPS");
        assertThat(browser().findElement(By.id("session-status")).getText()).isEqualTo("Draft");

        click(By.id("action-submitted"));
        assertThat(browser().findElement(By.id("session-status")).getText()).isEqualTo("Submitted for review");

        switchTo(REVIEWER_USER);
        open("/sessions/" + id);
        click(By.id("action-approved"));
        assertThat(browser().findElement(By.id("session-status")).getText()).isEqualTo("Approved");

        switchTo(ORGANIZER_USER);
        open("/sessions/" + id);
        click(By.id("action-scheduled"));
        assertThat(browser().findElement(By.id("session-status")).getText()).isEqualTo("Scheduled");
        assertThat(browser().findElement(By.id("flash-message")).getText()).contains("Scheduled");
    }

    @Test
    @DisplayName("a reviewer rejects a session and the note is kept")
    void rejectWithNote() {
        signInAs(SPEAKER_USER);
        long id = createSession(uniqueTitle("Rejected Talk"), "Dev Patel", "AI_ML");
        click(By.id("action-submitted"));

        switchTo(REVIEWER_USER);
        open("/sessions/" + id);
        // Each transition is its own form, so target the note box inside the
        // form that holds the Reject button rather than the first one on the page.
        browser().findElement(By.xpath(
                "//form[.//button[@id='action-rejected']]//input[@name='note']"))
                .sendKeys("Overlaps another talk");
        click(By.id("action-rejected"));

        assertThat(browser().findElement(By.id("session-status")).getText()).isEqualTo("Rejected");
        assertThat(browser().findElement(By.id("session-review-note")).getText()).isEqualTo("Overlaps another talk");
        assertThat(browser().findElement(By.id("no-transitions")).getText()).contains("no actions available");
    }

    @Test
    @DisplayName("a reviewer sends a session back to draft")
    void sendBackForChanges() {
        signInAs(SPEAKER_USER);
        long id = createSession(uniqueTitle("Send Back Talk"), "Meera Iyer", "CLOUD");
        click(By.id("action-submitted"));

        switchTo(REVIEWER_USER);
        open("/sessions/" + id);
        click(By.id("action-draft"));

        assertThat(browser().findElement(By.id("session-status")).getText()).isEqualTo("Draft");
    }

    @Test
    @DisplayName("a speaker is not shown approval buttons on their own submission")
    void speakerCannotApprove() {
        signInAs(SPEAKER_USER);
        createSession(uniqueTitle("Self Approval Talk"), "Asha Menon", "DEVOPS");
        click(By.id("action-submitted"));

        assertThat(isPresent(By.id("action-approved"))).isFalse();
        assertThat(browser().findElement(By.id("no-transitions")).getText()).contains("no actions available");
    }

    @Test
    @DisplayName("the server rejects a forbidden transition even if the request is forged")
    void serverEnforcesTheWorkflow() {
        signInAs(SPEAKER_USER);
        long id = createSession(uniqueTitle("Forged Request Talk"), "Rahul Nair", "WEB");

        // Post straight to the status endpoint, skipping the UI, as a speaker
        // trying to approve their own draft.
        forgeStatusChange(id, "APPROVED");

        assertThat(browser().findElement(By.id("flash-error")).getText()).contains("cannot move a session");
        assertThat(browser().findElement(By.id("session-status")).getText()).isEqualTo("Draft");
    }

    /** Builds and submits a status form by hand, bypassing the rendered buttons. */
    private void forgeStatusChange(long id, String target) {
        String csrf = browser().findElement(By.cssSelector("input[name='_csrf']")).getDomAttribute("value");
        ((org.openqa.selenium.JavascriptExecutor) browser()).executeScript("""
                const form = document.createElement('form');
                form.method = 'post';
                form.action = arguments[0];
                form.innerHTML = "<input name='target' value='" + arguments[1] + "'>"
                               + "<input name='_csrf' value='" + arguments[2] + "'>";
                document.body.appendChild(form);
                form.submit();
                """, "/sessions/" + id + "/status", target, csrf);
    }
}
