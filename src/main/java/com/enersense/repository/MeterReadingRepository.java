package com.enersense.repository;

import com.enersense.domain.MeterReading;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Reading access.
 *
 * <p>Every method that feeds an API response is "released": it returns only readings at or
 * before the cursor, expressed as {@code (timestamp, readingId) <= (cursorTs, cursorId)}.
 * The composite comparison matters — two readings can share a timestamp, and ordering by
 * timestamp alone would make the cursor ambiguous.
 */
public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {

    @Query("""
            SELECT r FROM MeterReading r
            WHERE r.meter.meterId = :meterId
            ORDER BY r.readingTimestamp ASC, r.readingId ASC
            """)
    List<MeterReading> findEarliest(@Param("meterId") String meterId, Limit limit);

    @Query("""
            SELECT r FROM MeterReading r
            WHERE r.meter.meterId = :meterId
              AND (r.readingTimestamp > :afterTs
                   OR (r.readingTimestamp = :afterTs AND r.readingId > :afterId))
            ORDER BY r.readingTimestamp ASC, r.readingId ASC
            """)
    List<MeterReading> findAfter(@Param("meterId") String meterId,
                                 @Param("afterTs") LocalDateTime afterTs,
                                 @Param("afterId") long afterId,
                                 Limit limit);

    @Query("""
            SELECT COUNT(r) > 0 FROM MeterReading r
            WHERE r.meter.meterId = :meterId
              AND (r.readingTimestamp > :afterTs
                   OR (r.readingTimestamp = :afterTs AND r.readingId > :afterId))
            """)
    boolean existsAfter(@Param("meterId") String meterId,
                        @Param("afterTs") LocalDateTime afterTs,
                        @Param("afterId") long afterId);

    @Query("""
            SELECT r FROM MeterReading r
            WHERE r.meter.meterId = :meterId
              AND r.readingId = :readingId
            """)
    Optional<MeterReading> findOwnReading(@Param("meterId") String meterId,
                                          @Param("readingId") long readingId);

    /** Released readings within a half-open window, oldest first. */
    @Query("""
            SELECT r FROM MeterReading r
            WHERE r.meter.meterId = :meterId
              AND r.readingTimestamp >= :fromInclusive
              AND r.readingTimestamp <  :toExclusive
              AND (r.readingTimestamp < :cursorTs
                   OR (r.readingTimestamp = :cursorTs AND r.readingId <= :cursorId))
            ORDER BY r.readingTimestamp ASC, r.readingId ASC
            """)
    List<MeterReading> findReleasedBetween(@Param("meterId") String meterId,
                                           @Param("fromInclusive") LocalDateTime fromInclusive,
                                           @Param("toExclusive") LocalDateTime toExclusive,
                                           @Param("cursorTs") LocalDateTime cursorTs,
                                           @Param("cursorId") long cursorId);

    /** Every released reading up to and including the cursor, oldest first. */
    @Query("""
            SELECT r FROM MeterReading r
            WHERE r.meter.meterId = :meterId
              AND (r.readingTimestamp < :cursorTs
                   OR (r.readingTimestamp = :cursorTs AND r.readingId <= :cursorId))
            ORDER BY r.readingTimestamp ASC, r.readingId ASC
            """)
    List<MeterReading> findAllReleased(@Param("meterId") String meterId,
                                       @Param("cursorTs") LocalDateTime cursorTs,
                                       @Param("cursorId") long cursorId);

    /**
     * Released readings at one hour of the day, newest first.
     *
     * <p>The anomaly baseline compares like with like: 7pm is judged against previous 7pms,
     * never against a daily average that would call every evening unusual.
     */
    @Query("""
            SELECT r FROM MeterReading r
            WHERE r.meter.meterId = :meterId
              AND HOUR(r.readingTimestamp) = :hourOfDay
              AND (r.readingTimestamp < :cursorTs
                   OR (r.readingTimestamp = :cursorTs AND r.readingId < :cursorId))
            ORDER BY r.readingTimestamp DESC, r.readingId DESC
            """)
    List<MeterReading> findReleasedAtHour(@Param("meterId") String meterId,
                                          @Param("hourOfDay") int hourOfDay,
                                          @Param("cursorTs") LocalDateTime cursorTs,
                                          @Param("cursorId") long cursorId,
                                          Limit limit);

    long countByMeterMeterId(String meterId);
}
