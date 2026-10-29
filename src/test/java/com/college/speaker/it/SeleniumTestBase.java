package com.college.speaker.it;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;

/**
 * Shared Selenium plumbing.
 *
 * <p>By default the test starts the application in-process on a random port, so
 * `mvn verify` needs nothing running. Pass -Dit.base.url=http://host:port to
 * point the same tests at a deployed instance instead — that is how the Jenkins
 * pipeline tests the running container.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:selenium;DB_CLOSE_DELAY=-1",
        "app.seed-demo-data=false"
})
@ExtendWith(ScreenshotOnFailure.class)
public abstract class SeleniumTestBase {

    private static final int TYPE_ATTEMPTS = 3;
    private static final int CLICK_ATTEMPTS = 3;

    protected static final String SPEAKER_USER = "speaker";
    protected static final String REVIEWER_USER = "reviewer";
    protected static final String ORGANIZER_USER = "organizer";

    private static WebDriver driver;
    protected static WebDriverWait wait;

    @LocalServerPort
    private int localPort;

    @BeforeEach
    void startBrowser() {
        ChromeOptions options = new ChromeOptions();
        // Pin the browser instead of using whatever Chrome is installed locally.
        // Selenium Manager downloads this exact "Chrome for Testing" build and a
        // matching driver once and caches them, so the suite behaves the same on
        // a laptop and on the Jenkins agent. Override with -Dit.chrome.version.
        options.setBrowserVersion(System.getProperty("it.chrome.version", "130"));
        if (Boolean.parseBoolean(System.getProperty("it.headless", "true"))) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1400,1000", "--no-sandbox", "--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
        // Deliberately no implicit wait: Selenium warns that mixing implicit and
        // explicit waits makes timeouts behave unpredictably. Everything below
        // waits explicitly instead.
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    /**
     * A brand-new browser per test. Slower than sharing one, but it means no
     * test can leak a signed-in session or a cached page into the next one —
     * which is exactly the kind of failure that is miserable to debug later.
     */
    @AfterEach
    void stopBrowser() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    /** Used by {@link ScreenshotOnFailure}; null once the browser has closed. */
    static WebDriver currentDriver() {
        return driver;
    }

    protected WebDriver browser() {
        return driver;
    }

    protected String baseUrl() {
        String external = System.getProperty("it.base.url");
        if (external != null && !external.isBlank()) {
            return external.replaceAll("/$", "");
        }
        return "http://localhost:" + localPort;
    }

    protected void open(String path) {
        driver.get(baseUrl() + path);
        waitForPageReady();
    }

    /**
     * Blocks until the browser has finished loading. Without this a click that
     * starts a navigation can still be in flight when the next step runs, and
     * the next step then acts on the half-replaced page.
     */
    protected void waitForPageReady() {
        wait.until(d -> "complete".equals(script("return document.readyState")));
    }

    // ---- Common actions ----------------------------------------------------

    protected void signIn(String username, String password) {
        // The timestamp makes the URL unique, so the browser always fetches a
        // fresh page and therefore a CSRF token that matches the current session.
        open("/login?t=" + System.nanoTime());
        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        click(By.id("login-button"));
    }

    /** Signs in with the development password for one of the demo accounts. */
    protected void signInAs(String username) {
        signIn(username, switch (username) {
            case SPEAKER_USER -> "speaker123";
            case REVIEWER_USER -> "reviewer123";
            case ORGANIZER_USER -> "organizer123";
            default -> throw new IllegalArgumentException("Unknown demo user: " + username);
        });
        wait.until(ExpectedConditions.urlContains("/dashboard"));
    }

    protected void signOut() {
        open("/dashboard");
        click(By.id("logout-button"));
        wait.until(ExpectedConditions.urlContains("/login"));
    }

    protected void switchTo(String username) {
        signOut();
        signInAs(username);
    }

    /**
     * Fills the new-session form and saves. Returns the id of the session just
     * created, read back from the detail page URL.
     *
     * <p>The room is unique per call (like {@link #uniqueTitle}) rather than a
     * fixed "Hall A": the service now refuses a room already scheduled for an
     * overlapping time, so two tests sharing one room and the same default
     * time slot would otherwise clash with each other.
     */
    protected long createSession(String title, String speakerName, String track) {
        open("/sessions/new");
        type(By.id("title"), title);
        type(By.id("speakerName"), speakerName);
        type(By.id("speakerEmail"), "speaker@example.com");
        selectByValue("track", track);
        type(By.id("room"), "Hall " + System.nanoTime());
        click(By.id("save-button"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("session-title")));
        return currentSessionId();
    }

    /** Same as {@link #createSession}, but with a caller-chosen room. */
    protected long createSessionInRoom(String title, String speakerName, String track, String room) {
        open("/sessions/new");
        type(By.id("title"), title);
        type(By.id("speakerName"), speakerName);
        type(By.id("speakerEmail"), "speaker@example.com");
        selectByValue("track", track);
        type(By.id("room"), room);
        click(By.id("save-button"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("session-title")));
        return currentSessionId();
    }

    protected long currentSessionId() {
        String url = driver.getCurrentUrl();
        return Long.parseLong(url.replaceAll(".*/sessions/(\\d+).*", "$1"));
    }

    /**
     * Clicks something that loads a new page and waits for that page to arrive.
     * Without this, an assertion can read the old DOM and see stale results.
     */
    protected void click(By locator) {
        for (int attempt = 1; attempt <= CLICK_ATTEMPTS; attempt++) {
            WebElement target = wait.until(ExpectedConditions.elementToBeClickable(locator));
            // Tag the current document, then wait for a document without the tag.
            // A fresh page always loses it, including when the server re-renders
            // the same URL (a form returning with validation errors, say) — which
            // a plain URL comparison would miss.
            script("document.documentElement.dataset.pageMarker = 'current'");
            try {
                if (attempt == 1) {
                    target.click();
                } else {
                    // ChromeDriver sometimes accepts a native click and never
                    // delivers it. Dispatching the click from the page itself
                    // still runs the real form submission and the real server
                    // round trip; only the browser-level hit test is skipped,
                    // and elementToBeClickable above already covered that.
                    script("arguments[0].click()", target);
                }
            } catch (StaleElementReferenceException alreadyNavigated) {
                waitForPageReady();
                return;
            }
            if (newDocumentLoaded()) {
                waitForPageReady();
                return;
            }
            // ChromeDriver occasionally accepts a click without delivering it.
            // Retrying is safe: had the click landed, the element above would be
            // stale on the next pass and we would have returned already.
        }
        throw new IllegalStateException("Clicking " + locator + " never loaded a new page");
    }

    private boolean newDocumentLoaded() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(d -> script("return document.documentElement.dataset.pageMarker") == null);
            return true;
        } catch (TimeoutException stillTheSamePage) {
            return false;
        }
    }

    private Object script(String js, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(js, args);
    }

    /**
     * Types into a field and checks the text actually arrived. ChromeDriver can
     * silently drop sendKeys — usually right after clear(), which leaves the
     * field unfocused — so this retries and then fails loudly rather than
     * letting a test submit a half-filled form.
     */
    protected void type(By locator, String value) {
        for (int attempt = 1; attempt <= TYPE_ATTEMPTS; attempt++) {
            WebElement field = wait.until(ExpectedConditions.elementToBeClickable(locator));
            field.clear();
            field.click();
            if (!value.isEmpty()) {
                field.sendKeys(value);
            }
            if (value.equals(field.getDomProperty("value"))) {
                return;
            }
        }
        throw new IllegalStateException("Could not type \"" + value + "\" into " + locator);
    }

    protected void selectByValue(String elementId, String value) {
        new org.openqa.selenium.support.ui.Select(driver.findElement(By.id(elementId))).selectByValue(value);
    }

    protected boolean isPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    /** A title no other test will collide with, so tests can run in any order. */
    protected String uniqueTitle(String prefix) {
        return prefix + " " + System.nanoTime();
    }
}
