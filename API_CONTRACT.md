# API Contract - AI Study Hub Backend

This document describes the currently available backend APIs so the frontend team knows which endpoints exist, what to send, and what each API returns.

> Local base URL: `http://localhost:8080`

---

## 1. Standard response format

All REST APIs return responses using this format.

> Note: Google OAuth2 browser login is a redirect flow, so the browser is redirected back to the frontend instead of receiving this JSON format directly.

```json
{
  "success": true,
  "message": "Action successfully",
  "data": {},
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

Error response:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": null,
  "errors": [
    {
      "field": "email",
      "message": "Email is invalid"
    }
  ],
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Common fields

| Field       | Type                  | Description                                                 |
| ----------- | --------------------- | ----------------------------------------------------------- |
| `success`   | boolean               | `true` for successful requests, `false` for failed requests |
| `message`   | string                | General response message                                    |
| `data`      | object / array / null | Response payload                                            |
| `errors`    | array / null          | Detailed error list                                         |
| `timestamp` | string                | Response creation time                                      |

### Error object

| Field     | Type          | Description                                                     |
| --------- | ------------- | --------------------------------------------------------------- |
| `field`   | string / null | Invalid field, for example `email`, `password`, `authorization` |
| `message` | string        | Error message                                                   |

---

## 2. Authentication APIs

All authentication endpoints are under `/api/auth`.

---

## 2.1. Register

Register a new account using email and password.

### Request

- Method: `POST`
- URL: `/api/auth/register`
- Auth: No JWT required
- Content-Type: `application/json`

```json
{
  "fullName": "Long Nguyen",
  "email": "long@example.com",
  "password": "password123"
}
```

### Request fields

| Field      | Type   | Required | Rule                                     |
| ---------- | ------ | -------- | ---------------------------------------- |
| `fullName` | string | Yes      | Must not be blank                        |
| `email`    | string | Yes      | Must not be blank, must be a valid email |
| `password` | string | Yes      | Must not be blank, minimum 8 characters  |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Register successfully",
  "data": {
    "message": "Register successfully. OTP has been sent to your email.",
    "email": "long@example.com"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message                   | Reason                                             |
| ------ | ------------------------- | -------------------------------------------------- |
| `400`  | `Validation failed`       | Invalid email, password, or fullName               |
| `400`  | `Validation failed`       | Email already exists                               |
| `500`  | `Unexpected server error` | Unexpected error, for example mail service failure |

---

## 2.2. Verify OTP

Verify the OTP after registration.

### Request

- Method: `POST`
- URL: `/api/auth/verify-otp`
- Auth: No JWT required
- Content-Type: `application/json`

```json
{
  "email": "long@example.com",
  "otp": "123456"
}
```

### Request fields

| Field   | Type   | Required | Rule                                     |
| ------- | ------ | -------- | ---------------------------------------- |
| `email` | string | Yes      | Must not be blank, must be a valid email |
| `otp`   | string | Yes      | Exactly 6 characters                     |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "OTP verified successfully",
  "data": {
    "message": "OTP verified successfully. Account actived."
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                      |
| ------ | -------------------- | --------------------------- |
| `400`  | `Validation failed`  | Invalid email or OTP format |
| `400`  | `Validation failed`  | Invalid OTP                 |
| `400`  | `Validation failed`  | OTP has expired             |
| `400`  | `Validation failed`  | Too many failed attempts    |
| `404`  | `Resource not found` | User or OTP not found       |

---

## 2.3. Login

Log in using email and password, then receive an access token and refresh token.

### Request

- Method: `POST`
- URL: `/api/auth/login`
- Auth: No JWT required
- Content-Type: `application/json`

```json
{
  "email": "long@example.com",
  "password": "password123"
}
```

### Request fields

| Field      | Type   | Required | Rule                                     |
| ---------- | ------ | -------- | ---------------------------------------- |
| `email`    | string | Yes      | Must not be blank, must be a valid email |
| `password` | string | Yes      | Must not be blank                        |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Login successfully",
  "data": {
    "accessToken": "jwt-token",
    "refreshToken": "refresh-token",
    "tokenType": "Bearer",
    "userId": 1,
    "email": "long@example.com",
    "role": "USER"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Frontend usage

After a successful login, store both `data.accessToken` and `data.refreshToken`.

Use the access token when calling private APIs:

```text
Authorization: Bearer <accessToken>
```

Use the refresh token only when calling `/api/auth/refresh` or `/api/auth/logout`.

### Error cases

| Status | Message             | Reason                                     |
| ------ | ------------------- | ------------------------------------------ |
| `400`  | `Validation failed` | Missing or invalid email/password          |
| `400`  | `Validation failed` | Invalid email or password                  |
| `400`  | `Validation failed` | Account has not completed OTP verification |

---

## 2.4. Resend OTP

Resend the registration OTP.

### Request

- Method: `POST`
- URL: `/api/auth/resend-otp`
- Auth: No JWT required
- Content-Type: `application/json`

```json
{
  "email": "long@example.com"
}
```

### Request fields

| Field   | Type   | Required | Rule                                     |
| ------- | ------ | -------- | ---------------------------------------- |
| `email` | string | Yes      | Must not be blank, must be a valid email |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "OTP resent successfully",
  "data": {
    "mesage": "OTP has been resent to your email.",
    "email": "long@example.com"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

> Note: the response field is currently `mesage` because that is the current DTO field in the codebase.

### Error cases

| Status | Message              | Reason                      |
| ------ | -------------------- | --------------------------- |
| `400`  | `Validation failed`  | Missing or invalid email    |
| `400`  | `Validation failed`  | Account is already verified |
| `404`  | `Resource not found` | User not found              |

---

## 2.5. Google OAuth2 Login Redirect Flow

Google OAuth2 login is a browser redirect flow. The frontend should redirect the browser to the backend OAuth2 authorization URL. After Google login finishes, the backend redirects the browser back to the React app.

### Request

- Method: `GET`
- URL: `/oauth2/authorization/google`
- Auth: OAuth2 login flow

The frontend should open this URL in the browser, not call it with Axios as a normal JSON API.

### Success redirect

When Google login succeeds and the backend can create or find the user account, the backend redirects to:

```text
http://localhost:5173/oauth2/redirect?token=<accessToken>&refreshToken=<refreshToken>&email=<email>&userId=<userId>&role=<role>&fullName=<fullName>
```

### Success query parameters

| Parameter      | Type   | Description                   |
| -------------- | ------ | ----------------------------- |
| `token`        | string | JWT access token              |
| `refreshToken` | string | Refresh token                 |
| `email`        | string | User email                    |
| `userId`       | number | User ID                       |
| `role`         | string | User role, for example `USER` |
| `fullName`     | string | Google account display name   |

### Error redirect

When Google login fails, or when the Google email is already registered with another provider, the backend redirects to:

```text
http://localhost:5173/login?error=<error-message>
```

Example:

```text
http://localhost:5173/login?error=Email%20already%20registered%20with%20another%20provider
```

### Frontend usage

1. Redirect the browser to `http://localhost:8080/oauth2/authorization/google`.
2. React handles `/oauth2/redirect`.
3. Read `token`, `refreshToken`, `email`, `userId`, `role`, and `fullName` from query parameters.
4. Store the latest `token` and `refreshToken`.
5. Redirect the user to the dashboard.
6. If the browser returns to `/login?error=...`, show the error message on the login page.

### Backend note

`GET /api/auth/google/success` may still exist internally, but the current frontend flow should not depend on it. The expected frontend integration point is the redirect URL above.

---

## 2.6. Refresh token

Use a refresh token to get a new access token and a new refresh token.

### Request

- Method: `POST`
- URL: `/api/auth/refresh`
- Auth: No access token required
- Content-Type: `application/json`

```json
{
  "refreshToken": "refresh-token"
}
```

### Request fields

| Field          | Type   | Required | Rule              |
| -------------- | ------ | -------- | ----------------- |
| `refreshToken` | string | Yes      | Must not be blank |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "new-jwt-token",
    "refreshToken": "new-refresh-token",
    "tokenType": "Bearer"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Refresh token rotation rule

This backend uses refresh token rotation.

When `/api/auth/refresh` succeeds:

- The old refresh token is revoked.
- The backend returns a new access token.
- The backend returns a new refresh token.
- The frontend must replace both old tokens with the new tokens.

