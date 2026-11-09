# Tích hợp sơ đồ ghế JavaFX

Khai báo `SeatMapView` trong FXML của màn hình bán vé. Đặt component trong
`ScrollPane` để cuộn khi sơ đồ lớn hơn vùng hiển thị. Component nạp bố cục từ `seat-map.fxml` và stylesheet riêng
`css/desktop-seat-map.css`.

```xml
<?import com.fu.cinema.desktop.component.SeatMapView?>
<?import javafx.scene.control.ScrollPane?>

<ScrollPane fitToWidth="true" pannable="true">
    <SeatMapView fx:id="seatMapView"/>
</ScrollPane>
```

Khi đổi suất, gọi `SeatMapFacade.load(showtimeId)` qua `FxAsync` rồi chuyển
`SeatMapData` vào `seatMapView.setData(data)` trên JavaFX Application Thread.
Mỗi lần nạp sẽ đặt lại danh sách ghế chọn. Khi người dùng đổi suất liên tiếp,
chỉ hiển thị kết quả của suất còn được chọn.

`getSelectedSeats()` trả về danh sách observable chỉ đọc. Có thể bind số ghế
bằng `Bindings.size(seatMapView.getSelectedSeats())`. Chụp một bản
`List.copyOf(seatMapView.getSelectedSeats())` trước khi gửi thao tác bán vé
sang luồng nền.

`BookingService.create` kiểm tra lại ghế và giới hạn 8 ghế trong transaction.
Nếu ghế vừa được đặt qua web, hiển thị lỗi nghiệp vụ và tải lại sơ đồ cho
suất đang chọn.

Chạy riêng test JavaFX bằng `mvn -Dtest=SeatMapViewTest test`. Trên Linux,
dùng `xvfb-run -a mvn -Dtest=SeatMapViewTest test` nếu máy thiếu màn hình X.
