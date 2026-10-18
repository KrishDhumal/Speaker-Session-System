package com.college.speaker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Three demo accounts, one per role. Passwords come from application.properties
 * so a real deployment can override them with environment variables.
 */
@Configuration
public class SecurityConfig {

    @Value("${app.users.speaker.password}")
    private String speakerPassword;

    @Value("${app.users.reviewer.password}")
    private String reviewerPassword;

    @Value("${app.users.organizer.password}")
    private String organizerPassword;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails speaker = User.withUsername("speaker")
                .password(encoder.encode(speakerPassword))
                .roles("SPEAKER")
                .build();

        UserDetails reviewer = User.withUsername("reviewer")
                .password(encoder.encode(reviewerPassword))
                .roles("REVIEWER")
                .build();

        UserDetails organizer = User.withUsername("organizer")
                .password(encoder.encode(organizerPassword))
                .roles("ORGANIZER", "REVIEWER")
                .build();

        return new InMemoryUserDetailsManager(speaker, reviewer, organizer);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/actuator/health", "/error").permitAll()
                        .requestMatchers("/sessions/new", "/sessions/*/edit").hasRole("SPEAKER")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?loggedOut")
                        .permitAll());

        return http.build();
    }
}