Example:

```text
Login returns: A1 + R1
POST /api/auth/refresh with R1
Backend returns: A2 + R2
Frontend must use A2 + R2 from now on
R1 must not be used again
```

If the frontend uses an old refresh token again, the backend may treat it as refresh token reuse and revoke sessions for security.

### Error cases

| Status | Message             | Reason                         |
| ------ | ------------------- | ------------------------------ |
| `400`  | `Validation failed` | Missing refresh token          |
| `400`  | `Validation failed` | Invalid refresh token          |
| `400`  | `Validation failed` | Refresh token expired          |
| `400`  | `Validation failed` | Refresh token was already used |

---

## 2.7. Logout

Logout by revoking the current refresh token.

### Request

- Method: `POST`
- URL: `/api/auth/logout`
- Auth: No access token required
- Content-Type: `application/json`

```json
{
  "refreshToken": "refresh-token"
}
```

### Request fields

| Field          | Type   | Required | Rule              |
| -------------- | ------ | -------- | ----------------- |
| `refreshToken` | string | Yes      | Must not be blank |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Logged out successfully",
  "data": null,
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message             | Reason                |
| ------ | ------------------- | --------------------- |
| `400`  | `Validation failed` | Missing refresh token |

---

## 2.8. Forgot password

Send a forgot-password OTP to a local email/password account.

### Request

- Method: `POST`
- URL: `/api/auth/forgot-password`
- Auth: No JWT required
- Content-Type: `application/json`

```json
{
  "email": "long@example.com"
}
```

### Request fields

| Field   | Type   | Required | Rule                                     |
| ------- | ------ | -------- | ---------------------------------------- |
| `email` | string | Yes      | Must not be blank, must be a valid email |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Forgot password OTP sent successfully",
  "data": {
    "message": "OTP has been sent to your email.",
    "email": "long@example.com"
  },
  "errors": null,
  "timestamp": "2026-06-16T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                               |
| ------ | -------------------- | ---------------------------------------------------- |
| `400`  | `Validation failed`  | Missing or invalid email                             |
| `400`  | `Validation failed`  | Account does not use password login, for example Google account |
| `404`  | `Resource not found` | User not found                                       |
| `500`  | `Unexpected server error` | Mail service or unexpected server error          |

---

## 2.9. Verify forgot-password OTP

Verify the OTP before resetting the password.

### Request

- Method: `POST`
- URL: `/api/auth/verify-forgot-password-otp`
- Auth: No JWT required
- Content-Type: `application/json`

```json
{
  "email": "long@example.com",
  "otp": "123456"
}
```

### Request fields

| Field   | Type   | Required | Rule                                     |
| ------- | ------ | -------- | ---------------------------------------- |
| `email` | string | Yes      | Must not be blank, must be a valid email |
| `otp`   | string | Yes      | Exactly 6 characters                     |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Forgot password OTP verified successfully",
  "data": {
    "message": "OTP verified successfully. You can reset your password."
  },
  "errors": null,
  "timestamp": "2026-06-16T10:35:00Z"
}
```

### Error cases

| Status | Message              | Reason                    |
| ------ | -------------------- | ------------------------- |
| `400`  | `Validation failed`  | Missing or invalid email/OTP |
| `400`  | `Validation failed`  | Invalid OTP               |
| `400`  | `Validation failed`  | OTP has expired           |
| `400`  | `Validation failed`  | Too many failed attempts  |
| `404`  | `Resource not found` | User or OTP not found     |

---

## 2.10. Reset password

Reset the password after the forgot-password OTP has been verified.

### Request

- Method: `POST`
- URL: `/api/auth/reset-password`
- Auth: No JWT required
- Content-Type: `application/json`

```json
{
  "email": "long@example.com",
  "newPassword": "newPassword123"
}
```

### Request fields

| Field         | Type   | Required | Rule                                     |
| ------------- | ------ | -------- | ---------------------------------------- |
| `email`       | string | Yes      | Must not be blank, must be a valid email |
| `newPassword` | string | Yes      | Must not be blank, minimum 8 characters  |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Password reset sucessfully",
  "data": {
    "message": "Password reset successfully."
  },
  "errors": null,
  "timestamp": "2026-06-16T10:40:00Z"
}
```

> Note: the top-level message is currently `Password reset sucessfully` because that is the current message in the codebase.

### Error cases

| Status | Message              | Reason                                   |
| ------ | -------------------- | ---------------------------------------- |
| `400`  | `Validation failed`  | Missing or invalid email/newPassword     |
| `400`  | `Validation failed`  | OTP has not been verified                |
| `404`  | `Resource not found` | User not found or OTP verification missing |

---

## 2.11. Recommended frontend token flow

```text
Login or Google login
-> Store accessToken and refreshToken
-> Call private APIs with accessToken
-> If private API returns 401 because accessToken expired
-> Call POST /api/auth/refresh with refreshToken
-> Store new accessToken and new refreshToken
-> Retry the failed private API request
-> On logout, call POST /api/auth/logout with the latest refreshToken
```

Important frontend notes:

- Always store the latest refresh token returned by `/api/auth/refresh`.
- Do not reuse an old refresh token after a successful refresh.
- Avoid sending multiple refresh requests at the same time with the same refresh token.

---

## 3. Document APIs

Private APIs require this header:

```text
Authorization: Bearer <accessToken>
```

Current public document APIs:

- `GET /api/documents/public`
- `GET /api/documents/public/{documentId}`
- `GET /api/documents/share-link/{token}`
- `GET /api/documents/share-link/{token}/preview-url`
- `GET /api/documents/share-link/{token}/download-url`

---

## 3.1. Document response object

Document APIs usually return a `DocumentUploadResponse` object:

```json
{
  "documentId": 1,
  "userId": 1,
  "folderId": null,
  "originalFileName": "example.pdf",
  "s3Key": "documents/1/uuid-example.pdf",
  "contentType": "application/pdf",
  "fileSize": 123456,
  "isPublic": false,
  "isDeleted": false,
  "isStarred": false,
  "status": "READY",
  "uploadedAt": "2026-06-14T10:30:00Z",
  "deletedAt": null
}
```

### Document fields

| Field              | Type          | Description                                  |
| ------------------ | ------------- | -------------------------------------------- |
| `documentId`       | number        | Document ID                                  |
| `userId`           | number        | Owner user ID                                |
| `folderId`         | number / null | Folder ID if the document is inside a folder |
| `originalFileName` | string        | Sanitized original file name                 |
| `s3Key`            | string        | S3 object key                                |
| `contentType`      | string        | MIME type                                    |
| `fileSize`         | number        | File size in bytes                           |
| `isPublic`         | boolean       | Public/private visibility                    |
| `isDeleted`        | boolean       | Soft delete flag                             |
| `isStarred`        | boolean       | Whether the document is starred by the owner |
| `status`           | string        | Document processing status                   |
| `uploadedAt`       | string        | Upload time                                  |
| `deletedAt`        | string / null | Soft delete time                             |

### Document status

| Status     | Description                                     |
| ---------- | ----------------------------------------------- |
| `UPLOADED` | File was uploaded and metadata was saved        |
| `PARSING`  | The system is extracting document text          |
| `INDEXING` | The system is chunking/embedding/indexing       |
| `READY`    | The document is ready for future RAG/chat usage |
| `FAILED`   | Parsing or indexing failed                      |

---

## 3.2. Upload document

Upload a file to S3 and save metadata. Parsing, chunking, and embedding run in a background job after the upload request succeeds.

### Request

- Method: `POST`
- URL: `/api/documents/upload`
- Auth: JWT required
- Content-Type: `multipart/form-data`

### Form-data fields

| Field      | Type    | Required | Rule                                             |
| ---------- | ------- | -------- | ------------------------------------------------ |
| `file`     | File    | Yes      | Uploaded file                                    |
| `isPublic` | boolean | No       | `true` or `false`; private by default if omitted |

### Supported files

Limits:

- Normal document/image maximum size: `20MB`.
- Video maximum size: controlled by `APP_MAX_VIDEO_FILE_SIZE`, default `52428800` bytes (`50MB`).
- Backend multipart limit is controlled by `APP_MAX_MULTIPART_FILE_SIZE` and `APP_MAX_MULTIPART_REQUEST_SIZE`, default `50MB`.
- Empty files are rejected.

