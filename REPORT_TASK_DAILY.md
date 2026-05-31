# Báo cáo công việc hằng ngày — AI Study Hub

Ngày: 2026-05-31
Scope tuân thủ: `AGENTS.md` + `docs/srs-summary.md` (chỉ phần Upload tài liệu + các bảng/persistence liên quan RAG Chat).

## Tóm tắt (Hôm nay đã làm gì)

- Cấu hình kết nối MySQL tới database `ai_hub_study` trong `src/main/resources/application.yaml`.
- Thêm JPA và tạo/điều chỉnh entity theo SRS Logical ERD:
  - `Document` map vào bảng `document`.
  - `DocumentChunk` map vào bảng `document_chunk` và bổ sung `embedding_vector` (lưu dạng `LONGTEXT`) theo chiến lược lưu vector trong `docs/srs-summary.md`.
- Implement luồng tối thiểu **Upload tài liệu lên Amazon S3** (object private) theo rules trong `AGENTS.md`:
  - Validate file (rỗng/kích thước <= 20MB/allow-list theo extension).
  - Sanitize tên file.
  - Tạo S3 object key an toàn: `documents/{userId}/{uuid}-{sanitizedFilename}` (hỗ trợ prefix tuỳ chọn).
  - Upload lên S3 (ACL private).
  - Lưu metadata document vào MySQL (bảng `document`).
  - Best-effort cleanup: nếu lưu DB thất bại sau khi upload S3 thì cố gắng xoá object trên S3.
- Tạo template `.env` cho biến môi trường AWS và đảm bảo `.env` không bị commit (đã ignore trong Git).

## API (Thống kê)

Tổng số API được thêm/cập nhật hôm nay: **1**

### 1) Upload tài liệu

- Method: `POST`
- Path: `/api/documents/upload`
- Content-Type: `multipart/form-data`
- Request fields:
  - `userId` (bắt buộc, `Long`)
  - `file` (bắt buộc, `MultipartFile`)
  - `isPublic` (tuỳ chọn, `Boolean`) — lưu vào `document.is_public`
- Response: `200 OK`
  - Body: `DocumentUploadResponse` (gồm `documentId`, `userId`, `originalFileName`, `s3Key`, `contentType`, `fileSize`, `isPublic`, `uploadedAt`)
- Lỗi có thể trả về:
  - `400 Bad Request`: lỗi validate (file rỗng, quá dung lượng, extension không hỗ trợ, thiếu userId)
  - `503 Service Unavailable`: upload S3 thất bại
  - `500 Internal Server Error`: lỗi đọc file hoặc lỗi không mong đợi

## Cấu hình / Môi trường

- `.env` (chỉ local; Spring Boot không tự load `.env`).
  - Biến bắt buộc cho S3 upload:
    - `AWS_ACCESS_KEY_ID`
    - `AWS_SECRET_ACCESS_KEY`
    - `AWS_REGION`
    - `AWS_S3_BUCKET_NAME`
  - Tuỳ chọn:
    - `AWS_S3_KEY_PREFIX`
    - `AWS_S3_ENDPOINT` (LocalStack/MinIO)
- Map env vars vào Spring config:
  - `aws.region: ${AWS_REGION:}`
  - `aws.s3.bucket-name: ${AWS_S3_BUCKET_NAME:}`
  - `aws.s3.key-prefix: ${AWS_S3_KEY_PREFIX:}`
  - `aws.s3.endpoint: ${AWS_S3_ENDPOINT:}`
- Set giới hạn upload multipart:
  - `spring.servlet.multipart.max-file-size: 20MB`
  - `spring.servlet.multipart.max-request-size: 20MB`

## Verification

- `.\mvnw.cmd test` — PASS (Spring context load OK, JPA scan entity OK, wiring repository OK).
- Manual: bạn xác nhận upload lên S3 thành công.

## Ghi chú / Việc tiếp theo

- Spring Boot không tự load `.env`; cần export env vars trong terminal hoặc cấu hình Run/Debug trong IDE.
- Bước tiếp theo (chưa làm hôm nay): parsing → chunking → embedding (Spring AI) → lưu vector → quản lý trạng thái (`UPLOADED/PARSING/INDEXING/READY/FAILED`) → chặn chat nếu document chưa index xong.
