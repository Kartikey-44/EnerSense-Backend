package com.enersense.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** A household. Identified by a short code such as APT35. */
@Entity
@Table(name = "consumer")
public class Consumer {

    @Id
    @Column(name = "consumer_id", nullable = false, length = 20)
    private String consumerId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Consumer() {
        // for JPA
    }

    public Consumer(String consumerId, String name, LocalDateTime createdAt) {
        this.consumerId = consumerId;
        this.name = name;
        this.createdAt = createdAt;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
