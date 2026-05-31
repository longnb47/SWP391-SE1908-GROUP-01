# AGENTS.md — AI Study Hub: Upload & AI Chatbox Scope

> This file provides working instructions for Codex when contributing to the AI Study Hub project.
> Current responsibility scope: **Document Upload** and **AI Chatbox/RAG**.
> Do not implement or modify unrelated modules unless explicitly requested.

## Requirement Reference

For daily coding, follow this AGENTS.md first.

For requirement context, read:

- `docs/srs-summary.md`

For detailed official documentation, refer to:

- `docs/SRS_AI_Study_Hub_V1.4.docx`

---

## 1. Project Context

AI Study Hub is a learning platform built with Spring Boot. It allows students to upload study documents and ask an AI chatbot questions based on the selected document content.

The current development scope focuses only on:

1. Document upload flow
2. File validation
3. Amazon S3 storage integration
4. Document metadata persistence
5. Text extraction/parsing
6. Chunking and indexing documents for RAG
7. AI chatbox flow
8. Chat session and chat message persistence
9. Context retrieval from selected documents
10. Ensuring AI answers only from the selected document context

Out of the current scope, unless explicitly requested:

- Admin dashboard
- User lock/unlock management
- Payment/subscription gateway implementation
- Momo/VNPAY integration
- Full analytics dashboard
- Public document moderation
- Google OAuth2 implementation
- Full frontend redesign
- Large unrelated refactors

---

## 2. Tech Stack

Backend:

- Java
- Spring Boot
- Spring AI
- Spring Security, if authentication is involved
- Spring Data JPA
- RESTful API
- Maven

Database and storage:

- MySQL for structured data, metadata, document chunks, and embedding vectors
- Amazon S3 for physical uploaded files
- No dedicated vector database in the current phase
- Embedding vectors are stored in MySQL as JSON or LONGTEXT
- During search, the backend loads chunks by documentId and calculates cosine similarity in Java

## AI/RAG Technology

This project uses Spring AI for the AI Chatbox/RAG feature.

- Use Spring AI to call the LLM.
- Use Spring AI `EmbeddingModel` if embedding is configured in the project.
- Prefer Spring AI `ChatClient`, or an equivalent service wrapper following the existing project pattern.
- Do not create a direct HTTP client for calling the LLM if the project already uses Spring AI.
- Do not add another AI library unless explicitly requested.

Suggested classes:

- `ChatService`
- `DocumentEmbeddingService`
- `VectorSearchService`
- `PromptBuilder`
- `SpringAiChatClient` or an `AiClient` wrapper

Frontend, only when needed:

- ReactJS
- TailwindCSS
- Axios
- React Router DOM

Always follow the actual dependencies and structure already present in the repository. Do not add new dependencies unless necessary and clearly explained.

---

## 3. Maven Commands

This is a Spring Boot project using Maven.

Prefer using Maven Wrapper if it exists in the project.

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
.\mvnw.cmd clean package
```

macOS/Linux:

```bash
./mvnw spring-boot:run
./mvnw test
./mvnw clean package
```

If Maven Wrapper is not available:

```bash
mvn spring-boot:run
mvn test
mvn clean package
```

If verification cannot be completed because a local service or secret/config is missing, clearly state what could not be verified.

Examples:

- Missing AWS credentials
- MySQL is not running
- Vector storage is not configured
- Missing AI API key
- Frontend is not running

Do not claim a command passed if it was not actually run successfully.

---

## 4. Expected Backend Structure

Prefer the actual structure already used by the project. If the project has naming conventions, follow them.

Common Spring Boot structure:

```text
src/main/java/...
  controller/
  service/
  repository/
  entity/
  dto/
  mapper/
  config/
  security/
  exception/
  util/

src/main/resources/
  application.yml

src/test/java/...
pom.xml
```

For upload and chatbox features, responsibilities should be separated as follows:

- Controller: receives HTTP requests, validates the basic request shape, calls services, and returns responses
- Service: handles business logic and orchestrates workflows
- Repository: only handles database access
- Entity: only represents database models
- DTO: only represents request/response data shapes
- Mapper: converts Entity to DTO and DTO to Entity if the project uses mappers
- Config: configures S3, AI, vector storage, security, CORS, Swagger
- Exception handler: centralizes standardized API error responses

Do not put business logic directly in controllers, entities, or DTOs.

---

## 5. Core Architecture Rules

Use a layered flow:

```text
Controller -> Service -> Repository / External Client -> MySQL / S3 / Vector Storage / AI API
```

For document upload:

```text
DocumentController
  -> DocumentService
    -> FileValidationService
    -> S3StorageService
    -> DocumentRepository
    -> DocumentParsingService
    -> DocumentChunkingService
    -> VectorIndexService
