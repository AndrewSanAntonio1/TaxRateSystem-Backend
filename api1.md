# API Documentation

This project is a Flutter mobile app built around repository interfaces and dependency injection. The app currently uses local/offline implementations rather than a real backend API, but the architecture is already designed so a backend can be swapped in without changing the screens.

> **This file has two parts.**
>
> * **Part 1 — Mobile client architecture** (the original content, below): repository interfaces,
>   controllers/session layer, routing and the local/offline implementations the app ships with.
> * **Part 2 — Backend REST API contract** (at the end of this file): a mirror of
>   `TaxRateSystem-Backend/TaxRateSystem/API.md`, which is the authoritative copy and lives in the
>   backend repository. It defines every endpoint, the tax-code vocabulary, the seed catalogue and
>   the calculation rules this client must match.
>
> Read Part 1 to work on the app, Part 2 to integrate against the service.

---

# Part 1 — Mobile client architecture

## 1. App architecture

The composition root lives in:

- `lib/core/di/app_dependencies.dart`

Dependencies are assembled once and injected through `DependencyScope`, so screens depend on interfaces instead of concrete implementations.

### Core abstractions

- `AuthRepository`
- `HistoryRepository`
- `TaxReferenceRepository`
- `TaxCalculationRepository`

These abstractions are defined in `lib/domain/repositories/` and implemented under `lib/data/repositories/`.

---

## 2. Authentication API

### Interface

File: `lib/domain/repositories/auth_repository.dart`

```dart
abstract interface class AuthRepository {
  Future<AppUser?> getCurrentUser();

  Future<AppUser> signIn({
    required String email,
    required String password,
  });

  Future<void> signOut();
}
```

### Current implementation

File: `lib/data/repositories/local_auth_repository.dart`

This is the active implementation in the app today.

Behavior:
- Validates email format (`must contain @`)
- Validates minimum password length (`6` characters)
- Creates a user from the email via `AppUser.fromEmail()`
- Tracks the current signed-in user in memory
- Does not call a real remote API

#### Validation rules

- Email is trimmed and lowercased
- Password length must be at least `6`
- If invalid, it throws `AuthException`

#### Session state

The app-wide session is managed by:

- `lib/presentation/controllers/session_controller.dart`

This controller exposes:
- `user`
- `state`
- `errorMessage`
- `isSignedIn`
- `signIn()`
- `signOut()`
- `ensureLoaded()`

### User model

File: `lib/domain/models/app_user.dart`

```dart
class AppUser {
  const AppUser({
    required this.email,
    required this.displayName,
  });
}
```

`displayName` is derived from the email local part, for example:
- `jane.doe@gmail.com` -> `Jane Doe`

---

## 3. Tax calculation API

### Repository contract

File: `lib/domain/repositories/tax_calculation_repository.dart`

```dart
abstract interface class TaxCalculationRepository {
  Future<TaxCalculation> calculate({
    required TaxTypeId taxType,
    required double amount,
  });
}
```

### Current implementation

File: `lib/data/repositories/local_tax_calculation_repository.dart`

This app computes tax locally using a tax service and a static rule set. The calculation logic is not calling a backend service right now.

### Tax reference API

File: `lib/domain/repositories/tax_reference_repository.dart`

```dart
abstract interface class TaxReferenceRepository {
  Future<List<TaxTypeId>> getSupportedTaxTypes();
  Future<TaxDetailData?> getDetail(TaxTypeId taxType);
}
```

The default implementation provides a local static catalogue of tax types and reference details.

---

## 4. History API

File: `lib/domain/repositories/history_repository.dart`

```dart
abstract interface class HistoryRepository {
  Future<List<SavedCalculation>> getHistory();
  Future<void> add(SavedCalculation item);
  Future<void> clear();
}
```

Current implementation:
- `lib/data/repositories/in_memory_history_repository.dart`

This is an in-memory local history store, which means saved calculations are available only while the app instance is active unless a persistent backend/store is introduced later.

---

## 5. Profile image API / persistence

The profile avatar currently uses a local picker and stores the selected image in app preferences.

### Local behavior

File: `lib/profile/profile_screen.dart`

- Uses `FilePicker.platform.pickFiles()`
- Accepts only JPG/JPEG/PNG files
- Reads file bytes with `withData: true`
- Persists the image as base64 in `SharedPreferences`
- Restores the avatar on app startup/re-open

### Storage key

```dart
static const String _profileImageKey = 'profile_image_bytes_base64';
```

### Example persistence flow

```dart
final SharedPreferences prefs = await SharedPreferences.getInstance();
await prefs.setString(_profileImageKey, base64Encode(fileBytes));
```

### Restore flow

```dart
final String? encoded = prefs.getString(_profileImageKey);
if (encoded != null && encoded.isNotEmpty) {
  _pickedProfileImageBytes = base64Decode(encoded);
}
```

This is local-only persistence, not a server API.

---

## 6. Runtime contract summary

### Auth flow

1. User enters email and password
2. `SessionController.signIn()` calls the repository
3. `LocalAuthRepository.signIn()` validates values
4. `AppUser` is created and stored in session state
5. Profile screen reads user data from `SessionController`

### Tax flow

1. User selects a tax type and amount
2. `TaxCalculationRepository.calculate()` is called
3. Local tax rules compute the output
4. Result is displayed in the calculator/history flow

### History flow

1. Calculation is saved
2. `HistoryRepository.add()` is called
3. History is read back via `getHistory()`

---

## 7. Migration path to a real API

The current code is intentionally structured for backend replacement:

- screens depend on interfaces, not concrete implementations
- repositories are injected centrally from `AppDependencies`
- changing the implementation is a single composition-root change

For a production backend, the expected migration is:

- Replace `LocalAuthRepository` with an HTTP-backed auth service
- Replace `InMemoryHistoryRepository` with `sqflite` or API-backed persistence
- Replace static tax reference data with remote catalog data
- Keep the same repository contracts and controller layer

This preserves the existing UI while swapping the backend implementation behind the same interfaces.

---

## 8. Notes

- This app's repositories are still local/offline only — no HTTP client is wired yet. The service contract they will integrate against is **Part 2** of this file, mirrored from the backend repository; the bullets here describe the current offline behaviour.
- The “API” in this project is primarily the repository boundary and session logic used by the Flutter UI.
- The profile image feature is local persistence-only and is not connected to a server endpoint.

---

# Part 2 — Backend REST API contract

> **Mirrored from** `TaxRateSystem-Backend/TaxRateSystem/API.md` (authoritative copy; synced 2026-09-23 with the v1 contract revision).
> The text below is unchanged from that file, including its contract-status banner and its §21
> gap report, which state exactly which parts are still placeholders in the backend repository.

> **Contract status — read first.** This file is the **intended** API contract for the TaxRateSystem backend, and it is the single source of truth for the HTTP interface between the Spring Boot backend and the Flutter client. As of 2026-09-23 the repository contains **structure only**: `AuthController`, `TaxController`, `CalculationController`, `UserController`, their services, every DTO, every entity except `User#id`, and all four Flyway migrations are empty placeholders (the `.sql` files are 0 bytes). **Every endpoint in this file is therefore marked `Implementation status: NOT IMPLEMENTED`**, and §21 is the authoritative gap report with evidence. Nothing here describes running behaviour today.
>
> Two rules were followed while writing it:
> * **Nothing is silently invented.** Paths and schemas are the *project contract*: they were derived from the scaffolded class names (`RegisterRequest`, `PasswordResetVerifyRequest`, `TaxBracketResponse`, ...), the real configuration files (`application*.yml`, `build.gradle`, `.env`), and the Flutter client that has to consume them (`../taxratesystem_mobile`). Where neither exists, the decision is called out in §21 instead of being presented as fact.
> * **Configuration is quoted, not invented.** Ports, token lifetimes, OTP rules and mail settings come from real files and are marked as such.

| | |
|---|---|
| API version | `v1` — all paths prefixed `/api/v1` |
| Document status | Contract for implementation (not yet enforced by code) |
| Last verified against source | 2026-09-23, backend commit `fadf5e2` |
| Intended consumers | Flutter mobile app (`taxratesystem_mobile`), future web/admin clients |

---

## 1. Overview

**Purpose.** TaxRateSystem is a Philippine tax information and calculator system. The API allows a user to:

* register and log in (JWT-based);
* browse the supported Philippine tax types with their rules, bracket tables and worked examples;
* calculate a tax from a taxable amount;
* view, save and delete their own calculation history;
* manage their profile, address and notification preferences.

**Backend technology** (from `build.gradle`, `application*.yml`):

| Concern | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1.1 (Spring Web MVC) |
| Persistence | Spring Data JPA / Hibernate (`ddl-auto: validate`) |
| Database | MySQL 8 (active profile `dev`; driver `com.mysql.cj.jdbc.Driver`) |
| Migrations | Flyway (`classpath:db/migration`, `baseline-on-migrate: true`) |
| Security | Spring Security with JWT bearer tokens *(planned)* |
| Password hashing | BCrypt *(planned)* |
| Mail | Spring Mail over Gmail SMTP `smtp.gmail.com:587`, STARTTLS |
| Validation | Jakarta Bean Validation |
| Build | Gradle wrapper (`gradlew`) |

**API version.** Every endpoint is versioned by path: `/api/v1/...`. No other version exists; if a breaking change is ever required, a sibling `/api/v2` prefix is added (§20).

**Authentication mechanism.** Stateless JWT bearer tokens (`Authorization: Bearer <JWT>`). `/auth/login` issues an access token (short-lived) and a refresh token (long-lived, persisted, rotated). See §3 and §6.

**Content type.** `application/json; charset=UTF-8` for every request and response that carries a body. `204 No Content` responses carry no body.

**Base URL.** Production and development URLs are listed in §2.

**Date/time format.** ISO-8601. Timestamps in payloads use UTC with the `Z` designator, e.g. `2026-09-23T13:00:00Z`; date-only fields use `YYYY-MM-DD` (e.g. `1996-04-17`). The database session timezone is `Asia/Manila` (`application-dev.yml`), so stored values are the same instants rendered in Philippine local time — clients must parse the offset and never assume a timezone. *(Contract: no Jackson date configuration exists yet.)*

