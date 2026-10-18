package com.college.speaker.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Journey 3 — finding a session again through the search bar.
 */
class SearchIT extends SeleniumTestBase {

    @Test
    @DisplayName("searching by keyword narrows the list to one session")
    void searchByKeyword() {
        signInAs(SPEAKER_USER);
        String title = uniqueTitle("Chaos Engineering Basics");
        createSession(title, "Sara Khan", "DEVOPS");

        open("/sessions");
        type(By.id("search-keyword"), title);
        click(By.id("search-button"));

        assertThat(browser().findElements(By.cssSelector("#sessions-table tbody tr"))).hasSize(1);
        assertThat(browser().findElement(By.id("sessions-table")).getText()).contains(title);
    }

    @Test
    @DisplayName("searching by speaker name works too")
    void searchBySpeakerName() {
        signInAs(SPEAKER_USER);
        String speaker = "Nikhil" + System.nanoTime();
        createSession(uniqueTitle("Accessible Front-ends"), speaker, "WEB");

        open("/sessions");
        type(By.id("search-keyword"), speaker);
        click(By.id("search-button"));

        assertThat(browser().findElements(By.cssSelector("#sessions-table tbody tr"))).hasSize(1);
    }

    @Test
    @DisplayName("keyword and track filters combine")
    void searchCombinesFilters() {
        signInAs(SPEAKER_USER);
        String title = uniqueTitle("Zero Trust Networking");
        createSession(title, "Sara Khan", "SECURITY");

        open("/sessions");
        type(By.id("search-keyword"), title);
        selectByValue("search-track", "SECURITY");
        click(By.id("search-button"));
        assertThat(browser().findElements(By.cssSelector("#sessions-table tbody tr"))).hasSize(1);

        selectByValue("search-track", "CLOUD");
        click(By.id("search-button"));
        assertThat(isPresent(By.id("no-results"))).isTrue();
    }

    @Test
    @DisplayName("a keyword that matches nothing shows the empty-state message")
    void searchWithNoMatches() {
        signInAs(REVIEWER_USER);
        open("/sessions");

        type(By.id("search-keyword"), "no-such-session-" + System.nanoTime());
        click(By.id("search-button"));

        assertThat(browser().findElement(By.id("no-results")).getText()).contains("No sessions match");
        assertThat(browser().findElement(By.id("result-count")).getText()).startsWith("0");
    }

    @Test
    @DisplayName("reset clears the filters and shows everything again")
    void resetClearsFilters() {
        signInAs(SPEAKER_USER);
        createSession(uniqueTitle("Resettable Talk"), "Asha Menon", "CLOUD");

        open("/sessions");
        type(By.id("search-keyword"), "no-such-session-" + System.nanoTime());
        click(By.id("search-button"));
        assertThat(isPresent(By.id("no-results"))).isTrue();

        click(By.id("search-reset"));
        assertThat(isPresent(By.id("sessions-table"))).isTrue();
    }
}
