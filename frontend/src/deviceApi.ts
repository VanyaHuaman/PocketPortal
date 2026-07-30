import { API_PATHS } from "./constants";
import type { DeviceListResponse } from "./types";

export async function fetchDevices(signal?: AbortSignal): Promise<DeviceListResponse> {
  const response = await fetch(API_PATHS.devices, {
    headers: { Accept: "application/json" },
    signal,
  });

  if (!response.ok) {
    throw new Error(`Device discovery failed with status ${response.status}`);
  }

  return response.json() as Promise<DeviceListResponse>;
}

export async function wakeDevice(serial: string): Promise<void> {
  const response = await fetch(API_PATHS.wakeDevice(serial), {
    method: "POST",
    headers: { Accept: "application/json" },
  });

  if (!response.ok) {
    throw new Error(`Wake action failed with status ${response.status}`);
  }
}