Directly supported extensions:

- `pdf`
- `doc`
- `docx`
- `pptx`
- `xls`
- `xlsx`
- `png`
- `mp4`
- `mov`
- `avi`
- `webm`

Files whose `Content-Type` starts with `image/` are also accepted.
Files whose `Content-Type` starts with `video/` are also accepted.

### Upload processing behavior

| File type | Storage | Parsing/indexing behavior |
| --------- | ------- | ------------------------- |
| PDF / DOC / DOCX / PPTX / XLS / XLSX | Uploaded to private S3 and metadata saved to database | Text is extracted, chunked, embedded, then saved to `document_chunk` |
| Image files | Uploaded to private S3 and metadata saved to database | OCR can extract text if Tesseract is enabled; otherwise indexing may fail or produce no useful text |
| Video files | Uploaded to private S3 and metadata saved to database | Current backend stores a placeholder chunk: `[VIDEO] Transcript pending...`; real video transcription is not implemented yet |

Important behavior:

- Upload response returns quickly after S3 upload and metadata save.
- The returned document status is usually `UPLOADED`.
- Backend continues processing asynchronously: `UPLOADED` -> `PARSING` -> `INDEXING` -> `READY`.
- If parsing, embedding, or indexing fails, status becomes `FAILED`.
- Frontend should poll document detail/list APIs and enable AI chat only when status is `READY`.

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Upload document successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "folderId": null,
    "originalFileName": "example.pdf",
    "s3Key": "documents/1/uuid-example.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": true,
    "isDeleted": false,
    "isStarred": false,
    "status": "UPLOADED",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                 |
| ------ | -------------------- | ------------------------------------------------------ |
| `400`  | `Validation failed`  | Empty file, missing filename, or unsupported extension |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                 |
| `413`  | `File upload failed` | Uploaded file exceeds the size limit                   |
| `500`  | `File read failed`   | Backend failed to read the file                        |
| `503`  | `S3 upload failed`   | S3 upload failed                                       |

---

## 3.3. Get my documents

Get documents that belong to the currently authenticated user.

### Request

- Method: `GET`
- URL: `/api/documents/my`
- Auth: JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get my documents successfully",
  "data": [
    {
      "documentId": 1,
      "userId": 1,
      "folderId": null,
      "originalFileName": "example.pdf",
      "s3Key": "documents/1/uuid-example.pdf",
      "contentType": "application/pdf",
      "fileSize": 123456,
      "isPublic": false,
      "isDeleted": false,
      "isStarred": false,
      "status": "READY",
      "uploadedAt": "2026-06-14T10:30:00Z",
      "deletedAt": null
    }
  ],
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message        | Reason                 |
| ------ | -------------- | ---------------------- |
| `401`  | `Unauthorized` | Missing or invalid JWT |

---

## 3.3.1. Get starred documents

Get active starred documents of the currently authenticated user.

### Request

- Method: `GET`
- URL: `/api/documents/starred`
- Auth: JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get starred documents successfully",
  "data": [
    {
      "documentId": 1,
      "userId": 1,
      "folderId": null,
      "originalFileName": "example.pdf",
      "s3Key": "documents/1/uuid-example.pdf",
      "contentType": "application/pdf",
      "fileSize": 123456,
      "isPublic": false,
      "isDeleted": false,
      "isStarred": true,
      "status": "READY",
      "uploadedAt": "2026-06-15T10:30:00Z",
      "deletedAt": null
    }
  ],
  "errors": null,
  "timestamp": "2026-06-15T10:30:00Z"
}
```

### Error cases

| Status | Message        | Reason                 |
| ------ | -------------- | ---------------------- |
| `401`  | `Unauthorized` | Missing or invalid JWT |

---

## 3.4. Get my document detail

Get the detail of a document owned by the current user.

### Request

- Method: `GET`
- URL: `/api/documents/{documentId}`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get document detail successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "folderId": null,
    "originalFileName": "example.pdf",
    "s3Key": "documents/1/uuid-example.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": false,
    "isDeleted": false,
    "isStarred": false,
    "status": "READY",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                                    |
| ------ | -------------------- | ------------------------------------------------------------------------- |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                                    |
| `404`  | `Resource not found` | Document does not exist, does not belong to the user, or was soft-deleted |

---

## 3.4.1. Rename my document

Rename the document metadata field `originalFileName`. This does not rename or move the physical file in S3.

### Request

- Method: `PATCH`
- URL: `/api/documents/{documentId}/rename`
- Auth: JWT required
- Content-Type: `application/json`

```json
{
  "originalFileName": "new-file-name.pdf"
}
```

### Request fields

| Field              | Type   | Required | Rule                                      |
| ------------------ | ------ | -------- | ----------------------------------------- |
| `originalFileName` | string | Yes      | Must not be blank, maximum 512 characters |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Rename document successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "folderId": null,
    "originalFileName": "new-file-name.pdf",
    "s3Key": "documents/1/uuid-example.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": false,
    "isDeleted": false,
    "isStarred": false,
    "status": "READY",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                                    |
| ------ | -------------------- | ------------------------------------------------------------------------- |
| `400`  | `Validation failed`  | Missing or invalid originalFileName                                       |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                                    |
| `404`  | `Resource not found` | Document does not exist, does not belong to the user, or was soft-deleted |

---

## 3.4.2. Move my document to folder

Move an owned active document into a folder, or remove it from the current folder.

### Request

- Method: `PATCH`
- URL: `/api/documents/{documentId}/folder`
- Auth: JWT required
- Content-Type: `application/json`

Move into a folder:

```json
{
  "folderId": 1
}
```

Remove from folder:

```json
{
  "folderId": null
}
```

### Request fields

| Field      | Type          | Required | Rule                                        |
| ---------- | ------------- | -------- | ------------------------------------------- |
| `folderId` | number / null | No       | Must belong to the current user if provided |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Move document to folder successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "folderId": 1,
    "originalFileName": "example.pdf",
    "s3Key": "documents/1/uuid-example.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": false,
    "isDeleted": false,
    "isStarred": false,
    "status": "READY",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                        |
| ------ | -------------------- | ------------------------------------------------------------- |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                        |
| `404`  | `Resource not found` | Document/folder does not exist or does not belong to the user |

---

## 3.4.3. Get my document preview URL

Get a temporary pre-signed URL for previewing an owned document.

### Request

- Method: `GET`
- URL: `/api/documents/{documentId}/preview-url`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get document preview URL successfully",
  "data": {
    "url": "https://s3-presigned-url",
    "expiresAt": "2026-06-14T10:40:00Z",
    "fileName": "example.pdf",
    "contentType": "application/pdf"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                                    |
| ------ | -------------------- | ------------------------------------------------------------------------- |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                                    |
| `404`  | `Resource not found` | Document does not exist, does not belong to the user, or was soft-deleted |
| `500`  | `Request failed`     | S3 pre-signer is not configured                                           |

---

## 3.4.4. Get my document download URL

Get a temporary pre-signed URL for downloading an owned document.

### Request

- Method: `GET`
- URL: `/api/documents/{documentId}/download-url`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get document download URL successfully",
  "data": {
    "url": "https://s3-presigned-url",
    "expiresAt": "2026-06-14T10:40:00Z",
    "fileName": "example.pdf",
    "contentType": "application/pdf"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                                    |
| ------ | -------------------- | ------------------------------------------------------------------------- |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                                    |
| `404`  | `Resource not found` | Document does not exist, does not belong to the user, or was soft-deleted |
| `500`  | `Request failed`     | S3 pre-signer is not configured                                           |

---

## 3.5. Get public documents

Get public documents for the community page.

### Request

- Method: `GET`
- URL: `/api/documents/public`
- Auth: Public, no JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get public documents successfully",
  "data": [
    {
      "documentId": 2,
      "userId": 1,
      "folderId": null,
      "originalFileName": "public-file.pdf",
      "s3Key": "documents/1/uuid-public-file.pdf",
      "contentType": "application/pdf",
      "fileSize": 123456,
      "isPublic": true,
      "isDeleted": false,
      "isStarred": false,
      "status": "READY",
      "uploadedAt": "2026-06-14T10:30:00Z",
      "deletedAt": null
    }
  ],
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

---

## 3.6. Get public document detail

Get the detail of a public document.

### Request

- Method: `GET`
- URL: `/api/documents/public/{documentId}`
- Auth: Public, no JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get public document detail successfully",
  "data": {
    "documentId": 2,
    "userId": 1,
    "folderId": null,
    "originalFileName": "public-file.pdf",
    "s3Key": "documents/1/uuid-public-file.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": true,
    "isDeleted": false,
    "isStarred": false,
    "status": "READY",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                          |
| ------ | -------------------- | ----------------------------------------------- |
| `404`  | `Resource not found` | Public document does not exist or is not public |

---

## 3.6.1. Get public document preview URL

Get a temporary pre-signed URL for previewing a public document.

### Request

- Method: `GET`
- URL: `/api/documents/public/{documentId}/preview-url`
- Auth: Public, no JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get public document preview URL successfully",
  "data": {
    "url": "https://s3-presigned-url",
    "expiresAt": "2026-06-14T10:40:00Z",
    "fileName": "public-file.pdf",
    "contentType": "application/pdf"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                          |
| ------ | -------------------- | ----------------------------------------------- |
| `404`  | `Resource not found` | Public document does not exist or is not public |
| `500`  | `Request failed`     | S3 pre-signer is not configured                 |

---

## 3.6.2. Get public document download URL

Get a temporary pre-signed URL for downloading a public document.

### Request

- Method: `GET`
- URL: `/api/documents/public/{documentId}/download-url`
- Auth: Public, no JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get public document download URL successfully",
  "data": {
    "url": "https://s3-presigned-url",
    "expiresAt": "2026-06-14T10:40:00Z",
    "fileName": "public-file.pdf",
    "contentType": "application/pdf"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                          |
| ------ | -------------------- | ----------------------------------------------- |
| `404`  | `Resource not found` | Public document does not exist or is not public |
| `500`  | `Request failed`     | S3 pre-signer is not configured                 |

---

## 3.7. Update document visibility

Update a document to public or private.

### Request

- Method: `PATCH`
- URL: `/api/documents/{documentId}/visibility?isPublic=true`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Query params

| Name       | Type    | Required | Example           |
| ---------- | ------- | -------- | ----------------- |
| `isPublic` | boolean | Yes      | `true` or `false` |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Update document visibility successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "originalFileName": "example.pdf",
    "s3Key": "documents/1/uuid-example.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": true,
    "isDeleted": false,
    "isStarred": false,
    "status": "READY",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                               |
| ------ | -------------------- | -------------------------------------------------------------------- |
| `400`  | `Validation failed`  | Missing `isPublic`                                                   |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                               |
| `404`  | `Resource not found` | Document does not exist, does not belong to the user, or is in Trash |

---

## 3.7.1. Update document starred

Star or unstar an owned active document.

### Request

- Method: `PATCH`
- URL: `/api/documents/{documentId}/star?isStarred=true`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Query params

| Name        | Type    | Required | Example           |
| ----------- | ------- | -------- | ----------------- |
| `isStarred` | boolean | Yes      | `true` or `false` |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Update document starred successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "folderId": null,
    "originalFileName": "example.pdf",
    "s3Key": "documents/1/uuid-example.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": false,
    "isDeleted": false,
    "isStarred": true,
    "status": "READY",
    "uploadedAt": "2026-06-15T10:30:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-15T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                               |
| ------ | -------------------- | -------------------------------------------------------------------- |
| `400`  | `Validation failed`  | Missing `isStarred`                                                  |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                               |
| `404`  | `Resource not found` | Document does not exist, does not belong to the user, or is in Trash |

---

## 3.8. Move document to Trash

Soft-delete a document. The physical file is not deleted from S3.

### Request

- Method: `DELETE`
- URL: `/api/documents/{documentId}`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Move document to trash successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "originalFileName": "example.pdf",
    "s3Key": "documents/1/uuid-example.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": false,
    "isDeleted": true,
    "isStarred": false,
    "status": "READY",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": "2026-06-14T10:40:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:40:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                 |
| ------ | -------------------- | ------------------------------------------------------ |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                 |
| `404`  | `Resource not found` | Document does not exist or does not belong to the user |

---

## 3.9. Get trash documents

Get soft-deleted documents of the current user.

### Request

- Method: `GET`
- URL: `/api/documents/trash`
- Auth: JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get trash documents successfully",
  "data": [
    {
      "documentId": 1,
      "userId": 1,
      "originalFileName": "example.pdf",
      "s3Key": "documents/1/uuid-example.pdf",
      "contentType": "application/pdf",
      "fileSize": 123456,
      "isPublic": false,
      "isDeleted": true,
      "isStarred": false,
      "status": "READY",
      "uploadedAt": "2026-06-14T10:30:00Z",
      "deletedAt": "2026-06-14T10:40:00Z"
    }
  ],
  "errors": null,
  "timestamp": "2026-06-14T10:40:00Z"
}
```

