package com.college.speaker.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Journey 2 — a speaker creates, views and edits a session, and the form
 * refuses invalid input.
 */
class SessionCrudIT extends SeleniumTestBase {

    @Test
    @DisplayName("a speaker creates a session and it opens as a draft")
    void createSessionStartsAsDraft() {
        signInAs(SPEAKER_USER);
        String title = uniqueTitle("Pipelines in Practice");

        createSession(title, "Asha Menon", "DEVOPS");

        assertThat(browser().findElement(By.id("session-title")).getText()).isEqualTo(title);
        assertThat(browser().findElement(By.id("session-status")).getText()).isEqualTo("Draft");
        assertThat(browser().findElement(By.id("session-track")).getText()).isEqualTo("DevOps");
        assertThat(browser().findElement(By.id("flash-message")).getText()).contains("created as a draft");
    }

    @Test
    @DisplayName("the server rejects a too-short title and a missing email")
    void validationBlocksBadInput() {
        signInAs(SPEAKER_USER);
        open("/sessions/new");

        // Note: the email field is left empty rather than filled with rubbish.
        // Chrome blocks submission of an invalid <input type="email"> itself, so
        // a malformed address never reaches the server-side rules under test.
        type(By.id("title"), "Hi");
        type(By.id("speakerName"), "");
        type(By.id("speakerEmail"), "");
        selectByValue("track", "CLOUD");
        click(By.id("save-button"));

        assertThat(browser().getCurrentUrl()).contains("/sessions/new");
        assertThat(browser().findElement(By.id("form-errors")).getText()).contains("fix the highlighted fields");
        assertThat(browser().findElements(By.cssSelector(".field-error")))
                .extracting(e -> e.getText())
                .anyMatch(text -> text.contains("between 5 and 120"))
                .anyMatch(text -> text.contains("email is required"));
    }

    @Test
    @DisplayName("a speaker edits a session and the change is saved")
    void editSession() {
        signInAs(SPEAKER_USER);
        long id = createSession(uniqueTitle("Draft Talk"), "Rahul Nair", "WEB");
        String newTitle = uniqueTitle("Renamed Talk");

        open("/sessions/" + id + "/edit");
        type(By.id("title"), newTitle);
        type(By.id("room"), "Lab 2");
        click(By.id("save-button"));

        assertThat(browser().findElement(By.id("session-title")).getText()).isEqualTo(newTitle);
        assertThat(browser().findElement(By.id("session-room")).getText()).isEqualTo("Lab 2");
    }

    @Test
    @DisplayName("a reviewer is not offered the edit link")
    void reviewerCannotEdit() {
        signInAs(SPEAKER_USER);
        long id = createSession(uniqueTitle("Read Only Talk"), "Meera Iyer", "SECURITY");

        switchTo(REVIEWER_USER);
        open("/sessions/" + id);

        assertThat(isPresent(By.id("edit-link"))).isFalse();
    }

    @Test
    @DisplayName("a new session shows up on the dashboard totals")
    void dashboardCountsTheNewSession() {
        signInAs(SPEAKER_USER);
        open("/dashboard");
        int before = Integer.parseInt(browser().findElement(By.id("stat-total")).getText());

        createSession(uniqueTitle("Counted Talk"), "Dev Patel", "AI_ML");

        open("/dashboard");
        int after = Integer.parseInt(browser().findElement(By.id("stat-total")).getText());
        assertThat(after).isEqualTo(before + 1);
    }
}
