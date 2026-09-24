import { type FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";

import { ApiError } from "../api/client";
import { useAuth } from "../auth/AuthContext";

export function LoginPage() {
    const navigate = useNavigate();
    const { login } = useAuth();
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState<string | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setError(null);
        setIsSubmitting(true);
        try {
            await login({ username, password });
            navigate("/", { replace: true });
        } catch (exception) {
            setError(resolveLoginError(exception));
        } finally {
            setIsSubmitting(false);
        }
    }

    return (
        <main>
            <h1>Support Ticket Management</h1>
            <form onSubmit={handleSubmit}>
                <label htmlFor="username">Username</label>
                <input
                    id="username"
                    name="username"
                    autoComplete="username"
                    required
                    value={username}
                    onChange={(event) => setUsername(event.target.value)}
                />

                <label htmlFor="password">Password</label>
                <input
                    id="password"
                    name="password"
                    type="password"
                    autoComplete="current-password"
                    required
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                />

                {error !== null && <p role="alert">{error}</p>}

                <button type="submit" disabled={isSubmitting}>
                    {isSubmitting ? "Signing in…" : "Sign in"}
                </button>
            </form>
        </main>
    );
}

function resolveLoginError(exception: unknown): string {
    if (exception instanceof ApiError && exception.status === 401) {
        return "The username or password is incorrect.";
    }
    return "Unable to sign in. Please try again.";
}
