# SRS Summary — AI Study Hub

## Current Development Scope

This developer is responsible only for:

- Document upload
- File validation
- Amazon S3 storage
- Document metadata storage
- Text extraction / parsing
- Document chunking
- Embedding generation with Spring AI
- Storing embedding vectors in MySQL as JSON or LONGTEXT
- Cosine similarity search in Java
- AI Chatbox / RAG
- Chat session and chat message persistence

## Out of Scope

Do not implement unless explicitly requested:

- Admin dashboard
- User lock/unlock
- Payment/subscription gateway
- Momo/VNPAY integration
- Analytics dashboard
- Public document moderation
- Google OAuth2
- Full frontend redesign

## Important SRS Correction

The original SRS may mention Firebase Storage.

The actual implementation uses:

- Amazon S3 for physical file storage
- MySQL for document metadata
- MySQL for document chunks and embedding vectors
- Spring AI for embedding and chat

Do not reintroduce Firebase Storage.

## Upload Requirements

- Users upload study materials.
- Physical file size limit: 20MB.
- Supported basic formats:
  - .pdf
  - .doc
  - .docx
  - .pptx
  - .xls
  - .xlsx
  - .png

Upload flow:

1. Validate file.
2. Upload physical file to Amazon S3.
3. Store document metadata in MySQL.
4. Extract text from the file.
5. Split text into chunks.
6. Generate embeddings for chunks using Spring AI.
7. Store chunks and embedding vectors in MySQL.
8. Mark document as ready for AI chat.

## Document Status

Recommended statuses:

- UPLOADED
- PARSING
- INDEXING
- READY
- FAILED

Users should not chat with documents that are not indexed successfully.

## AI Chatbox / RAG Requirements

The AI Chatbox must answer only from the selected document context.

Chat flow:

1. User selects a document.
2. User submits a question.
3. System checks document access permission.
4. System embeds the user question using Spring AI.
5. System loads chunks only from the selected document.
6. System calculates cosine similarity in Java.
7. System selects top relevant chunks.
8. System builds a strict prompt.
9. System calls Spring AI ChatClient.
10. System saves user message and AI response.
11. System returns the answer to the frontend.

## RAG Rule

The AI must not use outside knowledge.

If the selected document does not contain the answer, the system should respond that the answer cannot be found in the selected document.

## Vector Storage Strategy

Do not use an external vector database in the current phase.

Use MySQL to store:

- document ID
- chunk index
- chunk text
- embedding vector as JSON or LONGTEXT
- created/indexed timestamp

Java handles:

- chunk retrieval
- JSON/LONGTEXT vector parsing
- cosine similarity calculation
- top-k chunk selection

## Security Rules

- Upload and chat APIs should require authentication if the project uses protected endpoints.
- Private documents must only be accessible by their owner.
- S3 bucket should be private by default.
- Do not expose AWS credentials to frontend.
- Do not log sensitive values such as JWT, AWS keys, AI API keys, OTP, passwords, or full private document content.

## Main Technologies

- Backend: Spring Boot
- Build tool: Maven
- AI: Spring AI
- Chat model: Gemini or configured provider
- Embedding: Spring AI EmbeddingModel
- Database: MySQL
- File storage: Amazon S3

## Authoritative Database Tables from SRS Logical ERD

Use these table/entity names and fields as the source of truth. Do not let Codex invent extra tables or rename these entities unless the user explicitly asks for a schema refactor.

> Source: SRS v1.4, Section 6.2 Logical ERD, Figure 6.2.

### Tables in Scope for Current Development

These tables are relevant to document upload, parsing/chunking, RAG chat, and persistence.

#### `User`

| Field | Notes |
|---|---|
| `user_id` | Primary identifier |
| `email` | User login email |
| `password_hash` | Store hashed password only |
| `role` | Student/Admin role |
| `status` | Active/locked status |
| `created_at` | Account creation timestamp |
| `verifies_status` | Verification status from SRS ERD |

Relationships:

- One `User` uploads many `Document` records.
- One `User` owns many `Chat Session` records.
- One `User` can have many `OTP Verification` records.
- One `User` can have many `Subscription` records.
- One `User` can make many `Payment Transaction` records.
- One `User` can share documents through `Document Share`.

#### `Document`

| Field | Notes |
|---|---|
| `document_id` | Primary identifier |
| `user_id` | Owner user ID |
| `original_file_name` | Original uploaded filename |
| `s3_key` | Private S3 object key; do not expose AWS credentials |
| `content_type` | MIME/content type |
| `file_size` | Physical file size |
| `is_public` | Public/private visibility flag |
| `uploaded_at` | Upload timestamp |

Relationships:

- Many `Document` records belong to one `User`.
- One `Document` contains many `Document Chunk` records.
- One `Document` can be used as context by many `Chat Session` records.
- One `Document` can have many tags through `Document Tag`.
- One `Document` can have many share records through `Document Share`.

Implementation note:

- For current development, document processing status may be added as an implementation field, for example `status` with `UPLOADED`, `PARSING`, `INDEXING`, `READY`, `FAILED`.
- Do not create a separate physical file table. The SRS ERD stores file metadata directly in `Document`.

#### `Document Chunk`

| Field | Notes |
|---|---|
| `chunk_id` | Primary identifier |
| `document_id` | Parent document ID |
| `chunk_index` | Order of chunk within document |
| `content` | Chunk text content |
| `page_number` | Source page number if available |
| `created_at` | Chunk creation timestamp |

