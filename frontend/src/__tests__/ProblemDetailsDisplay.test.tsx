import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, describe, expect, it, vi } from "vitest";

import { TicketCreatePage } from "../pages/TicketCreatePage";
import { TicketDetailPage } from "../pages/TicketDetailPage";

afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
});

describe("Problem Details display", () => {
    it("shows backend field errors without a false creation success", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue(problemResponse(400, {
            title: "Validation failed",
            detail: "One or more request fields are invalid",
            errors: {
                title: "Title is required",
                description: "Description is required",
            },
        })));

        render(<MemoryRouter><TicketCreatePage /></MemoryRouter>);
        fireEvent.change(screen.getByLabelText("Title"), { target: { value: " " } });
        fireEvent.change(screen.getByLabelText("Description"), { target: { value: " " } });
        fireEvent.submit(screen.getByRole("button", { name: "Create ticket" }).closest("form")!);

        expect(await screen.findByText("Title is required")).toBeInTheDocument();
        expect(screen.getByText("Description is required")).toBeInTheDocument();
        expect(screen.queryByRole("status")).not.toBeInTheDocument();
    });

    it("shows the server detail for an illegal transition", async () => {
        const fetchMock = vi.fn()
            .mockResolvedValueOnce(okResponse(ticketDetail()))
            .mockResolvedValueOnce(problemResponse(422, {
                title: "Illegal ticket status transition",
                detail: "Transition from OPEN to RESOLVED is not allowed",
            }));
        vi.stubGlobal("fetch", fetchMock);

        renderDetailPage();
        fireEvent.click(await screen.findByRole("button", { name: "Move to RESOLVED" }));

        expect(await screen.findByRole("alert")).toHaveTextContent(
            "Transition from OPEN to RESOLVED is not allowed",
        );
        expect(screen.getByText("Status: OPEN")).toBeInTheDocument();
    });

    it("shows not-found and never renders ticket details", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue(problemResponse(404, {
            title: "Not found",
            detail: "Ticket not found",
        })));

        renderDetailPage();

        await waitFor(() => expect(screen.getByRole("alert")).toHaveTextContent("Ticket not found"));
        expect(screen.queryByRole("heading", { name: "Ticket details" })).not.toBeInTheDocument();
        expect(screen.queryByText(/saved|created/i)).not.toBeInTheDocument();
    });
});

function renderDetailPage() {
    render(
        <MemoryRouter initialEntries={["/tickets/00000000-0000-0000-0000-000000000001"]}>
            <Routes>
                <Route path="/tickets/:ticketId" element={<TicketDetailPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

function problemResponse(status: number, body: object) {
    return {
        ok: false,
        status,
        headers: new Headers({ "Content-Type": "application/problem+json" }),
        json: async () => ({ status, ...body }),
    };
}

function okResponse(body: object) {
    return {
        ok: true,
        status: 200,
        headers: new Headers({ "Content-Type": "application/json" }),
        json: async () => body,
    };
}

function ticketDetail() {
    return {
        id: "00000000-0000-0000-0000-000000000001",
        title: "Printer",
        description: "Offline",
        priority: "HIGH",
        assignee: null,
        status: "OPEN",
        createdAt: "2026-09-23T00:00:00Z",
        updatedAt: "2026-09-23T00:00:00Z",
        comments: [],
    };
}
