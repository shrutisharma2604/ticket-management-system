import { type FormEvent, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";

import { ApiError } from "../api/client";
import {
    addComment,
    getTicket,
    transitionTicket,
    type TicketDetailResponse,
    type TicketStatus,
    updateTicket,
} from "../api/tickets";
import { CommentList } from "../components/CommentList";
import { ErrorBanner } from "../components/ErrorBanner";

const ALL_STATUSES: TicketStatus[] = [
    "OPEN",
    "IN_PROGRESS",
    "RESOLVED",
    "CLOSED",
    "CANCELLED",
];

export function TicketDetailPage() {
    const { ticketId } = useParams<{ ticketId: string }>();
    const [ticket, setTicket] = useState<TicketDetailResponse | null>(null);
    const [error, setError] = useState<ApiError | string | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isSavingStatus, setIsSavingStatus] = useState(false);
    const [isSavingDetails, setIsSavingDetails] = useState(false);
    const [commentBody, setCommentBody] = useState("");
    const [isAddingComment, setIsAddingComment] = useState(false);

    useEffect(() => {
        if (ticketId === undefined) {
            setTicket(null);
            setError("Ticket not found");
            setIsLoading(false);
            return;
        }

        let cancelled = false;
        setIsLoading(true);
        setError(null);
        setTicket(null);

        getTicket(ticketId)
            .then((response) => {
                if (!cancelled) {
                    setTicket(response);
                }
            })
            .catch((exception) => {
                if (cancelled) {
                    return;
                }
                setError(exception instanceof ApiError ? exception : "Unable to load the ticket.");
            })
            .finally(() => {
                if (!cancelled) {
                    setIsLoading(false);
                }
            });

        return () => {
            cancelled = true;
        };
    }, [ticketId]);

    async function changeStatus(status: TicketStatus) {
        if (ticketId === undefined || ticket === null) {
            return;
        }
        setError(null);
        setIsSavingStatus(true);
        try {
            const updated = await transitionTicket(ticketId, status);
            setTicket({ ...ticket, status: updated.status, updatedAt: updated.updatedAt });
        } catch (exception) {
            setError(exception instanceof ApiError ? exception : "Unable to change ticket status.");
        } finally {
            setIsSavingStatus(false);
        }
    }

    async function saveDetails(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (ticketId === undefined || ticket === null) {
            return;
        }
        setError(null);
        setIsSavingDetails(true);
        try {
            const updated = await updateTicket(ticketId, {
                title: ticket.title,
                description: ticket.description,
                priority: ticket.priority,
                assignee: ticket.assignee,
            });
            setTicket({ ...ticket, ...updated });
        } catch (exception) {
            setError(exception instanceof ApiError ? exception : "Unable to update the ticket.");
        } finally {
            setIsSavingDetails(false);
        }
    }

    async function submitComment(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (ticketId === undefined || ticket === null) {
            return;
        }
        setError(null);
        setIsAddingComment(true);
        try {
            const comment = await addComment(ticketId, commentBody);
            setTicket({ ...ticket, comments: [...ticket.comments, comment] });
            setCommentBody("");
        } catch (exception) {
            setError(exception instanceof ApiError ? exception : "Unable to add the comment.");
        } finally {
            setIsAddingComment(false);
        }
    }

    return (
        <main>
            <Link className="text-decoration-none" to="/">← Back to tickets</Link>
            {isLoading && <p className="mt-4">Loading ticket…</p>}
            <ErrorBanner error={error} />
            {ticket !== null && (
                <article className="mt-3">
                    <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
                        <div>
                            <h1 className="h3 mb-1">Ticket details</h1>
                            <p className="text-body-secondary mb-0">{ticket.title}</p>
                        </div>
                        <p className={`badge fs-6 mb-0 ${statusBadgeClass(ticket.status)}`}>
                            Status: {ticket.status}
                        </p>
                    </div>

                    <div className="row g-4">
                        <div className="col-lg-8">
                            <div className="card border-0 shadow-sm mb-4">
                                <div className="card-header bg-white fw-semibold py-3">Edit ticket</div>
                                <div className="card-body p-4">
                                    <form onSubmit={(event) => void saveDetails(event)} noValidate>
                                        <div className="mb-3">
                                            <label className="form-label" htmlFor="edit-title">Title</label>
                                            <input
                                                className="form-control"
                                                id="edit-title"
                                                value={ticket.title}
                                                onChange={(event) => setTicket({
                                                    ...ticket,
                                                    title: event.target.value,
                                                })}
                                            />
                                        </div>
                                        <div className="mb-3">
                                            <label className="form-label" htmlFor="edit-description">
                                                Description
                                            </label>
                                            <textarea
                                                className="form-control"
                                                id="edit-description"
                                                rows={5}
                                                value={ticket.description}
                                                onChange={(event) => setTicket({
                                                    ...ticket,
                                                    description: event.target.value,
                                                })}
                                            />
                                        </div>
                                        <div className="row g-3 mb-4">
                                            <div className="col-md-6">
                                                <label className="form-label" htmlFor="edit-priority">
                                                    Priority
                                                </label>
                                                <select
                                                    className="form-select"
                                                    id="edit-priority"
                                                    value={ticket.priority}
                                                    onChange={(event) => setTicket({
                                                        ...ticket,
                                                        priority: event.target
                                                            .value as TicketDetailResponse["priority"],
                                                    })}
                                                >
                                                    <option value="LOW">Low</option>
                                                    <option value="MEDIUM">Medium</option>
                                                    <option value="HIGH">High</option>
                                                </select>
                                            </div>
                                            <div className="col-md-6">
                                                <label className="form-label" htmlFor="edit-assignee">
                                                    Assignee
                                                </label>
                                                <input
                                                    className="form-control"
                                                    id="edit-assignee"
                                                    value={ticket.assignee ?? ""}
                                                    onChange={(event) => setTicket({
                                                        ...ticket,
                                                        assignee: event.target.value || null,
                                                    })}
                                                />
                                            </div>
                                        </div>
                                        <button className="btn btn-primary" type="submit" disabled={isSavingDetails}>
                                            {isSavingDetails ? "Saving…" : "Save details"}
                                        </button>
                                    </form>
                                </div>
                            </div>

                            <section className="card border-0 shadow-sm">
                                <div className="card-header bg-white fw-semibold py-3">Comments</div>
                                <div className="card-body p-4">
                                    <CommentList comments={ticket.comments} />
                                    <form
                                        className="border-top pt-3 mt-3"
                                        onSubmit={(event) => void submitComment(event)}
                                        noValidate
                                    >
                                        <label className="form-label" htmlFor="comment-body">Add comment</label>
                                        <textarea
                                            className="form-control mb-3"
                                            id="comment-body"
                                            rows={3}
                                            value={commentBody}
                                            onChange={(event) => setCommentBody(event.target.value)}
                                        />
                                        <button
                                            className="btn btn-primary"
                                            type="submit"
                                            disabled={isAddingComment}
                                        >
                                            {isAddingComment ? "Adding…" : "Add comment"}
                                        </button>
                                    </form>
                                </div>
                            </section>
                        </div>

                        <div className="col-lg-4">
                            <div className="card border-0 shadow-sm mb-4">
                                <div className="card-header bg-white fw-semibold py-3">Overview</div>
                                <div className="card-body">
                                    <p><strong>Priority:</strong> {ticket.priority}</p>
                                    <p><strong>Assignee:</strong> {ticket.assignee ?? "Unassigned"}</p>
                                    <p><strong>Created:</strong> {ticket.createdAt}</p>
                                    <p className="mb-0"><strong>Last changed:</strong> {ticket.updatedAt}</p>
                                </div>
                            </div>
                            <div className="card border-0 shadow-sm">
                                <div className="card-header bg-white fw-semibold py-3">Change status</div>
                                <div className="card-body d-grid gap-2">
                                    <p className="small text-body-secondary">
                                        The server enforces allowed transitions.
                                    </p>
                                    {ALL_STATUSES.map((status) => (
                                        <button
                                            className="btn btn-outline-secondary"
                                            key={status}
                                            type="button"
                                            disabled={isSavingStatus}
                                            onClick={() => void changeStatus(status)}
                                        >
                                            Move to {status.replaceAll("_", " ")}
                                        </button>
                                    ))}
                                </div>
                            </div>
                        </div>
                    </div>
                </article>
            )}
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
