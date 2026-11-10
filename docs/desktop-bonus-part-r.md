# Đặc Tả & Kế Hoạch Triển Khai Part R — Bonus (+1.0 Điểm)

> **Dự án:** FU Cinema Ticket System — Desktop Extension  
> **Học phần:** HSF302 — Working with Spring Boot  
> **Căn cứ đề bài:** Mục 7 (Part R) — *Assignment 02: Ứng dụng quầy vé JavaFX tích hợp Spring Boot* (traltb@fe.edu.vn)  
> **Trạng thái:** Triển khai thử nghiệm tại Local (Proof-of-Concept)

---

## 1. Danh Mục Các Tính Năng Bonus Part R

Theo quy định đề bài, nhóm sinh viên có thể thực hiện thêm các tính năng mở rộng ngoài phạm vi cốt lõi để nhận tối đa **+1.0 điểm thưởng**:

| Mã | Hạng mục Bonus | Điểm thưởng | Yêu cầu kỹ thuật | Trạng thái Local |
|:---:|---|:---:|---|:---:|
| **B1** | **In vé xem phim** | `+0.25` | Sử dụng JavaFX Print API (`PrinterJob`) in vé/phiếu thanh toán ngay sau khi bán vé thành công. | 🟢 Sẵn sàng |
| **B2** | **Kiosk Mode cho khách** | `+0.50` | Cho phép tài khoản `CUSTOMER` đăng nhập tại máy Kiosk: xem phim, đặt vé (tái dùng `SeatMapView`), xem lịch sử và huỷ đơn theo BR-14. | 🟡 Thiết kế |
| **B3** | **Tự động làm mới sơ đồ ghế (Auto-refresh 10s)** | `+0.25` | Sơ đồ ghế tự động đồng bộ trạng thái mới nhất mỗi 10 giây khi mở màn hình Bán vé tại quầy (tránh giữ ghế cũ). | 🟢 Sẵn sàng |
| **B4** | **Kiến trúc Multi-module Maven** | `+0.50` | Tách dự án thành 3 module: `cinema-core` (entity/service/repo), `cinema-web` (Thymeleaf MVC) và `cinema-desktop` (JavaFX). | 🟡 Thiết kế |
| **B5** | **Chuyển đổi Theme Sáng / Tối (Dark/Light Mode)** | `+0.25` | Hỗ trợ nút chuyển đổi theme động ngay lúc chạy trên giao diện chính, hoặc đa ngôn ngữ (`ResourceBundle`). | 🟢 Sẵn sàng |
| **B6** | **Đóng gói cài đặt bằng `jpackage`** | `+0.25` | Đóng gói ứng dụng thành file thực thi độc lập kèm JRE rút gọn (`.exe` / `.msi`). | 🟡 Thiết kế |

---

## 2. Chi Tiết Triển Khai Tại Local

### 2.1. Bonus B3 — Sơ đồ ghế tự làm mới mỗi 10 giây (Auto-refresh)
- **Mục tiêu:** Khi nhân viên đang mở màn hình Bán vé tại quầy, nếu có khách hàng khác đặt vé online trên Website thành công, ghế đó phải tự động chuyển sang màu đỏ (ghế đã bán) trên màn hình quầy sau tối đa 10 giây mà không cần nhân viên phải thao tác chọn lại phim/suất chiếu.
- **Giải pháp kỹ thuật:**
  - Sử dụng JavaFX `Timeline` với chu kỳ 10 giây (`Duration.seconds(10)`).
  - Vòng lặp chỉ chạy khi màn hình `CounterSale` đang hiển thị (`root.getScene() != null`) và suất chiếu đang được chọn (`showtime.getValue() != null`).
  - Tự động dừng vòng lặp khi rời màn hình để tránh rò rỉ bộ nhớ (Memory Leak).
  - Giữ nguyên các ghế nhân viên đang chọn trên giao diện nếu ghế đó chưa bị bên khác mua.

### 2.2. Bonus B1 — In vé xem phim bằng `PrinterJob`
- **Mục tiêu:** Sau khi tạo đơn thành công (nút *Bán vé* hoàn tất), hệ thống đưa ra tùy chọn in vé xem phim ra máy in nhiệt hoặc máy in PDF của hệ điều hành.
- **Giải pháp kỹ thuật:**
  - Sử dụng class `javafx.print.PrinterJob` tiêu chuẩn của JavaFX.
  - Tự động dựng một Node giao diện vé (`VBox` mô phỏng vé xem phim kích thước chuẩn khổ giấy in nhiệt: Tên rạp, Tên phim, Phòng chiếu, Suất chiếu, Danh sách ghế, Tổng tiền và Mã đơn đặt vé).
  - Gọi `job.printPage(ticketNode)` và `job.endJob()`.

### 2.3. Bonus B5 — Chuyển đổi Theme Sáng / Tối (Light / Dark Mode Switcher)
- **Mục tiêu:** Hỗ trợ nhân viên làm việc trong các điều kiện ánh sáng khác nhau bằng cách chuyển đổi giữa giao diện Dark Theme (mặc định của rạp) và Light Theme (tươi sáng).
- **Giải pháp kỹ thuật:**
  - Thêm class CSS `.light-theme` vào file `desktop.css`.
  - Cung cấp nút chuyển đổi Theme `🌙 Tối / ☀️ Sáng` trên thanh trạng thái (Status Bar) hoặc thanh điều hướng.
  - Toggle class `.light-theme` trên `root` node của cửa sổ chính (`BorderPane`).

---

## 3. Kịch Bản Kiểm Thử Nghiệm Thu Bonus (TC-B01 → TC-B03)

| Mã test | Hạng mục | Các bước thực hiện | Kết quả mong đợi |
|:---:|---|---|---|
| **TC-B01** | B3 Auto-refresh | 1. Mở màn hình Bán vé quầy, chọn phim và suất chiếu.<br>2. Mở trình duyệt Web, đặt và thanh toán ghế `A5`.<br>3. Chờ 10 giây tại màn hình desktop. | Ghế `A5` trên sơ đồ máy quầy tự động chuyển sang trạng thái đã bán (khoá chọn) mà không cần reload trang. |
| **TC-B02** | B1 In vé | 1. Chọn khách hàng, chọn ghế và bấm *Bán vé*.<br>2. Hộp thoại in vé xuất hiện $\rightarrow$ Chọn Đồng ý. | Cửa sổ Print Dialog của Windows xuất hiện, cho phép in ra máy in hoặc Microsoft Print to PDF. |
| **TC-B03** | B5 Đổi Theme | 1. Bấm nút chuyển đổi theme `☀️ Sáng / 🌙 Tối` trên giao diện.<br>2. Quan sát màu nền sidebar, bảng biểu và thanh điều hướng. | Giao diện đổi sang tông màu sáng/tối đồng bộ tức thì, chữ và icon hiển thị tương phản rõ nét. |
