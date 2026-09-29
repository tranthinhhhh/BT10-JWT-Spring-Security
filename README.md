# Bài tập 10 - JWT với JJWT và Nimbus JOSE + JWT

Repository gồm hai ứng dụng Spring Boot 3 / Spring Security 6 có cùng chức năng:

- `jjwt-app/`: tạo và xác minh JWT bằng **JJWT 0.12.6**.
- `nimbus-app/`: tạo và xác minh JWT trực tiếp bằng **Nimbus JOSE + JWT 9.40**; module này không phụ thuộc JJWT.

Cả hai ứng dụng có entity, repository, service, BCrypt, đăng ký, đăng nhập, JWT filter, phân quyền USER/ADMIN, xử lý JSON 401/403 và giao diện AJAX `login.html` / `profile.html`.

## Lựa chọn cơ sở dữ liệu

Bài dùng **H2 file database** để có thể chấm và chạy ngay mà không cần cài MySQL. Dữ liệu chạy thật nằm trong `data/` và đã được gitignore. Hai module dùng hai database riêng:

- JJWT: `data/jjwt`
- Nimbus: `data/nimbus`

Test tự động dùng H2 in-memory nên không làm thay đổi dữ liệu chạy thật.

## Yêu cầu môi trường

- Windows PowerShell.
- JDK 17 trở lên.
- Không cần cài Maven toàn hệ thống vì repository đã có Maven Wrapper 3.9.9.

Kiểm tra Java:

```powershell
java -version
javac -version
```

Nếu `JAVA_HOME` chưa có, đặt cho cửa sổ PowerShell hiện tại. Đường dẫn phổ biến trên máy đã dùng để phát triển bài này:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
```

## Cấu hình biến môi trường

Ứng dụng cố ý không chứa khóa JWT hay mật khẩu admin thật. Trước khi chạy, đặt các biến trong đúng cửa sổ PowerShell:

```powershell
$env:JWT_SECRET = 'thay-bang-chuoi-ngau-nhien-it-nhat-32-byte'
$env:JWT_EXPIRATION_MS = '3600000'

# Không bắt buộc. Cần đặt nếu muốn thử endpoint GET /users dành cho ADMIN.
$env:ADMIN_EMAIL = 'admin@example.com'
$env:ADMIN_PASSWORD = 'mat-khau-admin-cuc-bo-du-manh'
$env:ADMIN_FULL_NAME = 'Administrator'
```

Có thể sinh secret ngẫu nhiên bằng PowerShell:

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
```

Tệp `.env.example` chỉ mô tả tên biến và giá trị giả. Không commit `.env`, secret hay mật khẩu thật.

## Chạy trên Windows

Mở PowerShell tại thư mục repository và đặt `JAVA_HOME`, `JWT_SECRET` như trên.

### Phiên bản JJWT

```powershell
.\mvnw.cmd -pl jjwt-app spring-boot:run
```

- Giao diện đăng nhập: http://localhost:8081/login.html
- Profile: http://localhost:8081/profile.html
- H2 Console: http://localhost:8081/h2-console

### Phiên bản Nimbus

Mở một cửa sổ PowerShell khác, đặt lại các biến môi trường rồi chạy:

```powershell
.\mvnw.cmd -pl nimbus-app spring-boot:run
```

- Giao diện đăng nhập: http://localhost:8082/login.html
- Profile: http://localhost:8082/profile.html
- H2 Console: http://localhost:8082/h2-console

Muốn đổi cổng, đặt `$env:SERVER_PORT` trước khi chạy module tương ứng.

## Build và test

