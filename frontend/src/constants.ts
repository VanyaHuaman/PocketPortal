export const API_PATHS = {
  devices: "/api/devices",
} as const;

export const DASHBOARD_CONSTANTS = {
  refreshIntervalMilliseconds: 5_000,
  unknownModelLabel: "Unknown Android device",
  unavailableMessage: "Device discovery is temporarily unavailable.",
} as const;

export const DEVICE_STATES = {
  online: "online",
  offline: "offline",
  unauthorized: "unauthorized",
} as const;