Relationships:

- Many `Document Chunk` records belong to one `Document`.

Implementation note:

- The SRS logical ERD does not show a separate vector table.
- For the current phase, store the embedding vector on `Document Chunk` as an implementation extension, for example `embedding_vector` as JSON or LONGTEXT.
- Do not introduce an external vector database or a new vector table unless explicitly requested.

#### `Chat Session`

| Field | Notes |
|---|---|
| `session_id` | Primary identifier |
| `user_id` | Owner user ID |
| `document_id` | Selected document used as RAG context |
| `title` | Chat session title |
| `started_at` | Session creation/start timestamp |

Relationships:

- Many `Chat Session` records belong to one `User`.
- Many `Chat Session` records use one `Document` as context.
- One `Chat Session` includes many `Message` records.

#### `Message`

| Field | Notes |
|---|---|
| `message_id` | Primary identifier |
| `session_id` | Parent chat session ID |
| `sender_type` | User or AI/system sender |
| `content` | Message text |
| `sent_at` | Message timestamp |

Relationships:

- Many `Message` records belong to one `Chat Session`.

### Supporting Tables from SRS Logical ERD

These tables exist in the SRS logical ERD, but some are outside the current developer scope unless specifically requested.

#### `Tag`

| Field | Notes |
|---|---|
| `tag_id` | Primary identifier |
| `name` | Tag name |
| `Gender` | Present in SRS ERD exactly as written; likely a diagram mistake |
| `Date of Birth` | Present in SRS ERD exactly as written; likely a diagram mistake |

Relationships:

- `Tag` is assigned to `Document` through `Document Tag`.

Implementation note:

- For the current project, do not add unrelated profile fields to `Tag` unless the team confirms the ERD typo. A practical implementation should use only `tag_id` and `name` for tags.

#### `Document Tag`

| Field | Notes |
|---|---|
| `document_id` | Parent document ID |
| `tag_id` | Parent tag ID |
| `Gender` | Present in SRS ERD exactly as written; likely a diagram mistake |
| `Date of Birth` | Present in SRS ERD exactly as written; likely a diagram mistake |

Relationships:

- Join table between `Document` and `Tag`.

Implementation note:

- For a clean join table, use `document_id` and `tag_id` only unless the ERD typo is intentionally corrected elsewhere.

#### `Document Share`

| Field | Notes |
|---|---|
| `share_id` | Primary identifier |
| `document_id` | Shared document ID |
| `shared_by_user_id` | User who shares the document |
| `shared_to_user_id` | User who receives access |
| `permission_type` | Share permission level/type |
| `shared_at` | Share timestamp |

Relationships:

- One `Document` can have many `Document Share` records.
- One `User` can share many documents.
- One `User` can receive many shared documents.

#### `OTP Verification`

| Field | Notes |
|---|---|
| `verification_id` | Primary identifier |
| `user_id` | Related user ID |
| `otp_code` | OTP value; do not log this |
| `verification_type` | Registration/password recovery/etc. |
| `attempts` | Number of OTP attempts |
| `expires_at` | Expiration timestamp |
| `created_at` | Creation timestamp |

Relationships:

- Many `OTP Verification` records belong to one `User`.

#### `Subscription Plan`

| Field | Notes |
|---|---|
| `plan_id` | Primary identifier |
| `plan_name` | Free/Plus/Pro plan name |
| `max_file_size` | Maximum allowed physical file size |
| `max_total_storage` | Storage limit |
| `allowed_formats` | File formats allowed by the plan |

Relationships:

- One `Subscription Plan` defines many `Subscription` records.

#### `Subscription`

| Field | Notes |
|---|---|
| `subscription_id` | Primary identifier |
| `user_id` | Subscribed user ID |
| `plan_id` | Subscription plan ID |
| `start_date` | Subscription start date |
| `end_date` | Subscription end date |
| `status` | Subscription status |

Relationships:

- Many `Subscription` records belong to one `User`.
- Many `Subscription` records reference one `Subscription Plan`.
- One `Subscription` can be paid by many `Payment Transaction` records.

#### `Payment Transaction`

| Field | Notes |
|---|---|
| `transaction_id` | Primary identifier |
| `user_id` | Paying user ID |
| `subscription_id` | Related subscription ID |
| `amount` | Payment amount |
| `payment_gateway` | Momo/VNPAY/etc. |
| `transaction_code` | Gateway transaction code |
| `status` | Payment status |
| `paid_at` | Payment timestamp |

Relationships:

- Many `Payment Transaction` records are made by one `User`.
- Many `Payment Transaction` records can pay for one `Subscription`.

### Table Creation Guardrails for Codex

- Do not invent tables such as `File`, `UploadedFile`, `VectorStore`, `Embedding`, `Conversation`, or `PromptHistory` when the existing SRS tables already cover the feature.
- Use `Document` for file metadata and S3 key storage.
- Use `Document Chunk` for parsed text chunks and, in this implementation, the embedding vector field.
- Use `Chat Session` and `Message` for RAG conversation persistence.
- Use `Document Tag` as the join table for document tags.
- Use `Document Share` only if document sharing is implemented.
- Use `Subscription`, `Subscription Plan`, and `Payment Transaction` only if subscription/payment features are implemented.
