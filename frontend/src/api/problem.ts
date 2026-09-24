export interface ProblemDetails {
    title: string;
    status: number;
    detail?: string;
    errors?: Record<string, string>;
}

export async function readProblem(response: Response): Promise<ProblemDetails> {
    const body = await response.json().catch(() => null) as Partial<ProblemDetails> | null;
    return {
        title: body?.title ?? "Request failed",
        status: body?.status ?? response.status,
        detail: body?.detail,
        errors: body?.errors,
    };
}
