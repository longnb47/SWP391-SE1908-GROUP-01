# API Contract — AI Study Hub Backend

This document describes the currently available backend APIs so the frontend team knows which endpoints exist, what to send, and what each API returns.

> Local base URL: `http://localhost:8080`

---

## 1. Standard response format

All APIs return responses using this format:

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

| Field | Type | Description |
|---|---|---|
| `success` | boolean | `true` for successful requests, `false` for failed requests |
| `message` | string | General response message |
| `data` | object / array / null | Response payload |
| `errors` | array / null | Detailed error list |
| `timestamp` | string | Response creation time |

### Error object

| Field | Type | Description |
|---|---|---|
| `field` | string / null | Invalid field, for example `email`, `password`, `authorization` |
| `message` | string | Error message |

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

| Field | Type | Required | Rule |
|---|---|---|---|
| `fullName` | string | Yes | Must not be blank |
| `email` | string | Yes | Must not be blank, must be a valid email |
| `password` | string | Yes | Must not be blank, minimum 8 characters |

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

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Invalid email, password, or fullName |
| `400` | `Validation failed` | Email already exists |
| `500` | `Unexpected server error` | Unexpected error, for example mail service failure |

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

| Field | Type | Required | Rule |
|---|---|---|---|
| `email` | string | Yes | Must not be blank, must be a valid email |
| `otp` | string | Yes | Exactly 6 characters |

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

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Invalid email or OTP format |
| `400` | `Validation failed` | Invalid OTP |
| `400` | `Validation failed` | OTP has expired |
| `400` | `Validation failed` | Too many failed attempts |
| `404` | `Resource not found` | User or OTP not found |

---

## 2.3. Login

Log in using email and password, then receive a JWT access token.

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

