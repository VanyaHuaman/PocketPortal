import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

describe("PocketPortal dashboard", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders discovered devices", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(
        JSON.stringify({
          devices: [
            {
              serial: "ABC123",
              state: "online",
              model: "Pixel_4_XL",
              product: "coral",
              connectionType: "usb",
              manufacturer: "Google",
              androidVersion: "13",
              sdkLevel: 33,
              batteryPercentage: 75,
              chargingState: "charging",
              screenState: "on",
              formFactor: "phone",
              observedAtEpochMillis: 1_234,
            },
          ],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );

    render(<App />);

    expect(await screen.findByText("Pixel 4 XL")).toBeInTheDocument();
    expect(screen.getByText("ABC123")).toBeInTheDocument();
    expect(screen.getByText("online")).toBeInTheDocument();
    expect(screen.getByText("01")).toBeInTheDocument();
    expect(screen.getByText("Android 13 · SDK 33")).toBeInTheDocument();
    expect(screen.getByText("75% · charging")).toBeInTheDocument();
    expect(screen.getByText("phone")).toBeInTheDocument();
    expect(screen.getByAltText("Current screen of Pixel 4 XL")).toHaveAttribute(
      "src",
      expect.stringMatching(/^\/api\/devices\/ABC123\/screenshot\?revision=\d+$/),
    );
    expect(
      screen.getByAltText("Current screen of Pixel 4 XL").closest(".device-visual"),
    ).toHaveClass("device-visual-phone");
  });

  it("shows a recoverable error state", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(null, { status: 503 }))
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ devices: [] }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }),
      );

    render(<App />);

    expect(await screen.findByText("We couldn’t reach ADB.")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Try again" }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    expect(await screen.findByText("No Android devices are visible yet.")).toBeInTheDocument();
  });

  it("sends a wake action for an online device", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            devices: [{
              serial: "ABC123",
              state: "online",
              model: "Pixel_4_XL",
              product: "coral",
              connectionType: "usb",
              manufacturer: "Google",
              androidVersion: "13",
              sdkLevel: 33,
              batteryPercentage: 75,
              chargingState: "charging",
              screenState: "off",
              formFactor: "tablet",
              observedAtEpochMillis: 1_234,
            }],
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        ),
      )
      .mockResolvedValueOnce(new Response(null, { status: 204 }));

    render(<App />);
    fireEvent.click(await screen.findByRole("button", { name: "Wake screen" }));

    expect(await screen.findByText("Wake sent")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenLastCalledWith(
      "/api/devices/ABC123/actions/wake",
      expect.objectContaining({ method: "POST" }),
    );
  });
});