### Error cases

| Status | Message        | Reason                 |
| ------ | -------------- | ---------------------- |
| `401`  | `Unauthorized` | Missing or invalid JWT |

---

## 3.10. Restore document

Restore a document from Trash.

### Request

- Method: `POST`
- URL: `/api/documents/{documentId}/restore`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Restore document successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "originalFileName": "example.pdf",
    "s3Key": "documents/1/uuid-example.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": false,
    "isDeleted": false,
    "isStarred": false,
    "status": "READY",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-14T10:45:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                 |
| ------ | -------------------- | ------------------------------------------------------ |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                 |
| `404`  | `Resource not found` | Document does not exist or does not belong to the user |

---

## 3.11. Delete document permanently

Permanently delete a document. Only documents already in Trash can be permanently deleted.

### Request

- Method: `DELETE`
- URL: `/api/documents/{documentId}/permanent`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Delete document permanently successfully",
  "data": null,
  "errors": null,
  "timestamp": "2026-06-14T10:50:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                 |
| ------ | -------------------- | ------------------------------------------------------ |
| `400`  | `Validation failed`  | Document is not in Trash                               |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                 |
| `404`  | `Resource not found` | Document does not exist or does not belong to the user |
| `503`  | `S3 delete failed`   | Failed to delete the file from S3                      |

---

## 3.12. Share document APIs

Document sharing supports two flows:

1. Share by public token link.
2. Share directly with another user by email.

Private share management APIs require:

```text
Authorization: Bearer <accessToken>
```

Share-link read APIs are public because anyone with the valid token can access the shared document.

---

## 3.12.1. Document share link response object

Share link APIs return a `DocumentShareLinkResponse` object:

```json
{
  "shareLinkId": 1,
  "documentId": 1,
  "token": "c1a2b3token",
  "accessPath": "/api/documents/share-link/c1a2b3token",
  "enabled": true,
  "expiresAt": null,
  "createdAt": "2026-06-19T10:30:00Z"
}
```

### Share link fields

| Field | Type | Description |
|---|---|---|
| `shareLinkId` | number | Share link ID |
| `documentId` | number | Shared document ID |
| `token` | string | Random share token |
| `accessPath` | string | Backend path for opening the shared document |
| `enabled` | boolean | Whether the share link is currently active |
| `expiresAt` | string / null | Expiration time; currently may be `null` |
| `createdAt` | string | Share link creation time |

---

## 3.12.2. Document user share response object

User share APIs return a `DocumentShareResponse` object:

```json
{
  "documentShareId": 1,
  "documentId": 1,
  "ownerId": 1,
  "sharedWithUserId": 2,
  "sharedWithEmail": "friend@example.com",
  "sharedWithName": "Friend User",
  "createdAt": "2026-06-19T10:30:00Z"
}
```

### User share fields

| Field | Type | Description |
|---|---|---|
| `documentShareId` | number | Document share record ID |
| `documentId` | number | Shared document ID |
| `ownerId` | number | Owner user ID |
| `sharedWithUserId` | number | User ID of the receiver |
| `sharedWithEmail` | string | Email of the receiver |
| `sharedWithName` | string | Full name of the receiver |
| `createdAt` | string | Share creation time |

---

## 3.12.3. Create document share link

Create or reuse an active share link for an owned active document.

### Request

