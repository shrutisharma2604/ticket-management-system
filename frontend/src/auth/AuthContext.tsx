import {
    createContext,
    type ReactNode,
    useCallback,
    useContext,
    useMemo,
    useState,
} from "react";

import {
    getAccessToken,
    login as requestLogin,
    type LoginCredentials,
    setAccessToken,
} from "../api/client";

interface AuthContextValue {
    isAuthenticated: boolean;
    login: (credentials: LoginCredentials) => Promise<void>;
    logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [token, setToken] = useState<string | null>(() => getAccessToken());

    const login = useCallback(async (credentials: LoginCredentials) => {
        const response = await requestLogin(credentials);
        setAccessToken(response.token);
        setToken(response.token);
    }, []);

    const logout = useCallback(() => {
        setAccessToken(null);
        setToken(null);
    }, []);

    const value = useMemo(
        () => ({
            isAuthenticated: token !== null,
            login,
            logout,
        }),
        [login, logout, token],
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error("useAuth must be used within an AuthProvider");
    }
    return context;
}
