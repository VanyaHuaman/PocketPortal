function displayName(device) {
  const model = device.model || device.product || device.serial;
  return model.replace(/_/g, " ");
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
      return `${device.serial}\t${label} · ${kind}`;
    })
    .join("\n");
}
