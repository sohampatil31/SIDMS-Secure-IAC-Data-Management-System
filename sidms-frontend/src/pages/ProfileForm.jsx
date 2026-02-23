import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "./ProfileForm.css";

const SKILL_OPTIONS = [
    "Java", "Python", "JavaScript", "React", "Spring Boot",
    "Node.js", "SQL", "MongoDB", "Docker", "AWS",
    "Machine Learning", "Cybersecurity", "Git", "REST API", "TypeScript",
];

function ProfileForm() {
    const { token } = useAuth();
    const navigate = useNavigate();

    const [form, setForm] = useState({
        firstName: "",
        lastName: "",
        email: "",
        mobile: "",
        college: "",
        degree: "",
    });
    const [skills, setSkills] = useState([]);
    const [resume, setResume] = useState(null);
    const [resumeName, setResumeName] = useState("");
    const [message, setMessage] = useState("");
    const [messageType, setMessageType] = useState("");
    const [loading, setLoading] = useState(false);
    const [skillsOpen, setSkillsOpen] = useState(false);

    /* ── Handlers ──────────────────────────────── */

    const handleChange = (e) => {
        setForm({ ...form, [e.target.name]: e.target.value });
    };

    const toggleSkill = (skill) => {
        setSkills((prev) =>
            prev.includes(skill) ? prev.filter((s) => s !== skill) : [...prev, skill]
        );
    };

    const handleFileChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            setResume(file);
            setResumeName(file.name);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setMessage("");
        setMessageType("");
        setLoading(true);

        try {
            const payload = {
                fullName: `${form.firstName} ${form.lastName}`.trim(),
                email: form.email,
                phoneNumber: form.mobile,
                address: form.college,
                resumeUrl: form.degree,
            };

            const response = await fetch("http://localhost:8080/api/members/me", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify(payload),
            });

            if (response.ok) {
                setMessage("Profile saved successfully. Redirecting to dashboard…");
                setMessageType("success");
                setTimeout(() => navigate("/dashboard"), 1500);
            } else {
                const data = await response.json().catch(() => ({}));
                setMessage(data.message || "Failed to save profile. Please try again.");
                setMessageType("error");
            }
        } catch {
            setMessage("Unable to reach the server. Please try again later.");
            setMessageType("error");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="profile-page">
            <div className="profile-card">
                {/* Header */}
                <div className="profile-header">
                    <div className="profile-header-icon">📋</div>
                    <h1>Profile Application</h1>
                    <p>Complete your internship profile details below</p>
                </div>

                <form className="profile-form" onSubmit={handleSubmit}>
                    {/* ── Name Row ──────────────────────── */}
                    <div className="profile-row">
                        <div className="form-group">
                            <label htmlFor="firstName">First Name</label>
                            <input
                                id="firstName"
                                name="firstName"
                                type="text"
                                placeholder="John"
                                value={form.firstName}
                                onChange={handleChange}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label htmlFor="lastName">Last Name</label>
                            <input
                                id="lastName"
                                name="lastName"
                                type="text"
                                placeholder="Doe"
                                value={form.lastName}
                                onChange={handleChange}
                                required
                            />
                        </div>
                    </div>

                    {/* ── Contact Row ───────────────────── */}
                    <div className="profile-row">
                        <div className="form-group">
                            <label htmlFor="email">Email</label>
                            <input
                                id="email"
                                name="email"
                                type="email"
                                placeholder="john@example.com"
                                value={form.email}
                                onChange={handleChange}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label htmlFor="mobile">Mobile Number</label>
                            <input
                                id="mobile"
                                name="mobile"
                                type="tel"
                                placeholder="+91 9876543210"
                                value={form.mobile}
                                onChange={handleChange}
                                required
                            />
                        </div>
                    </div>

                    {/* ── Education Row ─────────────────── */}
                    <div className="profile-row">
                        <div className="form-group">
                            <label htmlFor="college">College Name</label>
                            <input
                                id="college"
                                name="college"
                                type="text"
                                placeholder="University of Mumbai"
                                value={form.college}
                                onChange={handleChange}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label htmlFor="degree">Degree</label>
                            <input
                                id="degree"
                                name="degree"
                                type="text"
                                placeholder="B.Tech Computer Science"
                                value={form.degree}
                                onChange={handleChange}
                                required
                            />
                        </div>
                    </div>

                    {/* ── Skills Multi-Select ───────────── */}
                    <div className="form-group">
                        <label>Skills</label>
                        <div
                            className={`skills-dropdown ${skillsOpen ? "open" : ""}`}
                            onClick={() => setSkillsOpen(!skillsOpen)}
                            onKeyDown={(e) => e.key === "Enter" && setSkillsOpen(!skillsOpen)}
                            role="listbox"
                            tabIndex={0}
                            aria-expanded={skillsOpen}
                        >
                            <div className="skills-selected">
                                {skills.length === 0
                                    ? "Select your skills…"
                                    : skills.join(", ")}
                            </div>
                            <span className="skills-arrow">{skillsOpen ? "▲" : "▼"}</span>
                        </div>
                        {skillsOpen && (
                            <div className="skills-options">
                                {SKILL_OPTIONS.map((skill) => (
                                    <label key={skill} className="skills-option">
                                        <input
                                            type="checkbox"
                                            checked={skills.includes(skill)}
                                            onChange={() => toggleSkill(skill)}
                                        />
                                        <span>{skill}</span>
                                    </label>
                                ))}
                            </div>
                        )}
                    </div>

                    {/* ── Resume Upload ────────────────── */}
                    <div className="form-group">
                        <label htmlFor="resume">Resume</label>
                        <div className="file-upload-wrapper">
                            <label className="file-upload-btn" htmlFor="resume">
                                {resumeName || "Choose file…"}
                            </label>
                            <input
                                id="resume"
                                type="file"
                                accept=".pdf,.doc,.docx"
                                onChange={handleFileChange}
                                className="file-upload-input"
                            />
                            {resumeName && (
                                <span className="file-upload-name">✓ {resumeName}</span>
                            )}
                        </div>
                    </div>

                    {/* ── Message ──────────────────────── */}
                    {message && (
                        <div className={`login-message ${messageType}`}>{message}</div>
                    )}

                    {/* ── Submit ───────────────────────── */}
                    <button
                        type="submit"
                        className="profile-submit-btn"
                        disabled={loading}
                    >
                        {loading ? "Saving…" : "Save Profile"}
                    </button>
                </form>
            </div>
        </div>
    );
}

export default ProfileForm;
