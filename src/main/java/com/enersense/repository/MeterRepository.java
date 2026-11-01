package com.enersense.repository;

import com.enersense.domain.Meter;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeterRepository extends JpaRepository<Meter, String> {

    /**
     * Resolves a meter only when it belongs to the given consumer.
     *
     * <p>Ownership is enforced in the query rather than by loading the meter and comparing
     * afterwards, so there is no path that returns another consumer's meter.
     */
    @Query("""
            SELECT m FROM Meter m
            WHERE m.meterId = :meterId
              AND m.consumer.consumerId = :consumerId
            """)
    Optional<Meter> findOwned(@Param("consumerId") String consumerId, @Param("meterId") String meterId);
}
