import { readProblem, type ProblemDetails } from "./problem";

const TOKEN_STORAGE_KEY = "ticket-management.access-token";

let accessToken = window.sessionStorage.getItem(TOKEN_STORAGE_KEY);

export interface LoginCredentials {
    username: string;
    password: string;
}

export interface LoginResponse {
    token: string;
    tokenType: "Bearer";
}

export class ApiError extends Error {
    readonly status: number;
    readonly problem: ProblemDetails;

    constructor(problem: ProblemDetails) {
        super(problem.detail ?? problem.title);
        this.name = "ApiError";
        this.status = problem.status;
        this.problem = problem;
    }
}

export function getAccessToken(): string | null {
    return accessToken;
}

export function setAccessToken(token: string | null): void {
    accessToken = token;
    if (token === null) {
        window.sessionStorage.removeItem(TOKEN_STORAGE_KEY);
        return;
    }
    window.sessionStorage.setItem(TOKEN_STORAGE_KEY, token);
}

export async function login(credentials: LoginCredentials): Promise<LoginResponse> {
    return apiRequest<LoginResponse>("/api/auth/login", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(credentials),
    });
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
    const headers = new Headers(init.headers);
    if (accessToken !== null) {
        headers.set("Authorization", `Bearer ${accessToken}`);
    }

    const response = await fetch(path, {
        ...init,
        headers,
    });
    if (!response.ok) {
        throw new ApiError(await readProblem(response));
    }
    return response.json() as Promise<T>;
}