**Currency format.** Philippine peso (PHP), JSON **number** with up to two decimals — `62500.00`, never `"₱62,500"`. Calculation responses state `"currency": "PHP"`. Peso signs, thousands separators and en-dashes are **display** concerns owned by the client.

**Philippine-specific considerations.**

* Tax rules follow Philippine law: TRAIN Law for personal income (schedule effective 2023-01-01), VAT / percentage tax / withholding / estate (2018-01-01), CREATE Law for corporate income (2020-07-01), NIRC for capital gains and documentary stamp, Local Government Code for real property tax.
* The system is single-currency (PHP) and single-country.
* The canonical verification case used throughout this document is **₱600,000 taxable income → ₱62,500 personal income tax** (₱22,500 base + 20% of the ₱200,000 excess).
* The scaffolded model contains no TIN, RDO or taxpayer-registration fields; do not expect them.

---

## 2. Base URL

| Environment | Base URL |
|---|---|
| Production | `https://api.taxratesystem.example.com/api/v1` |
| Local development | `http://localhost:8080/api/v1` |
| Android emulator (host loopback) | `http://10.0.2.2:8080/api/v1` |
| Physical device on the same LAN | `http://<host-lan-ip>:8080/api/v1` |

Notes:

* The production host is an illustrative placeholder: this repository contains **no production deployment configuration**. The only real server settings are `server.port: 8080` and `server.address: 0.0.0.0` (`application.yml`).
* HTTPS is required outside local development. The application refuses to start without a real `JWT_SECRET` (the property has no default).

---

## 3. Authentication

**Expected flow.**

1. The user registers — `POST /auth/register`.
2. The user logs in — `POST /auth/login`.
3. The backend validates the credentials against the stored BCrypt hash.
4. The backend returns a JWT access token (plus a refresh token).
5. The client stores the tokens securely.
6. The client sends `Authorization: Bearer <JWT>` on protected endpoints.

**Token format.** JWT (RFC 7519), three dot-separated Base64URL parts. The scaffold declares `security/JwtService`, `security/JwtAuthenticationFilter` and `auth/enums/TokenType` (all empty), and the configuration keys below. The concrete claim set is part of this contract:

| Claim | Meaning |
|---|---|
| `sub` | user e-mail (the login identifier) |
| `uid` | numeric user id |
| `iat` / `exp` | issued-at / expiry, epoch seconds |

Tokens are signed with the secret in the `JWT_SECRET` environment variable. **Never** log, return or commit this value.

