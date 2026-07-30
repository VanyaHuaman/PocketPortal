function displayName(device) {
  const model = device.model || device.product || device.serial;
  return model.replace(/_/g, " ");
}

function safe(value) {
  return String(value == null ? "" : value).replace(/[\t\r\n]/g, " ");
}

function identifier(value) {
  return value
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "");
}

function run(arguments) {
  ObjC.import("Foundation");
  const inventoryPath = arguments[0];
  const requestedName = arguments[1] || "";
  const data = $.NSData.dataWithContentsOfFile(inventoryPath);

  if (!data) {
    throw new Error("PocketPortal device inventory could not be read");
  }

  const inventory = ObjC.deepUnwrap(
    $.NSJSONSerialization.JSONObjectWithDataOptionsError(data, 0, null),
  );

  const requestedIdentifier = identifier(requestedName);
  return inventory.devices
    .filter((device) => device.state === "online")
    .filter(
      (device) =>
        !requestedIdentifier ||
        identifier(displayName(device)) === requestedIdentifier ||
        identifier(device.serial) === requestedIdentifier,
    )
    .map((device) => {
      const label = displayName(device);
      const kind = (device.formFactor || "unknown").replace(/_/g, " ");
      const battery =
        device.batteryPercentage == null ? "—" : `${device.batteryPercentage}%`;
      return [
        device.serial,
        label,
        device.manufacturer || "Android",
        device.androidVersion || "—",
        kind,
        battery,
        device.chargingState || "unknown",
        device.screenState || "unknown",
      ]
        .map(safe)
        .join("\t");
    })
    .join("\n");
}
