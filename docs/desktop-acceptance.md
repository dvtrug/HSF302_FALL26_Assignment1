# Ma trận nghiệm thu FU Cinema Desktop

Ma trận này được lập từ tiêu chí nghiệm thu của các issue #44–#58.
TC-D09 và TC-D16 giữ nội dung trong kế hoạch nhóm. Dùng CSDL H2 riêng
cho test tự động và cơ sở dữ liệu demo khi nghiệm thu web cùng desktop.
Kết quả bên dưới phân biệt test tự động và thao tác thủ công.

| Mã | Issue | Thao tác và kết quả mong đợi | Kết quả hiện tại |
| --- | --- | --- | --- |
| TC-D01 | #44 | Mở DesktopLauncher, kiểm tra cửa sổ lên và log không chạy Tomcat. Đóng cửa sổ, tiến trình kết thúc. | Context desktop chạy trong test. Chờ nghiệm thu khởi động và đóng cửa sổ. |
| TC-D02 | #45 | Chuyển giữa các mục đã triển khai, nội dung đổi trong cùng cửa sổ. Đồng hồ và tên người dùng hiển thị trên thanh trạng thái. | Đã kiểm tra chuyển từ khung chính sang Báo cáo trong cùng cửa sổ. Chờ nghiệm thu toàn bộ menu. |
| TC-D03 | #46 | Mở tác vụ nền, kiểm tra UI phản hồi. Gửi form lỗi, kiểm tra viền đỏ và tooltip. | Chờ nghiệm thu trên form desktop. |
| TC-D04 | #47 | Dùng mật khẩu sai, tài khoản LOCKED và tài khoản CUSTOMER. Hệ thống từ chối đăng nhập. | Có test tự động cho ba trường hợp. |
| TC-D05 | #47 | Đăng nhập Admin, kiểm tra tên ở status bar. Đăng xuất, phiên bị xóa và hiện màn hình đăng nhập. | Đạt bằng test phiên và chạy thử đăng nhập, hiển thị tên và đăng xuất. |
| TC-D06 | #48 | Đối chiếu 3 KPI và suất hôm nay với web. Thêm suất, làm mới, sau đó rời Dashboard để kiểm tra timer dừng. | KPI web có test trong `ReportIntegrationTest`. Chờ #48 cho Dashboard desktop. |
| TC-D07 | #49 | Thêm và sửa thể loại. Thử trùng tên và xóa thể loại còn phim, thao tác bị chặn. | Chờ #49. |
| TC-D08 | #50 | Kết hợp từ khóa, thể loại và trạng thái, đổi bộ lọc trở lại trang đầu và dòng STOPPED hiển thị mờ. | Chờ #50. |
| TC-D09 | #51 | Nhập thời lượng ngoài 30–300 phút, lưu. Dialog vẫn mở, trường lỗi có viền đỏ và tooltip. | Chờ #51. |
| TC-D10 | #51 | Xóa phim có suất để chuyển STOPPED và xóa hẳn phim chưa có suất. | Chờ #51. |
| TC-D11 | #52 | Thêm và sửa phòng, kiểm tra sức chứa. Phòng MAINTENANCE vắng mặt trong danh sách lập lịch. | Chờ #52 và #53. |
| TC-D12 | #53 | Thử tạo suất trùng phòng và giờ, hệ thống báo xung đột. Hủy suất có đơn, đơn liên quan chuyển CANCELLED. | Chờ #53. |
| TC-D13 | #54 | Nạp phòng 8×12 có một ghế bán. Hiện đủ 96 ghế, ghế bán bị vô hiệu hóa. | Đạt bằng test JavaFX. |
| TC-D14 | #54 | Chọn 8 ghế rồi thử ghế thứ 9, kiểm tra cảnh báo và số ghế vẫn là 8. Bỏ chọn và chọn ghế khác. | Đạt bằng test JavaFX và chạy thử hộp thoại cảnh báo. |
| TC-D15 | #55 | Chọn khách, phim, suất và ghế. Kiểm tra tổng tiền và đơn CONFIRMED khi chọn Đã thanh toán. | Chờ tích hợp UI #55. |
| TC-D16 | #55 | Đặt cùng ghế từ web và desktop. Chỉ một đơn thành công, bên còn lại báo lỗi và cập nhật sơ đồ. | Chờ tích hợp UI #55. |
| TC-D17 | #56 | Lọc và mở chi tiết đơn web. Xác nhận PENDING, hủy PENDING/CONFIRMED và kiểm tra nút bị khóa khi CANCELLED. | Web có test chuyển trạng thái trong `BookingAdminIntegrationTest`. Chờ nghiệm thu tích hợp #56. |
| TC-D18 | #57 | Đối chiếu số liệu doanh thu với web và kiểm tra khoảng ngày đảo ngược bị từ chối. | Đạt bằng test facade trên H2. |
| TC-D19 | #57 | Chọn kỳ không có đơn, kiểm tra thông báo và nút Xuất CSV bị khóa. | Đạt bằng test facade và chạy thử giao diện JavaFX trên H2. |
| TC-D20 | #57 | Xuất CSV có tên phim và tên khách tiếng Việt, kiểm tra UTF-8 BOM rồi mở Excel để kiểm tra hiển thị. | Đạt kiểm tra byte và nhập vào Excel theo UTF-8, dấu phẩy. Excel giữ đúng tên tiếng Việt và giá trị tiền. Mở trực tiếp có thể dồn dữ liệu vào một cột tùy vùng. |

Bản tích hợp ba nhánh của E chạy clean verify: **100 test đạt**, 0 lỗi,
0 bỏ qua trên Windows. Dùng Mockito agent nạp sẵn để tránh cơ chế tự attach
trong sandbox. Đây là kết quả của ba nhánh E trên nền develop, trước khi
tích hợp các tính năng desktop còn lại.
