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
});