```

For AI chatbox:

```text
ChatController
  -> ChatService
    -> DocumentAccessService
    -> VectorSearchService
    -> PromptBuilderService
    -> LlmClient / AiClient
    -> ChatSessionRepository
    -> ChatMessageRepository
```

Each class should have a clear responsibility. Avoid creating a large service that handles validation, S3 storage, parsing, vector indexing, and LLM calls all at once.

---

## 6. Upload Scope Rules

The upload feature must support the project document ingestion pipeline.

### 6.1 Upload Validation

Before storing a file, check:

- The user is authenticated if the endpoint requires login
- The file is not empty
- The file size is within the allowed limit
- The file extension is supported
- The MIME type is valid
- The filename has been sanitized
- The user has permission to upload
- The user's tier allows that file format, if tier logic exists in the project

Current upload limit according to the SRS:

```text
Maximum physical file size: 20MB
```

Allowed file formats should follow the actual project requirements. If not finalized, use the SRS as a reference:

```text
Basic/free document formats:
- .pdf
- .doc
- .docx
- .pptx
- .xls
- .xlsx
- .png
```

If the current code uses a different list, follow the current code and ask before changing business rules.

### 6.2 Upload Flow

Recommended upload flow:

1. Receive upload request
2. Validate file
3. Generate a safe S3 object key
4. Upload the physical file to Amazon S3
5. Save document metadata in MySQL
6. Extract text from the uploaded file
7. Split text into chunks
8. Generate embeddings for chunks
9. Store chunks/embeddings in vector storage
10. Mark the document as ready for AI chat
11. Return the document response to the frontend

If parsing or indexing fails after S3 upload succeeds, do not ignore the failure. Store a clear status, for example:

```text
UPLOADED
PARSING
INDEXING
READY
FAILED
```

Use the existing enum/status style if the project already has one.

## Embedding Configuration

This project uses Spring AI to generate embeddings.

- Use Spring AI `EmbeddingModel`.
- Do not manually create embeddings in Java.
- Do not call OpenAI/Gemini HTTP APIs directly if Spring AI has already configured the provider.
- Default provider: Google Gemini through Spring AI, if the project is configured that way.
- The exact embedding model must follow `application.yml`.
- Do not hard-code model names in business logic.
- Store embedding vectors in MySQL as JSON or LONGTEXT.
- Java only handles text parsing, chunking, cosine similarity, and data storage/retrieval.

---

## 7. Amazon S3 Rules

The project uses Amazon S3 to store physical files.

Do:

- Store physical files in S3
- Store metadata in MySQL
- Store only the S3 object key, bucket name, filename, content type, file size, owner ID, visibility, and status in the database
- Keep the S3 bucket private by default
- Use pre-signed URLs only when needed and only after access permission has been checked
- Keep pre-signed URL expiration short
- Sanitize filenames
- Use unique object keys to avoid filename collisions

Recommended S3 object key format:

```text
users/{userId}/documents/{documentId}/{sanitizedFilename}
```

or:

```text
documents/{userId}/{uuid}-{sanitizedFilename}
```

Do not:

- Store complete file bytes in MySQL
- Expose AWS access keys to the frontend
- Commit AWS credentials to Git
- Make private user documents public on S3
- Add Firebase Storage code
- Store only public S3 URLs without access control for private documents

Credentials and bucket settings should come from environment variables/configuration, following the existing project style.

Common config names:

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
AWS_REGION
AWS_S3_BUCKET_NAME
```

If the project already has different config names, use the existing names.

---

## 8. Document Metadata Rules

Document metadata should be stored in MySQL.

Common fields:

```text
id
ownerId / userId
originalFileName
storedFileName
contentType
fileSize
s3Bucket
s3ObjectKey
visibility
status
tags
createdAt
updatedAt
parsedAt
indexedAt
```

Do not change the database schema casually. Before changing fields in an entity, check:

- Entity class
- Repository
- DTOs
- Existing migration or SQL scripts
- Frontend usage
- Tests

If the project uses Flyway or Liquibase, create a new migration instead of editing old migrations.

If the project uses JPA `ddl-auto`, follow the project convention and clearly state the impact.

---

## 9. Parsing and Indexing Rules

After a successful upload, the system needs to extract text for AI usage.

Parsing should support only the formats actually supported by the project.

Recommended parsing flow:

```text
Uploaded file -> Text extraction -> Chunking -> Embedding -> Vector storage
```

Rules:

- Do not assume every PDF has extractable text
- If OCR is not implemented, clearly treat image-based PDFs as unsupported or partially supported
- Do not allow parsing to hang indefinitely
- Handle parser exceptions clearly
- Store a failure status when parsing/indexing fails
- Do not allow AI chat on documents that are not indexed successfully, unless the project has a clear fallback behavior

Chunking rules:

