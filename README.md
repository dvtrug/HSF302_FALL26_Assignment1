# 🎬 FU Cinema Ticket Booking System

[![CI Workflow](https://github.com/dvtrug/HSF302_FALL26_Assignment1/actions/workflows/ci.yaml/badge.svg)](https://github.com/dvtrug/HSF302_FALL26_Assignment1/actions/workflows/ci.yaml)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![SQL Server](https://img.shields.io/badge/Database-MS%20SQL%20Server-blue.svg)](https://www.microsoft.com/sql-server)
[![Thymeleaf](https://img.shields.io/badge/Frontend-Thymeleaf%20%2B%20Bootstrap%205-005F0E.svg)](https://www.thymeleaf.org/)
[![License](https://img.shields.io/badge/License-Academic-lightgrey.svg)]()

> **Dự án Assignment 1 — Môn học HSF302 (Spring Framework / Spring Boot)**  
> **Repository:** [https://github.com/dvtrug/HSF302_FALL26_Assignment1](https://github.com/dvtrug/HSF302_FALL26_Assignment1)  
> **Phiên bản:** `v1.0.0`

---

## 📌 1. Giới thiệu dự án

**FU Cinema Ticket Booking System** là ứng dụng web hướng đối tượng theo kiến trúc **Monolith 3 tầng (3-Tier Architecture)** mô phỏng toàn diện hệ thống quản lý rạp chiếu phim và bán vé xem phim trực tuyến hiện đại. Hệ thống cung cấp hai phân hệ chính:
1. **Khách hàng (Customer):** Tra cứu danh sách phim đang chiếu/sắp chiếu, xem chi tiết và đánh giá phim, chọn suất chiếu, chọn ghế trực quan theo sơ đồ phòng, đặt vé xem phim trực tuyến, tra cứu lịch sử đơn hàng, hủy vé theo quy định và quản lý thông tin hồ sơ cá nhân.
2. **Quản trị viên (Admin / Manager):** Quản lý danh mục thể loại, phim (thêm, sửa, xóa mềm, lọc nâng cao), quản lý phòng chiếu và ghế, lập lịch và điều phối suất chiếu (kiểm soát chồng lấn phòng/giờ), quản lý tài khoản khách hàng (khóa/mở khóa), xử lý đơn đặt vé và báo cáo thống kê doanh thu đa chiều.

---

## 🏗️ 2. Kiến trúc & Công nghệ sử dụng

Hệ thống được thiết kế theo mô hình chuẩn **3-Layer Layered Architecture** đảm bảo phân tách rõ ràng trách nhiệm giữa các tầng (Separation of Concerns):

```
┌─────────────────────────────────────────────────────────────────┐
│                     Presentation Layer                          │
│   Thymeleaf 3 + Bootstrap 5 + HTML5/CSS3 + JavaScript Fetch     │
│   (Role-based dynamic navbar, Flash attributes, Error pages)    │
└───────────────────────────────┬─────────────────────────────────┘
                                │ HTTP Requests / View Model
┌───────────────────────────────▼─────────────────────────────────┐
│                      Controller Layer                           │
│   Spring MVC Controllers + Bean Validation (@Valid)             │
│   HandlerInterceptor (AuthInterceptor: /admin/**, /customer/**) │
│   @ControllerAdvice (GlobalExceptionHandler)                    │
└───────────────────────────────┬─────────────────────────────────┘
                                │ DTOs / Service Contracts
┌───────────────────────────────▼─────────────────────────────────┐
│                       Service Layer                             │
│   Business Logic + @Transactional Boundaries                    │
│   Password Hashing (BCryptPasswordEncoder)                      │
│   Custom Business Exceptions (BusinessException)                │
└───────────────────────────────┬─────────────────────────────────┘
                                │ Entities / JPQL / Native SQL
┌───────────────────────────────▼─────────────────────────────────┐
│                   Data Access Layer (DAO)                       │
│   Spring Data JPA (JpaRepository) + Hibernate ORM               │
│   Derived Queries + 12 Custom Queries (Q1 - Q12)                │
└───────────────────────────────┬─────────────────────────────────┘
                                │ JDBC Driver
┌───────────────────────────────▼─────────────────────────────────┐
│                        Database                                 │
│   Microsoft SQL Server 2019 / 2022 (FUCinemaTicketDB)           │
└─────────────────────────────────────────────────────────────────┘
```

### Chi tiết Stack công nghệ:
* **Ngôn ngữ lập trình:** Java 21 (LTS).
* **Framework:** Spring Boot 3.4.x (Spring Web, Spring Data JPA, Spring Validation).
* **Bảo mật:** Spring Security Crypto (`BCryptPasswordEncoder`) cho mã hóa mật khẩu; `HandlerInterceptor` kiểm soát phân quyền session-based.
* **Template Engine:** Thymeleaf 3 với layout dialect và fragment tái sử dụng.
* **Giao diện:** Bootstrap 5.3, Bootstrap Icons, CSS custom variables.
* **Cơ sở dữ liệu:** Microsoft SQL Server với Microsoft JDBC Driver 13.4.
* **Kiểm thử tự động:** JUnit 5, Mockito, Spring Test Context, MockMvc (83 automated test cases).
* **CI/CD:** GitHub Actions tự động build và chạy test trên môi trường Ubuntu runner với Java 21.

---

## 🗄️ 3. Mô hình Thực thể & Cơ sở Dữ liệu

Hệ thống quản lý **9 JPA Entities** tương ứng với 9 bảng trong CSDL `FUCinemaTicketDB`:

| Bảng CSDL | JPA Entity | Mô tả & Quan hệ chính |
| :--- | :--- | :--- |
| `accounts` | `Account` | Tài khoản đăng nhập hệ thống, lưu hash BCrypt, role (`ADMIN`, `CUSTOMER`, `STAFF`) và trạng thái (`ACTIVE`, `LOCKED`). |
| `customers` | `Customer` | Thông tin cá nhân khách hàng, liên kết 1-1 (`@OneToOne`) với `Account`. |
| `genres` | `Genre` | Thể loại phim (Hành động, Tình cảm, Hoạt hình,...), liên kết 1-N với `Movie`. |
| `movies` | `Movie` | Phim chiếu, lưu thời lượng, độ tuổi, đạo diễn, diễn viên, cờ xóa mềm `is_deleted`. |
| `rooms` | `Room` | Phòng chiếu rạp (`2D`, `3D`, `IMAX`), lưu sức chứa ghế và trạng thái. |
| `showtimes` | `Showtime` | Suất chiếu phim, liên kết khóa ngoại với `Movie` và `Room`, giá vé cơ bản. |
| `bookings` | `Booking` | Đơn đặt vé của khách hàng, lưu mã đơn `booking_code`, ngày đặt, tổng tiền, trạng thái (`PENDING`, `CONFIRMED`, `CANCELLED`). |
| `tickets` | `Ticket` | Vé xem phim chi tiết từng ghế (`seat_number`, `price`), liên kết 1-N cascade ALL với `Booking`. |
| `reviews` | `Review` | Đánh giá và bình luận phim, sử dụng khóa chính phức hợp `@EmbeddedId ReviewId` (`customer_id` + `movie_id`), giới hạn 1 khách chỉ đánh giá 1 phim 1 lần. |

---

## ⚖️ 4. Các Quy tắc Nghiệp vụ (Business Rules - BR)

Hệ thống tuân thủ chặt chẽ các ràng buộc nghiệp vụ theo yêu cầu đặc tả đề tài:
* **BR-01 (Tính duy nhất dữ liệu):** Ngăn chặn tuyệt đối việc đăng ký trùng lặp `username`, `email`, số điện thoại (`mobile`) hoặc CMND/CCCD (`identityCard`).
* **BR-02 (Ràng buộc đăng ký):** Khách hàng phải đủ từ 13 tuổi trở lên tính từ ngày sinh; mật khẩu tối thiểu 6 ký tự.
* **BR-03 (Chặn tài khoản khóa):** Chặn đăng nhập tức thì nếu tài khoản có trạng thái `LOCKED`.
* **BR-04 (Kiểm tra xung đột suất chiếu):** Không cho phép xếp lịch chiếu chồng lấn thời gian trong cùng một phòng chiếu.
* **BR-05 (Xóa mềm phim):** Phim đang có suất chiếu trong tương lai không được phép xóa cứng mà chuyển sang trạng thái xóa mềm (`is_deleted = true`).
* **BR-06 (An toàn phòng chiếu):** Phòng chiếu có suất chiếu trong tương lai không được phép xóa.
* **BR-07 (Quy định hủy vé):** Khách hàng chỉ được phép hủy đơn đặt vé trước thời điểm suất chiếu bắt đầu tối thiểu **60 phút**.
* **BR-08 (Ràng buộc ghế ngồi):** Vé trong cùng một suất chiếu không được trùng số ghế; kiểm tra tình trạng ghế theo thời gian thực.
* **BR-09 (Độc quyền đánh giá):** Khóa phức hợp `ReviewId` đảm bảo mỗi khách hàng chỉ gửi đánh giá điểm số (1 - 5 sao) và bình luận một lần cho mỗi bộ phim.
* **BR-10 (Báo cáo doanh thu):** Cho phép Admin tra cứu và tổng hợp doanh thu theo khoảng ngày, tính tổng số vé bán ra và tỷ lệ lấp đầy rạp.

---

## 🚀 5. Hướng dẫn Cài đặt & Khởi chạy ứng dụng

### 5.1. Yêu cầu môi trường
* **Java Development Kit (JDK):** Phiên bản **21** trở lên.
* **Apache Maven:** Phiên bản **3.9.x** trở lên (hoặc sử dụng wrapper `mvnw` đi kèm).
* **Cơ sở dữ liệu:** **Microsoft SQL Server** 2019 / 2022 (hoặc Azure SQL).
* **Trình duyệt web:** Chrome, Edge, Firefox phiên bản hiện đại.

### 5.2. Các bước triển khai

**Bước 1: Clone repository**
```bash
git clone https://github.com/dvtrug/HSF302_FALL26_Assignment1.git
cd fu-cinema-ticket-system
```

**Bước 2: Tạo Database trên SQL Server**
Mở SQL Server Management Studio (SSMS) hoặc Azure Data Studio, chạy câu lệnh:
```sql
CREATE DATABASE FUCinemaTicketDB;
GO
```

**Bước 3: Cấu hình thông tin kết nối CSDL**
Tạo file cấu hình cá nhân `src/main/resources/application-local.properties` (dựa theo file mẫu `application-local.properties.example`):
```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=FUCinemaTicketDB;encrypt=true;trustServerCertificate=true
spring.datasource.username=sa
spring.datasource.password=YourStrongPassword123
```

**Bước 4: Biên dịch và chạy kiểm thử tự động**
```bash
mvn clean test
```
*Kết quả kiểm thử tự động mong đợi:* **`Tests run: 83, Failures: 0, Errors: 0, Skipped: 0 - BUILD SUCCESS`**.

**Bước 5: Khởi động ứng dụng**
```bash
mvn spring-boot:run
```
Khi ứng dụng khởi động lần đầu, `DataSeeder` sẽ tự động tạo bảng và nạp đầy đủ dữ liệu mẫu (phim, rạp, suất chiếu, tài khoản, vé mẫu).

**Bước 6: Truy cập hệ thống**
Mở trình duyệt và truy cập:
👉 **`http://localhost:8080/`**

---

## 🔑 6. Danh sách Tài khoản Thử nghiệm (Demo Accounts)

Hệ thống đã nạp sẵn các tài khoản thử nghiệm đại diện cho các vai trò và trạng thái:

| Tên đăng nhập | Mật khẩu | Vai trò | Trạng thái | Mục đích kiểm thử |
| :--- | :--- | :--- | :--- | :--- |
| `admin` | `Admin@123` | **ADMIN** | `ACTIVE` | Quản trị toàn quyền: phim, phòng, lịch chiếu, khách hàng, vé, báo cáo doanh thu. |
| `customer1` | `123456` | **CUSTOMER** | `ACTIVE` | Khách hàng đã có sẵn lịch sử đặt vé và đánh giá phim. |
| `customer2` | `123456` | **CUSTOMER** | `ACTIVE` | Khách hàng đặt vé mới, đổi mật khẩu và cập nhật hồ sơ cá nhân. |
| `staff` | `123456` | **STAFF** | `ACTIVE` | Nhân viên vận hành rạp. |
| `locked_user` | `123456` | **CUSTOMER** | `LOCKED` | Kiểm thử nghiệp vụ **BR-03**: Hệ thống từ chối đăng nhập và báo lỗi tài khoản bị khóa. |

---

## 👥 7. Phân công Thành viên & Ma trận 32 TODOs

Dự án được phân chia thành 5 module tương ứng với 5 thành viên nhóm:

| STT | Họ tên | Vai trò | Nhánh & Phân công Module | Các TODO phụ trách |
| :---: | :--- | :--- | :--- | :--- |
| **1** | **Đỗ Võ Thanh Trung** (`dvtrug`) | **Leader (Thành viên A)** | `chore/1-repo-templates-ci`<br>`feature/2-project-setup`<br>`feature/3-entities-repositories`<br>`feature/4-seed-data`<br>`feature/5-service-contracts`<br>`feature/6-auth`<br>`docs/19-readme-release` | **TODO 1 – 15, TODO 32**<br>Quản trị dự án, thiết lập CI/CD, Nền tảng Spring Boot & Entities/Repositories Sprint 0; Module Xác thực & Phân quyền Sprint 1; Hoàn thiện tài liệu README & Release v1.0.0. |
| **2** | **Hồ Quốc Thắng** | **Thành viên B** | `feature/7-customer-admin`<br>`feature/16-profile` | **TODO 16, 21, 22, 30**<br>Module Quản trị Khách hàng (Admin) & Xem/cập nhật hồ sơ cá nhân, đổi mật khẩu (Customer). |
| **3** | **Thành viên C** | **Thành viên C** | `feature/8-genre-crud`<br>`feature/9-movie-crud`<br>`feature/13-home-movie-detail`<br>`feature/18-movie-review` | **TODO 17, 23, 24, 31**<br>Module Quản lý Thể loại, CRUD Phim, Trang chủ/Chi tiết phim và Đánh giá phim (Review). |
| **4** | **Thành viên D** | **Thành viên D** | `feature/10-room-crud`<br>`feature/11-showtime-scheduling` | **TODO 18, 25, 26**<br>Module Quản lý Phòng chiếu & Lập lịch suất chiếu (Showtime scheduling). |
| **5** | **Thành viên E** | **Thành viên E** | `feature/12-booking-admin`<br>`feature/14-booking-seat-selection`<br>`feature/15-booking-history`<br>`feature/17-revenue-report` | **TODO 19, 20, 27, 28, 29**<br>Module Chọn ghế, Đặt vé xem phim, Quản trị đơn đặt vé, Lịch sử/Hủy vé và Báo cáo doanh thu. |

---

## 🔄 8. Quy trình Phát triển (Gitflow & Conventional Commits)

Nhóm áp dụng nghiêm ngặt quy trình làm việc chuyên nghiệp:
* **Nhánh `main`:** Chỉ chứa mã nguồn ổn định đã phát hành chính thức (`Release v1.0.0`).
* **Nhánh `develop`:** Nhánh tích hợp chính của toàn bộ nhóm trong suốt các Sprint.
* **Nhánh tính năng (`feature/*`, `chore/*`, `docs/*`):** Tách từ `develop`, mở Pull Request chuẩn hóa 6 phần (*What / Why / How to test / Screenshots / Checklist / Closes #*).
* **Conventional Commits:** `feat:`, `fix:`, `chore:`, `docs:`, `test:`, `refactor:`.
* **CI Gate:** Bắt buộc 100% test case vượt qua trước khi merge vào `develop`.

---

## 📄 9. Bản quyền & Thông tin liên hệ

* **Học phần:** HSF302 — Java Web Application Development with Spring Framework.
* **Học kỳ:** Fall 2026 — FPT University.
* **Trưởng nhóm:** Đỗ Võ Thanh Trung (`thanhtrungxxz@gmail.com`).
* **GitHub Project:** [https://github.com/dvtrug/HSF302_FALL26_Assignment1](https://github.com/dvtrug/HSF302_FALL26_Assignment1)
