import { useCallback, useEffect, useRef, useState } from "react";
import { API_PATHS, DASHBOARD_CONSTANTS, DEVICE_STATES } from "./constants";
import { fetchDevices, wakeDevice } from "./deviceApi";
import type { AndroidDevice } from "./types";

type LoadState = "loading" | "ready" | "error";
type WakeState = "idle" | "running" | "completed" | "error";

function formatModel(model: string | null): string {
  return model?.replaceAll("_", " ") ?? DASHBOARD_CONSTANTS.unknownModelLabel;
}

function formatDeviceFamily(device: AndroidDevice): string {
  return [device.manufacturer, device.product]
    .filter((value): value is string => value != null && value.length > 0)
    .join(" · ") || "Android";
}

function DeviceCard({ device }: { device: AndroidDevice }) {
  const isOnline = device.state === DEVICE_STATES.online;
  const [screenshotRevision, setScreenshotRevision] = useState(() => Date.now());
  const [screenshotAvailable, setScreenshotAvailable] = useState(false);
  const [wakeState, setWakeState] = useState<WakeState>("idle");
  const batteryLabel = device.batteryPercentage == null
    ? DASHBOARD_CONSTANTS.unknownValueLabel
    : `${device.batteryPercentage}%`;
  const androidLabel = device.androidVersion == null
    ? DASHBOARD_CONSTANTS.unknownValueLabel
    : `Android ${device.androidVersion}`;
  const observedLabel = new Date(device.observedAtEpochMillis).toLocaleTimeString();

  useEffect(() => {
    setScreenshotAvailable(false);
    if (!isOnline) return;

    const timer = window.setInterval(
      () => setScreenshotRevision(Date.now()),
      DASHBOARD_CONSTANTS.screenshotRefreshIntervalMilliseconds,
    );
    return () => window.clearInterval(timer);
  }, [device.serial, isOnline]);

  const screenshotUrl =
    `${API_PATHS.deviceScreenshot(device.serial)}?revision=${screenshotRevision}`;
  const wake = async () => {
    setWakeState("running");
    try {
      await wakeDevice(device.serial);
      setWakeState("completed");
      setScreenshotRevision(Date.now());
    } catch {
      setWakeState("error");
    }
  };

  return (
    <article className="device-card">
      <div className={`device-visual device-visual-${device.formFactor}`}>
        <span className="speaker" />
        <span className="android-mark" aria-hidden="true">A</span>
        {isOnline && (
          <img
            className={`device-screen ${screenshotAvailable ? "device-screen-visible" : ""}`}
            src={screenshotUrl}
            alt={`Current screen of ${formatModel(device.model)}`}
            onLoad={() => setScreenshotAvailable(true)}
            onError={() => setScreenshotAvailable(false)}
          />
        )}
        <span className={`signal ${isOnline ? "signal-online" : ""}`} />
      </div>
      <div className="device-copy">
        <div className="device-heading">
          <div>
            <p className="eyebrow">{formatDeviceFamily(device)}</p>
            <h2>{formatModel(device.model)}</h2>
          </div>
          <span className={`state-badge state-${device.state}`}>{device.state}</span>
        </div>
        <dl>
          <div>
            <dt>Serial</dt>
            <dd>{device.serial}</dd>
          </div>
          <div>
            <dt>Connection</dt>
            <dd>{device.connectionType} · ADB</dd>
          </div>
          <div>
            <dt>Android</dt>
            <dd>{androidLabel}{device.sdkLevel == null ? "" : ` · SDK ${device.sdkLevel}`}</dd>
          </div>
          <div>
            <dt>Battery</dt>
            <dd>{batteryLabel} · {device.chargingState.replaceAll("_", " ")}</dd>
          </div>
          <div>
            <dt>Screen</dt>
            <dd>{device.screenState}</dd>
          </div>
          <div>
            <dt>Form</dt>
            <dd>{device.formFactor.replaceAll("_", " ")}</dd>
          </div>
          <div>
            <dt>Observed</dt>
            <dd>{observedLabel}</dd>
          </div>
        </dl>
        <div className="device-actions">
          <button
            type="button"
            disabled={!isOnline || wakeState === "running"}
            onClick={() => void wake()}
          >
            {wakeState === "running" ? "Waking…" : "Wake screen"}
          </button>
          <span className={`action-status action-${wakeState}`} aria-live="polite">
            {wakeState === "completed" && "Wake sent"}
            {wakeState === "error" && DASHBOARD_CONSTANTS.wakeFailureMessage}
          </span>
        </div>
      </div>
    </article>
  );
}

export default function App() {
  const [devices, setDevices] = useState<AndroidDevice[]>([]);
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const activeRequest = useRef<AbortController | null>(null);

  const refresh = useCallback(async () => {
    activeRequest.current?.abort();
    const controller = new AbortController();
    activeRequest.current = controller;

    try {
      const response = await fetchDevices(controller.signal);
      setDevices(response.devices);
      setLastUpdated(new Date());
      setLoadState("ready");
    } catch (error) {
      if (error instanceof DOMException && error.name === "AbortError") return;
      setLoadState("error");
    }
  }, []);

  useEffect(() => {
    void refresh();
    const timer = window.setInterval(
      () => void refresh(),
      DASHBOARD_CONSTANTS.refreshIntervalMilliseconds,
    );

    return () => {
      window.clearInterval(timer);
      activeRequest.current?.abort();
    };
  }, [refresh]);

  return (
    <main>
      <header className="masthead">
        <a className="brand" href="/" aria-label="PocketPortal dashboard">
          <span className="brand-glyph">P</span>
          <span>PocketPortal</span>
        </a>
        <div className="lab-status">
          <span className="pulse" />
          Lab connected
        </div>
      </header>

      <section className="hero">
        <p className="kicker">Physical device lab</p>
        <div className="hero-row">
          <div>
            <h1>Your devices,<br />within reach.</h1>
            <p className="hero-description">
              A quiet command center for the Android hardware connected to your home lab.
            </p>
          </div>
          <div className="device-count" aria-label={`${devices.length} connected devices`}>
            <strong>{devices.length.toString().padStart(2, "0")}</strong>
            <span>devices visible</span>
          </div>
        </div>
      </section>

      <section className="devices-section" aria-labelledby="devices-title">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Live inventory</p>
            <h2 id="devices-title">Android devices</h2>
          </div>
          <div className="refresh-area">
            <span aria-live="polite">
              {lastUpdated ? `Updated ${lastUpdated.toLocaleTimeString()}` : "Connecting…"}
            </span>
            <button type="button" onClick={() => void refresh()}>
              Refresh
            </button>
          </div>
        </div>

        {loadState === "loading" && (
          <div className="message-card" role="status">Finding your connected devices…</div>
        )}
        {loadState === "error" && (
          <div className="message-card error-card" role="alert">
            <strong>We couldn’t reach ADB.</strong>
            <span>{DASHBOARD_CONSTANTS.unavailableMessage}</span>
            <button type="button" onClick={() => void refresh()}>Try again</button>
          </div>
        )}
        {loadState === "ready" && devices.length === 0 && (
          <div className="message-card">
            <strong>No Android devices are visible yet.</strong>
            <span>Connect a device and authorize USB debugging to see it here.</span>
          </div>
        )}
        {loadState === "ready" && devices.length > 0 && (
          <div className="device-grid">
            {devices.map((device) => <DeviceCard key={device.serial} device={device} />)}
          </div>
        )}
      </section>

      <footer>
        <span>POCKETPORTAL / LOCAL LAB</span>
        <span>Safe device actions</span>
      </footer>
    </main>
  );
}
