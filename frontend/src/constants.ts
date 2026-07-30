export const API_PATHS = {
  devices: "/api/devices",
  deviceScreenshot: (serial: string) =>
    `/api/devices/${encodeURIComponent(serial)}/screenshot`,
  wakeDevice: (serial: string) =>
    `/api/devices/${encodeURIComponent(serial)}/actions/wake`,
} as const;

export const DASHBOARD_CONSTANTS = {
  refreshIntervalMilliseconds: 5_000,
  screenshotRefreshIntervalMilliseconds: 30_000,
  unknownModelLabel: "Unknown Android device",
  unavailableMessage: "Device discovery is temporarily unavailable.",
  unknownValueLabel: "Unknown",
  wakeFailureMessage: "Couldn’t wake device",
} as const;

export const DEVICE_STATES = {
  online: "online",
  offline: "offline",
  unauthorized: "unauthorized",
} as const;
