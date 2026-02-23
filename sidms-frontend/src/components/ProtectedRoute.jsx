import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

/**
 * Decode the JWT payload and extract the role claim.
 * Returns null if the token is missing or malformed.
 */
function getRoleFromToken(token) {
    if (!token) return null;
    try {
        const payload = token.split(".")[1];
        const decoded = JSON.parse(atob(payload));
        return decoded.role || decoded.authorities || null;
    } catch {
        return null;
    }
}

/**
 * Wrapper that redirects unauthenticated users to /login.
 * Optionally restricts access by role(s).
 *
 * Usage:
 *   <ProtectedRoute>                       — any authenticated user
 *   <ProtectedRoute roles={["ROLE_ADMIN"]}> — admin only
 */
function ProtectedRoute({ children, roles }) {
    const { isAuthenticated, token } = useAuth();

    if (!isAuthenticated()) {
        return <Navigate to="/login" replace />;
    }

    // If specific roles are required, check the JWT claim
    if (roles && roles.length > 0) {
        const userRole = getRoleFromToken(token);
        if (!userRole || !roles.includes(userRole)) {
            return <Navigate to="/dashboard" replace />;
        }
    }

    return children;
}

export default ProtectedRoute;