- Keep chunks small enough for efficient embedding and retrieval
- Preserve source document ID and chunk order
- Store enough metadata to trace AI answers back to the source document/chunk
- Do not mix private document chunks into another user's search results

---

## 10. AI Chatbox Scope Rules

The AI chatbox must answer based on the selected document context.

Core rule:

```text
The AI must answer only from the selected document content.
If the selected document does not contain the answer, the system should say that the answer cannot be found in the selected document.
```

Do not build a general-purpose chatbot that uses outside knowledge unless explicitly requested.

### 10.1 Preconditions Before Chat

Before allowing chat:

- The user must be authenticated if chat is a protected feature
- The selected document must exist
- The selected document must be accessible by the current user
- The selected document must be parsed/indexed successfully
- The prompt must not be empty

### 10.2 RAG Chat Flow

Recommended flow:

1. User selects a document as context
2. User sends a prompt
3. System checks access permission for the selected document
4. System embeds the user question
5. System searches vector chunks only inside the selected document
6. System assembles relevant context
7. System builds a strict prompt instruction
8. System sends prompt + context to the LLM
9. System saves the user message and AI response
10. System returns the AI answer to the frontend

### 10.3 Retrieval Scope

Vector search must be limited by access control.

For chat on one selected document:

```text
Only search chunks where documentId == selectedDocumentId
```

For multi-document chat, if implemented:

```text
Only search chunks where documentId IN selectedDocumentIds
AND the current user has access to all selected documents
```

Never search the entire vector storage for chat with private documents.

---

## 11. Prompt Building Rules

The prompt sent to the LLM must force grounded answers based on the context.

Recommended instruction style:

```text
You are an AI study assistant.
Answer the user's question using only the provided document context.
If the answer is not present in the context, say that the answer cannot be found in the selected document.
Do not use outside knowledge.
Do not invent citations, facts, or document content.
```

The prompt should include:

- System instruction
- Retrieved context
- User question
- Optional document title/source metadata

Avoid sending the whole document when only relevant chunks are needed.

Do not log the full private prompt/context unless truly needed for debugging and a safe redaction mechanism exists.

---

## 12. Chat Session and Message Rules

If chat history is stored, it must be stored clearly.

Common model:

```text
ChatSession
- id
- userId
- documentId or selectedDocumentIds
- title
- createdAt
- updatedAt

ChatMessage
- id
- sessionId
- role: USER / ASSISTANT
- content
- createdAt
```

Rules:

- Save user prompts and AI answers if the history feature requires it
- Attach each chat session to the correct user
- Attach each chat session to the correct selected document context
- Do not allow a user to access another user's chat session
- Only add delete/clear history if requested or already in project scope

---

## 13. Security Rules for Upload and Chat

Always preserve security.

Required checks:

- Authentication for upload and chat APIs
- Authorization/ownership checks for private documents
- Do not mix admin-only logic into normal user endpoints
- Validate file uploads on the backend
- Validate prompt input on the backend
- Do not expose private S3 object URLs without permission checks
- Do not expose JWTs, AWS secrets, AI keys, or DB credentials
- Do not log sensitive values

Sensitive values that must not be logged:

```text
password
OTP
JWT
refresh token
AWS access key
AWS secret key
AI API key
payment secret
full private document content
full private RAG context
```

If modifying Spring Security, JWT filters, CORS, or authentication logic, explain the risk and keep the change as small as possible.

---

## 14. API Design Rules for Upload and Chat

Prefer the endpoint convention already used in the project.

Possible upload endpoints:

```text
POST   /api/documents/upload
GET    /api/documents/my
GET    /api/documents/{id}
DELETE /api/documents/{id}
PATCH  /api/documents/{id}/visibility
PATCH  /api/documents/{id}/tags
```

Possible chat endpoints:

```text
POST /api/chat/sessions
GET  /api/chat/sessions
GET  /api/chat/sessions/{id}
POST /api/chat/sessions/{id}/messages
POST /api/chat/ask
```

Rules:

- Use DTOs for request/response if the project already uses DTOs
- Keep response shapes stable for the frontend
- Return meaningful HTTP status codes
- Return clear error messages
- Do not expose internal stack traces in API responses

Common status codes:

```text
400 Bad Request: invalid file, empty prompt
401 Unauthorized: unauthenticated user
403 Forbidden: no permission to access the document
404 Not Found: document/session not found
413 Payload Too Large: file exceeds size limit
500 Internal Server Error: unexpected server error
503 Service Unavailable: AI/vector/S3 service is unavailable
```

---

## 15. Frontend Coordination Rules

Only modify the frontend when needed to integrate upload/chatbox.

When modifying frontend:

