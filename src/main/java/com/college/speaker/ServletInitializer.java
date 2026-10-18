package com.college.speaker;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Lets the same WAR be dropped into a standalone Tomcat (webapps/) instead of
 * being run with `java -jar`.
 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SpeakerSessionApplication.class);
    }
}
