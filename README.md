# Enersense

Enersense is a Spring Boot API for exploring household electricity consumption. It loads a bundled hourly smart-meter dataset into PostgreSQL and replays the readings through an in-memory simulation cursor. Consumption, analytics, forecasts, and anomaly checks use readings released up to that cursor.

## Requirements

- JDK 21 or newer
- Maven 3.6 or newer
- PostgreSQL

The project uses Spring Boot 3.4.1, Spring Data JPA, Flyway, and Maven.

## Run locally

Create an empty PostgreSQL database named `enersense`. Set the database connection environment variables, then start the application:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/enersense
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
mvn spring-boot:run
```

PowerShell:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/enersense"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "your_password"
mvn spring-boot:run
```

`DB_URL` is optional; it defaults to `jdbc:postgresql://localhost:5432/enersense`. Username and password must be supplied. Flyway creates the schema on startup. On an empty database, the application imports the bundled readings for consumer `APT35` and meter `MTR35`; it skips the import when that meter already has readings.

Check that the service is running:

```bash
curl http://localhost:8080/api/health
```

Start the replay before calling the consumption, analytics, forecast, or anomaly endpoints:

```bash
curl -X POST "http://localhost:8080/api/simulation/start?consumerId=APT35&meterId=MTR35"
curl "http://localhost:8080/api/consumption/today?consumerId=APT35&meterId=MTR35"
```

To run the PowerShell walkthrough, start the app and run `./demo.ps1`. It advances the cursor, then displays consumption, analytics, a forecast, and anomaly status. Use `./demo.ps1 -Advance 720` to advance 720 readings or `./demo.ps1 -FindAnomaly` to continue until an anomaly episode is detected.

## API

All endpoints except `/api/health` require `consumerId` and `meterId` query parameters. Date ranges are inclusive and use `YYYY-MM-DD` dates. The `from` date must not be after `to`.

| Method | Path | Description |
|---|---|---|
| GET | `/api/health` | Liveness status |
| POST | `/api/simulation/start` | Start or reset the replay at the meter's earliest reading |
| POST | `/api/simulation/advance` | Advance by one reading; stays at the end once the series is exhausted |
| GET | `/api/simulation/state` | Return the current replay position |
| GET | `/api/consumption/today` | Consumption summary for the cursor's date |
| GET | `/api/consumption/{date}` | Summary for a specific date |
| GET | `/api/consumption/history?from=...&to=...` | Daily summaries over a date range |
| GET | `/api/analytics/{granularity}?from=...&to=...` | Aggregated consumption; granularity is `hourly`, `daily`, `weekly`, `monthly`, or `yearly` |
| GET | `/api/forecast/tomorrow` | Predict the next day's total and return a 24-hour distribution |
| GET | `/api/anomaly/current` | Assess the reading at the current cursor position |

Example requests:

```bash
curl -X POST "http://localhost:8080/api/simulation/start?consumerId=APT35&meterId=MTR35"
curl -X POST "http://localhost:8080/api/simulation/advance?consumerId=APT35&meterId=MTR35"
curl "http://localhost:8080/api/consumption/history?consumerId=APT35&meterId=MTR35&from=2017-01-01&to=2017-01-07"
curl "http://localhost:8080/api/analytics/weekly?consumerId=APT35&meterId=MTR35&from=2017-01-01&to=2017-03-31"
curl "http://localhost:8080/api/forecast/tomorrow?consumerId=APT35&meterId=MTR35"
```

Errors use a JSON response body. An unknown meter or a meter belonging to another consumer returns `404`; endpoints that need a replay return `409` if it has not been started; insufficient readings return `422`; invalid or missing parameters return `400`.

## Configuration

Configuration is in `src/main/resources/application.properties`. Database credentials are read from the environment. Forecast and anomaly settings can also be overridden with Spring Boot properties or environment variables.

| Setting | Default | Purpose |
|---|---|---|
| `enersense.forecast.strategy` | `EWMA` | Daily-total model: `PERSISTENCE`, `MOVING_AVERAGE`, or `EWMA` |
| `enersense.forecast.window-days` | `7` | Lookback for moving average and hourly profile |
| `enersense.forecast.ewma-alpha` | `0.5` | EWMA smoothing factor |
| `enersense.anomaly.min-baseline-days` | `3` | Minimum prior same-hour readings before assessment |
| `enersense.anomaly.max-baseline-days` | `14` | Maximum prior same-hour readings in the baseline |
| `enersense.anomaly.z-multiplier` | `2.5` | Standard-deviation threshold above the baseline mean |
| `enersense.anomaly.min-delta-wh` | `100` | Minimum absolute increase required to flag a reading |

The anomaly detector compares a reading with previous readings at the same hour. It requires both the configured standard-deviation threshold and the minimum absolute increase. Repeated alerts within one continuous episode are suppressed.

The forecast predicts a daily total using the configured model, then distributes that total over 24 hours using historical hourly shares from the configured window. It uses an even split if the available history does not contain a complete hourly profile. The cursor's current date is treated as incomplete; the forecast uses completed days before it and predicts the following date.

## Data and behavior

The bundled CSV is an hourly extract for meter `MTR35` / consumer `APT35`, spanning 2016-12-20 through 2017-12-27. The loader assigns rows to the configured consumer and meter IDs. The application screens readings for data-quality issues before forecasting and anomaly analysis.

The simulation cursor and anomaly episode state are held in memory. They reset when the process restarts, and each running application instance has independent state. This project is intended as a local demonstration; it does not provide authentication or HTTPS. Do not expose it to an untrusted network.

## Tests

Run the test suite with:

```bash
mvn test
```

Tests use an in-memory H2 database in PostgreSQL compatibility mode and do not require PostgreSQL.

## Project layout

```text
src/main/java/com/enersense/
  config/       Application configuration and CSV loader
  domain/       Entities and value types
  repository/   Spring Data repositories
  service/      Consumption, analytics, simulation, forecasting, and anomaly logic
  web/          REST controllers, validation, and error responses
src/main/resources/
  db/migration/ Flyway schema migration
  data/         Bundled smart-meter readings
src/test/       Unit and API integration tests
```
