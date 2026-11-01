package com.enersense.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A meter belonging to exactly one consumer. */
@Entity
@Table(name = "meter")
public class Meter {

    @Id
    @Column(name = "meter_id", nullable = false, length = 20)
    private String meterId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consumer_id", nullable = false)
    private Consumer consumer;

    @Column(name = "meter_type", length = 50)
    private String meterType;

    protected Meter() {
        // for JPA
    }

    public Meter(String meterId, Consumer consumer, String meterType) {
        this.meterId = meterId;
        this.consumer = consumer;
        this.meterType = meterType;
    }

    public String getMeterId() {
        return meterId;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public String getMeterType() {
        return meterType;
    }
}
