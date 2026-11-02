import './App.css'

function App() {
  return (
      <div className="app">

        {/* Sidebar */}
        <aside className="sidebar">
          <div className="logo">
            <div className="logo-icon">E</div>
            <div>
              <h2>Enersense</h2>
              <span>Energy Intelligence</span>
            </div>
          </div>

          <nav className="nav">
            <button className="nav-item active">
              <span>▦</span>
              Dashboard
            </button>

            <button className="nav-item">
              <span>◔</span>
              Consumption
            </button>

            <button className="nav-item">
              <span>▥</span>
              Analytics
            </button>

            <button className="nav-item">
              <span>◈</span>
              Forecast
            </button>

            <button className="nav-item">
              <span>⚠</span>
              Anomalies
            </button>
          </nav>

          <div className="sidebar-bottom">
            <div className="meter-status">
              <div className="status-dot"></div>
              <div>
                <strong>MTR35</strong>
                <span>Meter connected</span>
              </div>
            </div>
          </div>
        </aside>

        {/* Main content */}
        <main className="main">

          {/* Header */}
          <header className="header">
            <div>
              <p className="breadcrumb">Dashboard / Overview</p>
              <h1>Energy Dashboard</h1>
            </div>

            <div className="header-right">
              <div className="connection">
                <span className="status-dot"></span>
                Backend connected
              </div>

              <div className="profile">
                <div className="profile-avatar">A</div>
                <div>
                  <strong>APT35</strong>
                  <span>Demo Consumer</span>
                </div>
              </div>
            </div>
          </header>

          {/* Summary cards */}
          <section className="cards">

            <div className="card">
              <div className="card-top">
                <span>Today's Consumption</span>
                <div className="card-icon">⚡</div>
              </div>

              <div className="card-value">
                24.6 <span>kWh</span>
              </div>

              <p className="card-description">
                Energy consumed today
              </p>
            </div>

            <div className="card">
              <div className="card-top">
                <span>Tomorrow's Forecast</span>
                <div className="card-icon">◈</div>
              </div>

              <div className="card-value">
                27.1 <span>kWh</span>
              </div>

              <p className="card-description">
                Predicted consumption
              </p>
            </div>

            <div className="card">
              <div className="card-top">
                <span>Current Status</span>
                <div className="card-icon">✓</div>
              </div>

              <div className="card-value status-normal">
                Normal
              </div>

              <p className="card-description">
                No active anomalies
              </p>
            </div>

            <div className="card">
              <div className="card-top">
                <span>Meter</span>
                <div className="card-icon">⌁</div>
              </div>

              <div className="card-value">
                MTR35
              </div>

              <p className="card-description">
                Demo Apartment 35
              </p>
            </div>

          </section>

          {/* Main grid */}
          <section className="dashboard-grid">

            {/* Consumption chart placeholder */}
            <div className="panel chart-panel">
              <div className="panel-header">
                <div>
                  <h2>Energy Consumption</h2>
                  <p>Hourly consumption pattern</p>
                </div>

                <select defaultValue="today">
                  <option value="today">Today</option>
                  <option value="week">Last 7 days</option>
                  <option value="month">Last 30 days</option>
                </select>
              </div>

              <div className="chart-placeholder">
                <div className="chart-line">
                  <span></span>
                  <span></span>
                  <span></span>
                  <span></span>
                  <span></span>
                  <span></span>
                  <span></span>
                  <span></span>
                </div>

                <div className="chart-labels">
                  <span>00:00</span>
                  <span>04:00</span>
                  <span>08:00</span>
                  <span>12:00</span>
                  <span>16:00</span>
                  <span>20:00</span>
                  <span>24:00</span>
                </div>
              </div>
            </div>

            {/* Anomaly panel */}
            <div className="panel anomaly-panel">
              <div className="panel-header">
                <div>
                  <h2>Anomaly Detection</h2>
                  <p>Latest system analysis</p>
                </div>
              </div>

              <div className="anomaly-status">
                <div className="large-status-icon">✓</div>

                <div>
                  <strong>No anomaly detected</strong>
                  <p>Current consumption is within the expected range.</p>
                </div>
              </div>

              <div className="anomaly-details">
                <div>
                  <span>Baseline</span>
                  <strong>Normal</strong>
                </div>

                <div>
                  <span>Detection method</span>
                  <strong>Z-Score</strong>
                </div>
              </div>
            </div>

          </section>

          {/* Simulation */}
          <section className="panel simulation-panel">

            <div className="panel-header">
              <div>
                <h2>Simulation Control</h2>
                <p>Replay smart-meter readings through the system</p>
              </div>

              <div className="simulation-date">
                Simulation active
              </div>
            </div>

            <div className="simulation-controls">
              <button className="primary-button">
                ▶ Start Simulation
              </button>

              <button className="secondary-button">
                + Advance Reading
              </button>

              <div className="simulation-info">
                <span>Current reading</span>
                <strong>Waiting for backend...</strong>
              </div>
            </div>

          </section>

        </main>
      </div>
  )
}

export default App