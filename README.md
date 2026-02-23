# 🛡️ SIDMS — Secure IAC Data Management System

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)

**SIDMS** is a full-stack, production-grade **identity and profile management system** built with security at its core. It delivers enterprise-level features including email verification, OTP-based multi-factor authentication, JWT stateless sessions, role-based access control (RBAC), AES-256 field-level encryption, comprehensive audit logging, and secure key management through environment variables.

---

## 📑 Table of Contents

- [Project Overview](#-project-overview)
- [Tech Stack](#-tech-stack)
- [Architecture Overview](#-architecture-overview)
- [Features](#-features)
- [Security Features](#-security-features)
- [Database Schema Overview](#-database-schema-overview)
- [Setup Instructions](#-setup-instructions)
- [Environment Variables](#-environment-variables)
- [API Endpoints](#-api-endpoints)
- [Folder Structure](#-folder-structure)
- [Future Enhancements](#-future-enhancements)

---

## 🌐 Project Overview

SIDMS (Secure IAC Data Management System) is a comprehensive identity and access management solution designed for organizations that require robust, enterprise-grade security for managing user identities and sensitive profile data.

### Key Highlights

- **Multi-Factor Authentication** — Two-step login with password verification followed by a one-time password (OTP) delivered via email.
- **Email Verification** — Token-based email verification ensures only valid users can activate their accounts.
- **JWT Stateless Sessions** — Industry-standard JSON Web Tokens for scalable, stateless authentication.
- **Role-Based Access Control** — Fine-grained permissions with Admin, Manager, and Member roles.
- **Field-Level Encryption** — AES-256-GCM encryption protects sensitive profile data at rest.
- **Audit Trail** — Every security event is logged with user, action, status, IP address, and timestamp.
- **Secure Key Management** — All secrets (AES keys, JWT secrets, SMTP credentials) are managed via environment variables — never hardcoded.

---

## 🧰 Tech Stack

### Backend

| Technology          | Version | Purpose                              |
|---------------------|---------|--------------------------------------|
| Java                | 21      | Core language                        |
| Spring Boot         | 3.2.5   | Application framework                |
| Spring Security     | 6.x     | Authentication & authorization       |
| Spring Data JPA     | —       | ORM / data access (Hibernate)        |
| MySQL               | 8.x     | Relational database                  |
| JavaMailSender      | —       | Email dispatch (SMTP)                |
| JJWT                | 0.12.5  | JWT creation & validation            |
| BCrypt              | —       | Password hashing                     |
| Lombok              | 1.18.42 | Boilerplate reduction                |
| SpringDoc OpenAPI   | 2.3.0   | Swagger UI / API documentation       |

### Frontend

| Technology          | Version | Purpose                              |
|---------------------|---------|--------------------------------------|
| React               | 19      | UI framework                         |
| Vite                | 7.x     | Build tool & dev server              |
| React Router DOM    | 7.x     | Client-side routing                  |
| Context API         | —       | Global state management              |
| Fetch API           | —       | HTTP client for API calls            |

---

## 🏛️ Architecture Overview

### Layered Architecture

SIDMS follows a clean, layered separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                     React Frontend                          │
│  (Pages → Context API → Fetch API → REST Endpoints)         │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP (JSON)
┌──────────────────────────▼──────────────────────────────────┐
│                    Controller Layer                         │
│  AuthController · MemberProfileController · AdminController │
├─────────────────────────────────────────────────────────────┤
│                     Service Layer                           │
│  AuthService · MemberProfileService · OtpService            │
│  EmailService · AuditLogService · VerificationTokenService  │
├─────────────────────────────────────────────────────────────┤
│                   Repository Layer (JPA)                    │
│  UserRepository · OtpRepository · MemberProfileRepository   │
│  AuditLogRepository · VerificationTokenRepository           │
├─────────────────────────────────────────────────────────────┤
│                     MySQL Database                          │
│  users · otps · member_profiles · audit_logs                │
│  verification_tokens                                        │
└─────────────────────────────────────────────────────────────┘
```

### Security & Authentication Flow

```
 ┌──────────┐     ┌─────────────────┐     ┌───────────┐     ┌─────────┐     ┌──────────────────┐
 │ Register │────▶│ Email Sent with │────▶│  Verify   │────▶│  Login  │────▶│  OTP Sent via    │
 │          │     │ Verification    │     │  Email    │     │ (Creds) │     │  Email           │
 └──────────┘     │ Token           │     └───────────┘     └─────────┘     └────────┬─────────┘
                  └─────────────────┘                                                │
                                                                                     ▼
                                                                         ┌───────────────────┐
     ┌──────────────────┐     ┌─────────────┐                            │  Verify OTP       │
     │ Profile Complete │◀────│ JWT Issued  │◀───────────────────────────│  (Hashed Check)   │
     │ (Encrypted Data) │     │ with Roles  │                            └───────────────────┘
     └──────────────────┘     └─────────────┘
```

### Encryption Flow

```
Plaintext Data ──▶ AES-256-GCM Encrypt ──▶ Base64 Encoded ──▶ Stored in DB
                        │
                   SIDMS_AES_KEY
                  (env variable)

Stored Data ──▶ Base64 Decode ──▶ AES-256-GCM Decrypt ──▶ Plaintext Response
                        │
                   SIDMS_AES_KEY
                  (env variable)
```

---

## ✨ Features

| Feature                        | Description                                                         |
|--------------------------------|---------------------------------------------------------------------|
| **User Registration**          | New users register with username, email, and password               |
| **Email Verification**         | Token-based verification link sent to the user's email              |
| **Secure Login**               | Credential validation with BCrypt password comparison               |
| **OTP via Email**              | 6-digit OTP sent to the user's registered email after login         |
| **Hashed OTP Storage**         | OTPs are BCrypt-hashed before persistence — never stored in plain   |
| **JWT Authentication**         | Stateless authentication with role-embedded JWT tokens              |
| **Role-Based Route Protection**| Endpoints protected by `@PreAuthorize` with role checks             |
| **Profile Completion**         | Members create and manage their encrypted profile data              |
| **AES-256 Encryption**         | Sensitive fields (Aadhaar, PAN, phone) encrypted at the field level |
| **Audit Logging**              | All security events recorded with user, action, IP, and timestamp   |
| **Swagger UI**                 | Interactive API documentation at `/swagger-ui.html`                 |

---

## 🔒 Security Features

### Password Security
- **BCrypt hashing** with adaptive cost factor for all user passwords.
- Passwords are never logged or returned in API responses.

### OTP Security
- OTPs are **BCrypt-hashed** before storage — raw OTP is never persisted.
- **5-minute expiry** enforced at the service layer.
- Used OTPs are invalidated immediately after successful verification.

### Token Security
- **JWT tokens** signed with HMAC-SHA256 using a configurable secret.
- Tokens include role claims and have a configurable expiration (default: 1 hour).
- Verification tokens are single-use and time-limited.

### Data Protection
- **AES-256-GCM** field-level encryption for sensitive profile data.
- Encryption keys managed exclusively via environment variables.
- Data is encrypted before persistence and decrypted on retrieval.

### Access Control
- **RBAC** enforced at the controller level with Spring Security's `@PreAuthorize`.
- Three roles: `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_MEMBER`.
- Foreign key constraints ensure data integrity across all relationships.

### Infrastructure Security
- **CORS** configuration with configurable allowed origins.
- Environment-based secret management — no credentials in source code.
- Global exception handler prevents stack trace leakage to clients.

---

## 🗄️ Database Schema Overview

```sql
┌─────────────────────┐       ┌─────────────────────────┐
│       users         │       │   verification_tokens   │
├─────────────────────┤       ├─────────────────────────┤
│ id (PK)             │◀──┐   │ id (PK)                 │
│ username (UNIQUE)   │   │   │ token (UNIQUE)          │
│ email (UNIQUE)      │   └───│ user_id (FK → users)    │
│ password (hashed)   │       │ expiry_date             │
│ role (ENUM)         │       │ used (BOOLEAN)          │
│ enabled (BOOLEAN)   │       └─────────────────────────┘
│ created_at          │
└──────────┬──────────┘       ┌─────────────────────────┐
           │                  │          otps           │
           │                  ├─────────────────────────┤
           ├──────────────────│ id (PK)                 │
           │                  │ username                │
           │                  │ otp_hash (BCrypt)       |
           │                  │ expiry_time             │
           │                  │ used (BOOLEAN)          │
           │                  └─────────────────────────┘
           │
           │                  ┌─────────────────────────┐
           │                  │    member_profiles      │
           │                  ├─────────────────────────┤
           ├──────────────────│ id (PK)                 │
           │                  │ user_id (FK → users)    │
           │                  │ full_name               │
           │                  │ email (encrypted)       │
           │                  │ phone (encrypted)       │
           │                  │ aadhaar (encrypted)     │
           │                  │ pan (encrypted)         │
           │                  │ address                 │
           │                  │ created_at / updated_at │
           │                  └─────────────────────────┘
           │
           │                  ┌─────────────────────────┐
           │                  │      audit_logs         │
           │                  ├─────────────────────────┤
           └──────────────────│ id (PK)                 │
                              │ username                │
                              │ action                  │
                              │ status                  │
                              │ ip_address              │
                              │ details                 │
                              │ timestamp               │
                              └─────────────────────────┘
```

---

## 🚀 Setup Instructions

### Prerequisites

- **Java 21** — [Download](https://adoptium.net/)
- **MySQL 8+** — [Download](https://dev.mysql.com/downloads/)
- **Node.js 18+** — [Download](https://nodejs.org/) (for the frontend)
- **Maven** — included via Maven Wrapper (`./mvnw`)

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/SIDMS.git
cd SIDMS
```

### 2. Create the Database

```sql
CREATE DATABASE sidms_db;
```

### 3. Set Environment Variables

```bash
# AES-256 Encryption Key (required for persistent encryption)
export SIDMS_AES_KEY=$(openssl rand -base64 32)

# Email / SMTP Credentials (required for OTP & verification emails)
export SIDMS_MAIL_USERNAME="your-email@gmail.com"
export SIDMS_MAIL_PASSWORD="your-gmail-app-password"
```

> [!IMPORTANT]
> For Gmail, use an [App Password](https://support.google.com/accounts/answer/185833), not your regular Gmail password.

### 4. Run the Backend

```bash
./mvnw spring-boot:run
```

The API starts on **http://localhost:8080**.  
Swagger UI is available at **http://localhost:8080/swagger-ui.html**.

### 5. Run the Frontend

```bash
cd sidms-frontend
npm install
npm run dev
```

The frontend starts on **http://localhost:5173**.

---

## 🔐 Environment Variables

All secrets are injected via environment variables. **No credentials should ever be committed to version control.**

| Variable              | Description                                      | Required | Default                    |
|-----------------------|--------------------------------------------------|----------|----------------------------|
| `SIDMS_AES_KEY`       | Base64-encoded 32-byte AES encryption key        | Yes*     | _(ephemeral if unset)_     |
| `SIDMS_MAIL_USERNAME` | SMTP email address for sending OTP/verification  | Yes      | —                          |
| `SIDMS_MAIL_PASSWORD` | SMTP email password (App Password for Gmail)     | Yes      | —                          |
| `SIDMS_DB_USERNAME`   | MySQL database username                          | No       | `root`                     |
| `SIDMS_DB_PASSWORD`   | MySQL database password                          | No       | `root`                     |
| `SIDMS_JWT_SECRET`    | Base64-encoded JWT signing key                   | No       | _(default embedded key)_   |
| `SIDMS_CORS_ORIGINS`  | Comma-separated allowed CORS origins             | No       | `http://localhost:5173`    |

> **\*** If `SIDMS_AES_KEY` is not set, a random ephemeral key is generated at startup. Encrypted data will **not survive application restarts**.

### Setting Environment Variables (macOS / Linux)

**Temporary (current shell session):**

```bash
export SIDMS_AES_KEY="your-base64-key"
export SIDMS_MAIL_USERNAME="your-email@gmail.com"
export SIDMS_MAIL_PASSWORD="your-app-password"
```

**Persistent (add to `~/.zshrc` or `~/.bashrc`):**

```bash
echo 'export SIDMS_AES_KEY="your-base64-key"' >> ~/.zshrc
echo 'export SIDMS_MAIL_USERNAME="your-email@gmail.com"' >> ~/.zshrc
echo 'export SIDMS_MAIL_PASSWORD="your-app-password"' >> ~/.zshrc
source ~/.zshrc
```

### Generating an AES Key

```bash
openssl rand -base64 32
```

---

## 📡 API Endpoints

### Authentication (`/api/auth`) — Public

| Method | Endpoint               | Description                                        |
|--------|------------------------|----------------------------------------------------|
| POST   | `/api/auth/register`   | Register a new user (account disabled until verified) |
| GET    | `/api/auth/verify`     | Verify email via token (`?token=...`)              |
| POST   | `/api/auth/login`      | Step 1 — Validate credentials, trigger OTP via email |
| POST   | `/api/auth/verify-otp` | Step 2 — Submit OTP, receive JWT token             |

### Member Profiles (`/api/members`) — Authenticated

| Method | Endpoint              | Access                           | Description                    |
|--------|-----------------------|----------------------------------|--------------------------------|
| POST   | `/api/members/me`     | `ROLE_MEMBER`                    | Create own profile             |
| GET    | `/api/members/me`     | `ROLE_MEMBER`                    | View own profile               |
| GET    | `/api/members`        | All authenticated                | List profiles (role-filtered)  |
| GET    | `/api/members/{id}`   | All authenticated                | Get profile by ID              |
| POST   | `/api/members`        | `ROLE_ADMIN`                     | Create a profile (admin)       |
| PUT    | `/api/members/{id}`   | Owner / Manager / Admin          | Update a profile               |
| DELETE | `/api/members/{id}`   | `ROLE_ADMIN`                     | Delete a profile               |

### Admin (`/api/admin`) — `ROLE_ADMIN` Only

| Method | Endpoint                 | Description              |
|--------|--------------------------|--------------------------|
| GET    | `/api/admin/users`       | List all users           |
| GET    | `/api/admin/users/{id}`  | Get user by ID           |
| PUT    | `/api/admin/assign-role` | Assign role to a user    |
| GET    | `/api/admin/audit-logs`  | View all audit logs      |

---

## 📁 Folder Structure

```
SIDMS/
├── pom.xml                              # Maven project configuration
├── mvnw / mvnw.cmd                      # Maven Wrapper
├── src/
│   └── main/
│       ├── java/com/sidms/
│       │   ├── SidmsApplication.java    # Application entry point
│       │   ├── config/
│       │   │   ├── CorsConfig.java      # CORS configuration
│       │   │   ├── DataLoader.java      # Seed data on first run
│       │   │   ├── OpenApiConfig.java   # Swagger/OpenAPI setup
│       │   │   └── SecurityConfig.java  # Spring Security filter chain
│       │   ├── controller/
│       │   │   ├── AuthController.java          # Registration, login, OTP, email verify
│       │   │   ├── MemberProfileController.java # Profile CRUD with RBAC
│       │   │   └── AdminController.java         # Admin operations & audit logs
│       │   ├── dto/                     # Request/Response data transfer objects
│       │   ├── entity/                  # JPA entities (User, Otp, MemberProfile, etc.)
│       │   ├── exception/              # Custom exceptions & global handler
│       │   ├── repository/             # Spring Data JPA repositories
│       │   ├── security/
│       │   │   ├── JwtAuthenticationFilter.java  # JWT validation filter
│       │   │   ├── JwtService.java               # JWT creation & parsing
│       │   │   └── CustomUserDetailsService.java # User loading for Spring Security
│       │   ├── service/                # Business logic layer
│       │   │   ├── AuthService.java
│       │   │   ├── MemberProfileService.java
│       │   │   ├── OtpService.java
│       │   │   ├── EmailService.java
│       │   │   ├── AuditLogService.java
│       │   │   ├── UserService.java
│       │   │   └── VerificationTokenService.java
│       │   └── util/
│       │       └── EncryptionService.java  # AES-256-GCM encrypt/decrypt
│       └── resources/
│           └── application.yml          # Application configuration
│
├── sidms-frontend/
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html
│   └── src/
│       ├── main.jsx                     # React entry point
│       ├── App.jsx                      # Root component & routing
│       ├── context/
│       │   └── AuthContext.jsx          # Authentication state management
│       ├── components/
│       │   └── ProtectedRoute.jsx       # Route guard (role-based)
│       ├── pages/
│       │   ├── Register.jsx             # User registration page
│       │   ├── VerifyEmail.jsx          # Email verification handler
│       │   ├── Login.jsx                # Login page
│       │   ├── OtpVerification.jsx      # OTP input page
│       │   ├── Dashboard.jsx            # User dashboard
│       │   └── ProfileForm.jsx          # Profile creation/editing
│       └── utils/
│           └── apiClient.js             # Centralized API client
```

---

## 🔮 Future Enhancements

- [ ] **Account Lockout** — Lock accounts after repeated failed login attempts
- [ ] **Rate Limiting** — Throttle API requests to prevent brute-force attacks
- [ ] **Refresh Tokens** — Implement token refresh for seamless session management
- [ ] **Cloud Deployment** — Docker containerization and deployment to AWS / Azure / GCP
- [ ] **Password Reset** — Self-service password recovery via email
- [ ] **Two-Factor App Support** — TOTP-based MFA via authenticator apps (Google Authenticator, Authy)
- [ ] **Admin Dashboard** — Web-based admin panel for user and audit log management

---

<p align="center">
  Built with ❤️ using <strong>Spring Boot</strong> and <strong>React</strong>
</p>
