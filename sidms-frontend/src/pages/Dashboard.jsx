import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { get } from "../utils/apiClient";
import "./Dashboard.css";

function Dashboard() {
    const navigate = useNavigate();
    const { username, role, token, logout } = useAuth();
    const [profileLoaded, setProfileLoaded] = useState(false);
    const [checking, setChecking] = useState(true);

    /** Decode the JWT payload to extract extra claims (role, exp, etc.) */
    const decodeToken = () => {
        if (!token) return null;
        try {
            const payload = token.split(".")[1];
            return JSON.parse(atob(payload));
        } catch {
            return null;
        }
    };

    const decoded = decodeToken();

    // Prefer decoded role from JWT, fall back to context role
    const displayRole = decoded?.role || decoded?.authorities || role || "User";
    const displayUsername = decoded?.sub || username || "User";

    // Check if profile exists on load
    useEffect(() => {
        const checkProfile = async () => {
            try {
                const response = await get("/api/members/me");

                if (response.ok) {
                    setProfileLoaded(true);
                } else if (response.status === 404) {
                    // No profile — redirect to complete profile
                    navigate("/complete-profile", { replace: true });
                    return;
                }
            } catch {
                // Network error — still show dashboard
                setProfileLoaded(true);
            } finally {
                setChecking(false);
            }
        };

        checkProfile();
    }, [navigate]);

    const handleLogout = () => {
        logout();
        navigate("/login");
    };

    // Show loading while checking profile
    if (checking) {
        return (
            <div className="dashboard-page">
                <nav className="dashboard-navbar">
                    <div className="dashboard-navbar-brand">
                        <div className="dashboard-navbar-logo">🔐</div>
                        <span>SIDMS</span>
                    </div>
                </nav>
                <main className="dashboard-content">
                    <div className="dashboard-welcome-card">
                        <div className="dashboard-welcome-icon">⏳</div>
                        <h1>Loading…</h1>
                        <p>Checking your profile status.</p>
                    </div>
                </main>
            </div>
        );
    }

    return (
        <div className="dashboard-page">
            {/* ── Navbar ──────────────────────────────── */}
            <nav className="dashboard-navbar">
                <div className="dashboard-navbar-brand">
                    <div className="dashboard-navbar-logo">🔐</div>
                    <span>SIDMS</span>
                </div>
                <div className="dashboard-navbar-actions">
                    <span className="dashboard-role-badge">{displayRole}</span>
                    <button className="dashboard-logout-btn" onClick={handleLogout}>
                        Sign Out
                    </button>
                </div>
            </nav>

            {/* ── Main Content ───────────────────────── */}
            <main className="dashboard-content">
                <div className="dashboard-welcome-card">
                    <div className="dashboard-welcome-icon">👋</div>
                    <h1>Welcome, {displayUsername}!</h1>
                    <p>
                        You are signed in to the Secure IAC Data Management System.
                        Your session is active and authenticated.
                    </p>

                    {/* Info Grid */}
                    <div className="dashboard-info-grid">
                        <div className="dashboard-info-item">
                            <div className="dashboard-info-label">Username</div>
                            <div className="dashboard-info-value">{displayUsername}</div>
                        </div>
                        <div className="dashboard-info-item">
                            <div className="dashboard-info-label">Role</div>
                            <div className="dashboard-info-value">{displayRole}</div>
                        </div>
                        {decoded?.exp && (
                            <div className="dashboard-info-item" style={{ gridColumn: "1 / -1" }}>
                                <div className="dashboard-info-label">Token Expires</div>
                                <div className="dashboard-info-value">
                                    {new Date(decoded.exp * 1000).toLocaleString()}
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </main>
        </div>
    );
}

export default Dashboard;
