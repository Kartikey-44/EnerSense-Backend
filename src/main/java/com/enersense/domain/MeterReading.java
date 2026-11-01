package com.enersense.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One hourly observation of energy drawn through a meter. */
@Entity
@Table(name = "meter_reading")
public class MeterReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reading_id", nullable = false)
    private Long readingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meter_id", nullable = false)
    private Meter meter;

    @Column(name = "reading_timestamp", nullable = false)
    private LocalDateTime readingTimestamp;

    @Column(name = "energy_consumption_wh", nullable = false, precision = 12, scale = 3)
    private BigDecimal energyConsumptionWh;

    protected MeterReading() {
        // for JPA
    }

    public MeterReading(Meter meter, LocalDateTime readingTimestamp, BigDecimal energyConsumptionWh) {
        this.meter = meter;
        this.readingTimestamp = readingTimestamp;
        this.energyConsumptionWh = energyConsumptionWh;
    }

    public Long getReadingId() {
        return readingId;
    }

    public Meter getMeter() {
        return meter;
    }

    public LocalDateTime getReadingTimestamp() {
        return readingTimestamp;
    }

    public BigDecimal getEnergyConsumptionWh() {
        return energyConsumptionWh;
    }
}
