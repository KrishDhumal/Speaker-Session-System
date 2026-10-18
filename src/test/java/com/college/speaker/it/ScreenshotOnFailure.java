package com.college.speaker.it;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Whenever a Selenium test fails, drop a PNG of what the browser was showing —
 * plus the page HTML — into target/selenium-screenshots, so the Jenkins build
 * can archive them.
 *
 * <p>This runs as an {@code afterTestExecution} callback rather than a
 * {@code TestWatcher}: a watcher fires after {@code @AfterEach}, by which point
 * the browser has already been closed and there is nothing left to photograph.
 */
public class ScreenshotOnFailure implements AfterTestExecutionCallback {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (context.getExecutionException().isEmpty()) {
            return;
        }
        WebDriver driver = SeleniumTestBase.currentDriver();
        if (driver == null) {
            return;
        }
        String dir = System.getProperty("it.screenshot.dir", "target/selenium-screenshots");
        String name = context.getRequiredTestClass().getSimpleName()
                + "." + context.getRequiredTestMethod().getName()
                + "-" + LocalDateTime.now().format(STAMP);
        try {
            Path target = Paths.get(dir);
            Files.createDirectories(target);
            Path shot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath();
            Files.move(shot, target.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
            // The HTML explains failures a picture cannot, e.g. a redirect to an
            // error page or a missing CSRF field.
            Files.writeString(target.resolve(name + ".html"),
                    "<!-- url: " + driver.getCurrentUrl() + " -->\n" + driver.getPageSource());
            Object values = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "return [...document.querySelectorAll('input,select,textarea')]"
                            + ".map(e => e.name + '=' + e.value).join(' | ')");
            System.out.println("[selenium] field values: " + values);
            System.out.println("[selenium] failure at " + driver.getCurrentUrl()
                    + " -> " + target.resolve(name + ".png").toAbsolutePath());
        } catch (IOException | RuntimeException e) {
            System.out.println("[selenium] could not capture screenshot: " + e.getMessage());
        }
    }
}
