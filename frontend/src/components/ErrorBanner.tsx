import { ApiError } from "../api/client";

interface ErrorBannerProps {
    error: ApiError | string | null;
}

export function ErrorBanner({ error }: ErrorBannerProps) {
    if (error === null) {
        return null;
    }
    if (typeof error === "string") {
        return <p className="alert alert-danger mt-3" role="alert">{error}</p>;
    }

    const fieldErrors = Object.entries(error.problem.errors ?? {});
    return (
        <div className="alert alert-danger mt-3" role="alert">
            <p className="mb-1">{error.problem.detail ?? error.problem.title}</p>
            {fieldErrors.length > 0 && (
                <ul className="mb-0">
                    {fieldErrors.map(([field, message]) => (
                        <li key={field}>{message}</li>
                    ))}
                </ul>
            )}
        </div>
    );
}