- Keep the existing UI style
- Use the existing Axios/API client pattern
- Do not redesign unrelated pages
- Show upload progress/loading state when appropriate
- Show clear upload success/failure notifications
- Disable the send button when the prompt is empty
- Show an AI loading state while waiting for a response
- Show understandable errors if the selected document is not ready/indexed

Frontend must not:

- Store AWS credentials
- Call S3 directly with secret keys
- Bypass backend authorization
- Decide access control by itself
- Assume private file URLs will remain public forever

---

## 16. Testing and Verification

When modifying upload or chat logic, run relevant tests if possible.

Preferred commands:

```bash
./mvnw test
./mvnw clean package
```

Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
```

If full tests are too broad, run the related test class if available.

Recommended tests:

Upload:

- Reject empty file
- Reject oversized file
- Reject unsupported format
- Successfully upload a valid file
- Save metadata correctly
- Handle S3 failure
- Handle parsing/indexing failure

Chatbox:

- Reject empty prompt
- Reject chat with a document the user cannot access
- Reject chat with a document that has not been indexed
- Retrieve chunks only from the selected document
- Save chat messages correctly
- Handle AI API timeout/failure

If tests cannot be run because infrastructure is missing, clearly state why.

---

## 17. Logging Rules

Useful logs:

- Upload started/completed/failed
- S3 upload failure
- Parsing failure
- Indexing failure
- AI API timeout/failure
- Vector search failure
- Unauthorized document access attempt

Avoid logging:

- Full text of private documents
- Full RAG context
- Secrets/tokens/API keys
- Raw passwords or OTPs

Use the existing logging framework, usually SLF4J with Logback.

---

## 18. Before Editing Code

Before making changes:

1. Read this `AGENTS.md` file
2. Inspect related files first
3. Identify whether the task belongs to upload, S3, parsing, indexing, or chatbox
4. For non-trivial tasks, explain a short plan before editing
5. Make the smallest safe change
6. Follow existing patterns
7. Avoid unrelated refactors

For upload tasks, inspect likely related files:

```text
DocumentController
DocumentService
S3StorageService
DocumentRepository
Document entity
Document DTOs
application properties/yml
pom.xml
```

For AI chatbox tasks, inspect likely related files:

```text
ChatController
ChatService
ChatSession entity
ChatMessage entity
VectorSearchService
EmbeddingService
PromptBuilderService
AiClient / LlmClient
Document access logic
```

---

## 19. After Editing Code

After making changes, respond using this format:

```text
Summary:
- What changed

Changed files:
- List of modified files

Verification:
- Commands run and results
- Or why verification could not be completed

Risks / Notes:
- Remaining issues, missing config, or integration risks
```

Do not hide failing tests or failed commands.

---

## 20. Do Not Do

Do not:

- Bring Firebase Storage back
- Implement payment unless explicitly requested
- Implement admin dashboard unless explicitly requested
- Implement user lock/unlock unless explicitly requested
- Modify unrelated frontend pages
- Perform large refactors unrelated to upload/chatbox
- Add a new dependency without explanation
- Store files directly in MySQL
- Expose AWS credentials to the frontend
- Make private S3 objects public by default
- Allow AI to answer outside the selected document context
- Search private vectors globally without access control
- Ignore parsing/indexing failures
- Weaken authentication or authorization just to make code run
- Claim tests passed if they were not run

---

## 21. Useful Prompts for Codex in VS Code

Use these prompts when working with Codex.

### Investigate Upload

```text
Read AGENTS.md first. Focus only on the document upload flow. Inspect controller, service, S3 storage, entity, repository, DTO, and config files. Do not edit code yet. First explain the current flow and where the issue is likely located.
```

### Implement Upload

```text
Read AGENTS.md first. Implement the smallest and safest change for document upload. This project uses Amazon S3, not Firebase Storage. Follow the existing Spring Boot pattern and do not modify unrelated modules.
```

### Fix S3

```text
Read AGENTS.md first. Fix S3 upload/download issues. Do not expose AWS credentials. Keep the bucket private and follow the existing config style.
```

### Fix Parsing/Indexing

```text
Read AGENTS.md first. Focus on document parsing and vector indexing after upload. Ensure failures are stored clearly and prevent chat on documents that have not been indexed successfully.
```

### Investigate AI Chatbox

```text
Read AGENTS.md first. Focus only on the AI chatbox/RAG flow. Inspect chat controller, chat service, vector search, prompt builder, AI client, and chat persistence. Do not edit code yet. Explain the current flow and where the issue is likely located.
```

### Implement AI Chatbox

```text
Read AGENTS.md first. Implement the smallest and safest change for the AI chatbox. AI must answer only from the selected document context. Ensure vector search is scoped only to the selected document and checks the user's access permission.
```

### Verification

```text
After editing, run the relevant Maven test/build if possible. If it cannot run due to missing MySQL, AWS, vector storage, or AI API config, clearly explain which part could not be verified.
```
