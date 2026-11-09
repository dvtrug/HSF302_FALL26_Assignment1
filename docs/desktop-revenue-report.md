# Báo cáo doanh thu trên Desktop

Đăng nhập Admin, chọn **Báo cáo**, chọn hai ngày và bấm **Xem báo cáo**.
Báo cáo dùng ngày đặt của các đơn `CONFIRMED`, tính đủ cả ngày bắt đầu và
ngày kết thúc. Mặc định là 7 ngày tính cả hôm nay. Mốc ngày và số liệu dùng
chung với báo cáo web tại `/admin/reports`.

- Biểu đồ cột tổng hợp tiền vé theo phim. Rê chuột lên cột để xem tên phim,
  số vé và doanh thu. Số thứ tự tách các phim trùng tên trên trục ngang.
- Biểu đồ tròn thể hiện số vé từng phim chia cho tổng số vé đã xác nhận.
- Bảng đơn mặc định xếp theo ngày đặt mới nhất. Bấm tên cột để sắp toàn bộ kết quả, dùng nút trang trước/sau để xem 15 đơn mỗi trang. Dòng tổng hiển thị số đơn,
  số vé và doanh thu. Kỳ rỗng hiện thông báo và khóa nút xuất CSV.

**Xuất CSV** lưu các đơn đã xác nhận của kỳ đang hiển thị. File dùng UTF-8
có BOM, cột phân cách bằng dấu phẩy, dòng kết thúc bằng CRLF và tổng tiền
là số VND. Mỗi đơn có mã, ngày đặt, tên khách, tên phim, số vé và tổng tiền.
Nếu Excel để tất cả dữ liệu vào một cột do thiết lập vùng, nhập qua
**Data → From Text/CSV** và chọn dấu phẩy làm dấu phân cách.

Đối chiếu với web bằng cùng khoảng ngày. Kiểm tra thêm một kỳ không có
đơn và một khoảng ngày đảo ngược. Chạy `mvn -Dtest=ReportFacadeTest,CsvExporterTest test`
để kiểm tra dữ liệu H2, ngày và định dạng CSV.
