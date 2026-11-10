# Test Plan FU Cinema Desktop

Ma trận dựa trên mục 9.2–9.3 của tài liệu yêu cầu cuối kỳ HSF302 và
các issue #44–#58. TC-D09 và TC-D16 giữ nội dung trong kế hoạch nhóm.
Các hậu tố a, b, c tách những bộ input được thi hành riêng. TC-D21 và TC-D22
bổ sung hai luồng bắt buộc của cô. Pass là kết quả đã ghi nhận trên phần được
mô tả, còn Chờ là ca chưa thể kết luận trên desktop. Môi trường kiểm thử là
Windows, JDK 21, JavaFX và dữ liệu mẫu H2 theo profile `test`.

| Mã | Issue | Input | Expected | Actual | Kết quả |
| --- | --- | --- | --- | --- | --- |
| TC-D01 | #44 | DesktopLauncher, profile desktop | Cửa sổ mở, Tomcat không chạy, đóng cửa sổ thì thoát tiến trình. | Spring context desktop khởi tạo được trong `DesktopBootstrapTest`. Chưa kiểm tra toàn vòng đời launcher. | Chờ |
| TC-D02 | #45 | Đăng nhập và chuyển từ khung chính sang Báo cáo | Nội dung đổi trong cùng cửa sổ, status bar hiện tên và đồng hồ. | Chạy thử từ khung chính sang Báo cáo, một cửa sổ, status bar hiện tên Admin và đồng hồ. | Pass |
| TC-D03 | #46, #57 | Gửi khoảng ngày lỗi và chạy một báo cáo hợp lệ | Ô lỗi hiện viền đỏ và tooltip. Có tiến trình trong lúc đọc dữ liệu. | `ReportControllerTest` kiểm tra pseudo-class, tooltip, tiến trình và nút bấm trong khi gọi service. | Pass |
| TC-D04a | #47 | Admin với mật khẩu sai | Từ chối đăng nhập. | `AuthFacadeTest` nhận BusinessException. | Pass |
| TC-D04b | #47 | Tài khoản LOCKED với mật khẩu đúng | Từ chối đăng nhập, phiên trống. | `DesktopFacadeTest` nhận lỗi tài khoản bị khóa và phiên trống. | Pass |
| TC-D04c | #47 | Tài khoản CUSTOMER hợp lệ | Từ chối truy cập quầy theo BR-D1. | `DesktopFacadeTest` nhận lỗi yêu cầu ADMIN và phiên trống. | Pass |
| TC-D05 | #47 | Đăng nhập Admin, rồi đăng xuất | Status bar hiện tên, đăng xuất xóa phiên và quay lại login. | Test phiên và chạy thử ghi nhận tên Admin và màn hình đăng nhập sau khi thoát. | Pass |
| TC-D06 | #48 | Cùng DB web/desktop, tạo thêm suất hôm nay | KPI và biểu đồ khớp web, cập nhật khi làm mới, timer dừng khi rời trang. | Test web kiểm tra KPI. Chờ dashboard desktop. | Chờ |
| TC-D07 | #49 | Thêm/sửa thể loại, tên trùng, thể loại đang có phim | Lưu hợp lệ. Chặn tên trùng và xóa thể loại còn phim. | Chờ màn hình thể loại. | Chờ |
| TC-D08 | #50 | Từ khóa, thể loại, trạng thái, hai trang phim | Kết hợp lọc đúng, lọc lại về trang đầu và dòng STOPPED mờ. | Chờ màn hình phim. | Chờ |
| TC-D09 | #51 | Thời lượng ngoài 30–300 phút | Dialog không đóng, trường lỗi đỏ và có tooltip. | Chờ dialog phim. | Chờ |
| TC-D10 | #51 | Một phim có suất và một phim chưa có suất | Phim có suất chuyển STOPPED. Phim chưa có suất được xóa hẳn. | Chờ dialog phim. | Chờ |
| TC-D11 | #52, #53 | Phòng 8×12 và phòng MAINTENANCE | Sức chứa 96. Phòng MAINTENANCE không hiện khi lập lịch. | Chờ tích hợp phòng và suất. | Chờ |
| TC-D12 | #53 | Suất trùng phòng/giờ và suất có đơn | Chặn trùng lịch. Hủy suất có đơn làm các đơn liên quan CANCELLED. | Chờ màn hình suất chiếu. | Chờ |
| TC-D13 | #54 | Phòng 8×12, A1 đã bán | Hiện đúng 96 ghế. A1 bị khóa và không thể chọn. | `SeatMapViewTest` xác nhận 96 nút và A1 không vào danh sách chọn. | Pass |
| TC-D14 | #54 | Chọn 8 ghế rồi chọn ghế thứ 9 | Hiện cảnh báo, ghế thứ 9 tự bỏ chọn và tổng vẫn là 8. | Test JavaFX và ảnh hộp thoại xác nhận cảnh báo và giới hạn. | Pass |
| TC-D15 | #55 | Chọn khách, phim, suất, ghế và Đã thanh toán | Tổng tiền đúng, đơn CONFIRMED sau khi bán. | `CounterSaleControllerTest` chọn ghế, đối chiếu tổng và xác nhận đơn CONFIRMED qua JavaFX/H2. | Pass |
| TC-D16 | #55 | Web và desktop đặt cùng ghế | Chỉ một đơn thành công. Bên thua thấy cảnh báo và ghế vừa bán. | MockMvc gọi endpoint đặt vé web đồng thời với UI quầy trên SQL Express 17.0. Chỉ một bên thành công, bên thua báo lỗi và sơ đồ cập nhật. | Pass |
| TC-D17 | #56 | Các đơn PENDING, CONFIRMED, CANCELLED | Xác nhận/hủy theo trạng thái, chặn đổi đơn CANCELLED. | Test web kiểm tra chuyển trạng thái. Chờ nghiệm thu desktop. | Chờ |
| TC-D18a | #57 | Cùng kỳ có dữ liệu trên web và desktop | Tổng đơn, vé, tiền và danh sách đơn CONFIRMED khớp. | `ReportFacadeTest` đối chiếu với service web trên H2, dữ liệu map trong transaction. | Pass |
| TC-D18b | #57 | Ngày thiếu hoặc ngày kết thúc trước ngày bắt đầu | Service từ chối. UI báo đúng trường và không truy vấn. | `ReportFacadeTest` nhận BusinessException. `ReportControllerTest` xác nhận đánh dấu trường và không gọi facade. | Pass |
| TC-D18c | #57 | 16 đơn, 15 dòng/trang, sắp theo mã đơn tăng dần | Có hai trang. Sắp toàn bộ kết quả trước khi cắt trang. | `ReportControllerTest` kiểm tra trang đầu và cuối trước/sau khi sắp. | Pass |
| TC-D19 | #57 | 01/01/2001, kỳ không có đơn | Hiện trạng thái rỗng và khóa nút Xuất CSV. | Test H2 trả rỗng. Chạy thử JavaFX xác nhận nút Xuất CSV bị khóa. | Pass |
| TC-D20 | #57 | CSV có tên khách và tên phim tiếng Việt | Có UTF-8 BOM. Excel hiển thị đúng dấu và các cột. | `CsvExporterTest` xác nhận BOM/escape. Excel nhập đúng tên và giá trị với UTF-8, dấu phẩy. | Pass |
| TC-D21 | §9.2 | Hai tiến trình cùng sửa một bản ghi | Một thao tác thành công. Thao tác còn lại nhận lỗi Optimistic Lock. | Entity dùng chung chưa có `@Version`. Chờ triển khai và nghiệm thu trên bản tích hợp. | Chờ |
| TC-D22 | §9.2 | Đăng nhập, thêm, sửa, xóa, đăng xuất | Luồng CRUD và đăng xuất hoạt động xuyên suốt. | Đã chạy đăng nhập/đăng xuất. Chờ màn hình CRUD của nhóm để chạy cả luồng. | Chờ |

Test tự động của hai nhánh chức năng nằm trong PR #66 và #67. Kết quả
nghiệm thu cuối kỳ cần được chạy lại sau khi tất cả các màn hình đã tích hợp.

Trên nhánh #58, `clean verify` chạy 111 test, 0 lỗi và 0 bỏ qua trên
Windows. JaCoCo ghi nhận 354/465 dòng service và facade được chạy
(**76,13%**). Mở `target/site/jacoco/index.html` sau khi chạy verify
để xem chi tiết theo lớp.

Bản ghép #54, #55, #57, #58 và dependency #46 đạt **136 test** trên
Windows, 0 lỗi và 0 bỏ qua. JaCoCo ghi nhận **78,46%** (386/492 dòng)
ở phạm vi service và desktop facade. TC-D16 còn cần chạy lại với
SQL Server 2019/2022 của môi trường nộp.