| Field | Type | Required | Rule |
|---|---|---|---|
| `email` | string | Yes | Must not be blank, must be a valid email |
| `password` | string | Yes | Must not be blank |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Login successfully",
  "data": {
    "accessToken": "jwt-token",
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

After a successful login, store `data.accessToken` and send it when calling private APIs:

```text
Authorization: Bearer <accessToken>
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing or invalid email/password |
| `400` | `Validation failed` | Invalid email or password |
| `400` | `Validation failed` | Account has not completed OTP verification |

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

| Field | Type | Required | Rule |
|---|---|---|---|
| `email` | string | Yes | Must not be blank, must be a valid email |

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

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing or invalid email |
| `400` | `Validation failed` | Account is already verified |
| `404` | `Resource not found` | User not found |

---

## 2.5. Google OAuth2 Success

Callback endpoint after a successful Google OAuth2 login.

### Request

- Method: `GET`
- URL: `/api/auth/google/success`
- Auth: OAuth2 login flow

The frontend usually does not call this endpoint like a normal JSON API. It is used as part of the Google OAuth2 flow.

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Google login successfully",
  "data": {
    "message": "Google login successfully",
    "token": "jwt-token",
    "userId": 1,
    "email": "long@example.com",
    "role": "USER",
    "fullName": "Long Nguyen"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Email is already registered with another provider |
| `401` | `Unauthorized` | Invalid OAuth2 authentication |

---

## 3. Document APIs

Private APIs require this header:

```text
Authorization: Bearer <accessToken>
```

Current public document APIs:

- `GET /api/documents/public`
- `GET /api/documents/public/{documentId}`

---

## 3.1. Document response object

Document APIs usually return a `DocumentUploadResponse` object:

```json
{
  "documentId": 1,
  "userId": 1,
  "originalFileName": "example.pdf",
  "s3Key": "documents/1/uuid-example.pdf",
  "contentType": "application/pdf",
  "fileSize": 123456,
  "isPublic": false,
  "isDeleted": false,
  "status": "READY",
  "uploadedAt": "2026-06-14T10:30:00Z",
  "deletedAt": null
}
```

### Document fields

| Field | Type | Description |
|---|---|---|
| `documentId` | number | Document ID |
| `userId` | number | Owner user ID |
| `originalFileName` | string | Sanitized original file name |
| `s3Key` | string | S3 object key |
| `contentType` | string | MIME type |
| `fileSize` | number | File size in bytes |
| `isPublic` | boolean | Public/private visibility |
| `isDeleted` | boolean | Soft delete flag |
| `status` | string | Document processing status |
| `uploadedAt` | string | Upload time |
| `deletedAt` | string / null | Soft delete time |

### Document status

| Status | Description |
|---|---|
| `UPLOADED` | File was uploaded and metadata was saved |
| `PARSING` | The system is extracting document text |
| `INDEXING` | The system is chunking/embedding/indexing |
| `READY` | The document is ready for future RAG/chat usage |
| `FAILED` | Parsing or indexing failed |

---

## 3.2. Upload document

Upload a file to S3, save metadata, and parse/chunk/embed it if supported.

### Request

- Method: `POST`
- URL: `/api/documents/upload`
- Auth: JWT required
- Content-Type: `multipart/form-data`

### Form-data fields

| Field | Type | Required | Rule |
|---|---|---|---|
| `file` | File | Yes | Uploaded file |
| `isPublic` | boolean | No | `true` or `false`; private by default if omitted |

### Supported files

Limits:

- Maximum size: `20MB`.
- Empty files are rejected.

Directly supported extensions:

- `pdf`
- `doc`
- `docx`
- `pptx`
- `xls`
- `xlsx`
- `png`

Files whose `Content-Type` starts with `image/` are also accepted.

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Upload document successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "originalFileName": "example.pdf",
    "s3Key": "documents/1/uuid-example.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": true,
    "isDeleted": false,
    "status": "READY",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Empty file, missing filename, or unsupported extension |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `413` | `File upload failed` | Uploaded file exceeds the size limit |
| `500` | `File read failed` | Backend failed to read the file |
| `503` | `S3 upload failed` | S3 upload failed |

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
      "originalFileName": "example.pdf",
      "s3Key": "documents/1/uuid-example.pdf",
      "contentType": "application/pdf",
      "fileSize": 123456,
      "isPublic": false,
      "isDeleted": false,
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

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |

---

## 3.4. Get my document detail

Get the detail of a document owned by the current user.

### Request

- Method: `GET`
- URL: `/api/documents/{documentId}`
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
  "message": "Get document detail successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "originalFileName": "example.pdf",
    "s3Key": "documents/1/uuid-example.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": false,
    "isDeleted": false,
    "status": "READY",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Document does not exist, does not belong to the user, or was soft-deleted |

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
      "originalFileName": "public-file.pdf",
      "s3Key": "documents/1/uuid-public-file.pdf",
      "contentType": "application/pdf",
      "fileSize": 123456,
      "isPublic": true,
      "isDeleted": false,
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

| Name | Type | Required |
|---|---|---|
| `documentId` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get public document detail successfully",
  "data": {
    "documentId": 2,
    "userId": 1,
    "originalFileName": "public-file.pdf",
    "s3Key": "documents/1/uuid-public-file.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": true,
    "isDeleted": false,
    "status": "READY",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `404` | `Resource not found` | Public document does not exist or is not public |

---

## 3.7. Update document visibility

Update a document to public or private.

### Request

- Method: `PATCH`
- URL: `/api/documents/{documentId}/visibility?isPublic=true`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `documentId` | number | Yes |

### Query params

| Name | Type | Required | Example |
|---|---|---|---|
| `isPublic` | boolean | Yes | `true` or `false` |

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
    "status": "READY",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing `isPublic` |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Document does not exist, does not belong to the user, or is in Trash |

---

## 3.8. Move document to Trash

Soft-delete a document. The physical file is not deleted from S3.

### Request

- Method: `DELETE`
- URL: `/api/documents/{documentId}`
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
    "status": "READY",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": "2026-06-14T10:40:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:40:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Document does not exist or does not belong to the user |

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

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |

---

## 3.10. Restore document

Restore a document from Trash.

### Request

- Method: `POST`
- URL: `/api/documents/{documentId}/restore`
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
    "status": "READY",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-14T10:45:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Document does not exist or does not belong to the user |

---

## 3.11. Delete document permanently

Permanently delete a document. Only documents already in Trash can be permanently deleted.

### Request

- Method: `DELETE`
- URL: `/api/documents/{documentId}/permanent`
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
  "message": "Delete document permanently successfully",
  "data": null,
  "errors": null,
  "timestamp": "2026-06-14T10:50:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Document is not in Trash |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Document does not exist or does not belong to the user |
| `503` | `S3 delete failed` | Failed to delete the file from S3 |

---

## 4. Tag APIs

Tag APIs allow users to create custom document tags with custom colors and attach them to documents.

Private tag APIs require:

```text
Authorization: Bearer <accessToken>
```

---

## 4.1. Tag response object

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

| Field | Type | Description |
|---|---|---|
| `tagId` | number | Tag ID |
| `userId` | number | Owner user ID |
| `name` | string | Tag name |
| `color` | string | HEX color, for example `#8B5CF6` |
| `createdAt` | string | Tag creation time |

---

## 4.2. Create tag

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

| Field | Type | Required | Rule |
|---|---|---|---|
| `name` | string | Yes | Must not be blank, maximum 100 characters |
| `color` | string | Yes | Must be a valid HEX color, for example `#8B5CF6` or `#FFF` |

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

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing/invalid name or color |
| `400` | `Validation failed` | Tag name already exists for the current user |
| `401` | `Unauthorized` | Missing or invalid JWT |

---

## 4.3. Get my tags

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

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |

---

## 4.4. Update tag

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

| Name | Type | Required |
|---|---|---|
| `tagId` | number | Yes |

### Request fields

| Field | Type | Required | Rule |
|---|---|---|---|
| `name` | string | Yes | Must not be blank, maximum 100 characters |
| `color` | string | Yes | Must be a valid HEX color |

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

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing/invalid name or color |
| `400` | `Validation failed` | Tag name already exists for the current user |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Tag does not exist or does not belong to the user |

---

## 4.5. Delete tag

Delete a tag owned by the authenticated user. Existing document-tag links for that tag are also removed.

### Request

- Method: `DELETE`
- URL: `/api/tags/{tagId}`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `tagId` | number | Yes |

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

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Tag does not exist or does not belong to the user |

---

## 4.6. Add tag to document

Attach an owned tag to an owned active document.

### Request

- Method: `POST`
- URL: `/api/documents/{documentId}/tags/{tagId}`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `documentId` | number | Yes |
| `tagId` | number | Yes |

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

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Document/tag does not exist or does not belong to the user |

---

## 4.7. Remove tag from document

Detach a tag from an owned active document.

### Request

- Method: `DELETE`
- URL: `/api/documents/{documentId}/tags/{tagId}`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `documentId` | number | Yes |
| `tagId` | number | Yes |

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

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Document/tag does not exist or does not belong to the user |

---

## 4.8. Get document tags

Get tags attached to an owned active document.

### Request

- Method: `GET`
- URL: `/api/documents/{documentId}/tags`
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

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Document does not exist or does not belong to the user |

---

## 4.9. Get public document tags

Get tags attached to a public document.

### Request

- Method: `GET`
- URL: `/api/documents/public/{documentId}/tags`
- Auth: Public, no JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `documentId` | number | Yes |

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

| Status | Message | Reason |
|---|---|---|
| `404` | `Resource not found` | Public document does not exist or is not public |

---

## 5. Common HTTP status codes

| Status | Description |
|---|---|
| `200 OK` | Request succeeded |
| `400 Bad Request` | Missing or invalid request data |
| `401 Unauthorized` | Missing, invalid, or expired JWT |
| `403 Forbidden` | Authenticated but not allowed to access the resource |
| `404 Not Found` | Resource not found |
| `413 Payload Too Large` | Uploaded file exceeds the size limit |
| `500 Internal Server Error` | Unexpected server error |
| `503 Service Unavailable` | S3 or external service failure |

---

## 6. Frontend notes

- Private APIs do not require `userId`; the backend reads the current user from JWT.
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
- There is no pre-signed URL / file preview API yet.
- There is no ChatBox/RAG API yet.
- `ResendOtpResponse.mesage` is currently misspelled according to the existing DTO. If the team wants `message`, the DTO/backend should be updated later.
- Tag colors should be sent as HEX values such as `#8B5CF6`, `#22C55E`, or `#FFF`.
