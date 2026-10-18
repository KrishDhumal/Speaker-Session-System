package com.college.speaker.repo;

import com.college.speaker.domain.SessionStatus;
import com.college.speaker.domain.SpeakerSession;
import com.college.speaker.domain.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SpeakerSessionRepository extends JpaRepository<SpeakerSession, Long> {

    /**
     * One query backs the whole search screen: every filter is optional, and a
     * null filter simply drops out of the WHERE clause.
     */
    @Query("""
            select s from SpeakerSession s
            where (:keyword is null
                   or lower(s.title) like lower(concat('%', :keyword, '%'))
                   or lower(s.speakerName) like lower(concat('%', :keyword, '%')))
              and (:status is null or s.status = :status)
              and (:track is null or s.track = :track)
            order by s.startTime asc
            """)
    List<SpeakerSession> search(@Param("keyword") String keyword,
                                @Param("status") SessionStatus status,
                                @Param("track") Track track);

    long countByStatus(SessionStatus status);

    long countByTrack(Track track);

    List<SpeakerSession> findByStatusAndStartTimeAfterOrderByStartTimeAsc(SessionStatus status, LocalDateTime after);
}
