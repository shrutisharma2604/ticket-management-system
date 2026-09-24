import { apiRequest } from "./client";

export type TicketPriority = "LOW" | "MEDIUM" | "HIGH";
export type TicketStatus = "OPEN" | "IN_PROGRESS" | "RESOLVED" | "CLOSED" | "CANCELLED";

export interface CreateTicketRequest {
    title: string;
    description: string;
    priority: TicketPriority;
    assignee?: string;
}

export interface TicketResponse {
    id: string;
    title: string;
    description: string;
    priority: TicketPriority;
    assignee: string | null;
    status: TicketStatus;
    createdAt: string;
    updatedAt: string;
}

export interface TicketSummary {
    id: string;
    title: string;
    priority: TicketPriority;
    assignee: string | null;
    status: TicketStatus;
}

export interface TicketPage {
    content: TicketSummary[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
}

export interface CommentResponse {
    id: string;
    body: string;
    createdAt: string;
}

export interface TicketDetailResponse extends TicketResponse {
    comments: CommentResponse[];
}

export interface UpdateTicketRequest {
    title?: string;
    description?: string;
    priority?: TicketPriority;
    assignee?: string | null;
}

export function createTicket(request: CreateTicketRequest): Promise<TicketResponse> {
    return apiRequest<TicketResponse>("/api/tickets", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(request),
    });
}

export function listTickets(
    page = 0,
    size = 20,
    keyword = "",
    status: TicketStatus | "" = "",
): Promise<TicketPage> {
    const params = new URLSearchParams({
        page: String(page),
        size: String(size),
    });
    if (keyword.trim() !== "") {
        params.set("q", keyword.trim());
    }
    if (status !== "") {
        params.set("status", status);
    }
    return apiRequest<TicketPage>(`/api/tickets?${params.toString()}`);
}

export function getTicket(ticketId: string): Promise<TicketDetailResponse> {
    return apiRequest<TicketDetailResponse>(`/api/tickets/${ticketId}`);
}

export function transitionTicket(ticketId: string, status: TicketStatus): Promise<TicketResponse> {
    return apiRequest<TicketResponse>(`/api/tickets/${ticketId}/status`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({ status }),
    });
}

export function updateTicket(ticketId: string, request: UpdateTicketRequest): Promise<TicketResponse> {
    return apiRequest<TicketResponse>(`/api/tickets/${ticketId}`, {
        method: "PATCH",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(request),
    });
}

export function addComment(ticketId: string, body: string): Promise<CommentResponse> {
    return apiRequest<CommentResponse>(`/api/tickets/${ticketId}/comments`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({ body }),
    });
}
