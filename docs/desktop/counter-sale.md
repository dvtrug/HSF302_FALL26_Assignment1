# Bán vé tại quầy (TODO 43, #55)

Mở **Bán vé tại quầy** sau khi đăng nhập Admin. Tìm khách bằng tên, số
điện thoại hoặc email, chọn phim, suất chiếu và ghế. Tổng tiền cập nhật
theo số ghế đang chọn. **Đã thanh toán** tạo đơn CONFIRMED. Bỏ chọn để tạo
đơn PENDING.

- `CounterSaleFacade` của B dùng các service hiện có và trả record cho UI.
  `sell` chạy trong transaction và kiểm tra phiên Admin. Nếu xác nhận thất bại,
  đơn và vé đều rollback.
- `CounterSaleController` tải dữ liệu qua `FxAsync`. Tìm khách đợi 300 ms,
  kết quả cũ bị bỏ qua khi đổi lựa chọn. Khi rời màn hình, các tác vụ đọc
  được hủy, còn thao tác ghi đã gửi hoàn tất theo transaction.
- Sơ đồ dùng `SeatMapFacade` và `SeatMapView` của #54. Nếu đơn thành công
  hoặc ghế vừa bị mua, UI tải lại dữ liệu ghế của suất đang chọn.

## Kiểm thử

Chạy `mvn -Dtest=CounterSaleFacadeTest,CounterSaleControllerTest,SeatMapViewTest test`
trên Windows có giao diện hoặc Linux qua `xvfb-run`.

- `CounterSaleFacadeTest` kiểm tra record, thanh toán, độ tuổi, phân quyền,
  rollback và hai transaction trên H2 tranh cùng một ghế.
- `CounterSaleControllerTest` chạy FXML với H2, xác nhận tổng tiền và đơn
  CONFIRMED. Sau khi luồng service của web mua ghế tiếp theo, quầy báo lỗi và
  cập nhật ghế đã bán.
- Kiểm thử bổ sung với SQL Express 17.0 dùng CSDL QA riêng. MockMvc gọi
  endpoint đặt vé web đồng thời với thao tác JavaFX. Một bên thành công,
  bên còn lại nhận lỗi ghế đã bán. Sơ đồ cập nhật. Lượt kiểm thử này
  dùng bản SQL Express cục bộ, cần chạy lại trên SQL Server 2019/2022
  khi nghiệm thu môi trường nộp.
- [Ảnh giao diện quầy vé](../screenshots/desktop/counter-sale.png).

PR #65 phụ thuộc tiện ích #63 và sơ đồ ghế #66. Sau khi các dependency vào
`develop`, cập nhật base của PR #65 và review kết quả trên bản tích hợp.