Chạy toàn bộ hai module:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\mvnw.cmd clean test
```

Đóng gói hai file JAR:

```powershell
.\mvnw.cmd -DskipTests package
```

Kết quả:

- `jjwt-app/target/jjwt-app-1.0.0.jar`
- `nimbus-app/target/nimbus-app-1.0.0.jar`

## Endpoint

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| POST | `/auth/signup` | Public | Đăng ký USER, password được BCrypt |
| POST | `/auth/login` | Public | Xác thực và trả JWT có hạn dùng |
| GET | `/users/me` | USER hoặc ADMIN | Thông tin người đang đăng nhập |
| GET | `/users` | ADMIN | Danh sách người dùng |
| GET | `/login.html` | Public | Form đăng ký/đăng nhập AJAX |
| GET | `/profile.html` | Public page | JavaScript gọi API bằng Bearer token |

`/users/me` và `/users` không bao giờ trả trường password.

### JSON đăng ký

```json
{
  "email": "student@example.com",
  "password": "Student123!",
  "fullName": "Nguyen Van A"
}
```

### JSON đăng nhập

```json
{
  "email": "student@example.com",
  "password": "Student123!"
}
```

Response:

```json
{
  "token": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 3600000
}
```

## Kiểm tra nhanh bằng PowerShell

Ví dụ dưới đây dùng JJWT ở cổng 8081. Đổi sang 8082 để thử Nimbus.

```powershell
$base = 'http://localhost:8081'

$signupBody = @{
  email = 'student@example.com'
  password = 'Student123!'
  fullName = 'Nguyen Van A'
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "$base/auth/signup" `
  -ContentType 'application/json' -Body $signupBody

$loginBody = @{
  email = 'student@example.com'
  password = 'Student123!'
} | ConvertTo-Json

$login = Invoke-RestMethod -Method Post -Uri "$base/auth/login" `
  -ContentType 'application/json' -Body $loginBody

$headers = @{ Authorization = "Bearer $($login.token)" }
Invoke-RestMethod -Method Get -Uri "$base/users/me" -Headers $headers

# USER thường nhận HTTP 403:
Invoke-RestMethod -Method Get -Uri "$base/users" -Headers $headers
```

Đăng nhập bằng tài khoản được tạo từ `ADMIN_EMAIL` / `ADMIN_PASSWORD` để gọi `GET /users` thành công.

## Quy tắc lỗi bảo mật

- Sai email/mật khẩu: HTTP 401.
- Thiếu Bearer token tại endpoint được bảo vệ: HTTP 401.
- Header không dùng Bearer, token hỏng, sai chữ ký hoặc hết hạn: HTTP 401.
- Đã xác thực nhưng USER gọi `GET /users`: HTTP 403.
- Email đăng ký trùng: HTTP 409.
- JSON không hợp lệ: HTTP 400.

## Khác nhau giữa JJWT và Nimbus

| Nội dung | JJWT | Nimbus JOSE + JWT |
|---|---|---|
| Dependency | `jjwt-api`, `jjwt-impl`, `jjwt-jackson` | `nimbus-jose-jwt` |
| Tạo token | `Jwts.builder()` | `JWTClaimsSet` + `SignedJWT` + `MACSigner` |
| Xác minh | `Jwts.parser().verifyWith(...)` | `SignedJWT.parse()` + `MACVerifier` |
| Thuật toán | HS256 | HS256 và kiểm tra header bắt buộc |
| Issuer | `bt10-jjwt` | `bt10-nimbus` |

Hai module dùng code Spring Security/API tương đương, nhưng toàn bộ bước ký và xác minh JWT của module Nimbus do Nimbus thực hiện.

## Kết quả kiểm tra thực tế

Ngày 29/09/2026, trên Windows với JDK 17:

- `.\mvnw.cmd clean test`: **BUILD SUCCESS** cho cả hai module.
- Mỗi module: 3 test tích hợp, 0 failure, 0 error.
- Đã chạy đồng thời JAR trên 8081 và 8082.
- Đã thử thực tế: signup 201, login 200, `/users/me` 200, thiếu token 401, sai chữ ký 401, token hết hạn 401, sai mật khẩu 401, USER gọi `/users` 403, ADMIN gọi `/users` 200.
- Chrome headless/Selenium đã thao tác form đăng ký, đăng nhập, profile, logout và bảng user trên cả hai module.

## Cấu trúc chính

```text
.
├── jjwt-app/
│   ├── src/main/java/...      # Entity, repository, service, filter, security, controller
│   ├── src/main/resources/    # application.properties + UI
│   └── src/test/              # Test tích hợp JJWT
├── nimbus-app/
│   ├── src/main/java/...      # Cùng chức năng, JwtService dùng Nimbus
│   ├── src/main/resources/    # application.properties + UI
│   └── src/test/              # Test tích hợp Nimbus
├── .mvn/
├── mvnw
├── mvnw.cmd
└── pom.xml                    # Maven reactor cho cả hai module
```
