import { type FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import { ApiError } from "../api/client";
import {
    createTicket,
    type TicketPriority,
} from "../api/tickets";
import { ErrorBanner } from "../components/ErrorBanner";

export function TicketCreatePage() {
    const navigate = useNavigate();
    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");
    const [priority, setPriority] = useState<TicketPriority>("MEDIUM");
    const [assignee, setAssignee] = useState("");
    const [error, setError] = useState<ApiError | string | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setIsSubmitting(true);
        try {
            const ticket = await createTicket({
                title,
                description,
                priority,
                assignee: assignee.trim() || undefined,
            });
            navigate(`/tickets/${ticket.id}`, { replace: true });
        } catch (exception) {
            setError(exception instanceof ApiError ? exception : "Unable to create the ticket.");
        } finally {
            setIsSubmitting(false);
        }
    }

    return (
        <main className="mx-auto" style={{ maxWidth: "760px" }}>
            <Link className="text-decoration-none" to="/">← Back to tickets</Link>
            <div className="card border-0 shadow-sm mt-3">
                <div className="card-body p-4 p-md-5">
                    <h1 className="h3 mb-4">Create support ticket</h1>
                    <form onSubmit={handleSubmit} noValidate>
                        <div className="mb-3">
                            <label className="form-label" htmlFor="title">Title</label>
                            <input
                                className="form-control"
                                id="title"
                                name="title"
                                value={title}
                                onChange={(event) => setTitle(event.target.value)}
                            />
                        </div>

                        <div className="mb-3">
                            <label className="form-label" htmlFor="description">Description</label>
                            <textarea
                                className="form-control"
                                id="description"
                                name="description"
                                rows={5}
                                value={description}
                                onChange={(event) => setDescription(event.target.value)}
                            />
                        </div>

                        <div className="mb-3">
                            <label className="form-label" htmlFor="priority">Priority</label>
                            <select
                                className="form-select"
                                id="priority"
                                name="priority"
                                value={priority}
                                onChange={(event) => setPriority(event.target.value as TicketPriority)}
                            >
                                <option value="LOW">Low</option>
                                <option value="MEDIUM">Medium</option>
                                <option value="HIGH">High</option>
                            </select>
                        </div>

                        <div className="mb-4">
                            <label className="form-label" htmlFor="assignee">Assignee (optional)</label>
                            <input
                                className="form-control"
                                id="assignee"
                                name="assignee"
                                value={assignee}
                                onChange={(event) => setAssignee(event.target.value)}
                            />
                        </div>

                        <ErrorBanner error={error} />

                        <div className="d-flex justify-content-end gap-2">
                            <Link className="btn btn-outline-secondary" to="/">Cancel</Link>
                            <button className="btn btn-primary" type="submit" disabled={isSubmitting}>
                                {isSubmitting ? "Creating…" : "Create ticket"}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </main>
    );
}
