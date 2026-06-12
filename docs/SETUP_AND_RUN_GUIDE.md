# Hướng dẫn clone và chạy dự án AI Study Hub Backend

Tài liệu này hướng dẫn thành viên trong team clone project backend về máy local, cấu hình môi trường và chạy được hệ thống.

---

## 1. Yêu cầu môi trường

Cần cài sẵn:

- Git
- Java JDK 26
- SQL Server
- SQL Server Management Studio hoặc Azure Data Studio
- Postman
- Maven Wrapper đã có sẵn trong project, không bắt buộc cài Maven global
- Tesseract OCR nếu muốn test OCR
- AWS S3 account nếu muốn test upload file thật
- Gemini API key nếu muốn test embedding thật
- Gmail app password nếu muốn test gửi OTP email
- Google OAuth2 Client nếu muốn test login Google

Kiểm tra Java:

```powershell
java -version
```

Kiểm tra Git:

```powershell
git --version
```

---

## 2. Clone project

```powershell
git clone https://github.com/longnb47/SWP391-SE1908-GROUP-01.git
cd SWP391-SE1908-GROUP-01\group01
```

Nếu cần làm trên nhánh upload/auth hiện tại:

```powershell
git checkout feature/upload
git pull origin feature/upload
```

---

## 3. Tạo database SQL Server

Mở SQL Server Management Studio và chạy:

```sql
CREATE DATABASE lms_ai;
```

Kiểm tra database:

```sql
SELECT name FROM sys.databases WHERE name = 'lms_ai';
```

Database mặc định của project:

```text
lms_ai
```

---

## 4. Tạo file `.env`

Project có file mẫu:

```text
.env.example
```

Copy file mẫu thành `.env`:

```powershell
Copy-Item .env.example .env
```

Sau đó mở `.env` và điền thông tin thật.

Lưu ý:

- Không commit `.env`.
- Không gửi secret lên GitHub.
- `.env` là file local của từng máy.

---

## 5. Các biến môi trường cần cấu hình

### 5.1. SQL Server

```env
SQLSERVER_DATASOURCE_URL=jdbc:sqlserver://localhost:1433;databaseName=lms_ai;encrypt=true;trustServerCertificate=true
SQLSERVER_DATASOURCE_USERNAME=sa
SQLSERVER_DATASOURCE_PASSWORD=your_sql_server_password
```

Nếu SQL Server của bạn không dùng user `sa`, hãy đổi username/password tương ứng.

---

### 5.2. JWT

```env
APP_JWT_SECRET=replace_with_at_least_32_characters_secret
APP_JWT_EXPIRATION_MS=300000
```

Lưu ý:

- `APP_JWT_SECRET` nên dài tối thiểu 32 ký tự.
- `APP_JWT_EXPIRATION_MS=300000` nghĩa là token hết hạn sau 5 phút.

---

### 5.3. Mail OTP

```env
MAIL_USERNAME=your_gmail_address
MAIL_PASSWORD=your_gmail_app_password
```

Lưu ý:

- Không dùng password Gmail thường.
- Cần tạo Gmail App Password.
- Dùng cho chức năng gửi OTP khi register/resend OTP.

---

### 5.4. Google OAuth2

```env
GOOGLE_CLIENT_ID=your_google_oauth_client_id
GOOGLE_CLIENT_SECRET=your_google_oauth_client_secret
```

Nếu chưa test Google login thì vẫn có thể để placeholder, nhưng chức năng Google OAuth2 sẽ không hoạt động thật.

---

### 5.5. AWS S3

```env
AWS_ACCESS_KEY_ID=your_aws_access_key
AWS_SECRET_ACCESS_KEY=your_aws_secret_key
AWS_REGION=ap-southeast-1
AWS_S3_BUCKET_NAME=your_s3_bucket_name
AWS_S3_KEY_PREFIX=
AWS_S3_ENDPOINT=
```

Lưu ý:

- `AWS_S3_KEY_PREFIX` có thể để trống.
- `AWS_S3_ENDPOINT` chỉ cần nếu dùng LocalStack/MinIO.
- Bucket nên để private.
- IAM user cần quyền upload/delete object.

---

### 5.6. Gemini Embedding

```env
SPRING_AI_MODEL_EMBEDDING_TEXT=google-genai
GEMINI_API_KEY=your_gemini_api_key
```

Dùng cho bước tạo embedding sau khi parse/chunk tài liệu.

---

### 5.7. Tesseract OCR

