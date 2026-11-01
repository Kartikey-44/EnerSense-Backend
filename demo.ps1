# Drives the Enersense simulation and prints what each endpoint returns.
#
#   .\demo.ps1                 # advance one week, then show everything
#   .\demo.ps1 -Advance 720    # advance 30 days
#   .\demo.ps1 -FindAnomaly    # advance until the detector fires

param(
    [int]$Advance = 168,
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ConsumerId = "APT35",
    [string]$MeterId = "MTR35",
    [switch]$FindAnomaly
)

$ErrorActionPreference = "Stop"
$q = "consumerId=$ConsumerId&meterId=$MeterId"

function Api($path, $method = "Get") {
    Invoke-RestMethod -Method $method -Uri "$BaseUrl/api/$path"
}
function Head($text) { Write-Host "`n=== $text ===" -ForegroundColor Cyan }

Head "Health"
Api "health" | Format-List

Head "Starting simulation"
$state = Api "simulation/start?$q" "Post"
Write-Host "cursor at $($state.currentTimestamp)"

Head "Advancing $Advance readings ($([math]::Round($Advance / 24, 1)) days)"
$sw = [Diagnostics.Stopwatch]::StartNew()
for ($i = 1; $i -le $Advance; $i++) {
    $state = Api "simulation/advance?$q" "Post"
    if ($i % 24 -eq 0) { Write-Host "  day $($i / 24): $($state.currentDate)" }
    if ($state.endOfSeries) { Write-Host "  reached end of series"; break }
}
$sw.Stop()
Write-Host "  done in $([math]::Round($sw.Elapsed.TotalSeconds, 1))s, now at $($state.currentTimestamp)"

if ($FindAnomaly) {
    Head "Advancing until the detector fires"
    for ($i = 1; $i -le 2000; $i++) {
        $state = Api "simulation/advance?$q" "Post"
        $a = Api "anomaly/current?$q"
        if ($a.status -eq "UNUSUAL" -and $a.newEpisode) {
            Write-Host "  found after $i more readings, at $($a.at)" -ForegroundColor Yellow
            break
        }
        if ($state.endOfSeries) { Write-Host "  end of series, none found"; break }
    }
}

Head "Today"
Api "consumption/today?$q" | Format-List

Head "Last 7 days"
$to = ([datetime]$state.currentDate).ToString("yyyy-MM-dd")
$from = ([datetime]$state.currentDate).AddDays(-6).ToString("yyyy-MM-dd")
(Api "consumption/history?$q&from=$from&to=$to").days | Format-Table date, totalWh, readingCount, complete

Head "Daily analytics, same window"
(Api "analytics/daily?$q&from=$from&to=$to").buckets | Format-Table periodStart, totalWh, readingCount

Head "Forecast for tomorrow"
$f = Api "forecast/tomorrow?$q"
Write-Host "date      : $($f.forecastDate)"
Write-Host "predicted : $($f.predictedWh) Wh"
Write-Host "model     : $($f.model)"
Write-Host "hourly points: $($f.hourly.Count)"
$sum = ($f.hourly | Measure-Object -Property predictedWh -Sum).Sum
Write-Host "hourly sum: $sum Wh  (should equal the daily prediction)"
$f.hourly | Select-Object -First 5 | Format-Table timestamp, predictedWh

Head "Anomaly status now"
Api "anomaly/current?$q" | Format-List status, at, currentWh, baselineMeanWh, differenceWh, percentAboveBaseline, newEpisode, message
