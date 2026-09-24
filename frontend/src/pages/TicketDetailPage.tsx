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
            <Link to="/">Back to tickets</Link>
            {isLoading && <p>Loading ticket…</p>}
            <ErrorBanner error={error} />
            {ticket !== null && (
                <article>
                    <h1>Ticket details</h1>
                    <p>Title: {ticket.title}</p>
                    <p>Description: {ticket.description}</p>
                    <p>Priority: {ticket.priority}</p>
                    <p>Assignee: {ticket.assignee ?? "Unassigned"}</p>
                    <p>Status: {ticket.status}</p>
                    <p>Created: {ticket.createdAt}</p>
                    <p>Last changed: {ticket.updatedAt}</p>
                    <form onSubmit={(event) => void saveDetails(event)} noValidate>
                        <label htmlFor="edit-title">Title</label>
                        <input
                            id="edit-title"
                            value={ticket.title}
                            onChange={(event) => setTicket({ ...ticket, title: event.target.value })}
                        />
                        <label htmlFor="edit-description">Description</label>
                        <textarea
                            id="edit-description"
                            value={ticket.description}
                            onChange={(event) => setTicket({ ...ticket, description: event.target.value })}
                        />
                        <label htmlFor="edit-priority">Priority</label>
                        <select
                            id="edit-priority"
                            value={ticket.priority}
                            onChange={(event) => setTicket({
                                ...ticket,
                                priority: event.target.value as TicketDetailResponse["priority"],
                            })}
                        >
                            <option value="LOW">Low</option>
                            <option value="MEDIUM">Medium</option>
                            <option value="HIGH">High</option>
                        </select>
                        <label htmlFor="edit-assignee">Assignee</label>
                        <input
                            id="edit-assignee"
                            value={ticket.assignee ?? ""}
                            onChange={(event) => setTicket({
                                ...ticket,
                                assignee: event.target.value || null,
                            })}
                        />
                        <button type="submit" disabled={isSavingDetails}>
                            {isSavingDetails ? "Saving…" : "Save details"}
                        </button>
                    </form>
                    <div>
                        <p>Change status (server enforces allowed transitions):</p>
                        {ALL_STATUSES.map((status) => (
                            <button
                                key={status}
                                type="button"
                                disabled={isSavingStatus}
                                onClick={() => void changeStatus(status)}
                            >
                                Move to {status.replaceAll("_", " ")}
                            </button>
                        ))}
                    </div>
                    <section>
                        <h2>Comments</h2>
                        <CommentList comments={ticket.comments} />
                        <form onSubmit={(event) => void submitComment(event)} noValidate>
                            <label htmlFor="comment-body">Add comment</label>
                            <textarea
                                id="comment-body"
                                value={commentBody}
                                onChange={(event) => setCommentBody(event.target.value)}
                            />
                            <button type="submit" disabled={isAddingComment}>
                                {isAddingComment ? "Adding…" : "Add comment"}
                            </button>
                        </form>
                    </section>
                </article>
            )}
        </main>
    );
}