```env
TESSERACT_ENABLED=false
TESSERACT_EXECUTABLE_PATH=C:\Program Files\Tesseract-OCR\tesseract.exe
TESSERACT_TESSDATA_PATH=C:\Program Files\Tesseract-OCR\tessdata
TESSERACT_LANGUAGE=eng
TESSERACT_TIMEOUT_SECONDS=60
TESSERACT_PDF_DPI=200
TESSERACT_PDF_TEXT_MIN_CHARACTERS=20
```

Nếu muốn bật OCR:

```env
TESSERACT_ENABLED=true
```

Kiểm tra Tesseract:

```powershell
tesseract --version
tesseract --list-langs
```

Cần thấy ngôn ngữ:

```text
eng
```

---

## 6. Load `.env` trước khi chạy app

Spring Boot không tự load file `.env`, nên cần load biến môi trường trong PowerShell.

Chạy tại thư mục `group01`:

```powershell
Get-Content .env | ForEach-Object {
  $line = $_.Trim()
  if (!$line -or $line.StartsWith('#')) { return }
  if ($line -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
    [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2], "Process")
  }
}
```

Kiểm tra một vài biến:

```powershell
echo $env:SQLSERVER_DATASOURCE_URL
echo $env:AWS_S3_BUCKET_NAME
echo $env:GEMINI_API_KEY
```

Không chụp/gửi màn hình có secret thật.

---

## 7. Chạy project

Chạy test trước:

```powershell
.\mvnw.cmd clean test
```

Chạy app:

```powershell
.\mvnw.cmd spring-boot:run
```

Nếu chạy thành công sẽ thấy log:

```text
Tomcat started on port 8080
Started Group01Application
```

Backend chạy tại:

```text
http://localhost:8080
```

---

## 8. Swagger UI

Sau khi app chạy:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI docs:

```text
http://localhost:8080/v3/api-docs
```

---

## 9. Test Auth API bằng Postman

### 9.1. Register

```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json
```

Body:

```json
{
  "fullName": "Long Nguyen",
  "email": "long@example.com",
  "password": "password123"
}
```

Sau khi register, kiểm tra email để lấy OTP.

---

### 9.2. Verify OTP

```http
POST http://localhost:8080/api/auth/verify-otp
Content-Type: application/json
```

Body:

```json
{
  "email": "long@example.com",
  "otp": "123456"
}
```

---

### 9.3. Login

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json
```

Body:

```json
{
  "email": "long@example.com",
  "password": "password123"
}
```

Response sẽ có:

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
  "timestamp": "2026-06-12T10:30:00Z"
}
```

Copy `accessToken` để test API document.

---

### 9.4. Resend OTP

```http
POST http://localhost:8080/api/auth/resend-otp
Content-Type: application/json
```

Body:

```json
{
  "email": "long@example.com"
}
```

---

## 10. Test Document API bằng Postman

Các API document private cần header:

```http
Authorization: Bearer <accessToken>
```

Không truyền `userId` nữa. Backend tự lấy user từ JWT.

---

### 10.1. Upload file

```http
POST http://localhost:8080/api/documents/upload
Authorization: Bearer <accessToken>
Content-Type: multipart/form-data
```

Body form-data:

| Key | Type | Required | Value |
|---|---|---|---|
| file | File | Yes | chọn file |
| isPublic | Text | No | true hoặc false |

---

### 10.2. List document của current user

```http
GET http://localhost:8080/api/documents/my
Authorization: Bearer <accessToken>
```

---

### 10.3. Detail document

```http
GET http://localhost:8080/api/documents/{documentId}
Authorization: Bearer <accessToken>
```

---

### 10.4. List public document

```http
GET http://localhost:8080/api/documents/public
Authorization: Bearer <accessToken>
```

Ghi chú:

- Về business, API này phục vụ community/public.
- Theo `SecurityConfig` hiện tại, API này vẫn cần JWT.
- Nếu muốn guest xem được thì cần chỉnh security permit endpoint này.

---

### 10.5. Detail public document

```http
GET http://localhost:8080/api/documents/public/{documentId}
Authorization: Bearer <accessToken>
```

---

### 10.6. Đổi visibility

```http
PATCH http://localhost:8080/api/documents/{documentId}/visibility?isPublic=true
Authorization: Bearer <accessToken>
```

---

### 10.7. Chuyển vào Trash

```http
DELETE http://localhost:8080/api/documents/{documentId}
Authorization: Bearer <accessToken>
```

---

### 10.8. List Trash

```http
GET http://localhost:8080/api/documents/trash
Authorization: Bearer <accessToken>
```

---

### 10.9. Restore document

```http
POST http://localhost:8080/api/documents/{documentId}/restore
Authorization: Bearer <accessToken>
```

---

### 10.10. Xoá vĩnh viễn

