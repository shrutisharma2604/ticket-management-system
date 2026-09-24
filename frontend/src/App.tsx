import { BrowserRouter, Link, Navigate, Outlet, Route, Routes } from "react-router-dom";

import { AuthProvider, useAuth } from "./auth/AuthContext";
import { LoginPage } from "./pages/LoginPage";
import { TicketCreatePage } from "./pages/TicketCreatePage";
import { TicketDetailPage } from "./pages/TicketDetailPage";
import { TicketListPage } from "./pages/TicketListPage";

export function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <Routes>
                    <Route path="/login" element={<LoginPage />} />
                    <Route element={<ProtectedRoute />}>
                        <Route index element={<TicketListPage />} />
                        <Route path="/tickets/new" element={<TicketCreatePage />} />
                        <Route path="/tickets/:ticketId" element={<TicketDetailPage />} />
                    </Route>
                    <Route path="*" element={<Navigate to="/" replace />} />
                </Routes>
            </AuthProvider>
        </BrowserRouter>
    );
}

function ProtectedRoute() {
    const { isAuthenticated, logout } = useAuth();
    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }

    return (
        <>
            <header className="navbar navbar-dark bg-dark shadow-sm">
                <div className="container">
                    <Link className="navbar-brand fw-semibold" to="/">
                        Support Ticket Management
                    </Link>
                    <button className="btn btn-outline-light btn-sm" type="button" onClick={logout}>
                        Sign out
                    </button>
                </div>
            </header>
            <div className="container py-4">
                <Outlet />
            </div>
        </>
    );
}
