# AetherDocs

> An AI-powered document workspace for storing, organising, sharing, and querying learning materials with Retrieval-Augmented Generation (RAG).

**Academic capstone project · Team of 4**

AetherDocs helps students keep study materials in one place, then ask grounded questions over selected documents or their personal document library. The backend provides secure document storage, asynchronous ingestion, semantic retrieval, AI chat with sources, and supporting collaboration and subscription features.

## Highlights

- Store documents and avatars in private AWS S3 storage.
- Organise documents with folders, tags, favourites, filters, trash, and restore actions.
- Share documents through public visibility, share links, and direct sharing with friends.
- Process uploaded documents asynchronously: extract text, chunk content, generate embeddings, and index it for retrieval.
- Ask questions over one document, selected documents, or a user's document library.
- Return source metadata for RAG responses so the client can show where an answer came from.
- Authenticate users with email/OTP, Google OAuth2, JWT access tokens, refresh-token rotation, logout, and password reset.
- Support subscription plans and VNPay payment flows.
- Provide admin APIs for user and payment management.

## My Contribution

I focused on the document storage and AI/RAG capabilities:

- Implemented private document and avatar storage with **AWS S3**, while persisting metadata in **SQL Server**.
- Built the document ingestion flow: upload, text extraction, chunking, embedding generation, and document readiness states.
- Integrated **Spring AI** with Gemini for embedding and document-grounded RAG chat.
- Implemented semantic retrieval with cosine similarity and returned source references with chat responses.

## RAG Flow

```mermaid
flowchart LR
    U[User uploads a document] --> API[Spring Boot API]
    API --> S3[Private AWS S3 storage]
    API --> DB[(SQL Server metadata)]
    API --> JOB[Async ingestion job]
    JOB --> PARSE[Parse and chunk content]
    PARSE --> EMBED[Spring AI / Gemini embeddings]
    EMBED --> VECTOR[(Document chunks and vectors)]

    Q[User question] --> RETRIEVE[Semantic retrieval]
    VECTOR --> RETRIEVE
    RETRIEVE --> LLM[Gemini chat model]
    LLM --> A[Grounded answer with sources]