```http
DELETE http://localhost:8080/api/documents/{documentId}/permanent
Authorization: Bearer <accessToken>
```

---

## 11. File upload được hỗ trợ

### Tài liệu có parse/index

- `.pdf`
- `.doc`
- `.docx`
- `.pptx`
- `.xls`
- `.xlsx`

### Ảnh

- `.png`
- `.jpg`
- `.jpeg`
- `.webp`
- `.gif`
- `.bmp`
- `.tif`
- `.tiff`
- `Content-Type: image/*`

Giới hạn dung lượng:

```text
20MB
```

---

## 12. Luồng upload hiện tại

```text
Client gửi file + JWT
-> Backend lấy current user từ SecurityContext/JWT
-> Validate file
-> Upload file lên S3
-> Lưu metadata document vào SQL Server
-> Parse nội dung file
-> OCR nếu cần và được bật
-> Chunk bằng Spring AI TokenTextSplitter
-> Embedding bằng Gemini
-> Lưu document_chunk
-> Cập nhật trạng thái document
```

---

## 13. Lỗi thường gặp

### 13.1. Không kết nối được SQL Server

Kiểm tra:

- SQL Server đã chạy chưa.
- Database `lms_ai` đã tạo chưa.
- Username/password trong `.env` đúng chưa.
- SQL Server có bật TCP/IP chưa.
- Port `1433` có đúng không.

---

### 13.2. App không đọc `.env`

Spring Boot không tự đọc `.env`.

Cần chạy lại lệnh load `.env` trong cùng cửa sổ PowerShell trước khi chạy app:

```powershell
Get-Content .env | ForEach-Object {
  $line = $_.Trim()
  if (!$line -or $line.StartsWith('#')) { return }
  if ($line -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
    [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2], "Process")
  }
}
```

---

### 13.3. Document API bị 401

Kiểm tra:

- Đã login chưa.
- Có copy đúng `accessToken` không.
- Header có đúng format không:

```http
Authorization: Bearer <accessToken>
```

- Token có hết hạn chưa.

---

### 13.4. Upload lỗi S3

Kiểm tra:

- AWS key/secret đúng chưa.
- Bucket name đúng chưa.
- Region đúng chưa.
- IAM user có quyền `PutObject` và `DeleteObject` không.
- Bucket có tồn tại không.

---

### 13.5. Embedding lỗi Gemini

Kiểm tra:

- `GEMINI_API_KEY` đúng chưa.
- `SPRING_AI_MODEL_EMBEDDING_TEXT=google-genai`
- API key có quyền gọi Gemini API không.
- Máy có internet không.

---

### 13.6. OCR không chạy

Kiểm tra:

```powershell
tesseract --version
tesseract --list-langs
```

Kiểm tra `.env`:

```powershell
echo $env:TESSERACT_ENABLED
echo $env:TESSERACT_EXECUTABLE_PATH
echo $env:TESSERACT_TESSDATA_PATH
```

---

### 13.7. Register không gửi được email

Kiểm tra:

- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- Gmail App Password đã bật chưa.
- Gmail có chặn đăng nhập SMTP không.

---

### 13.8. Google OAuth không chạy

Kiểm tra:

- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- Redirect URI trong Google Cloud Console.
- App đã load `.env` chưa.

---

## 14. Quy tắc khi code

- Không commit `.env`.
- Không commit API key, password database, AWS key, Gemini key, Gmail password, Google client secret.
- Không sửa module không liên quan task.
- Không viết business logic trong Controller.
- Controller nhận request và gọi Service.
- Service xử lý business logic.
- Repository chỉ thao tác database.
- Entity chỉ mapping table.
- Tất cả API nên trả response theo format `ApiResponse`.

---

## 15. Lệnh Git cơ bản

Luôn pull code mới trước khi code:

```powershell
git checkout feature/upload
git pull origin feature/upload
```

Xem file thay đổi:

```powershell
git status
```

Commit:

```powershell
git add .
git commit -m "feat(scope): short description"
```

Push:

```powershell
git push origin feature/upload
```

Không push file `.env`, report cá nhân hoặc file không liên quan task nếu không được yêu cầu.

---

## 16. Checklist chạy dự án nhanh

```text
1. Clone project
2. Checkout đúng branch
3. Tạo database lms_ai
4. Copy .env.example thành .env
5. Điền secret thật vào .env
6. Load .env trong PowerShell
7. Chạy .\mvnw.cmd clean test
8. Chạy .\mvnw.cmd spring-boot:run
9. Register account
10. Verify OTP
11. Login lấy JWT
12. Test upload document với Authorization Bearer token
```
