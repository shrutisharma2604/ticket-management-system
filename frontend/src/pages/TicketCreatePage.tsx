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
        <main>
            <Link to="/">Back to tickets</Link>
            <h1>Create support ticket</h1>
            <form onSubmit={handleSubmit} noValidate>
                <label htmlFor="title">Title</label>
                <input
                    id="title"
                    name="title"
                    value={title}
                    onChange={(event) => setTitle(event.target.value)}
                />

                <label htmlFor="description">Description</label>
                <textarea
                    id="description"
                    name="description"
                    value={description}
                    onChange={(event) => setDescription(event.target.value)}
                />

                <label htmlFor="priority">Priority</label>
                <select
                    id="priority"
                    name="priority"
                    value={priority}
                    onChange={(event) => setPriority(event.target.value as TicketPriority)}
                >
                    <option value="LOW">Low</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High</option>
                </select>

                <label htmlFor="assignee">Assignee (optional)</label>
                <input
                    id="assignee"
                    name="assignee"
                    value={assignee}
                    onChange={(event) => setAssignee(event.target.value)}
                />

                <ErrorBanner error={error} />

                <button type="submit" disabled={isSubmitting}>
                    {isSubmitting ? "Creating…" : "Create ticket"}
                </button>
            </form>
        </main>
    );
}
