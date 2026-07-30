export type AndroidDevice = {
  serial: string;
  state: string;
  model: string | null;
  product: string | null;
  connectionType: string;
  manufacturer: string | null;
  androidVersion: string | null;
  sdkLevel: number | null;
  batteryPercentage: number | null;
  chargingState: string;
  screenState: string;
  formFactor: "phone" | "tablet" | "foldable_clamshell" | "foldable_book" | "unknown";
  observedAtEpochMillis: number;
};

export type DeviceListResponse = {
  devices: AndroidDevice[];
};
