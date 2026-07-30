export type AndroidDevice = {
  serial: string;
  state: string;
  model: string | null;
  product: string | null;
};

export type DeviceListResponse = {
  devices: AndroidDevice[];
};
