import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { TicketDetailPage } from "../pages/TicketDetailPage";

describe("TicketDetailPage not-found copy", () => {
    beforeEach(() => {
        vi.stubGlobal(
            "fetch",
            vi.fn().mockResolvedValue({
                ok: false,
                status: 404,
                json: async () => ({
                    title: "Not found",
                    status: 404,
                    detail: "Ticket not found",
                }),
            }),
        );
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.restoreAllMocks();
    });

    it("shows a clear not-found message when the ticket is missing", async () => {
        render(
            <MemoryRouter initialEntries={["/tickets/00000000-0000-0000-0000-000000000001"]}>
                <Routes>
                    <Route path="/tickets/:ticketId" element={<TicketDetailPage />} />
                </Routes>
            </MemoryRouter>,
        );

        await waitFor(() => {
            expect(screen.getByRole("alert")).toHaveTextContent(/ticket not found/i);
        });
        expect(screen.queryByRole("heading", { name: /ticket details/i })).not.toBeInTheDocument();
    });
});
