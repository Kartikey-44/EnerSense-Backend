package com.enersense.config;

import com.enersense.domain.Consumer;
import com.enersense.domain.Meter;
import com.enersense.repository.ConsumerRepository;
import com.enersense.repository.MeterReadingRepository;
import com.enersense.repository.MeterRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Imports the bundled consumption series the first time the application runs against an
 * empty database.
 *
 * <p>This exists so the project needs no manual SQL step: create a database, start the
 * app, and it populates itself. If readings are already present the loader does nothing,
 * so restarting is free.
 */
@Component
public class DataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int BATCH_SIZE = 1_000;

    private final DataLoadProperties properties;
    private final ConsumerRepository consumers;
    private final MeterRepository meters;
    private final MeterReadingRepository readings;
    private final ResourceLoader resourceLoader;
    private final DataSource dataSource;

    public DataLoader(DataLoadProperties properties,
                      ConsumerRepository consumers,
                      MeterRepository meters,
                      MeterReadingRepository readings,
                      ResourceLoader resourceLoader,
                      DataSource dataSource) {
        this.properties = properties;
        this.consumers = consumers;
        this.meters = meters;
        this.readings = readings;
        this.resourceLoader = resourceLoader;
        this.dataSource = dataSource;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        long existing = readings.countByMeterMeterId(properties.meterId());
        if (existing > 0) {
            log.info("Meter {} already holds {} readings; skipping import.", properties.meterId(), existing);
            return;
        }

        Consumer consumer = consumers.findById(properties.consumerId())
                .orElseGet(() -> consumers.save(
                        new Consumer(properties.consumerId(), properties.consumerName(), LocalDateTime.now())));
        meters.findById(properties.meterId())
                .orElseGet(() -> meters.save(
                        new Meter(properties.meterId(), consumer, "RESIDENTIAL_SINGLE_PHASE")));
        // Push the consumer and meter rows to the database now. They are still uncommitted,
        // so the reading inserts below must run on this same transaction's connection to
        // satisfy the foreign key — see importReadings().
        meters.flush();

        int imported = importReadings();
        log.info("Imported {} readings for meter {}.", imported, properties.meterId());
    }

    private int importReadings() throws IOException, java.sql.SQLException {
        String sql = """
                INSERT INTO meter_reading (meter_id, reading_timestamp, energy_consumption_wh)
                VALUES (?, ?, ?)
                """;
        int total = 0;
        // DataSourceUtils returns the connection already bound to the active transaction.
        // dataSource.getConnection() would hand back a different pooled connection, which
        // cannot see the uncommitted meter row and would fail the foreign key on every row.
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (var in = resourceLoader.getResource(properties.csv()).getInputStream();
             var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
             PreparedStatement statement = connection.prepareStatement(sql)) {

            String header = reader.readLine();
            if (header == null) {
                throw new IllegalStateException("Reading CSV " + properties.csv() + " is empty.");
            }

            String line;
            int pending = 0;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                // consumer_id,meter_id,timestamp,energy_consumption_wh
                String[] parts = line.split(",", -1);
                if (parts.length < 4) {
                    throw new IllegalStateException("Malformed row in reading CSV: " + line);
                }
                statement.setString(1, properties.meterId());
                statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.parse(parts[2].trim(), TIMESTAMP)));
                statement.setBigDecimal(3, new BigDecimal(parts[3].trim()));
                statement.addBatch();

                if (++pending == BATCH_SIZE) {
                    total += statement.executeBatch().length;
                    pending = 0;
                }
            }
            if (pending > 0) {
                total += statement.executeBatch().length;
            }
        } finally {
            // Hands it back to the transaction rather than closing it outright.
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
        return total;
    }
}