- Method: `POST`
- URL: `/api/documents/{documentId}/share-link`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `documentId` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Create document share link successfully",
  "data": {
    "shareLinkId": 1,
    "documentId": 1,
    "token": "c1a2b3token",
    "accessPath": "/api/documents/share-link/c1a2b3token",
    "enabled": true,
    "expiresAt": null,
    "createdAt": "2026-06-19T10:30:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-19T10:30:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Document does not exist, does not belong to the user, or was soft-deleted |

---

## 3.12.4. Disable document share link

Disable the active share link of an owned active document.

### Request

- Method: `DELETE`
- URL: `/api/documents/{documentId}/share-link`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `documentId` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Disable document share link successfully",
  "data": {
    "shareLinkId": 1,
    "documentId": 1,
    "token": "c1a2b3token",
    "accessPath": "/api/documents/share-link/c1a2b3token",
    "enabled": false,
    "expiresAt": null,
    "createdAt": "2026-06-19T10:30:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-19T10:35:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Document or active share link not found |

---

## 3.12.5. Get document by share link

Get shared document metadata using a share token.

### Request

- Method: `GET`
- URL: `/api/documents/share-link/{token}`
- Auth: Public, no JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `token` | string | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get shared document successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "folderId": null,
    "originalFileName": "shared-file.pdf",
    "s3Key": "documents/1/uuid-shared-file.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": false,
    "isDeleted": false,
    "isStarred": false,
    "status": "READY",
    "uploadedAt": "2026-06-19T10:00:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-19T10:40:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing token |
| `404` | `Resource not found` | Share link not found, disabled, expired, or document was soft-deleted |

---

## 3.12.6. Get share-link preview URL

Get a temporary pre-signed URL for previewing a document by share token.

### Request

- Method: `GET`
- URL: `/api/documents/share-link/{token}/preview-url`
- Auth: Public, no JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `token` | string | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get shared document preview URL successfully",
  "data": {
    "url": "https://s3-presigned-url",
    "expiresAt": "2026-06-19T10:50:00Z",
    "fileName": "shared-file.pdf",
    "contentType": "application/pdf"
  },
  "errors": null,
  "timestamp": "2026-06-19T10:40:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing token |
| `404` | `Resource not found` | Share link not found, disabled, expired, or document was soft-deleted |
| `500` | `Request failed` | S3 pre-signer is not configured |

---

## 3.12.7. Get share-link download URL

Get a temporary pre-signed URL for downloading a document by share token.

### Request

- Method: `GET`
- URL: `/api/documents/share-link/{token}/download-url`
- Auth: Public, no JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `token` | string | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get shared document download URL successfully",
  "data": {
    "url": "https://s3-presigned-url",
    "expiresAt": "2026-06-19T10:50:00Z",
    "fileName": "shared-file.pdf",
    "contentType": "application/pdf"
  },
  "errors": null,
  "timestamp": "2026-06-19T10:40:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing token |
| `404` | `Resource not found` | Share link not found, disabled, expired, or document was soft-deleted |
| `500` | `Request failed` | S3 pre-signer is not configured |

---

## 3.12.8. Share document with user

Share an owned active document with another user by email. The receiver must already be a friend.

### Request

- Method: `POST`
- URL: `/api/documents/{documentId}/shares/users`
- Auth: JWT required
- Content-Type: `application/json`

```json
{
  "email": "friend@example.com"
}
```

### Path variables

| Name | Type | Required |
|---|---|---|
| `documentId` | number | Yes |

### Request fields

| Field | Type | Required | Rule |
|---|---|---|---|
| `email` | string | Yes | Must not be blank, must be a valid email |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Share document with user successfully",
  "data": {
    "documentShareId": 1,
    "documentId": 1,
    "ownerId": 1,
    "sharedWithUserId": 2,
    "sharedWithEmail": "friend@example.com",
    "sharedWithName": "Friend User",
    "createdAt": "2026-06-19T10:45:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-19T10:45:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing or invalid email |
| `400` | `Validation failed` | Sharing with yourself, receiver is not a friend, or document already shared with this user |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Document or receiver user not found |

---

## 3.12.9. Remove user share

Remove a direct document share from a user.

### Request

- Method: `DELETE`
- URL: `/api/documents/{documentId}/shares/users/{userId}`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `documentId` | number | Yes |
| `userId` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Remove document share successfully",
  "data": null,
  "errors": null,
  "timestamp": "2026-06-19T10:50:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Document or document share not found |

---

## 3.12.10. Get documents shared with me

Get active documents directly shared with the authenticated user.

### Request

- Method: `GET`
- URL: `/api/documents/shared-with-me`
- Auth: JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get documents shared with me successfully",
  "data": [
    {
      "documentId": 1,
      "userId": 1,
      "folderId": null,
      "originalFileName": "shared-file.pdf",
      "s3Key": "documents/1/uuid-shared-file.pdf",
      "contentType": "application/pdf",
      "fileSize": 123456,
      "isPublic": false,
      "isDeleted": false,
      "isStarred": false,
      "status": "READY",
      "uploadedAt": "2026-06-19T10:00:00Z",
      "deletedAt": null
    }
  ],
  "errors": null,
  "timestamp": "2026-06-19T10:55:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |

---

## 3.12.11. Get shared-with-me document detail

Get detail of an active document directly shared with the authenticated user.

### Request

- Method: `GET`
- URL: `/api/documents/shared-with-me/{documentId}`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `documentId` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get shared document detail successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "folderId": null,
    "originalFileName": "shared-file.pdf",
    "s3Key": "documents/1/uuid-shared-file.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": false,
    "isDeleted": false,
    "isStarred": false,
    "status": "READY",
    "uploadedAt": "2026-06-19T10:00:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-19T10:55:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Shared document not found or was soft-deleted |

---

## 3.12.12. Get shared-with-me preview URL

Get a temporary pre-signed URL for previewing a document directly shared with the authenticated user.

### Request

- Method: `GET`
- URL: `/api/documents/shared-with-me/{documentId}/preview-url`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `documentId` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get shared document preview URL successfully",
  "data": {
    "url": "https://s3-presigned-url",
    "expiresAt": "2026-06-19T11:05:00Z",
    "fileName": "shared-file.pdf",
    "contentType": "application/pdf"
  },
  "errors": null,
  "timestamp": "2026-06-19T10:55:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Shared document not found or was soft-deleted |
| `500` | `Request failed` | S3 pre-signer is not configured |

---

## 3.12.13. Get shared-with-me download URL

Get a temporary pre-signed URL for downloading a document directly shared with the authenticated user.

### Request

- Method: `GET`
- URL: `/api/documents/shared-with-me/{documentId}/download-url`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `documentId` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get shared document download URL successfully",
  "data": {
    "url": "https://s3-presigned-url",
    "expiresAt": "2026-06-19T11:05:00Z",
    "fileName": "shared-file.pdf",
    "contentType": "application/pdf"
  },
  "errors": null,
  "timestamp": "2026-06-19T10:55:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Shared document not found or was soft-deleted |
| `500` | `Request failed` | S3 pre-signer is not configured |

---

## 4. Document Folder APIs

Document folder APIs allow users to organize documents into simple personal folders. A folder only has a name. Folders are private and require JWT.

Private folder APIs require:

```text
Authorization: Bearer <accessToken>
```

---

## 4.1. Document folder response object

Folder APIs return a `DocumentFolderResponse` object:

```json
{
  "folderId": 1,
  "userId": 1,
  "name": "Semester 1",
  "isStarred": false,
  "createdAt": "2026-06-15T10:30:00Z",
  "updatedAt": "2026-06-15T10:30:00Z"
}
```

### Folder fields

| Field       | Type    | Description                                |
| ----------- | ------- | ------------------------------------------ |
| `folderId`  | number  | Folder ID                                  |
| `userId`    | number  | Owner user ID                              |
| `name`      | string  | Folder name                                |
| `isStarred` | boolean | Whether the folder is starred by the owner |
| `createdAt` | string  | Folder creation time                       |
| `updatedAt` | string  | Last update time                           |

---

## 4.2. Create document folder

Create a new personal document folder.

### Request

- Method: `POST`
- URL: `/api/document-folders`
- Auth: JWT required
- Content-Type: `application/json`

```json
{
  "name": "Semester 1"
}
```

### Request fields

| Field  | Type   | Required | Rule                                      |
| ------ | ------ | -------- | ----------------------------------------- |
| `name` | string | Yes      | Must not be blank, maximum 100 characters |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Create document folder successfully",
  "data": {
    "folderId": 1,
    "userId": 1,
    "name": "Semester 1",
    "createdAt": "2026-06-15T10:30:00Z",
    "updatedAt": "2026-06-15T10:30:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-15T10:30:00Z"
}
```

### Error cases

| Status | Message             | Reason                                          |
| ------ | ------------------- | ----------------------------------------------- |
| `400`  | `Validation failed` | Missing or invalid name                         |
| `400`  | `Validation failed` | Folder name already exists for the current user |
| `401`  | `Unauthorized`      | Missing or invalid JWT                          |

---

## 4.3. Get my document folders

Get all folders owned by the authenticated user.

### Request

- Method: `GET`
- URL: `/api/document-folders`
- Auth: JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get my document folders successfully",
  "data": [
    {
      "folderId": 1,
      "userId": 1,
      "name": "Semester 1",
      "isStarred": false,
      "createdAt": "2026-06-15T10:30:00Z",
      "updatedAt": "2026-06-15T10:30:00Z"
    }
  ],
  "errors": null,
  "timestamp": "2026-06-15T10:30:00Z"
}
```

### Error cases

| Status | Message        | Reason                 |
| ------ | -------------- | ---------------------- |
| `401`  | `Unauthorized` | Missing or invalid JWT |

---

## 4.3.1. Get starred document folders

Get starred folders owned by the authenticated user.

### Request

- Method: `GET`
- URL: `/api/document-folders/starred`
- Auth: JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get starred document folders successfully",
  "data": [
    {
      "folderId": 1,
      "userId": 1,
      "name": "Semester 1",
      "isStarred": true,
      "createdAt": "2026-06-15T10:30:00Z",
      "updatedAt": "2026-06-15T10:30:00Z"
    }
  ],
  "errors": null,
  "timestamp": "2026-06-15T10:30:00Z"
}
```

### Error cases

| Status | Message        | Reason                 |
| ------ | -------------- | ---------------------- |
| `401`  | `Unauthorized` | Missing or invalid JWT |

---

## 4.4. Update document folder

Rename a folder owned by the authenticated user.

### Request

- Method: `PATCH`
- URL: `/api/document-folders/{folderId}`
- Auth: JWT required
- Content-Type: `application/json`

```json
{
  "name": "Semester 2"
}
```

### Path variables

| Name       | Type   | Required |
| ---------- | ------ | -------- |
| `folderId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Update document folder successfully",
  "data": {
    "folderId": 1,
    "userId": 1,
    "name": "Semester 2",
    "isStarred": false,
    "createdAt": "2026-06-15T10:30:00Z",
    "updatedAt": "2026-06-15T10:40:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-15T10:40:00Z"
}
```

### Error cases

| Status | Message              | Reason                                               |
| ------ | -------------------- | ---------------------------------------------------- |
| `400`  | `Validation failed`  | Missing or invalid name                              |
| `400`  | `Validation failed`  | Folder name already exists for the current user      |
| `401`  | `Unauthorized`       | Missing or invalid JWT                               |
| `404`  | `Resource not found` | Folder does not exist or does not belong to the user |

---

## 4.4.1. Update document folder starred

Star or unstar a folder owned by the authenticated user.

### Request

- Method: `PATCH`
- URL: `/api/document-folders/{folderId}/star?isStarred=true`
- Auth: JWT required

### Path variables

| Name       | Type   | Required |
| ---------- | ------ | -------- |
| `folderId` | number | Yes      |

### Query params

| Name        | Type    | Required | Example           |
| ----------- | ------- | -------- | ----------------- |
| `isStarred` | boolean | Yes      | `true` or `false` |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Update document folder starred successfully",
  "data": {
    "folderId": 1,
    "userId": 1,
    "name": "Semester 1",
    "isStarred": true,
    "createdAt": "2026-06-15T10:30:00Z",
    "updatedAt": "2026-06-15T10:40:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-15T10:40:00Z"
}
```

### Error cases

| Status | Message              | Reason                                               |
| ------ | -------------------- | ---------------------------------------------------- |
| `400`  | `Validation failed`  | Missing `isStarred`                                  |
| `401`  | `Unauthorized`       | Missing or invalid JWT                               |
| `404`  | `Resource not found` | Folder does not exist or does not belong to the user |

---

## 4.5. Delete document folder

Delete a folder owned by the authenticated user. Documents inside the folder are not deleted; their `folderId` is set to `null`.

### Request

- Method: `DELETE`
- URL: `/api/document-folders/{folderId}`
- Auth: JWT required

### Path variables

| Name       | Type   | Required |
| ---------- | ------ | -------- |
| `folderId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Delete document folder successfully",
  "data": null,
  "errors": null,
  "timestamp": "2026-06-15T10:45:00Z"
}
```

### Error cases

| Status | Message              | Reason                                               |
| ------ | -------------------- | ---------------------------------------------------- |
| `401`  | `Unauthorized`       | Missing or invalid JWT                               |
| `404`  | `Resource not found` | Folder does not exist or does not belong to the user |

---

## 4.6. Get documents in folder

Get active documents inside an owned folder.

### Request

- Method: `GET`
- URL: `/api/document-folders/{folderId}/documents`
- Auth: JWT required

### Path variables

| Name       | Type   | Required |
| ---------- | ------ | -------- |
| `folderId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get document folder documents successfully",
  "data": [
    {
      "documentId": 1,
      "userId": 1,
      "folderId": 1,
      "originalFileName": "example.pdf",
      "s3Key": "documents/1/uuid-example.pdf",
      "contentType": "application/pdf",
      "fileSize": 123456,
      "isPublic": false,
      "isDeleted": false,
      "isStarred": false,
      "status": "READY",
      "uploadedAt": "2026-06-15T10:30:00Z",
      "deletedAt": null
    }
  ],
  "errors": null,
  "timestamp": "2026-06-15T10:50:00Z"
}
```

### Error cases

| Status | Message              | Reason                                               |
| ------ | -------------------- | ---------------------------------------------------- |
| `401`  | `Unauthorized`       | Missing or invalid JWT                               |
| `404`  | `Resource not found` | Folder does not exist or does not belong to the user |

---

## 5. Tag APIs

Tag APIs allow users to create custom document tags with custom colors and attach them to documents.

Private tag APIs require:

```text
Authorization: Bearer <accessToken>
```

---

## 5.1. Tag response object

Tag APIs return a `TagResponse` object:

```json
{
  "tagId": 1,
  "userId": 1,
  "name": "AI",
  "color": "#8B5CF6",
  "createdAt": "2026-06-14T10:30:00Z"
}
```

### Tag fields

| Field       | Type   | Description                      |
| ----------- | ------ | -------------------------------- |
| `tagId`     | number | Tag ID                           |
| `userId`    | number | Owner user ID                    |
| `name`      | string | Tag name                         |
| `color`     | string | HEX color, for example `#8B5CF6` |
| `createdAt` | string | Tag creation time                |

---

## 5.2. Create tag

Create a new personal tag for the authenticated user.

### Request

- Method: `POST`
- URL: `/api/tags`
- Auth: JWT required
- Content-Type: `application/json`

```json
{
  "name": "AI",
  "color": "#8B5CF6"
}
```

### Request fields

| Field   | Type   | Required | Rule                                                       |
| ------- | ------ | -------- | ---------------------------------------------------------- |
| `name`  | string | Yes      | Must not be blank, maximum 100 characters                  |
| `color` | string | Yes      | Must be a valid HEX color, for example `#8B5CF6` or `#FFF` |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Create tag successfully",
  "data": {
    "tagId": 1,
    "userId": 1,
    "name": "AI",
    "color": "#8B5CF6",
    "createdAt": "2026-06-14T10:30:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message             | Reason                                       |
| ------ | ------------------- | -------------------------------------------- |
| `400`  | `Validation failed` | Missing/invalid name or color                |
| `400`  | `Validation failed` | Tag name already exists for the current user |
| `401`  | `Unauthorized`      | Missing or invalid JWT                       |

---

## 5.3. Get my tags

Get all tags owned by the authenticated user.

### Request

- Method: `GET`
- URL: `/api/tags`
- Auth: JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get my tags successfully",
  "data": [
    {
      "tagId": 1,
      "userId": 1,
      "name": "AI",
      "color": "#8B5CF6",
      "createdAt": "2026-06-14T10:30:00Z"
    }
  ],
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message        | Reason                 |
| ------ | -------------- | ---------------------- |
| `401`  | `Unauthorized` | Missing or invalid JWT |

---

## 5.4. Update tag

Update a tag owned by the authenticated user.

### Request

- Method: `PATCH`
- URL: `/api/tags/{tagId}`
- Auth: JWT required
- Content-Type: `application/json`

```json
{
  "name": "Machine Learning",
  "color": "#22C55E"
}
```

### Path variables

| Name    | Type   | Required |
| ------- | ------ | -------- |
| `tagId` | number | Yes      |

### Request fields

| Field   | Type   | Required | Rule                                      |
| ------- | ------ | -------- | ----------------------------------------- |
| `name`  | string | Yes      | Must not be blank, maximum 100 characters |
| `color` | string | Yes      | Must be a valid HEX color                 |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Update tag successfully",
  "data": {
    "tagId": 1,
    "userId": 1,
    "name": "Machine Learning",
    "color": "#22C55E",
    "createdAt": "2026-06-14T10:30:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:35:00Z"
}
```

### Error cases

| Status | Message              | Reason                                            |
| ------ | -------------------- | ------------------------------------------------- |
| `400`  | `Validation failed`  | Missing/invalid name or color                     |
| `400`  | `Validation failed`  | Tag name already exists for the current user      |
| `401`  | `Unauthorized`       | Missing or invalid JWT                            |
| `404`  | `Resource not found` | Tag does not exist or does not belong to the user |

---

## 5.5. Delete tag

Delete a tag owned by the authenticated user. Existing document-tag links for that tag are also removed.

### Request

- Method: `DELETE`
- URL: `/api/tags/{tagId}`
- Auth: JWT required

### Path variables

| Name    | Type   | Required |
| ------- | ------ | -------- |
| `tagId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Delete tag successfully",
  "data": null,
  "errors": null,
  "timestamp": "2026-06-14T10:40:00Z"
}
```

### Error cases

| Status | Message              | Reason                                            |
| ------ | -------------------- | ------------------------------------------------- |
| `401`  | `Unauthorized`       | Missing or invalid JWT                            |
| `404`  | `Resource not found` | Tag does not exist or does not belong to the user |

---

## 5.6. Add tag to document

Attach an owned tag to an owned active document.

### Request

- Method: `POST`
- URL: `/api/documents/{documentId}/tags/{tagId}`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |
| `tagId`      | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Add tag to document successfully",
  "data": {
    "tagId": 1,
    "userId": 1,
    "name": "AI",
    "color": "#8B5CF6",
    "createdAt": "2026-06-14T10:30:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:45:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                     |
| ------ | -------------------- | ---------------------------------------------------------- |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                     |
| `404`  | `Resource not found` | Document/tag does not exist or does not belong to the user |

---

## 5.7. Remove tag from document

Detach a tag from an owned active document.

### Request

- Method: `DELETE`
- URL: `/api/documents/{documentId}/tags/{tagId}`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |
| `tagId`      | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Remove tag from document successfully",
  "data": null,
  "errors": null,
  "timestamp": "2026-06-14T10:50:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                     |
| ------ | -------------------- | ---------------------------------------------------------- |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                     |
| `404`  | `Resource not found` | Document/tag does not exist or does not belong to the user |

---

## 5.8. Get document tags

Get tags attached to an owned active document.

### Request

- Method: `GET`
- URL: `/api/documents/{documentId}/tags`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get document tags successfully",
  "data": [
    {
      "tagId": 1,
      "userId": 1,
      "name": "AI",
      "color": "#8B5CF6",
      "createdAt": "2026-06-14T10:30:00Z"
    }
  ],
  "errors": null,
  "timestamp": "2026-06-14T10:55:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                 |
| ------ | -------------------- | ------------------------------------------------------ |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                 |
| `404`  | `Resource not found` | Document does not exist or does not belong to the user |

---

## 5.9. Get public document tags

Get tags attached to a public document.

### Request

- Method: `GET`
- URL: `/api/documents/public/{documentId}/tags`
- Auth: Public, no JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get public document tags successfully",
  "data": [
    {
      "tagId": 1,
      "userId": 1,
      "name": "AI",
      "color": "#8B5CF6",
      "createdAt": "2026-06-14T10:30:00Z"
    }
  ],
  "errors": null,
  "timestamp": "2026-06-14T11:00:00Z"
}
```

### Error cases

| Status | Message              | Reason                                          |
| ------ | -------------------- | ----------------------------------------------- |
| `404`  | `Resource not found` | Public document does not exist or is not public |

---

## 6. AI Chat APIs

AI chat APIs answer questions from the selected document content using the indexed document chunks.

Private chat APIs require:

```text
Authorization: Bearer <accessToken>
```

Current scope:

- Chat works on one selected document per request.
- The selected document must be accessible by the authenticated user.
- The selected document must have status `READY`.
- Retrieval is scoped only to chunks of the selected document.
- The AI is instructed to answer only from the retrieved document context.
- Chat session/history persistence is not implemented yet.

---

## 6.1. Ask a question about a document

Ask the AI a question using one selected document as context.

### Request

- Method: `POST`
- URL: `/api/chat/ask`
- Auth: JWT required
- Content-Type: `application/json`

```json
{
  "documentId": 1,
  "question": "What is the main idea of this document?"
}
```

### Request fields

| Field | Type | Required | Rule |
|---|---|---|---|
| `documentId` | number | Yes | Must point to an accessible document |
| `question` | string | Yes | Must not be blank |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Ask document successfully",
  "data": {
    "documentId": 1,
    "answer": "The main idea of the document is ...",
    "sources": [
      {
        "chunkId": 10,
        "chunkIndex": 0,
        "pageNumber": 1,
        "score": 0.8732
      }
    ]
  },
  "errors": null,
  "timestamp": "2026-06-16T10:30:00Z"
}
```

### Response fields

| Field | Type | Description |
|---|---|---|
| `documentId` | number | The selected document ID |
| `answer` | string | AI answer grounded by retrieved chunks |
| `sources` | array | Retrieved chunks used as context |
| `sources[].chunkId` | number | Source chunk ID |
| `sources[].chunkIndex` | number | Chunk order in the document |
| `sources[].pageNumber` | number / null | Source page number if available |
| `sources[].score` | number | Cosine similarity score |

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing documentId or blank question |
| `400` | `Validation failed` | Document is not `READY` |
| `400` | `Validation failed` | Document has no indexed content for chat |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Document does not exist or is not accessible |
| `503` | `Spring AI chat model is not configured...` | Missing Spring AI chat configuration |
| `503` | `AI service is unavailable` | Gemini/Spring AI call failed |

### Frontend usage

1. Let the user select a document.
2. Only enable chat when the selected document has status `READY`.
3. Send the selected `documentId` and the user's question to `/api/chat/ask`.
4. Render `data.answer`.
5. Optionally show `data.sources` for debugging or future citation UI.

Important:

- Do not send the full document content from the frontend.
- Do not call Gemini directly from the frontend.
- The backend handles embedding, vector search, prompt building, and AI calling.

---

## 7. Friend APIs

Friend APIs allow authenticated users to send, manage, and list friendship relationships.

Private friend APIs require:

```text
Authorization: Bearer <accessToken>
```

---

## 7.1. Friend request response object

Friend request APIs return a `FriendRequestResponse` object:

```json
{
  "requestId": 1,
  "senderId": 1,
  "senderName": "Long Nguyen",
  "senderEmail": "long@example.com",
  "receiverId": 2,
  "receiverName": "Teammate",
  "receiverEmail": "teammate@example.com",
  "status": "PENDING",
  "createdAt": "2026-06-19T10:30:00Z",
  "respondedAt": null
}
```

### Friend request fields

| Field | Type | Description |
|---|---|---|
| `requestId` | number | Friend request ID |
| `senderId` | number | User ID of the sender |
| `senderName` | string | Full name of the sender |
| `senderEmail` | string | Email of the sender |
| `receiverId` | number | User ID of the receiver |
| `receiverName` | string | Full name of the receiver |
| `receiverEmail` | string | Email of the receiver |
| `status` | string | `PENDING`, `ACCEPTED`, `REJECTED`, or `CANCELLED` |
| `createdAt` | string | Request creation time |
| `respondedAt` | string / null | Time when the request was accepted, rejected, or cancelled |

---

## 7.2. Friend response object

Friend list APIs return a `FriendResponse` object:

```json
{
  "friendshipId": 1,
  "userId": 2,
  "fullName": "Teammate",
  "email": "teammate@example.com",
  "createdAt": "2026-06-19T10:30:00Z"
}
```

### Friend fields

| Field | Type | Description |
|---|---|---|
| `friendshipId` | number | Friendship ID |
| `userId` | number | Friend user ID |
| `fullName` | string | Friend full name |
| `email` | string | Friend email |
| `createdAt` | string | Friendship creation time |

---

## 7.3. Send friend request

Send a friend request to another user by email.

### Request

- Method: `POST`
- URL: `/api/friends/request`
- Auth: JWT required
- Content-Type: `application/json`

```json
{
  "email": "teammate@example.com"
}
```

### Request fields

| Field | Type | Required | Rule |
|---|---|---|---|
| `email` | string | Yes | Must not be blank, must be a valid email |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Send friend request successfully",
  "data": {
    "requestId": 1,
    "senderId": 1,
    "senderName": "Long Nguyen",
    "senderEmail": "long@example.com",
    "receiverId": 2,
    "receiverName": "Teammate",
    "receiverEmail": "teammate@example.com",
    "status": "PENDING",
    "createdAt": "2026-06-19T10:30:00Z",
    "respondedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-19T10:30:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing or invalid email |
| `400` | `Validation failed` | Sending request to yourself, already friends, duplicate pending request, or reverse pending request exists |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Receiver or sender not found |

---

## 7.4. Get incoming friend requests

Get pending friend requests received by the authenticated user.

### Request

- Method: `GET`
- URL: `/api/friends/requests/incoming`
- Auth: JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get incoming friend requests successfully",
  "data": [
    {
      "requestId": 1,
      "senderId": 2,
      "senderName": "Teammate",
      "senderEmail": "teammate@example.com",
      "receiverId": 1,
      "receiverName": "Long Nguyen",
      "receiverEmail": "long@example.com",
      "status": "PENDING",
      "createdAt": "2026-06-19T10:30:00Z",
      "respondedAt": null
    }
  ],
  "errors": null,
  "timestamp": "2026-06-19T10:30:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |

---

## 7.5. Get outgoing friend requests

Get pending friend requests sent by the authenticated user.

### Request

- Method: `GET`
- URL: `/api/friends/requests/outgoing`
- Auth: JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get outgoing friend requests successfully",
  "data": [
    {
      "requestId": 1,
      "senderId": 1,
      "senderName": "Long Nguyen",
      "senderEmail": "long@example.com",
      "receiverId": 2,
      "receiverName": "Teammate",
      "receiverEmail": "teammate@example.com",
      "status": "PENDING",
      "createdAt": "2026-06-19T10:30:00Z",
      "respondedAt": null
    }
  ],
  "errors": null,
  "timestamp": "2026-06-19T10:30:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |

---

## 7.6. Accept friend request

Accept a pending friend request received by the authenticated user.

### Request

- Method: `POST`
- URL: `/api/friends/requests/{requestId}/accept`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `requestId` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Accept friend request successfully",
  "data": {
    "requestId": 1,
    "senderId": 2,
    "senderName": "Teammate",
    "senderEmail": "teammate@example.com",
    "receiverId": 1,
    "receiverName": "Long Nguyen",
    "receiverEmail": "long@example.com",
    "status": "ACCEPTED",
    "createdAt": "2026-06-19T10:30:00Z",
    "respondedAt": "2026-06-19T10:35:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-19T10:35:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | User is not allowed to respond, request is not pending, or users are already friends |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Friend request or user not found |

---

## 7.7. Reject friend request

Reject a pending friend request received by the authenticated user.

### Request

- Method: `POST`
- URL: `/api/friends/requests/{requestId}/reject`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `requestId` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Reject friend request successfully",
  "data": {
    "requestId": 1,
    "senderId": 2,
    "senderName": "Teammate",
    "senderEmail": "teammate@example.com",
    "receiverId": 1,
    "receiverName": "Long Nguyen",
    "receiverEmail": "long@example.com",
    "status": "REJECTED",
    "createdAt": "2026-06-19T10:30:00Z",
    "respondedAt": "2026-06-19T10:35:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-19T10:35:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | User is not allowed to respond or request is not pending |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Friend request not found |

---

## 7.8. Cancel friend request

Cancel a pending friend request sent by the authenticated user.

### Request

- Method: `DELETE`
- URL: `/api/friends/requests/{requestId}/cancel`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `requestId` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Cancel friend request successfully",
  "data": {
    "requestId": 1,
    "senderId": 1,
    "senderName": "Long Nguyen",
    "senderEmail": "long@example.com",
    "receiverId": 2,
    "receiverName": "Teammate",
    "receiverEmail": "teammate@example.com",
    "status": "CANCELLED",
    "createdAt": "2026-06-19T10:30:00Z",
    "respondedAt": "2026-06-19T10:35:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-19T10:35:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | User is not allowed to cancel or request is not pending |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Friend request not found |

---

## 7.9. Unfriend

Remove an existing friendship.

### Request

- Method: `DELETE`
- URL: `/api/friends/{friendId}`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `friendId` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Unfriend successfully",
  "data": null,
  "errors": null,
  "timestamp": "2026-06-19T10:40:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | User tries to unfriend themselves |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Friendship not found |

---

## 7.10. Get friends

Get the authenticated user's friend list.

### Request

- Method: `GET`
- URL: `/api/friends`
- Auth: JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get friends successfully",
  "data": [
    {
      "friendshipId": 1,
      "userId": 2,
      "fullName": "Teammate",
      "email": "teammate@example.com",
      "createdAt": "2026-06-19T10:30:00Z"
    }
  ],
  "errors": null,
  "timestamp": "2026-06-19T10:40:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |

---

## 8. Common HTTP status codes

| Status                      | Description                                          |
| --------------------------- | ---------------------------------------------------- |
| `200 OK`                    | Request succeeded                                    |
| `400 Bad Request`           | Missing or invalid request data                      |
| `401 Unauthorized`          | Missing, invalid, or expired JWT                     |
| `403 Forbidden`             | Authenticated but not allowed to access the resource |
| `404 Not Found`             | Resource not found                                   |
| `413 Payload Too Large`     | Uploaded file exceeds the size limit                 |
| `500 Internal Server Error` | Unexpected server error                              |
| `503 Service Unavailable`   | S3 or external service failure                       |

---

## 9. Frontend notes

- Private APIs do not require `userId`; the backend reads the current user from JWT.
- After login or Google login, store both `accessToken` and `refreshToken`.
- When refreshing tokens, always replace the old refresh token with the new one from the response.
- Header for private APIs:

```text
Authorization: Bearer <accessToken>
```

- File upload must use `multipart/form-data`, not raw JSON.
- Send `isPublic` as a boolean-like string in form-data/query:

```text
true
false
```

- `s3Key` is an internal object key for backend/S3 usage. The frontend should not use `s3Key` to access files directly.
- Use preview/download URL APIs to access files from private S3 safely.
- Pre-signed URLs are temporary. If a URL expires, call the backend again to get a new one.
- Preview rendering and lazy loading pages are frontend responsibilities.
- Share-link APIs are public by token. Anyone with a valid enabled share token can open the shared document metadata and request preview/download URLs.
- Direct user sharing requires friendship. The backend rejects sharing with non-friends.
- Documents in `shared-with-me` can be previewed/downloaded through the shared-with-me URL APIs.
- Use `POST /api/chat/ask` for document-grounded AI chat.
- Chat currently supports one selected document per request and requires document status `READY`.
- Chat currently supports owned documents and public documents; shared-with-me document chat access is not documented as supported yet.
- Chat history/session APIs are not implemented yet.
- `ResendOtpResponse.mesage` is currently misspelled according to the existing DTO. If the team wants `message`, the DTO/backend should be updated later.
- Tag colors should be sent as HEX values such as `#8B5CF6`, `#22C55E`, or `#FFF`.
- Video upload currently supports storing the video file and metadata, but real transcript extraction is not available yet.
- For video upload above `20MB`, the backend must run with multipart env values such as `APP_MAX_MULTIPART_FILE_SIZE=50MB` and `APP_MAX_MULTIPART_REQUEST_SIZE=50MB`.
