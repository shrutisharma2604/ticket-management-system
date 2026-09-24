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
        <main className="container min-vh-100 d-flex align-items-center justify-content-center py-5">
            <div className="card border-0 shadow-sm w-100" style={{ maxWidth: "440px" }}>
                <div className="card-body p-4 p-md-5">
                    <div className="text-center mb-4">
                        <h1 className="h3 mb-2">Support Ticket Management</h1>
                        <p className="text-body-secondary mb-0">Sign in to access your dashboard.</p>
                    </div>
                    <form onSubmit={handleSubmit}>
                        <div className="mb-3">
                            <label className="form-label" htmlFor="username">Username</label>
                            <input
                                className="form-control"
                                id="username"
                                name="username"
                                autoComplete="username"
                                required
                                value={username}
                                onChange={(event) => setUsername(event.target.value)}
                            />
                        </div>

                        <div className="mb-4">
                            <label className="form-label" htmlFor="password">Password</label>
                            <input
                                className="form-control"
                                id="password"
                                name="password"
                                type="password"
                                autoComplete="current-password"
                                required
                                value={password}
                                onChange={(event) => setPassword(event.target.value)}
                            />
                        </div>

                        {error !== null && <p className="alert alert-danger" role="alert">{error}</p>}

                        <button className="btn btn-primary w-100" type="submit" disabled={isSubmitting}>
                            {isSubmitting ? "Signing in…" : "Sign in"}
                        </button>
                    </form>
                </div>
            </div>
        </main>
    );
}

function resolveLoginError(exception: unknown): string {
    if (exception instanceof ApiError && exception.status === 401) {
        return "The username or password is incorrect.";
    }
    return "Unable to sign in. Please try again.";
}
