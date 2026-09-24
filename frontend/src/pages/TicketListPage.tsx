import { type FormEvent, useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { ApiError } from "../api/client";
import { listTickets, type TicketPage, type TicketStatus } from "../api/tickets";
import { useAuth } from "../auth/AuthContext";

export function TicketListPage() {
    const { logout } = useAuth();
    const [pageIndex, setPageIndex] = useState(0);
    const [ticketPage, setTicketPage] = useState<TicketPage | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [keywordInput, setKeywordInput] = useState("");
    const [statusInput, setStatusInput] = useState<TicketStatus | "">("");
    const [keyword, setKeyword] = useState("");
    const [status, setStatus] = useState<TicketStatus | "">("");

    const loadPage = useCallback(async (page: number) => {
        setIsLoading(true);
        setError(null);
        try {
            setTicketPage(await listTickets(page, 20, keyword, status));
        } catch (exception) {
            setTicketPage(null);
            setError(exception instanceof ApiError ? exception.message : "Unable to load tickets.");
        } finally {
            setIsLoading(false);
        }
    }, [keyword, status]);

    useEffect(() => {
        void loadPage(pageIndex);
    }, [loadPage, pageIndex]);

    const totalPages = ticketPage?.totalPages ?? 0;
    const canGoPrevious = pageIndex > 0;
    const canGoNext = totalPages > 0 && pageIndex < totalPages - 1;
    const hasFilters = keyword !== "" || status !== "";

    function submitFilters(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setPageIndex(0);
        setKeyword(keywordInput.trim());
        setStatus(statusInput);
    }

    return (
        <main>
            <h1>Support tickets</h1>
            <nav>
                <Link to="/tickets/new">Create ticket</Link>
                <button type="button" onClick={logout}>Sign out</button>
            </nav>
            <form onSubmit={submitFilters}>
                <label htmlFor="ticket-search">Search</label>
                <input
                    id="ticket-search"
                    value={keywordInput}
                    onChange={(event) => setKeywordInput(event.target.value)}
                />
                <label htmlFor="status-filter">Status</label>
                <select
                    id="status-filter"
                    value={statusInput}
                    onChange={(event) => setStatusInput(event.target.value as TicketStatus | "")}
                >
                    <option value="">All statuses</option>
                    <option value="OPEN">Open</option>
                    <option value="IN_PROGRESS">In progress</option>
                    <option value="RESOLVED">Resolved</option>
                    <option value="CLOSED">Closed</option>
                    <option value="CANCELLED">Cancelled</option>
                </select>
                <button type="submit">Apply</button>
            </form>

            {isLoading && <p>Loading tickets…</p>}
            {error !== null && <p role="alert">{error}</p>}
            {!isLoading && error === null && ticketPage !== null && ticketPage.content.length === 0 && (
                <p>{hasFilters ? "No matching tickets." : "No tickets yet."}</p>
            )}
            {!isLoading && ticketPage !== null && ticketPage.content.length > 0 && (
                <table>
                    <thead>
                        <tr>
                            <th>Title</th>
                            <th>Priority</th>
                            <th>Assignee</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        {ticketPage.content.map((ticket) => (
                            <tr key={ticket.id}>
                                <td>
                                    <Link to={`/tickets/${ticket.id}`}>{ticket.title}</Link>
                                </td>
                                <td>{ticket.priority}</td>
                                <td>{ticket.assignee ?? "Unassigned"}</td>
                                <td>{ticket.status}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}

            <div>
                <button type="button" disabled={!canGoPrevious} onClick={() => setPageIndex((current) => current - 1)}>
                    Previous
                </button>
                <span>
                    Page {totalPages === 0 ? 0 : pageIndex + 1} of {totalPages}
                </span>
                <button type="button" disabled={!canGoNext} onClick={() => setPageIndex((current) => current + 1)}>
                    Next
                </button>
            </div>
        </main>
    );
}