**Token usage.**

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqdWFuQGV4YW1wbGUuY29tIn0...
Content-Type: application/json
```

**Token expiration.** Configurable — do not hard-code:

| Property | Environment variable | Default value (from real config) |
|---|---|---|
| `jwt.access-token-expiration` | `JWT_ACCESS_EXPIRATION` | `900000` ms = **15 minutes** |
| `jwt.refresh-token-expiration` | `JWT_REFRESH_EXPIRATION` | `604800000` ms = **7 days** |

The `expiresIn` field of login/refresh responses reports the access-token lifetime in **seconds** (e.g. `900`).

**Protected and public endpoints.** The full matrix is §12: `/auth/**` and `GET /taxes/**` are public; `/users/**` and `/calculations/**` require a token. There is **no role model** in the scaffold (no roles, no authorities, no role table) — every authenticated user is a plain user, and the Spring Security configuration that expresses this split is still an empty placeholder.

**What happens when the token is missing.** Protected endpoint without an `Authorization` header → `401 Unauthorized` with `WWW-Authenticate: Bearer`.

**What happens when the token is invalid** (malformed, bad signature, wrong algorithm) → `401 Unauthorized`; the client must treat it exactly like a missing token (do not retry with the same token).

**What happens when the token expires** → `401 Unauthorized`. The client should call `POST /auth/refresh` **once** and replay the original request; if the refresh also returns `401`, clear both stored tokens and send the user back to login.

**Logout.** Server-side invalidation does **not exist today** (see §6.6). Until it does, "logout" means **client-side deletion of the stored tokens**; the JWT itself remains technically valid until it expires.

---

## 4. Standard HTTP Status Codes

| Status | Meaning | Where this API uses it |
|---|---|---|
| `200` | Successful request | Reads, updates, verifications |
| `201` | Resource created | `POST /auth/register`, `POST /calculations` |
| `204` | Success, no response body | `POST /auth/logout`, all `DELETE`s |
| `400` | Bad request (malformed JSON, invalid path/query parameter) | All endpoints |
| `401` | Authentication required / invalid credentials / invalid or expired token | Login + protected endpoints |
| `403` | Forbidden — authenticated but the account may not act (unverified, suspended) | Login + protected endpoints |
| `404` | Resource not found | Unknown tax-type code, unknown calculation id |
| `409` | Conflict — duplicate e-mail, OTP or reset token already used | Register, verify, reset confirm |
| `422` | Request body failed field validation | All request bodies |
| `429` | Too many requests — OTP resend throttling *(planned)* | `POST /auth/resend-otp` |
| `500` | Internal server error | All endpoints |

> **Implementation note.** No status code is produced by the backend yet: `GlobalExceptionHandler` and `ErrorResponse` are empty placeholders. This table is the contract the handler must implement (§21).

---

## 5. Standard Error Response

Every non-2xx response must use one envelope. `common/response/ErrorResponse` and `common/exception/GlobalExceptionHandler` are **empty placeholders**, so the structures below are the contract to implement — changing them requires updating this file first (§20).

**General error** (any status except field validation):

```json
{
  "timestamp": "2026-09-23T13:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "No tax type with code 'crypto_tax'.",
  "path": "/api/v1/taxes/crypto_tax"
}
```

**Validation error** (`422`; also `400` when the JSON itself cannot be parsed):

```json
{
  "timestamp": "2026-09-23T13:00:00Z",
  "status": 422,
  "error": "Validation Error",
  "message": "Invalid request",
  "path": "/api/v1/auth/register",
  "fields": {
    "email": "Invalid email format",
    "password": "Password must be at least 8 characters"
  }
}
```

Rules:

* `fields` appears **only** when the failure is field-level — bean-validation on a DTO, `ValidationException`, or a body that cannot be bound to the DTO.
* `message` is safe to display and never echoes a password, hash, token or SQL fragment.
* `timestamp` is ISO-8601 UTC; `path` is the request URI without the query string.
* Unknown path variables (`{code}`, `{id}`) are `404` via `ResourceNotFoundException` — they are not validation errors.
* `500` responses use the same envelope with a generic message; stack traces and exception class names must never be serialised.

---

## 6. Authentication API

Base path: **`/api/v1/auth`**.

> **Implementation status: NOT IMPLEMENTED for every route in this section.** `AuthController`, `AuthService`, `OtpService`, `PasswordResetService` and all auth DTOs are empty classes. The OTP and reset-token rules quoted below are real configuration (`app.otp.*`, `app.password-reset.*` in `application.yml`): code length **6**, code validity **5 minutes**, reset-token validity **15 minutes**.

### 6.1 `POST /auth/register`

> **Implementation status: NOT IMPLEMENTED** — `RegisterRequest` and `AddressRequest` are empty placeholders.

**Purpose.** Create a new user account and e-mail a verification code.

**Authentication.** Public.

**Request body** (contract; shape derived from the scaffolded DTO names and the client signup form):

```json
{
  "firstName": "Juan",
  "lastName": "Dela Cruz",
  "email": "juan@example.com",
  "password": "SecurePassword123",
  "phoneNumber": "09171234567",
  "address": {
    "street": "123 Main Street",
    "barangay": "Barangay 1",
    "city": "Manila",
    "province": "Metro Manila",
    "postalCode": "1000"
  }
}
```

| Field | Required | Type / format |
|---|---|---|
| `firstName` | yes | string, 2–60 chars |
| `lastName` | yes | string, 2–60 chars |
| `email` | yes | valid e-mail, unique (case-insensitive), stored trimmed + lower-cased |
| `password` | yes | string, §13 password policy |
| `phoneNumber` | no | PH mobile, 11 digits starting `09` |
| `address` | no | `AddressRequest` object (§7.3); may be provided at signup or later |

**Success response** — `201 Created`:

```json
{
  "message": "Account created. Enter the 6-digit code we e-mailed you.",
  "userId": 1,
  "email": "juan@example.com",
  "emailVerificationRequired": true
}
```

**Behavior.** The password is hashed with BCrypt. The account starts as `PENDING_VERIFICATION` (`user/enums/UserStatus`). A 6-digit code (`OtpPurpose.REGISTRATION`) is e-mailed by `OtpEmailService`; it expires after 5 minutes and can be re-sent after the cooldown in §6.3.

**Error responses.**

| Status | When | `message` |
|---|---|---|
| `409` | e-mail already registered (any casing) | `That e-mail address is already registered.` |
| `422` | field validation failed | `Invalid request` + `fields` map |
| `500` | SMTP failure while sending the code | generic |

**Duplicate e-mail behavior.** The request is rejected with `409` and the *same* message whether the existing account is pending, active or suspended — registration must not reveal account state, and should take comparable time in both branches.

### 6.2 `POST /auth/verify-registration`

> **Implementation status: NOT IMPLEMENTED** — `VerifyRegistrationRequest` is an empty placeholder.

**Purpose.** Activate the account with the e-mailed code.

**Authentication.** Public.

**Request body:**

```json
{ "email": "juan@example.com", "code": "482913" }
```

**Success response** — `200 OK`:

```json
{ "message": "Account verified.", "email": "juan@example.com", "status": "ACTIVE" }
```

**Errors.** `404` no pending account for that e-mail · `422` wrong, expired or attempt-exhausted code (`code` is single-use, **max 5 attempts**, then invalidated) · `409` account already active or code already used.

### 6.3 `POST /auth/resend-otp`

> **Implementation status: NOT IMPLEMENTED** — `ResendOtpRequest` and `OtpPurpose` are empty placeholders.

**Purpose.** Re-send a registration or password-reset code.

**Authentication.** Public.

**Request body:**

```json
{ "email": "juan@example.com", "purpose": "REGISTRATION" }
```

`purpose` — `OtpPurpose` enum: `REGISTRATION` | `PASSWORD_RESET`.

**Success response** — `200 OK`, also for an unknown address (no account enumeration):

```json
{
  "message": "If that address needs a code, one is on its way.",
  "otpExpiresInSeconds": 300,
  "resendAvailableInSeconds": 45
}
```

**Errors.** `429` when called inside the cooldown window, with a `Retry-After` header. The 45-second cooldown matches the mobile client, whose OTP widget counts down `AppDimens.resendCooldownSeconds = 45` before enabling *Resend*.

### 6.4 `POST /auth/login`

> **Implementation status: NOT IMPLEMENTED** — `LoginRequest` and `AuthResponse` are empty placeholders.

**Purpose.** Exchange credentials for tokens.

**Authentication.** Public.

**Request body:**

```json
{ "email": "juan@example.com", "password": "SecurePassword123" }
```

**Success response** — `200 OK`:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "9f4c1a...b29a",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": 1,
    "firstName": "Juan",
    "lastName": "Dela Cruz",
    "email": "juan@example.com"
  }
}
```

**Errors.**

| Status | When | `message` |
|---|---|---|
| `401` | unknown e-mail **or** wrong password | `Invalid e-mail or password.` (identical text for both) |
| `403` | `PENDING_VERIFICATION` account | `Verify your e-mail address before signing in.` |
| `403` | `SUSPENDED` / `DEACTIVATED` account | `This account is not available.` |
| `422` | missing/blank fields | `Invalid request` + `fields` |

**Side effect.** One `refresh_tokens` row per successful login (hashed value, expiry from `JWT_REFRESH_EXPIRATION`).

### 6.5 `POST /auth/refresh`

> **Implementation status: NOT IMPLEMENTED** — `RefreshTokenRequest` and the `RefreshToken` entity are empty placeholders.

**Purpose.** Rotate a refresh token and obtain a new access token.

**Authentication.** Public (the refresh token itself is the credential).

**Request body:** `{ "refreshToken": "9f4c1a...b29a" }`

**Success response** — `200 OK`: the same shape as §6.4 (new `accessToken`, new `refreshToken`, new `expiresIn`).

**Errors.** `401` unknown, expired, revoked or already-rotated token. Rotation is mandatory: the presented token is revoked and replaced. If a revoked/rotated token is presented again, the whole token family for that user must be revoked (reuse detection).

### 6.6 `POST /auth/logout` — NOT IMPLEMENTED

> **Implementation status: NOT IMPLEMENTED — and today there is no server-side logout at all.** Nothing in the backend revokes a JWT or a refresh token; the `RefreshToken` entity and `RefreshTokenRepository` are empty placeholders, and Spring Security is not configured yet. **Clients must not call this endpoint.**

**What “logout” means today:** the client deletes the stored access and refresh tokens. The access token remains valid until it expires — a known, accepted limitation until this endpoint is implemented.

**Planned contract (do not build against it before §21 updates).** `POST /auth/logout`, bearer token required, body `{ "refreshToken": "...", "allDevices": false }` → `204 No Content`; revokes the presented refresh token (or every refresh token of the user when `allDevices` is `true`). Idempotent: an already-revoked token still returns `204`.

### 6.7 `POST /auth/password-reset/request`

> **Implementation status: NOT IMPLEMENTED** — `PasswordResetRequest` is an empty placeholder.

**Purpose.** Start password recovery.

**Authentication.** Public.

**Request body:** `{ "email": "juan@example.com" }`

**Success response** — `200 OK`, always the same (registered or not):

```json
{
  "message": "If that address is registered, a reset code is on its way.",
  "otpExpiresInSeconds": 300,
  "resendAvailableInSeconds": 45
}
```

**Behavior.** Creates a `PASSWORD_RESET` OTP and mails it. A pending (unverified) account may still reset its password; suspended/deactivated accounts are silently ignored. No response ever reveals whether the address exists.

### 6.8 `POST /auth/password-reset/verify`

> **Implementation status: NOT IMPLEMENTED** — `PasswordResetVerifyRequest` is an empty placeholder.

**Purpose.** Exchange a valid recovery code for a short-lived reset token.

**Request body:** `{ "email": "juan@example.com", "code": "482913" }`

**Success response** — `200 OK`:

```json
{ "resetToken": "b7f2c1...", "expiresInSeconds": 900 }
```

The reset token is single-use, is **not** a session credential, and is accepted only by §6.9. Validity is `app.password-reset.expiration-minutes` (15 minutes, real config).

**Errors.** `422` wrong/expired code · `429` attempts exhausted · `409` code already used.

### 6.9 `POST /auth/password-reset/confirm`

> **Implementation status: NOT IMPLEMENTED** — `PasswordResetConfirmRequest` is an empty placeholder.

**Purpose.** Set the new password.

**Request body:**

```json
{
  "resetToken": "b7f2c1...",
  "newPassword": "SecurePassword456",
  "confirmPassword": "SecurePassword456"
}
```

**Success response** — `200 OK`: `{ "message": "Password updated. Sign in with your new password." }`

**Behavior.** The password policy of §13 applies. On success the hash is replaced and **every existing refresh token of the user is revoked** (so stolen sessions cannot continue).

**Errors.** `422` mismatch or weak password · `409` reset token already used · `410` reset token expired.

---

## 7. User / Profile API

Base path: **`/api/v1/users`**. Every route requires `Authorization: Bearer <JWT>` and operates on the caller only — **no route accepts a user id**, which removes IDOR bugs by construction.

> **Implementation status: NOT IMPLEMENTED for every route in this section.** `UserController`, `UserService`, `UserResponse`, `UpdateProfileRequest`, `AddressRequest`, `ChangePasswordRequest` and `NotificationSettingsRequest` are empty classes; the `User` entity holds only `id`.

### 7.1 `GET /users/me`

**Purpose.** Read the authenticated user's profile.

**Success response** — `200 OK` (contract field set; every name below exists as a placeholder class in `user/`):

```json
{
  "id": 1,
  "firstName": "Juan",
  "lastName": "Dela Cruz",
  "email": "juan@example.com",
  "phoneNumber": "09171234567",
  "gender": "MALE",
  "dateOfBirth": "1996-04-17",
  "status": "ACTIVE",
  "address": {
    "street": "123 Main Street",
    "barangay": "Barangay 1",
    "city": "Manila",
    "province": "Metro Manila",
    "postalCode": "1000"
  },
  "notificationSettings": {
    "taxUpdates": true,
    "calculationReminders": false,
    "generalNotifications": true
  },
  "createdAt": "2026-09-23T13:00:00Z"
}
```

| Field | Type | Notes |
|---|---|---|
| `id` | integer | user id |
| `firstName` / `lastName` | string | — |
| `email` | string | login identifier, lower-cased |
| `phoneNumber` | string or null | PH mobile format |
| `gender` | enum string or null | `MALE` \| `FEMALE` \| `OTHER` \| `PREFER_NOT_TO_SAY` (contract for the empty `Gender` enum) |
| `dateOfBirth` | date (`YYYY-MM-DD`) or null | no user under 18 should be able to register (contract) |
| `status` | enum string | `PENDING_VERIFICATION` \| `ACTIVE` \| `SUSPENDED` \| `DEACTIVATED` (contract for the empty `UserStatus` enum) |
| `address` | object or null | present only when the user has stored one |
| `notificationSettings` | object | always present; defaults in §11 |
| `createdAt` | timestamp | ISO-8601 UTC |

**Errors.** `401` missing/invalid/expired token.

### 7.2 `PUT /users/me`

**Purpose.** Update profile fields (not the address — see §7.3 — and not the password — see §7.4).

**Request body** — `UpdateProfileRequest`; every field optional: **absent = unchanged, explicit `null` = clear**.

```json
{
  "firstName": "Juan",
  "lastName": "Dela Cruz",
  "phoneNumber": "09179876543",
  "gender": "MALE",
  "dateOfBirth": "1996-04-17"
}
```

**Success response** — `200 OK`: the full updated §7.1 object.

**Errors.** `409` phone number already used by another account · `422` invalid name/date/phone · `401`/`403` as usual.

### 7.3 `PUT /users/me/address`

**Purpose.** Create or replace the address (one address per user, so this is a full replace, not a patch).

**Request body** — `AddressRequest`:

```json
{
  "street": "123 Main Street",
  "barangay": "Barangay 1",
  "city": "Manila",
  "province": "Metro Manila",
  "postalCode": "1000"
}
```

| Field | Required | Rules |
|---|---|---|
| `street` | yes | 2–160 chars |
| `barangay` | no | 2–80 chars |
| `city` | yes | 2–80 chars |
| `province` | yes | 2–80 chars |
| `postalCode` | no | 4 digits (`^\d{4}$`) |

**Success response** — `200 OK`: the full updated §7.1 object.

### 7.4 `PUT /users/me/password`

**Purpose.** Change the password of the signed-in user.

**Request body** — `ChangePasswordRequest`:

```json
{
  "currentPassword": "SecurePassword123",
  "newPassword": "SecurePassword456",
  "confirmPassword": "SecurePassword456"
}
```

**Success response** — `200 OK`: `{ "message": "Password changed successfully." }`

**Errors.** `401` current password wrong (deliberately distinct from validation — the client shows it like a failed login) · `422` new passwords differ or violate the §13 policy · `409` new password equal to the current one (contract).

**Behavior.** All refresh tokens of the user except the one used for this call are revoked (contract), so other devices are signed out.

### 7.5 `GET /users/me/notification-settings`

**Success response** — `200 OK`:

```json
{ "taxUpdates": true, "calculationReminders": false, "generalNotifications": true }
```

### 7.6 `PUT /users/me/notification-settings`

**Request body** — `NotificationSettingsRequest`: all three booleans required (the client always sends the full switch set); the same shape as §7.5. **Success response** — `200 OK`: the persisted object.

### 7.7 `DELETE /users/me` — not part of this contract

Account deletion is **not implemented** and is not requested by the current client (the profile screen offers no delete action). No delete semantics are defined; if it is ever added, it must appear here and in §21 first.

---

## 8. Tax Types API

Base path: **`/api/v1/taxes`**. All four routes are **public** (no token) and read-only: the catalogue is data, seeded from §8.6, and identical for every client.

> **Implementation status: NOT IMPLEMENTED for every route in this section.** `TaxController`, `TaxService`, `TaxType`, `TaxBracket`, `TaxExample`, their repositories, `TaxTypeCode` and `DataSeeder` are empty placeholders. The contract below defines what they must expose.

### 8.1 `GET /taxes`

**Purpose.** List all supported tax types (summary form) — drives the client's *Taxes* tab and the calculator's tax-type picker.

**Query parameters.** None.

**Success response** — `200 OK`: an array of summaries.

```json
[
  {
    "code": "personal_income_tax",
    "name": "Personal Income Tax",
    "description": "Tax on an individual's net taxable income under the Philippine TRAIN Law.",
    "currentRate": "0% – 35%",
    "effectiveDate": "2023-01-01",
    "bracketCount": 6,
    "hasExample": true
  },
  {
    "code": "vat",
    "name": "VAT",
    "description": "Value-Added Tax is a consumption tax levied on the sale of goods and services at each stage of the supply chain.",
    "currentRate": "12%",
    "effectiveDate": "2018-01-01",
    "bracketCount": 3,
    "hasExample": true
  }
]
```

| Field | Type | Notes |
|---|---|---|
| `code` | string | stable transport key (§8.5) — clients key their models on it |
| `name` | string | display name |
| `description` | string | one-line explanation |
| `currentRate` | string | display string, e.g. `0% – 35%` |
| `effectiveDate` | date | ISO-8601 date |
| `bracketCount` | integer | convenience for the UI |
| `hasExample` | boolean | whether §8.4 returns at least one example |

**Errors.** `500` only. The response is never paginated (exactly 10 items).

### 8.2 `GET /taxes/{code}`

**Purpose.** One tax type with its bracket table and worked example — everything the client's detail screen renders.

**Path parameter.** `code` — one of the codes in §8.5 (lower-snake-case).

**Success response** — `200 OK`:

```json
{
  "code": "personal_income_tax",
  "name": "Personal Income Tax",
  "description": "Tax on an individual's net taxable income under the Philippine TRAIN Law.",
  "currentRate": "0% – 35%",
  "effectiveDate": "2023-01-01",
  "brackets": [
    { "range": "₱0 – ₱250,000", "rate": "Exempt" },
    { "range": "₱250,001 – ₱400,000", "rate": "15% of excess over ₱250,000" },
    { "range": "₱400,001 – ₱800,000", "rate": "₱22,500 + 20% of excess" }
  ],
  "example": {
    "amount": "₱600,000",
    "computation": "₱22,500 + 20% of ₱200,000",
    "estimatedTax": "₱62,500"
  }
}
```

`range`, `rate`, `amount`, `computation` and `estimatedTax` are **display strings** on purpose: the client renders them verbatim (peso signs, en-dashes, *“of excess”* phrasing). Do not “improve” the formatting without updating the client.

**Errors.** `404` unknown code (`No tax type with code '...'.`).

### 8.3 `GET /taxes/{code}/brackets`

**Purpose.** Bracket table only. **Success response** — `200 OK`: `TaxBracketResponse[]` (`range`, `rate` — same rows as §8.2). **Errors.** `404` unknown code.

### 8.4 `GET /taxes/{code}/examples`

**Purpose.** Worked example(s) only. **Success response** — `200 OK`: `TaxExampleResponse[]` (`amount`, `computation`, `estimatedTax`; today exactly one per type). **Errors.** `404` unknown code.

---

### 8.5 The ten tax-type codes (`TaxTypeCode`)

The wire value is **lower-snake-case** and is identical to the Flutter client's `TaxTypeId.id` values — `lib/domain/models/tax_type_id.dart` declares those strings as *“the stable, transport-safe key intended for the backend API”*, and `TaxTypeId.fromId()` compares them verbatim. The Java enum constants are SCREAMING_SNAKE (Java convention); map them explicitly (for example `@JsonValue` on the code string) so the JSON below never changes.

| Wire `code` (JSON) | Java enum constant (`TaxTypeCode`) | `name` |
|---|---|---|
| `personal_income_tax` | `PERSONAL_INCOME_TAX` | Personal Income Tax |
| `corporate_income_tax` | `CORPORATE_INCOME_TAX` | Corporate Income Tax |
| `vat` | `VAT` | VAT |
| `percentage_tax` | `PERCENTAGE_TAX` | Percentage Tax |
| `cgt_real_property` | `CGT_REAL_PROPERTY` | CGT – Real Property |
| `cgt_shares` | `CGT_SHARES` | CGT – Shares |
| `documentary_stamp_tax` | `DOCUMENTARY_STAMP_TAX` | Documentary Stamp Tax |
| `withholding_tax` | `WITHHOLDING_TAX` | Withholding Tax |
| `estate_tax` | `ESTATE_TAX` | Estate Tax |
| `real_property_tax` | `REAL_PROPERTY_TAX` | Real Property Tax |

Unknown codes are rejected with `422` on calculation requests (§9) and with `404` on catalogue lookups (§8.2–8.4). Clients must also tolerate *future* codes they do not know: the Flutter enum resolves them to `null` and skips them.

### 8.6 Reference data to seed (`DataSeeder` + `V3__create_tax_tables.sql`)

`DataSeeder` is an empty placeholder and `V3__create_tax_tables.sql` is a 0-byte file. The rows below are the **data contract**: they mirror the client's offline catalogue (`taxratesystem_mobile/lib/data/repositories/static_tax_reference_repository.dart`), which the app renders verbatim — keep the peso signs, en-dashes and phrasing exactly as written.

| `code` | Brackets (`range` → `rate`) | Example (`amount` → `computation` = `estimatedTax`) |
|---|---|---|
| `personal_income_tax` | `₱0 – ₱250,000` → `Exempt`; `₱250,001 – ₱400,000` → `15% of excess over ₱250,000`; `₱400,001 – ₱800,000` → `₱22,500 + 20% of excess`; `₱800,001 – ₱2,000,000` → `₱102,500 + 25% of excess`; `₱2,000,001 – ₱8,000,000` → `₱402,500 + 30% of excess`; `Over ₱8,000,000` → `₱2,202,500 + 35% of excess` | `₱600,000` → `₱22,500 + 20% of ₱200,000` = `₱62,500` |
| `corporate_income_tax` | `Domestic corp., net taxable income ≤ ₱5M` → `20% (CREATE reduced rate)`; `Domestic corp., net taxable income > ₱5M` → `25%`; `Resident foreign corp.` → `25%`; `Non-resident foreign corp.` → `30%` | `₱10,000,000` → `₱10,000,000 × 25%` = `₱2,500,000` |
| `vat` | `Sale of goods or properties` → `12%`; `Sale of services` → `12%`; `Importation of goods` → `12%` | `₱100,000` → `₱100,000 × 12%` = `₱12,000` |
| `percentage_tax` | `Gross sales or receipts ≤ ₱3M` → `3%`; `VAT-registered taxpayers` → `Not applicable` | `₱2,000,000` → `₱2,000,000 × 3%` = `₱60,000` |
| `cgt_real_property` | `Selling price or zonal value` → `6%`; `Higher of the two` → `6%`; `Sale of principal residence (exemption)` → `Exempt` | `₱5,000,000` → `₱5,000,000 × 6%` = `₱300,000` |
| `cgt_shares` | `Net gain ≤ ₱100,000` → `5%`; `Net gain > ₱100,000` → `10% of the excess` | `₱150,000 net gain` → `₱5,000 + 10% of ₱50,000` = `₱10,000` |
| `documentary_stamp_tax` | `Loan agreements` → `0.5%`; `Deeds of sale` → `1.5%`; `Lease agreements` → `0.5%` | `₱2,000,000 deed of sale` → `₱2,000,000 × 1.5%` = `₱30,000` |
| `withholding_tax` | `Professional fees` → `10%`; `Rentals` → `5%`; `Commissions` → `10%` | `₱100,000 professional fees` → `₱100,000 × 10%` = `₱10,000` |
| `estate_tax` | `Net estate ≤ ₱5M (standard deduction)` → `Exempt`; `Net estate > ₱5M` → `6% of the excess` | `₱15,000,000` → `₱15,000,000 − ₱5,000,000 = ₱10,000,000 × 6%` = `₱600,000` |
| `real_property_tax` | `Basic RPT (city or municipality)` → `1%`; `Basic RPT (province)` → `1%`; `Additional levy (Special Education Fund)` → `Up to 1%` | `₱5,000,000 assessed value` → `₱5,000,000 × 1%` = `₱50,000` |

Per-type `description`, `currentRate` and `effectiveDate` (the §8.1/§8.2 summary fields):

| `code` | `description` | `currentRate` | `effectiveDate` |
|---|---|---|---|
| `personal_income_tax` | Tax on an individual's net taxable income under the Philippine TRAIN Law. | `0% – 35%` | `2023-01-01` |
| `corporate_income_tax` | Tax on the net taxable income of domestic and resident foreign corporations under the CREATE Law. | `20% – 25%` | `2020-07-01` |
| `vat` | Consumption tax on the sale of goods and services at each stage of the supply chain. | `12%` | `2018-01-01` |
| `percentage_tax` | Business tax on gross sales/receipts of non-VAT taxpayers below the VAT threshold. | `3%` | `2018-01-01` |
| `cgt_real_property` | Capital Gains Tax on the sale of Philippine real property held as a capital asset. | `6%` | `1998-01-01` |
| `cgt_shares` | Capital Gains Tax on shares of stock not traded through the local stock exchange. | `5% – 10%` | `1998-01-01` |
| `documentary_stamp_tax` | Tax on documents, instruments and papers showing transfer of rights. | `0.5% – 1.5%` | `2005-01-01` |
| `withholding_tax` | Tax withheld by the payer of income from the payee at the prescribed rate. | `1% – 15%` | `2018-01-01` |
| `estate_tax` | Tax on the right to transmit the estate of a decedent to the lawful heirs. | `6%` | `2018-01-01` |
| `real_property_tax` | Annual ad valorem tax assessed by the LGU on real property. | `1% – 2%` | `1992-01-01` |

Entity mapping: one `tax_types` row per code, `tax_brackets` rows (`range`, `rate`, display order) and `tax_examples` rows (`amount`, `computation`, `estimated_tax`) referencing it — the relationships the scaffolded `TaxType` / `TaxBracket` / `TaxExample` entities must express.

---

## 9. Tax Calculator API

Base path: **`/api/v1/calculations`**. Calculating requires a signed-in user because every successful calculation is persisted for that user and appears in their history (§10).

> **Implementation status: NOT IMPLEMENTED.** `CalculationController`, `CalculationService`, `CalculationRequest`, `CalculationResponse`, the `Calculation` entity, `CalculationRepository` and `CalculationStatus` are empty placeholders.

### 9.1 `POST /calculations`

**Purpose.** Compute the tax for one tax type and one taxable amount, persist the result for the caller, and return the full explanation.

**Authentication.** Required (`Authorization: Bearer <JWT>`).

**Request headers.** `Content-Type: application/json`.

**Request body** — `CalculationRequest`:

```json
{
  "taxType": "personal_income_tax",
  "taxableIncome": 600000.00
}
```

| Field | Required | Type / rules |
|---|---|---|
| `taxType` | yes | one of the ten codes in §8.5; unknown code → `422` |
| `taxableIncome` | yes | JSON number, finite, **> 0**, at most `999999999999.99`; the client sends the raw amount the user typed |

**Success response** — `201 Created` — `CalculationResponse`:

```json
{
  "id": 243,
  "taxType": "personal_income_tax",
  "taxTypeName": "Personal Income Tax",
  "taxableIncome": 600000.00,
  "calculatedTax": 62500.00,
  "currency": "PHP",
  "applicableBracket": "₱400,001 – ₱800,000 (20% excess rate)",
  "effectiveRule": "TRAIN Law Series (Jan 1, 2023)",
  "status": "COMPLETED",
  "breakdown": [
    { "label": "Base Tax", "value": "₱22,500.00", "detail": null },
    { "label": "Excess Amount", "value": "₱200,000.00", "detail": "20% = ₱40,000.00" },
    { "label": "Total Tax Due", "value": "₱62,500.00", "detail": null }
  ],
  "createdAt": "2026-09-23T13:00:00Z"
}
```

| Field | Type | Notes |
|---|---|---|
| `id` | integer | id of the persisted calculation — use it for §10.2/§10.3 |
| `taxType` | string | the requested code, echoed back |
| `taxTypeName` | string | display name (convenience for history rows) |
| `taxableIncome` | number | the request amount, echoed back |
| `calculatedTax` | number | the computed tax, 2-decimals |
| `currency` | string | always `PHP` |
| `applicableBracket` | string | display string describing the bracket/rule applied |
| `effectiveRule` | string | law + effective date, display string |
| `status` | enum string | `CalculationStatus` — contract values `COMPLETED` (persisted) and `FAILED` (kept for completeness; the enum class is empty, finalise before implementing) |
| `breakdown` | array | ordered explanation rows, rendered verbatim by the client |
| `createdAt` | timestamp | ISO-8601 UTC; this is the timestamp shown as “saved at” in history |

**Canonical test case.** `personal_income_tax` with `taxableIncome: 600000` **must** return `calculatedTax: 62500.00` (₱22,500 + 20% of ₱200,000) — the same figure the mobile app computes offline, the same figure in §8.6. Use it as the first integration test.

**Decimal handling.** Money is `BigDecimal` server-side (`decimal(15,2)` columns), never `double`/`float`. Intermediate arithmetic keeps full precision; the **final** amount is rounded once, `HALF_UP`, scale 2. Values are serialised as JSON numbers (trailing zeros may be dropped by the JSON writer — `62500` and `62500.00` are the same value and both acceptable on the wire).

**Validation.** `422` with a `fields` map when `taxType` is unknown (`"taxType": "unknown tax type code"`) or `taxableIncome` is missing, zero, negative, non-finite or above the maximum (`"taxableIncome": "must be greater than 0"`). `400` for malformed JSON. `401` without a valid token. `500` only for genuine server faults.

**Side effect.** The calculation is stored against the caller and is immediately visible via §10. `POST /calculations` is therefore also the client's *Save to history* action — there is no separate save endpoint.

### 9.2 Calculation rules (must match the mobile engine exactly)

The mobile app already computes all ten taxes offline (`lib/domain/services/tax_calculation_service.dart`). The backend must reproduce the same numbers so a calculation does not change when the client switches to the API.

```text
personal_income_tax  : tax = baseTax + (income − lowerBound) × rate     [progressive table below]
corporate_income_tax : rate = income ≤ 5,000,000 ? 0.20 : 0.25 ; tax = income × rate
vat                  : tax = grossSales × 0.12
percentage_tax       : tax = grossSales × 0.03
cgt_real_property    : tax = higherOf(sellingPrice, zonalValue) × 0.06
cgt_shares           : gain ≤ 100,000 ? gain × 0.05 : 5,000 + (gain − 100,000) × 0.10
documentary_stamp_tax: tax = documentValue × 0.015        [deeds of sale; loans/leases 0.005]
withholding_tax      : tax = grossPayment × 0.10          [professional fees]
estate_tax           : netEstate ≤ 5,000,000 ? 0 : (netEstate − 5,000,000) × 0.06
real_property_tax    : tax = assessedValue × 0.01
```

Progressive schedule for `personal_income_tax` (TRAIN Law, effective 2023-01-01) — the **only** progressive table:

| `label` | `lowerBound` | `upperBound` | `baseTax` | `rate` |
|---|---|---|---|---|
| `₱0 – ₱250,000` | 0 | 250,000 | 0 | 0 |
| `₱250,001 – ₱400,000` | 250,000 | 400,000 | 0 | 0.15 |
| `₱400,001 – ₱800,000` | 400,000 | 800,000 | 22,500 | 0.20 |
| `₱800,001 – ₱2,000,000` | 800,000 | 2,000,000 | 102,500 | 0.25 |
| `₱2,000,001 – ₱8,000,000` | 2,000,000 | 8,000,000 | 402,500 | 0.30 |
| `Over ₱8,000,000` | 8,000,000 | *(unbounded)* | 2,202,500 | 0.35 |

Bracket lookup: the **first** bracket whose `upperBound` is not exceeded wins; the table must end with an unbounded row.

**Display strings the response must reproduce** (the client renders them byte-for-byte):

* `applicableBracket` — e.g. `₱400,001 – ₱800,000 (20% excess rate)`; exempt brackets append `(Exempt)`. Flat-rate types use `"<bracket> (<rate>%)"`.
* `effectiveRule` — `TRAIN Law Series (Jan 1, 2023)` for personal income; `TRAIN Law (Jan 1, 2018)` for VAT, percentage tax, withholding and estate; `NIRC – Capital Gains (Jan 1, 1998)` for both CGT types; `NIRC – DST (Jan 1, 2005)` for documentary stamp; `Local Government Code (Jan 1, 1992)` for real property; `CREATE Law (Jul 1, 2020)` for corporate income.
* `breakdown` — per family, and the **last row must be labelled exactly `Total Tax Due`** (the client keys its sum styling on that literal string):

| Family | Rows |
|---|---|
| Personal income (progressive) | `Base Tax`, `Excess Amount` (detail `"<rate>% = ₱x"`), `Total Tax Due` — the first two are omitted in the exempt bracket |
| Corporate income | `Net Taxable Income`, `Rate Applied`, `Total Tax Due` |
| Capital gains – shares | `Base Tax` (only above ₱100,000), `Applicable Rate` (detail `"On gain of ₱x"`), `Total Tax Due` |
| Estate | `Net Estate`, `Standard Deduction`, `Excess Rate` (detail `"On excess of ₱x"`), `Total Tax Due` |
| Flat-rate (VAT, percentage, CGT property, DST, withholding, RPT) | `Gross Sales` / `Selling Price` / `Document Value` / `Gross Payment` / `Assessed Value`, `Rate Applied`, `Total Tax Due` |

Constants: estate standard deduction `5,000,000`; estate rate `0.06`; CGT-shares threshold `100,000`; corporate reduced-rate ceiling `5,000,000`.

---

## 10. Calculation History API

Base path: **`/api/v1/calculations`**. All routes require a bearer token and are **strictly scoped to the caller**: a user can only ever read, list or delete their own calculations.

> **Implementation status: NOT IMPLEMENTED.** `CalculationRepository` is an empty interface and `V4__create_calculation_tables.sql` is a 0-byte file.

### 10.1 `GET /calculations/history`

**Purpose.** The signed-in user's saved calculations, newest first — the data behind the client's *History* tab.

**Query parameters.**

| Name | Type | Default | Limits |
|---|---|---|---|
| `page` | integer | `0` | 0-based, ≥ 0 |
| `size` | integer | `20` | 1–100 |
| `sort` | string | `createdAt,desc` | any of `createdAt`, `calculatedTax`, `taxType` |
| `taxType` | string | *(none)* | optional filter by §8.5 code; unknown value → empty page, not `404` |

**Success response** — `200 OK` — paged envelope (§15):

```json
{
  "content": [
    {
      "id": 243,
      "taxType": "personal_income_tax",
      "taxTypeName": "Personal Income Tax",
      "taxableIncome": 600000.00,
      "calculatedTax": 62500.00,
      "currency": "PHP",
      "createdAt": "2026-09-23T13:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true,
  "hasNext": false
}
```

Items use the same fields as §9.1 (the `breakdown` array may be omitted in the list form — if it is ever omitted, that must be documented here first). An empty history is `200` with an empty `content` array, never `404`.

**Errors.** `401` invalid token; `403` suspended account.

### 10.2 `GET /calculations/{id}`

**Purpose.** One saved calculation, including its full `breakdown`.

**Path parameter.** `id` — integer id returned by §9.1.

**Success response** — `200 OK`: the complete §9.1 object.

**Errors.** `404` when the id does not exist **or belongs to another user** — deliberately `404` rather than `403`, so ids cannot be probed for existence.

### 10.3 `DELETE /calculations/{id}`

**Purpose.** Delete one history entry.

**Success response** — `204 No Content`. Idempotent: deleting an already-deleted id that once belonged to the caller still returns `204`.

**Errors.** `404` for an id that never belonged to the caller.

### 10.4 `DELETE /calculations/history`

**Purpose.** Clear the caller's whole history (the client's *clear history* action).

**Success response** — `204 No Content`. Scoped to the caller only — never touches other users' rows.

---

## 11. Notification API

> **Implementation status: NOT IMPLEMENTED.** `NotificationSettings` (entity) and `NotificationSettingsRequest` (DTO) are empty placeholders. The mobile client's own screen (`notification_settings_screen.dart`) already renders exactly these three switches, which is why the contract below uses them instead of a single on/off flag.

### 11.1 `GET /users/me/notification-settings`

Documented in §7.5. Response:

```json
{ "taxUpdates": true, "calculationReminders": false, "generalNotifications": true }
```

### 11.2 `PUT /users/me/notification-settings`

Documented in §7.6. All three booleans are required; response is the persisted object.

| Preference | Default after registration | Meaning |
|---|---|---|
| `taxUpdates` | `true` | announcements about TRAIN/CREATE law updates |
| `calculationReminders` | `false` | reminders to compute periodic liabilities |
| `generalNotifications` | `true` | app updates and news |

**Scope notes.** These preferences control notification delivery only — they do not affect calculations or history. No push-notification infrastructure exists in the scaffold (no device-token entity, no FCM/APNs configuration); e-mail is the only delivery channel available (Spring Mail is configured). Do not expect endpoints for device registration or a notification inbox.

---

## 12. Endpoint Authentication Matrix

Target state (see the note below the table — none of it is enforced yet):

| Endpoint | Method | Authentication | Role | Implemented |
|---|---|---|---|---|
| `/auth/register` | POST | Public | — | No |
| `/auth/verify-registration` | POST | Public | — | No |
| `/auth/resend-otp` | POST | Public | — | No |
| `/auth/login` | POST | Public | — | No |
| `/auth/refresh` | POST | Public | — | No |
| `/auth/logout` | POST | Bearer required *(planned)* | USER | No |
| `/auth/password-reset/request` | POST | Public | — | No |
| `/auth/password-reset/verify` | POST | Public | — | No |
| `/auth/password-reset/confirm` | POST | Public | — | No |
| `/users/me` | GET | Bearer required | USER | No |
| `/users/me` | PUT | Bearer required | USER | No |
| `/users/me/address` | PUT | Bearer required | USER | No |
| `/users/me/password` | PUT | Bearer required | USER | No |
| `/users/me/notification-settings` | GET | Bearer required | USER | No |
| `/users/me/notification-settings` | PUT | Bearer required | USER | No |
| `/taxes` | GET | Public | — | No |
| `/taxes/{code}` | GET | Public | — | No |
| `/taxes/{code}/brackets` | GET | Public | — | No |
| `/taxes/{code}/examples` | GET | Public | — | No |
| `/calculations` | POST | Bearer required | USER | No |
| `/calculations/history` | GET | Bearer required | USER | No |
| `/calculations/{id}` | GET | Bearer required | USER | No |
| `/calculations/{id}` | DELETE | Bearer required | USER | No |
| `/calculations/history` | DELETE | Bearer required | USER | No |

> **Note.** `SecurityConfig` is an empty placeholder, so *no* route is actually protected yet. There is **no role model** in the scaffold — there are no roles, no authorities and no role tables — so the *Role* column is literal: every authenticated user is a plain `USER` and no endpoint requires an elevated role. The intended split is: everything under `/auth/**` and every `GET /taxes/**` permit-all; everything under `/users/**` and `/calculations/**` authenticated.

---

## 13. Request Validation

> **Implementation status: NOT IMPLEMENTED as annotations.** Every DTO is an empty class, so no constraint currently exists in code. The tables below are the **contract** — when the DTOs are implemented, the annotations must express exactly these rules, and this section must stay in sync (§20).

### 13.1 Registration and login

| Field | Required | Type | Rules |
|---|---|---|---|
| `firstName` | yes (register) | string | 2–60 chars; letters, spaces, hyphens, apostrophes |
| `lastName` | yes (register) | string | 2–60 chars; same character set |
| `email` | yes (both) | string | valid e-mail format, max 254 chars; normalised to lower-case; unique (case-insensitive) |
| `password` | yes (both) | string | see §13.2 |
| `phoneNumber` | no | string | PH mobile: exactly 11 digits starting `09` (`^09\d{9}$`); stored normalised |
| `address` (object) | no | object | fields validated per §13.3 |
| `code` (OTP) | yes (verify flows) | string | exactly 6 digits (`^\d{6}$`, matches `OTP_LENGTH=6`) |
| `resetToken` | yes (confirm) | string | server-issued opaque value, exactly as received |

### 13.2 Password policy

| Rule | Value |
|---|---|
| Minimum length | 8 characters |
| Maximum length | 72 bytes (BCrypt input limit) |
| Must contain | at least one uppercase letter, at least one digit |
| Not allowed | all-whitespace; the same string as the account e-mail |
| Storage | BCrypt hash only — the plaintext never leaves the request and is never logged |

Derivation: the client's reset screen enforces exactly *min 8 / one uppercase / one digit* live (checked-off rules in `new_password_screen.dart`). Note that the client's *change password* screen currently enforces only 6 characters — that screen must be aligned with this policy (see §21.4, gap C4).

### 13.3 Address

| Field | Required | Rules |
|---|---|---|
| `street` | yes | 2–160 chars |
| `barangay` | no | 2–80 chars |
| `city` | yes | 2–80 chars |
| `province` | yes | 2–80 chars |
| `postalCode` | no | 4 digits (`^\d{4}$`) |

### 13.4 Profile updates and settings

| Field | Rules |
|---|---|
| `firstName`, `lastName` | same as §13.1; optional in `PUT /users/me` |
| `gender` | one of `MALE`, `FEMALE`, `OTHER`, `PREFER_NOT_TO_SAY` |
| `dateOfBirth` | `YYYY-MM-DD`; must be in the past; user must be 18 or older (contract) |
| `newPassword`, `confirmPassword` | must match; `newPassword` follows §13.2; must differ from `currentPassword` |
| `taxUpdates`, `calculationReminders`, `generalNotifications` | booleans, all required in `PUT` |

### 13.5 Calculations

| Field | Rules |
|---|---|
| `taxType` | required; exactly one of the ten §8.5 codes (lower-snake-case) |
| `taxableIncome` | required; JSON number; finite; `> 0`; `≤ 999999999999.99`; at most 2 decimal places |

### 13.6 Path and query parameters

| Parameter | Rules | Failure |
|---|---|---|
| `{code}` | one of the ten §8.5 codes | `404` (catalogue) / `422` (calculation body) |
| `{id}` | positive integer | `404` when not found or not owned |
| `page` | integer ≥ 0 | `400` |
| `size` | integer 1–100 | `400` |
| `sort` | `field,asc\|desc` with `field` in the whitelist of §10.1 | `400` |

---

## 14. Data Types

| Type | Wire format | Example | Notes |
|---|---|---|---|
| ID | JSON number (integer) | `1`, `243` | `User.id` is `int` in the scaffold; calculation ids are integers too |
| String | JSON string | `"Juan"` | UTF-8 |
| Email | JSON string | `"juan@example.com"` | lower-cased on the wire |
| Money | JSON number, decimal(15,2) | `62500.00` | PHP only; never a formatted string |
| Percentage/rate | JSON number 0–1 where computed; display strings in catalogue data | `0.20`, `"20%"` | rates inside `brackets[].rate` are display strings by design (§8.2) |
| Date | ISO-8601 date | `"2023-01-01"` | `YYYY-MM-DD` |
| DateTime | ISO-8601 timestamp, UTC | `"2026-09-23T13:00:00Z"` | always with offset designator |
| Boolean | JSON boolean | `true` | never `"true"` |
| Enum | JSON string, SCREAMING_SNAKE_CASE | `"ACTIVE"`, `"PERSONAL_INCOME_TAX"` *(enum fields only)* | see the tax-code exception below |
| Tax type code | JSON string, lower_snake_case | `"personal_income_tax"` | the one enum that is **not** SCREAMING_SNAKE on the wire: the client compares these strings verbatim (§8.5) |
| Null | JSON `null` | `null` | optional fields may be absent or null |

**Unknown properties.** The server ignores unknown JSON properties in request bodies (Jackson default) so newer clients cannot break older servers; clients must not rely on unknown properties being rejected.

---

## 15. Pagination

> **Implementation status: NOT IMPLEMENTED.** `PageResponse` is an empty placeholder class. Pagination is nonetheless the contract for the single list endpoint, because the placeholder shows the intent and an unpaginated history cannot survive real use.

Used by exactly one endpoint: `GET /calculations/history` (§10.1). No other endpoint is paginated — `/taxes` returns all ten items, and profile/settings routes return single objects.

**Query parameters**

```text
GET /api/v1/calculations/history?page=0&size=20&sort=createdAt,desc
```

| Parameter | Default | Minimum | Maximum | Notes |
|---|---|---|---|---|
| `page` | `0` | `0` | — | 0-based page index |
| `size` | `20` | `1` | `100` | values above 100 are rejected with `400`, not clamped |
| `sort` | `createdAt,desc` | — | — | whitelist: `createdAt`, `calculatedTax`, `taxType`; suffix `asc` or `desc` |

**Response envelope** (shape of the `PageResponse` placeholder):

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true,
  "hasNext": false
}
```

**Stability.** The default sort is deterministic because ties on `createdAt` are broken by `id desc` (contract) — without it, two calculations saved in the same second could swap pages between requests.

---

## 16. Security Rules

> **Implementation status: NOT IMPLEMENTED.** `SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`, `CustomUserDetailsService`, `MailConfig` and the `RefreshToken`/`OtpCode` entities are all empty placeholders. This section is the security contract the implementation must satisfy.

**JWT authentication.** Stateless bearer tokens (§3). Every protected request is authenticated by validating the token signature, expiry and subject; there is no session cookie and no server-side session store (`SessionCreationPolicy.STATELESS`). Access tokens are short-lived (default 15 minutes, configurable); refresh tokens are persisted **hashed**, rotated on every use, and revoked on password change (contract §6.9, §7.4).

**BCrypt password hashing.** Passwords are stored only as BCrypt hashes (Spring Security dependency is already present). The plaintext password never leaves the request body: it is never logged, never echoed in responses, and never included in error messages. Hash comparisons use the framework constant-time check.

**CORS.** The API is consumed by a native mobile app, which is not subject to browser CORS. The only allowed browser origin is the configured `app.frontend-url` (default `http://localhost:3000`), intended for a future web build. Wildcard (`*`) origins must not be enabled together with credentials.

**CSRF.** Not applicable once implemented correctly: authentication is header-based and no cookies are used, so CSRF protection is disabled while the API remains stateless. If cookie-based auth is ever added, CSRF must be re-enabled and this section updated first.

**Public routes.** `POST /auth/register`, `/auth/verify-registration`, `/auth/resend-otp`, `/auth/login`, `/auth/refresh`, the three `/auth/password-reset/**` routes, and `GET /taxes/**` (§12).

**Protected routes.** Everything under `/users/**` and `/calculations/**`, and the planned `POST /auth/logout`.

**Role-based authorization.** **None exists.** The scaffold contains no roles, no authorities and no role tables; every authenticated user has identical rights over their own data only. If roles are introduced, this document and §12 must be updated before the code.

**Password requirements.** §13.2 (minimum 8 characters, one uppercase, one digit, BCrypt maximum 72 bytes).

**Token expiration.** Configurable, with real defaults: access token 15 minutes (`JWT_ACCESS_EXPIRATION`, default `900000` ms), refresh token 7 days (`JWT_REFRESH_EXPIRATION`, default `604800000` ms). Expiry is enforced server-side; the client must not extend a session locally.

**Sensitive data handling.**

* Never expose passwords, password hashes, JWT secrets, database credentials, mail credentials or environment variables in responses, logs or this document.
* Error messages must not reveal whether an e-mail is registered (§6.1, §6.3, §6.7) and must not include hashes, tokens, SQL or stack traces.
* History rows are scoped to the authenticated subject; cross-user reads return `404` (§10.2).
* Secrets live only in environment variables (`.env` is git-ignored; `JWT_SECRET` has no default so the app cannot silently start with a placeholder). `API.md` contains placeholders only — never real values (§19).
* Rate limiting exists only as the OTP-resend throttle (§6.3, `429`); there is no global request throttle in the scaffold.

---

## 17. API Examples

> These examples describe the **intended** behaviour of the contract. Against the current backend none of them can succeed: no controllers are registered, so requests fail before authentication or validation. They are written so that each one becomes a smoke test the moment the corresponding endpoint lands.

All examples assume `BASE=http://localhost:8080/api/v1`.

### Flow 1 — Registration

```bash
curl -i -X POST "$BASE/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Juan",
    "lastName": "Dela Cruz",
    "email": "juan.delacruz@example.com",
    "password": "SecurePassword123",
    "phoneNumber": "09171234567",
    "address": {
      "street": "123 Rizal Avenue",
      "barangay": "Barangay 1",
      "city": "Manila",
      "province": "Metro Manila",
      "postalCode": "1000"
    }
  }'
```

Expected: `201` with `emailVerificationRequired: true`, a 6-digit code arrives by e-mail, then:

```bash
curl -i -X POST "$BASE/auth/verify-registration" \
  -H "Content-Type: application/json" \
  -d '{ "email": "juan.delacruz@example.com", "code": "482913" }'
```

Expected: `200` with `"status": "ACTIVE"`.

### Flow 2 — Authentication

```bash
curl -i -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{ "email": "juan.delacruz@example.com", "password": "SecurePassword123" }'
```

Expected: `200` with `accessToken`, `refreshToken`, `tokenType: "Bearer"`, `expiresIn: 900` and the user summary. Every later call sends the token:

```bash
curl -i "$BASE/users/me" -H "Authorization: Bearer $ACCESS_TOKEN"
```

After 15 minutes (or any `401`), exchange the refresh token before retrying:

```bash
curl -i -X POST "$BASE/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{ "refreshToken": "9f4c1a...b29a" }'
```

### Flow 3 — Tax browsing

```bash
curl -i "$BASE/taxes"
curl -i "$BASE/taxes/personal_income_tax"
curl -i "$BASE/taxes/vat/brackets"
```

Expected: the ten-type catalogue, the personal income detail (six brackets, example ₱600,000 → ₱62,500), and the VAT bracket table. Public — no token.

### Flow 4 — Calculation (and implicit save)

```bash
curl -i -X POST "$BASE/calculations" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "taxType": "personal_income_tax", "taxableIncome": 600000 }'
```

Expected: `201` with `calculatedTax: 62500.00`, `currency: "PHP"`, bracket `₱400,001 – ₱800,000 (20% excess rate)`, rule `TRAIN Law Series (Jan 1, 2023)`, and a `breakdown` ending in `Total Tax Due = ₱62,500.00`. The returned `id` is the history entry — persisting it was the save.

### Flow 5 — History

```bash
curl -i "$BASE/calculations/history?page=0&size=20&sort=createdAt,desc" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Expected: a paged envelope whose `content` holds the caller's saved calculations (§10.1), newest first. Another user's id via `GET /calculations/{id}` returns `404`.

### Flow 6 — Profile

```bash
curl -i "$BASE/users/me" -H "Authorization: Bearer $ACCESS_TOKEN"

curl -i -X PUT "$BASE/users/me" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "firstName": "Juan Miguel", "phoneNumber": "09179876543" }'
```

Expected: the profile object of §7.1, then the same object with the updated fields.

---

## 18. Flutter Integration Guide

**Current state of the client.** The Flutter app (`taxratesystem_mobile`) is offline-only today: `pubspec.yaml` declares no HTTP client (`cupertino_icons`, `file_picker`, `shared_preferences` only), and every screen talks to repository interfaces whose implementations are local (`LocalAuthRepository`, `StaticTaxReferenceRepository`, `LocalTaxCalculationRepository`, `InMemoryHistoryRepository`). Integration means adding an HTTP layer behind those same interfaces and swapping the implementations in the composition root `lib/core/di/app_dependencies.dart` — no screen changes. Because no HTTP package exists yet, this guide does not prescribe one.

**Base URL configuration.** Keep one configurable constant, never hard-code per-screen URLs:

```dart
const String apiBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://10.0.2.2:8080/api/v1', // Android emulator loopback
);
```

Run with `flutter run --dart-define=API_BASE_URL=http://localhost:8080/api/v1` for desktop/web and with `http://<host-lan-ip>:8080/api/v1` for a physical device. See §2 for the URL per environment.

**Authentication interceptor.** Attach the token in exactly one place (the HTTP client wrapper) instead of per call:

```http
Authorization: Bearer <JWT>
Content-Type: application/json
```

Never add the header to `/auth/**` calls.

**Token storage.** Access and refresh tokens must go into platform secure storage, not `SharedPreferences` (which the app already uses for the profile image — that is not appropriate for credentials). Persist only the tokens and the user id; re-fetch the profile from `GET /users/me` on launch rather than caching it in plain text.

**Handling 401.** One retry maximum: call `POST /auth/refresh` with the stored refresh token, replace both tokens with the response, replay the original request once. If the refresh returns `401` (or there is no refresh token), delete the stored tokens and navigate to login. Do not loop.

**Handling validation errors.** `422` responses carry a `fields` map keyed by field name — bind those messages to the matching form inputs; show the envelope `message` in a banner. `401` on login should be shown as an inline error on the form (the login screen already has a dedicated error banner for this). `409` on register must be shown without confirming whether the address exists.

**JSON serialization.** All body keys are camelCase (`firstName`, `taxableIncome`, `createdAt`), identical to Dart field naming — a manual `fromJson`/`toJson` per model, or generated code, both work; no key renaming is needed. The two naming exceptions to remember: tax-type **codes** are lower_snake_case *values* (`"personal_income_tax"`) and enums are SCREAMING_SNAKE *values* (`"ACTIVE"`, `"MALE"`). Money stays `num`/`double` in Dart purely for display; never send a formatted string back.

**Network timeout.** Apply an explicit timeout (10–15 s) to every request; treat a timeout like a transport error, not like a server error. Retry only idempotent `GET`s, at most twice, with a short backoff. `POST /calculations` and the auth mutations must not be auto-retried by the transport layer (a retried register or calculation would create a duplicate).

**Error handling.** Parse the §5 envelope for every non-2xx response; fall back to a generic message when the body is not JSON (captive portals, gateways). Distinguish user-fixable problems (401/403/404/409/422 — show the message) from infrastructure problems (5xx/timeouts — offer retry).

**Repository mapping (target).**

| Client repository method | Endpoint |
|---|---|
| `AuthRepository.signIn` | `POST /auth/login` (+ store tokens) |
| `AuthRepository.getCurrentUser` | `GET /users/me` |
| `AuthRepository.signOut` | client-side token deletion (until §6.6 exists) |
| `TaxReferenceRepository.getSupportedTaxTypes` | `GET /taxes` |
| `TaxReferenceRepository.getDetail` | `GET /taxes/{code}` |
| `TaxCalculationRepository.calculate` | `POST /calculations` |
| `HistoryRepository.getHistory` | `GET /calculations/history` |
| `HistoryRepository.clear` | `DELETE /calculations/history` |

Client changes required before integration are listed in §21.4.

---

## 19. Environment Configuration

Placeholders only — **never** commit real passwords, secrets or credentials, and never add them to this file. The backend reads these from the environment (`.env` is git-ignored; `application.yml` / `application-dev.yml` reference them):

```env
# --- Client ---
API_BASE_URL=http://localhost:8080/api/v1

# --- Database (dev profile) ---
DB_URL=jdbc:mysql://localhost:3306/taxratesystem?useSSL=false&serverTimezone=Asia/Manila&allowPublicKeyRetrieval=true
DB_USERNAME=your_database_username
DB_PASSWORD=your_database_password

# --- JWT (no defaults in code; the app will not start without JWT_SECRET) ---
JWT_SECRET=your_long_random_secret
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# --- Mail (Gmail SMTP, STARTTLS) ---
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_gmail_address
MAIL_PASSWORD=your_gmail_app_password

# --- Application ---
FRONTEND_URL=http://localhost:3000
OTP_LENGTH=6
OTP_EXPIRATION_MINUTES=5
PASSWORD_RESET_EXPIRATION_MINUTES=15
```

The repository's `.env` already contains exactly these placeholder keys (`your_database_password`, `your_long_random_secret`, ...) and is git-ignored — keep it that way.

> **Security cleanup required (gap).** `src/main/resources/application.properties` still carries a hard-coded local database password from before the profile split, and is tracked by git. Move the value to the environment, delete the literal from the file (see §21.2), and rotate the credential if it was ever real.

---

## 20. API Change Policy

`API.md` is the single source of truth for the HTTP contract. Any change to it must land **before** (or in the same pull request as) the corresponding backend change, and must update every affected section in the same commit:

| Change | Required updates in `API.md` |
|---|---|
| New endpoint / new route | Request/response schema, §12 matrix, §21 status table, at least one example in §17 |
| Endpoint path or HTTP method change | All sections that reference it, §12, §17 examples |
| Request or response DTO change | The endpoint section, §13 validation table, §14 data types if new shapes appear |
| Authentication or token behaviour | §3, §6.x, §12, §16 |
| Validation rule change | §13 (the affected table) |
| New or changed status code | The endpoint section and §4 |
| Behaviour change with no signature change | The endpoint's *Behaviour* paragraph |

Additional rules:

* **Implementation status discipline.** Moving a feature from *Gap* to *Complete* in §21 requires the endpoints to exist and an example in §17 to pass — no status upgrades on intent alone.
* **Breaking changes.** A breaking change (removing/renaming a field, changing a code, changing auth) increments the path version (`/api/v2`) rather than mutating `v1` silently; both versions may be documented side by side during a deprecation window.
* **Naming.** Keep the terminology of this document consistent everywhere: *tax type*, *taxable income*, *calculated tax*, *history*, *notification settings*. Do not introduce synonyms in new sections.
* **Reviewer checklist.** Every backend PR that touches controllers, DTOs, security or migrations must state whether `API.md` changed, and why not if it did not.

---

## 21. Backend Implementation Gap Report

> Produced by inspecting this repository at commit `fadf5e2`. Nothing below is aspirational: every claim cites the file that was inspected. **Do not read any endpoint in this document as available until its row in §21.1 says *Complete*.**

### 21.1 Implementation status

| Feature | API Documentation | Backend Implementation | Status |
|---|---|---|---|
| Registration | Yes (§6.1) | No | **Gap** |
| Login | Yes (§6.4) | No | **Gap** |
| JWT | Yes (§3, §6.4) | No | **Gap** |
| Tax Types | Yes (§8) | No | **Gap** |
| Calculator | Yes (§9) | No | **Gap** |
| Calculation History | Yes (§10) | No | **Gap** |
| Profile | Yes (§7) | No | **Gap** |
| Notifications | Yes (§11) | No | **Gap** |
| Refresh tokens | Yes (§6.5) | No | **Gap** |
| Logout / token revocation | Yes (§6.6) | No | **Gap** |
| Password reset (OTP) | Yes (§6.7–6.9) | No | **Gap** |
| OTP e-mail delivery | Yes (§6) | No | **Gap** |
| Address management | Yes (§7.3) | No | **Gap** |
| Error envelope / handler | Yes (§5) | No | **Gap** |
| Pagination | Yes (§15) | No | **Gap** |
| Database schema (Flyway V1–V4) | Yes (§21.2) | No — files are 0 bytes | **Gap** |
| Tax data seeding | Yes (§8.6) | No | **Gap** |
| Security configuration (route rules, CORS, CSRF) | Yes (§16) | No | **Gap** |
| Role-based authorization | Documented as absent | No | N/A |
| Account deletion | Deliberately absent (§7.7) | No | N/A |

### 21.2 Evidence — what was inspected and what is empty

| Area | Files inspected | Finding |
|---|---|---|
| Controllers | `auth/controller/AuthController.java`, `tax/controller/TaxController.java`, `calculation/controller/CalculationController.java`, `user/controller/UserController.java` | class bodies empty; **no HTTP mappings anywhere** |
| Services | `AuthService`, `OtpService`, `PasswordResetService`, `TaxService`, `CalculationService`, `UserService` | empty classes |
| DTOs | auth (10), tax (3), calculation (2), user (5) | all empty classes — every schema in this file is therefore the **contract**, not extracted code |
| Entities | `User` (only `id`), `Address`, `NotificationSettings`, `OtpCode`, `RefreshToken`, `Calculation`, `TaxType`, `TaxBracket`, `TaxExample` | empty except `User#id` |
| Repositories | `UserRepository`, `OtpRepository`, `RefreshTokenRepository`, `TaxTypeRepository`, `TaxBracketRepository`, `TaxExampleRepository`, `CalculationRepository` | empty interfaces, no query methods |
| Enums | `OtpPurpose`, `TokenType`, `CalculationStatus`, `TaxTypeCode`, `Gender`, `UserStatus` | declared as empty *classes*, not enums — the enum values in this document are the contract |
| Security | `SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`, `CustomUserDetailsService` | empty — nothing is authenticated today |
| Mail / OTP utilities | `MailConfig`, `EmailService`, `OtpEmailService`, `common/util/OtpGenerator` | empty |
| Errors / pagination | `ErrorResponse`, `PageResponse`, `GlobalExceptionHandler`, `ResourceNotFoundException`, `ValidationException` | empty — §5 and §15 are unimplemented contracts |
| Migrations | `V1__create_users.sql` … `V4__create_calculation_tables.sql` | **0 bytes each** — the schema does not exist, and `ddl-auto: validate` will fail startup |
| Seed data | `config/DataSeeder.java` | empty — the ten tax types of §8.6 are not seeded |
| HTTP scratch files | `http/auth.http`, `http/calculation.http`, `http/tax.http`, `http/user.http` | **0 bytes** — no smoke tests exist |
| Application entry point | `TaxRateSystemApplication.java` | real `@SpringBootApplication` class |
| Configuration | `application.yml`, `application-dev.yml`, `application.properties`, `build.gradle`, `.env`, `.gitignore` | real, and the source of every “quoted as fact” value in this document |

### 21.3 Verified real (not a gap)

* Build: Spring Boot 4.1.1, Java 25 toolchain, Gradle wrapper, the dependency set in §1.
* Server: port `8080`, bind `0.0.0.0`, active profile `dev`.
* Security config keys: `JWT_SECRET` (required), access/refresh expirations.
* OTP/reset rules: length 6, code 5 minutes, reset token 15 minutes.
* Mail: Gmail SMTP host/port, STARTTLS, auth (credentials from the environment).
* Database: MySQL dev URL, `ddl-auto: validate`, Flyway locations.
* `.gitignore` excludes `.env`; `.env` holds placeholders only.

### 21.4 Client-side gaps (`taxratesystem_mobile`) to close before integration

| # | Gap | Where |
|---|---|---|
| C1 | Signup collects a single *Full name* plus phone/password/country and **no e-mail**, while §6.1 requires `firstName`, `lastName` and `email` | `lib/auth/register_screen.dart` |
| C2 | The OTP screens never call the API: *Verify* navigates without checking the code, and *Resend* only restarts the local 45 s timer | `lib/auth/password_reset_otp_screen.dart`, `lib/widgets/otp_input_field.dart` |
| C3 | No HTTP client dependency exists | `pubspec.yaml` |
| C4 | Change-password enforces 6 characters while the contract policy is 8 + uppercase + digit | `lib/profile/change_password_screen.dart` |
| C5 | `AppUser` models only `email` + `displayName`; §7.1 returns a full profile | `lib/domain/models/app_user.dart` |
| C6 | `SavedCalculation.savedAt` vs the API field `createdAt` — align or adapt | `lib/domain/models/saved_calculation.dart` |
| C7 | The avatar is base64 in `SharedPreferences`; no avatar endpoint is defined in this contract | `lib/profile/profile_screen.dart` |
| C8 | `taxratesystem_mobile/api.md` mirrors this file (Part 2 added 2026-09-23) — re-sync it whenever this document changes | `taxratesystem_mobile/api.md` |

### 21.5 Recommended implementation order

1. **Flyway schema** `V1`–`V4` — `ddl-auto: validate` blocks startup until the tables exist.
2. **Security foundation** — `SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`, `CustomUserDetailsService`, BCrypt encoder, route rules of §12/§16.
3. **Error contract** — `ErrorResponse`, `GlobalExceptionHandler`, `ValidationException`, `ResourceNotFoundException` (§5) so every later slice ships with correct errors.
4. **Auth slice** — register → verify-registration → resend-otp → login → refresh, with `OtpGenerator`, `MailConfig`, `EmailService`, `OtpEmailService` (§6.1–6.5).
5. **User slice** — profile, address, password change, notification settings (§7, §11).
6. **Tax catalogue** — entities, repositories, `DataSeeder` from §8.6, controller (§8).
7. **Calculations** — `CalculationService` per §9.2, persistence, history with `PageResponse` (§9, §10, §15).
8. **Password reset** (§6.7–6.9), then the planned logout/token revocation (§6.6).
9. **Smoke tests** — fill the four `http/*.http` files with the §17 flows; update the §21.1 table as each row flips to *Complete*.

---

**End of `API.md`.** Every endpoint in this document is the contract to implement; §21 is the record of what still does not exist. When in doubt, trust the backend source over this file — and fix this file in the same commit (§20).
