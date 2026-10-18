package com.college.speaker.config;

import com.college.speaker.domain.SessionStatus;
import com.college.speaker.domain.SpeakerSession;
import com.college.speaker.domain.Track;
import com.college.speaker.repo.SpeakerSessionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Puts a handful of rows in an empty database so the dashboard and search
 * screens are not blank on a fresh deployment. Disable with
 * app.seed-demo-data=false.
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements CommandLineRunner {

    private final SpeakerSessionRepository repository;

    public DemoDataSeeder(SpeakerSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        LocalDateTime base = LocalDateTime.now().plusDays(14).withHour(9).withMinute(0).withSecond(0).withNano(0);

        repository.save(session("Shipping Faster with Jenkins Pipelines", "Asha Menon", "asha@example.com",
                Track.DEVOPS, "Hall A", base, 45, SessionStatus.SCHEDULED));
        repository.save(session("Containers Without Tears", "Rahul Nair", "rahul@example.com",
                Track.DEVOPS, "Hall A", base.plusHours(1), 45, SessionStatus.APPROVED));
        repository.save(session("Serverless Cost Traps", "Meera Iyer", "meera@example.com",
                Track.CLOUD, "Hall B", base.plusHours(2), 30, SessionStatus.SUBMITTED));
        repository.save(session("Prompt Engineering for Testers", "Dev Patel", "dev@example.com",
                Track.AI_ML, "Hall B", base.plusDays(1), 60, SessionStatus.SUBMITTED));
        repository.save(session("Threat Modelling in One Hour", "Sara Khan", "sara@example.com",
                Track.SECURITY, null, base.plusDays(1).plusHours(2), 60, SessionStatus.DRAFT));
        repository.save(session("Accessible Front-ends by Default", "Nikhil Rao", "nikhil@example.com",
                Track.WEB, null, base.plusDays(2), 45, SessionStatus.REJECTED));
    }

    private SpeakerSession session(String title, String speaker, String email, Track track,
                                   String room, LocalDateTime start, int duration, SessionStatus status) {
        SpeakerSession s = new SpeakerSession();
        s.setTitle(title);
        s.setSpeakerName(speaker);
        s.setSpeakerEmail(email);
        s.setTrack(track);
        s.setRoom(room);
        s.setStartTime(start);
        s.setDurationMinutes(duration);
        s.setSummary("Seed data for the " + track.getLabel() + " track.");
        s.setStatus(status);
        s.setCreatedBy("seed");
        return s;
    }
}
