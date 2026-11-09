# Bán vé tại quầy (TODO 43, #55)

Mở **Bán vé tại quầy** sau khi đăng nhập Admin. Tìm khách bằng tên, số
điện thoại hoặc email (debounce 300 ms), chọn phim, suất chiếu và ghế.
Đổi phim/suất xóa ngay ghế cũ và tổng tiền; kết quả tải cũ bị bỏ qua.
Tổng tiền dùng BigDecimal và binding theo giá suất, số ghế đang chọn.
Nút bán chỉ bật khi có khách, suất, ghế và không đang bán.

**Bán vé** mở hộp xác nhận có khách, ghế, tổng tiền và trạng thái dự kiến.
Chọn Không/đóng hộp thoại không tạo đơn. Chọn Có mới gửi giao dịch.
**Đã thanh toán** tạo đơn CONFIRMED; bỏ chọn tạo đơn PENDING (BR-D2).
Thông báo thành công có mã đơn, ghế, tổng tiền và trạng thái. Thành công
hoặc thất bại đều tải lại sơ đồ ghế để phản ánh giao dịch của người khác.

## Kiến trúc và quyền

- CounterSaleFacade dùng service hiện có, map record trong transaction,
  kiểm tra Admin trước khi tìm thông tin khách và trước khi bán vé.
- sell tạo đơn/vé và xác nhận đã thanh toán trong cùng transaction.
  Lỗi confirm rollback cả đơn lẫn vé. Backend kiểm tra lại giới hạn ghế,
  độ tuổi và ghế đã bán; không tin trạng thái ghế trên UI.
- Controller chỉ gọi facade; FXML/CSS riêng. Đọc dữ liệu qua FxAsync,
  hủy tác vụ và bỏ callback cũ khi rời view. Giao dịch ghi đã gửi hoàn tất
  theo transaction; không hứa hủy JDBC sẽ hoàn tác giao dịch đã commit.
- Dùng SeatMapView, SeatMapFacade, SeatMapData của E (#66), tích hợp
  cùng giao diện từ #69, giữ nguyên tác giả các commit của E.

## Kiểm thử trên Windows/JDK 21

```powershell
mvn test '-Dtest=CounterSaleFacadeTest,CounterSaleControllerTest,SeatMapViewTest'
mvn clean verify '-Dspring.profiles.active=test'
```

- CounterSaleFacadeTest: record, paid/unpaid, BR-10/11, quyền Admin,
  rollback và hai transaction H2 tranh cùng một ghế.
- CounterSaleControllerTest: FXML và JavaFX thật với H2; đổi phim xóa
  lựa chọn, 3 ghế A1/A3/A4 = 240.000đ (TC-D13), từ chối xác nhận không ghi
  đơn, paid/unpaid, báo lỗi và reload khi web đã mua ghế A2, khách 15 tuổi
  bị chặn ở phim T18 (TC-D15). Thông báo có mã đơn/ghế/tiền/trạng thái.
- SeatMapViewTest của E kiểm tra ghế đã bán và giới hạn 8 ghế (TC-D14).
- GUI tests cần desktop Windows hoặc Linux có DISPLAY/Xvfb.

## TC-D16 trên SQL Server: web endpoint và JavaFX

CounterSaleSqlServerTest là bộ kiểm thử opt-in. Chỉ bật với CSDL local
riêng, có tên FU_Cinema_QA_B55_<chữ số>. Test dùng **create-drop**, vì vậy
database này phải dành riêng cho kiểm thử, không dùng dữ liệu ứng dụng.

1. Tạo CSDL QA rỗng trên SQL Server local cổng 1433.
2. Đặt FU_COUNTER_QA_URL theo mẫu
   `jdbc:sqlserver://localhost:1433;databaseName=FU_Cinema_QA_B55_20261109;encrypt=true;trustServerCertificate=true`.
   Đây là kết nối chứng chỉ tự ký của môi trường QA local.
3. Đặt FU_COUNTER_QA_USER và FU_COUNTER_QA_PASSWORD trong môi trường;
   không lưu mật khẩu vào Git hoặc truyền mật khẩu qua CLI.
4. Chạy `mvn test '-Dtest=CounterSaleSqlServerTest'`.

Test tải FXML thật, chọn C8 và xác nhận bán trong JavaFX, đồng thời
MockMvc gửi POST /customer/bookings có phiên CUSTOMER qua interceptor
và controller web thật. Hai yêu cầu được đồng bộ bằng latch rồi cùng
truy cập SQL Server. Kiểm tra chỉ một bên thành công, ghế bị khóa sau
reload, đúng 1 vé hoạt động và không có nhóm ghế trùng trong DB.
Test còn chạy lại ca UI paid/unpaid/tuổi trên SQL Server.

Ngày 09/11/2026: SQL Express 17.0.1000.7, 2 test đạt, 0 lỗi/bỏ qua.
Lượt tranh chấp: desktop thành công, web nhận lỗi ghế đã có người đặt;
activeTickets=1, duplicateSeatGroups=0. Đây là kiểm thử endpoint bằng
MockMvc và giao diện JavaFX, chưa phải hai người thao tác bằng trình duyệt.
Chạy lại trên SQL Server 2019/2022 nếu môi trường nghiệm thu yêu cầu.
Không đặt biến QA thì lớp SQL Server được bỏ qua, bộ H2 vẫn chạy bình thường.

## Bằng chứng và dependency PR

Kết quả kiểm tra cuối ngày 09/11/2026: bộ facade/UI 7 test đạt và bộ
SQL Server 2 test đạt. Lần clean verify toàn bộ chạy 110 test có 1 lỗi
chờ trạng thái nút bán trong CounterSaleControllerTest và 2 test SQL Server
bỏ qua do không bật QA. Chạy chẩn đoán nhóm AuthFacade/SeatMap/UI sau đó
đạt 7 test; chưa xác định dứt điểm lỗi không ổn định trong toàn bộ suite.
Dừng kiểm thử thêm theo yêu cầu người phụ trách; chuyển Ready for review
không đồng nghĩa toàn bộ regression đã đạt. Reviewer cần kiểm tra lại
điểm này trước merge.

Ảnh chụp từ scene/dialog thật và cửa sổ log Maven được lưu trong
local-evidence/assignment02/B/todo43/ (ignored). Upload ảnh riêng vào
Conversation PR #65; folder và ảnh không có trong cây file của PR.
Ảnh đã tồn tại trên nhánh E được loại khỏi cây file khi tích hợp; lịch sử
commit E vẫn giữ nguyên. Dữ liệu xuất hiện trong ảnh là dữ liệu mẫu test.

PR #65 dựa trên utilities #63 và đã tích hợp E #66/#69. Sau khi #63 và
#66 vào develop, đổi base #65 sang develop và kiểm tra diff trước merge.
Không tự merge PR #63/#66 vào develop trong quá trình hoàn thiện TODO 43.
