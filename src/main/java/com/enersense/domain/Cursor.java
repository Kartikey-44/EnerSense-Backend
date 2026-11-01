package com.enersense.domain;

import java.time.LocalDateTime;

/**
 * A position in one meter's reading sequence.
 *
 * <p>Readings are ordered by timestamp, then by reading id to break ties. Everything the
 * API exposes is derived only from readings at or before the cursor — that is what keeps a
 * forecast genuinely out-of-sample.
 *
 * <p>Deliberately a value type holding the id rather than the entity: a cursor outlives the
 * transaction that produced it, and a detached entity with lazy associations would blow up
 * the moment anything touched them.
 */
public record Cursor(long readingId, LocalDateTime timestamp) {

    public static Cursor of(MeterReading reading) {
        return new Cursor(reading.getReadingId(), reading.getReadingTimestamp());
    }
}
