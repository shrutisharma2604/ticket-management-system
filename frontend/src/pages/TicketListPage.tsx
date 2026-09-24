import { type FormEvent, useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { ApiError } from "../api/client";
import { listTickets, type TicketPage, type TicketStatus } from "../api/tickets";

export function TicketListPage() {
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
            <div className="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h1 className="h3 mb-1">Support tickets</h1>
                    <p className="text-body-secondary mb-0">View and manage customer support requests.</p>
                </div>
                <Link className="btn btn-primary" to="/tickets/new">Create ticket</Link>
            </div>

            <div className="card border-0 shadow-sm mb-4">
                <div className="card-body">
                    <form className="row g-3 align-items-end" onSubmit={submitFilters}>
                        <div className="col-md-7">
                            <label className="form-label" htmlFor="ticket-search">Search</label>
                            <input
                                className="form-control"
                                id="ticket-search"
                                value={keywordInput}
                                onChange={(event) => setKeywordInput(event.target.value)}
                            />
                        </div>
                        <div className="col-md-3">
                            <label className="form-label" htmlFor="status-filter">Status</label>
                            <select
                                className="form-select"
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
                        </div>
                        <div className="col-md-2 d-grid">
                            <button className="btn btn-outline-primary" type="submit">Apply</button>
                        </div>
                    </form>
                </div>
            </div>

            {isLoading && <p>Loading tickets…</p>}
            {error !== null && <p className="alert alert-danger" role="alert">{error}</p>}
            {!isLoading && error === null && ticketPage !== null && ticketPage.content.length === 0 && (
                <div className="card border-0 shadow-sm">
                    <div className="card-body text-center text-body-secondary py-5">
                        {hasFilters ? "No matching tickets." : "No tickets yet."}
                    </div>
                </div>
            )}
            {!isLoading && ticketPage !== null && ticketPage.content.length > 0 && (
                <div className="card border-0 shadow-sm overflow-hidden">
                    <div className="table-responsive">
                        <table className="table table-hover align-middle mb-0">
                            <thead className="table-light">
                                <tr>
                                    <th className="px-4 py-3">Title</th>
                                    <th className="py-3">Priority</th>
                                    <th className="py-3">Assignee</th>
                                    <th className="py-3">Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                {ticketPage.content.map((ticket) => (
                                    <tr key={ticket.id}>
                                        <td className="px-4 py-3">
                                            <Link className="fw-semibold text-decoration-none" to={`/tickets/${ticket.id}`}>
                                                {ticket.title}
                                            </Link>
                                        </td>
                                        <td>{ticket.priority}</td>
                                        <td>{ticket.assignee ?? "Unassigned"}</td>
                                        <td>
                                            <span className={`badge ${statusBadgeClass(ticket.status)}`}>
                                                {ticket.status.replaceAll("_", " ")}
                                            </span>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            <div className="d-flex justify-content-between align-items-center mt-4">
                <button
                    className="btn btn-outline-secondary"
                    type="button"
                    disabled={!canGoPrevious}
                    onClick={() => setPageIndex((current) => current - 1)}
                >
                    Previous
                </button>
                <span className="text-body-secondary">
                    Page {totalPages === 0 ? 0 : pageIndex + 1} of {totalPages}
                </span>
                <button
                    className="btn btn-outline-secondary"
                    type="button"
                    disabled={!canGoNext}
                    onClick={() => setPageIndex((current) => current + 1)}
                >
                    Next
                </button>
            </div>
        </main>
    );
}

function statusBadgeClass(status: TicketStatus): string {
    const classes: Record<TicketStatus, string> = {
        OPEN: "text-bg-primary",
        IN_PROGRESS: "text-bg-warning",
        RESOLVED: "text-bg-success",
        CLOSED: "text-bg-secondary",
        CANCELLED: "text-bg-danger",
    };
    return classes[status];
}
