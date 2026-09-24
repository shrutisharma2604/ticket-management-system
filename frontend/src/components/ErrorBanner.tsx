import { ApiError } from "../api/client";

interface ErrorBannerProps {
    error: ApiError | string | null;
}

export function ErrorBanner({ error }: ErrorBannerProps) {
    if (error === null) {
        return null;
    }
    if (typeof error === "string") {
        return <p role="alert">{error}</p>;
    }

    const fieldErrors = Object.entries(error.problem.errors ?? {});
    return (
        <div role="alert">
            <p>{error.problem.detail ?? error.problem.title}</p>
            {fieldErrors.length > 0 && (
                <ul>
                    {fieldErrors.map(([field, message]) => (
                        <li key={field}>{message}</li>
                    ))}
                </ul>
            )}
        </div>
    );
}
