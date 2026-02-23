import { createContext, useContext, useState, useMemo } from "react";

const AuthContext = createContext(null);

/**
 * Provides authentication state and helpers to the entire app.
 *
 * Stored keys in localStorage:
 *   token, tokenType, role, username
 */
export function AuthProvider({ children }) {
    const [token, setToken] = useState(() => localStorage.getItem("token"));
    const [tokenType, setTokenType] = useState(() => localStorage.getItem("tokenType") || "Bearer");
    const [role, setRole] = useState(() => localStorage.getItem("role") || "");
    const [username, setUsername] = useState(() => localStorage.getItem("username") || "");

    /**
     * Persist auth data after successful OTP verification.
     * @param {{ token: string, tokenType?: string, role?: string, username?: string }} authData
     */
    const login = (authData) => {
        const t = authData.token;
        const tt = authData.tokenType || "Bearer";
        const r = authData.role || "";
        const u = authData.username || "";

        localStorage.setItem("token", t);
        localStorage.setItem("tokenType", tt);
        localStorage.setItem("role", r);
        localStorage.setItem("username", u);

        setToken(t);
        setTokenType(tt);
        setRole(r);
        setUsername(u);
    };

    /** Clear all auth data and redirect responsibility is on the caller. */
    const logout = () => {
        localStorage.removeItem("token");
        localStorage.removeItem("tokenType");
        localStorage.removeItem("role");
        localStorage.removeItem("username");

        setToken(null);
        setTokenType("Bearer");
        setRole("");
        setUsername("");
    };

    /** Returns true when a JWT token exists in state. */
    const isAuthenticated = () => !!token;

    // Memoize value to avoid unnecessary re-renders
    const value = useMemo(
        () => ({ token, tokenType, role, username, login, logout, isAuthenticated }),
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [token, tokenType, role, username]
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/**
 * Hook to access auth context.
 * Must be used within an <AuthProvider>.
 */
export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) {
        throw new Error("useAuth must be used within an <AuthProvider>");
    }
    return ctx;
}
