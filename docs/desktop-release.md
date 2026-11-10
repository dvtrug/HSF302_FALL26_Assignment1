# Chuẩn bị phát hành FU Cinema v2.0.0

1. Tích hợp các PR của #44–#58 vào `develop` sau khi CI và review đạt.
2. Trên bản tích hợp, chạy `mvn clean verify`. Trên Linux, dùng
   `xvfb-run -a mvn clean verify` để chạy cả kiểm thử JavaFX.
3. Chạy thử web và DesktopLauncher với cùng SQL Server. Hoàn tất
   [ma trận TC-D01–TC-D20](desktop-acceptance.md) và cập nhật ảnh trong README.
4. Tách nhánh phát hành từ `develop`, đổi phiên bản Maven thành `2.0.0`,
   chạy lại verify rồi mở PR phát hành vào `main`.
5. Sau khi PR phát hành merge, tạo tag `v2.0.0` tại commit phát hành đã
   xác minh trên `main`. Đính kèm JAR do Maven tạo từ đúng commit đó nếu
   phân phối bản web. README ghi lệnh chạy desktop bằng Maven.
6. Ghi chú phát hành trên GitHub gồm tính năng desktop đã nghiệm thu,
   môi trường chạy, cấu hình DB, lệnh chạy và liên kết ma trận kiểm thử.
   Xác nhận tag, ghi chú và tệp đính kèm trỏ tới cùng phiên bản.
