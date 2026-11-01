package com.enersense.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.enersense.domain.Consumer;
import com.enersense.domain.Meter;
import com.enersense.domain.MeterReading;
import com.enersense.repository.ConsumerRepository;
import com.enersense.repository.MeterReadingRepository;
import com.enersense.repository.MeterRepository;
import com.enersense.service.AnomalyService;
import com.enersense.service.SimulationService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * End-to-end over the real HTTP stack against H2.
 *
 * <p>Fixture: 10 days of a repeating daily shape, with a 3000 Wh spike at 19:00 on the
 * final day. That makes both the forecast and the anomaly outcome predictable by hand,
 * so a failure points at the code rather than at the data.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiIntegrationTest {

    private static final int[] SHAPE =
            {100, 90, 85, 80, 80, 85, 150, 220, 300, 280, 200, 180,
             190, 185, 180, 190, 210, 260, 340, 420, 480, 430, 300, 180};
    private static final int DAYS = 10;
    private static final String C = "TEST01";
    private static final String M = "TESTMTR";

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;
    @Autowired ConsumerRepository consumers;
    @Autowired MeterRepository meters;
    @Autowired MeterReadingRepository readings;
    @Autowired SimulationService simulation;
    @Autowired AnomalyService anomaly;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @BeforeEach
    void seed() {
        simulation.reset();
        anomaly.reset();
        if (readings.countByMeterMeterId(M) > 0) {
            return;
        }
        Consumer consumer = consumers.save(new Consumer(C, "Test Consumer", LocalDateTime.now()));
        Meter meter = meters.save(new Meter(M, consumer, "RESIDENTIAL_SINGLE_PHASE"));

        List<MeterReading> batch = new ArrayList<>();
        LocalDateTime start = LocalDateTime.of(2017, 1, 1, 0, 0);
        for (int d = 0; d < DAYS; d++) {
            for (int h = 0; h < 24; h++) {
                int value = (d == DAYS - 1 && h == 19) ? 3000 : SHAPE[h];
                batch.add(new MeterReading(meter, start.plusDays(d).plusHours(h), BigDecimal.valueOf(value)));
            }
        }
        readings.saveAll(batch);
    }

    private ResponseEntity<String> get(String path) {
        return rest.getForEntity(url(path + "?consumerId=" + C + "&meterId=" + M), String.class);
    }

    @Test
    @DisplayName("health responds without any simulation")
    void health() {
        assertThat(rest.getForEntity(url("/api/health"), String.class).getBody()).contains("UP");
    }

    @Test
    @DisplayName("cursor-dependent endpoints refuse until the simulation starts")
    void requiresStart() {
        assertThat(get("/api/consumption/today").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(get("/api/forecast/tomorrow").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(get("/api/anomaly/current").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("start places the cursor on the earliest reading")
    void startAtBeginning() {
        String body = rest.postForEntity(
                url("/api/simulation/start?consumerId=" + C + "&meterId=" + M), null, String.class).getBody();
        assertThat(body).contains("\"currentDate\":\"2017-01-01\"");
        assertThat(body).contains("\"position\":0");
        assertThat(body).contains("\"endOfSeries\":false");
    }

    @Test
    @DisplayName("an unknown meter is a 404, and another consumer's meter is indistinguishable from one")
    void ownershipEnforced() {
        ResponseEntity<String> other = rest.getForEntity(
                url("/api/consumption/today?consumerId=SOMEONE&meterId=" + M), String.class);
        assertThat(other.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("invalid parameters are rejected before any work happens")
    void validation() {
        assertThat(rest.getForEntity(url("/api/consumption/today?consumerId=&meterId=" + M), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rest.getForEntity(url("/api/consumption/today?meterId=" + M), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("a reversed date range is rejected, and for that reason")
    void reversedRangeRejected() {
        start();
        ResponseEntity<String> response = rest.getForEntity(
                url("/api/analytics/daily?consumerId=" + C + "&meterId=" + M
                        + "&from=2017-01-05&to=2017-01-01"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        // Without this the test would also pass on a 400 raised for some unrelated reason —
        // which is exactly how a granularity binding bug once hid behind it.
        assertThat(response.getBody()).contains("'from' must be on or before 'to'");
    }

    @Test
    @DisplayName("granularity binds regardless of casing")
    void granularityIsCaseInsensitive() {
        start();
        advanceTo(24);
        for (String spelling : new String[] {"daily", "DAILY", "Daily"}) {
            ResponseEntity<String> response = rest.getForEntity(
                    url("/api/analytics/" + spelling + "?consumerId=" + C + "&meterId=" + M
                            + "&from=2017-01-01&to=2017-01-10"), String.class);
            assertThat(response.getStatusCode()).as(spelling).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).as(spelling).contains("\"periodStart\"");
        }
    }

    @Test
    @DisplayName("an unknown granularity is a 400, not a 500")
    void unknownGranularityRejected() {
        start();
        assertThat(rest.getForEntity(
                url("/api/analytics/fortnightly?consumerId=" + C + "&meterId=" + M
                        + "&from=2017-01-01&to=2017-01-10"), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("the forecast never sees beyond the cursor")
    void forecastIsOutOfSample() {
        start();
        // Advance to the end of day 3 so three complete days are released.
        advanceTo(4 * 24 - 1);

        String body = get("/api/forecast/tomorrow").getBody();
        assertThat(body).contains("\"forecastDate\":\"2017-01-05\"");
        // Persistence predicts the previous complete day, whose total is the shape sum.
        int dailySum = 0;
        for (int v : SHAPE) {
            dailySum += v;
        }
        assertThat(body).contains("\"predictedWh\":" + dailySum + ".0");
    }

    @Test
    @DisplayName("hourly forecast parts sum exactly to the daily prediction")
    void hourlyPartsSumToWhole() {
        start();
        advanceTo(4 * 24 - 1);
        String body = get("/api/forecast/tomorrow").getBody();
        assertThat(body).contains("\"hourly\":[");
        // 24 points, one per hour of the target day.
        assertThat(body.split("\"timestamp\"", -1).length - 1).isEqualTo(24);
    }

    @Test
    @DisplayName("the detector stays quiet while it lacks a baseline")
    void learningBeforeBaseline() {
        start();
        assertThat(get("/api/anomaly/current").getBody()).contains("\"status\":\"LEARNING\"");
    }

    @Test
    @DisplayName("a clear spike is flagged, and flagged only once")
    void spikeRaisesOneEpisode() {
        start();
        advanceTo(9 * 24 + 19);   // the 3000 Wh reading on the final day

        String body = get("/api/anomaly/current").getBody();
        assertThat(body).contains("\"status\":\"UNUSUAL\"");
        assertThat(body).contains("\"newEpisode\":true");

        // Asking again without moving must not re-raise the episode.
        assertThat(get("/api/anomaly/current").getBody()).contains("\"newEpisode\":false");
    }

    @Test
    @DisplayName("ordinary hours are not flagged")
    void normalHoursAreQuiet() {
        start();
        advanceTo(9 * 24 + 10);
        assertThat(get("/api/anomaly/current").getBody()).contains("\"status\":\"NORMAL\"");
    }

    @Test
    @DisplayName("analytics buckets respect the cursor")
    void analyticsRespectCursor() {
        start();
        advanceTo(2 * 24 - 1);      // two full days released
        String body = rest.getForEntity(
                url("/api/analytics/daily?consumerId=" + C + "&meterId=" + M
                        + "&from=2017-01-01&to=2017-01-10"), String.class).getBody();
        // Only the released days appear, not all ten.
        assertThat(body.split("\"periodStart\"", -1).length - 1).isEqualTo(2);
    }

    private void start() {
        rest.postForEntity(url("/api/simulation/start?consumerId=" + C + "&meterId=" + M), null, String.class);
    }

    private void advanceTo(int steps) {
        for (int i = 0; i < steps; i++) {
            rest.postForEntity(url("/api/simulation/advance?consumerId=" + C + "&meterId=" + M), null, String.class);
        }
    }
}
