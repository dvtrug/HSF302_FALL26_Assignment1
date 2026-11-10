Assignment 1 HSF302 – Cinema Ticket Booking System 
Link github bài tập nhóm: https://github.com/dvtrug/HSF302_FALL26_Assignment1
Người thực hiện: [Họ và tên của bạn] – [Mã SV của bạn] – Thành viên C (Git: nhattan0114)

Họ tên thành viên nhóm
| STT | Mã SV | Họ tên | Vai trò | Phân công theo module, công việc |
|---|---|---|---|---|
| 1 | DE200118 | Đỗ Võ Thanh Trung | Leader / Thành viên A | Quản trị dự án & CI/CD; Nền tảng hệ thống Sprint 0 (TODO 1-12); Phân quyền & Xác thực Sprint 1 (TODO 13-15) |
| 2 | DE200049 | Hồ Quốc Thắng | Thành viên B | Quản lý khách hàng & Hồ sơ cá nhân (Customer Management & Profile) – TODO 16, 17, 30; issue #7, #16 |
| 3 | [Mã SV C] | [Họ tên C] | Thành viên C | Quản lý Thể loại, Phim & Đánh giá (Genre, Movie & Review); Trang chủ & Chi tiết phim – TODO 18–21, 27, 31; issue #8, #9, #13, #18 |
| 4 | [Mã SV D] | [Họ tên D] | Thành viên D | Quản lý Phòng chiếu & Suất chiếu (Room & Showtime) – TODO 22, 23, 28; issue #10, #11, #14 |
| 5 | [Mã SV E] | [Họ tên E] | Thành viên E | Quản lý Đặt vé, Vé & Báo cáo doanh thu (Booking, Ticket & Revenue Report) – TODO 24–26, 29; issue #12, #15, #17 |

---

### Mục lục
I. Quản lý Thể loại phim (Sprint 1: Genre Management – Issue #8)
  1.1. TODO 18: Danh sách, thêm, sửa và xoá thể loại theo quy tắc BR-05
II. Quản lý Phim (Sprint 1: Movie Management – Issue #9)
  2.1. TODO 19: Danh sách phim: tìm kiếm, lọc thể loại/trạng thái, phân trang và sắp xếp
  2.2. TODO 20: Thêm và chỉnh sửa thông tin phim, xác thực dữ liệu (Validation)
  2.3. TODO 21: Xoá phim và cơ chế xoá mềm (Soft Delete) theo quy tắc BR-06
III. Trang chủ và Chi tiết phim cho Khách hàng (Sprint 2: Home & Movie Detail – Issue #13)
  3.1. TODO 27: Trang chủ hiển thị danh sách phim đang chiếu / sắp chiếu và tìm kiếm
  3.2. TODO 27: Trang chi tiết phim, thông tin độ tuổi, đạo diễn và lịch chiếu theo ngày
IV. Đánh giá phim (Bonus) (Sprint 2: Movie Review – Issue #18)
  4.1. TODO 31: Đánh giá phim (1–5 sao), nhận xét của khán giả đã xem phim theo BR-16

---

# I. Quản lý Thể loại phim (Sprint 1: Genre Management – Issue #8)

## TODO 18: Danh sách, thêm, sửa và xoá thể loại theo quy tắc BR-05

Chụp hình kết quả: Trang `/admin/genres` hiển thị danh sách toàn bộ các thể loại được sắp xếp theo tên tăng dần, cùng nút "+ Thêm thể loại", "Sửa" và "Xoá".
<!-- [NOTE HÌNH ẢNH 1: Chụp màn hình trang quản lý thể loại tại URL http://localhost:8080/admin/genres khi đăng nhập tài khoản Admin (admin@fucinema.vn). Thấy danh sách các thể loại: Hành động, Hoạt hình, Kinh dị, Tình cảm, Khoa học viễn tưởng...] -->

Chụp hình kết quả: Form thêm mới thể loại tại `/admin/genres/new` và form chỉnh sửa thể loại tại `/admin/genres/{id}/edit`.
<!-- [NOTE HÌNH ẢNH 2: Chụp màn hình form thêm thể loại tại /admin/genres/new với ô Tên thể loại, Mô tả, nút Lưu và nút Huỷ] -->

Chụp hình kết quả: Báo lỗi validation khi để trống trường bắt buộc hoặc nhập tên thể loại đã tồn tại (không phân biệt hoa thường).
<!-- [NOTE HÌNH ẢNH 3: Chụp màn hình form khi bấm Lưu với tên thể loại bị để trống (hiển thị thông báo "Tên thể loại không được để trống") hoặc nhập "hành động" trùng với thể loại đã có (hiển thị alert đỏ "Tên thể loại đã tồn tại")] -->

Chụp hình kết quả: Thực thi BR-05 khi xoá thể loại vẫn còn phim liên kết — bị từ chối và hiển thị thông báo lỗi nghiệp vụ.
<!-- [NOTE HÌNH ẢNH 4: Chụp màn hình trang /admin/genres sau khi bấm Xoá thể loại "Hành động" (đang có phim Biệt Đội Siêu Tốc gắn vào). Hệ thống chặn xoá và hiển thị flash message màu đỏ: 'Không thể xoá: vẫn còn phim thuộc thể loại "Hành động"'] -->

Chụp hình kết quả: Xoá thành công thể loại chưa có phim nào liên kết (ví dụ thể loại "Khoa học viễn tưởng").
<!-- [NOTE HÌNH ẢNH 5: Chụp màn hình trang /admin/genres sau khi xoá thành công thể loại "Khoa học viễn tưởng". Hiển thị alert xanh: 'Đã xoá thể loại'] -->

Commit: b1c6b12 feat(genre): add genre service with duplicate and delete checks (Refs #8)
Commit: dd4e99c feat(genre): add admin genre pages (Refs #8)
Commit: cdd9ecd fix(config): support utf-8 encoding and nvarchar for sql server (Refs #8)

Checklist đã hoàn thành:
[x] Tạo `GenreForm` chứa các trường `genreName` (@NotBlank, @Size tối đa 50 ký tự) và `description` (@NotBlank, @Size tối đa 500 ký tự).
[x] Xây dựng `GenreRepository` với derived query `existsByGenreNameIgnoreCase`, `existsByGenreNameIgnoreCaseAndGenreIdNot` và `findAllByOrderByGenreNameAsc()`.
[x] Triển khai `GenreServiceImpl` xử lý logic: kiểm tra trùng tên không phân biệt hoa thường (khi sửa có loại trừ chính id hiện tại).
[x] Thực thi nghiêm ngặt quy tắc **BR-05**: Kiểm tra ràng buộc với bảng phim bằng `movieRepository.existsByGenre_GenreId(genreId)`. Nếu còn phim, ném `BusinessException` chặn xoá và giữ nguyên dữ liệu trong cơ sở dữ liệu.
[x] Xây dựng `AdminGenreController` đầy đủ các endpoint GET `/admin/genres`, GET/POST `/admin/genres/new`, GET/POST `/admin/genres/{id}/edit`, POST `/admin/genres/{id}/delete`.
[x] Đảm bảo controller tuân thủ TECH-02: chỉ gọi qua `GenreService`, không inject trực tiếp Repository; thao tác ghi sử dụng phương thức POST.
[x] Thiết kế giao diện Thymeleaf gồm `admin/genres/list.html` và `admin/genres/form.html` dùng chung layout chuẩn, xử lý binding lỗi validation và thông báo flash attributes (`success`, `error`).

---

# II. Quản lý Phim (Sprint 1: Movie Management – Issue #9)

## 2.1. TODO 19: Danh sách phim: tìm kiếm, lọc thể loại/trạng thái, phân trang và sắp xếp

Chụp hình kết quả: Trang `/admin/movies` hiển thị danh sách phim với thanh tìm kiếm từ khoá, dropdown chọn thể loại, dropdown chọn trạng thái và dropdown chọn tiêu chí sắp xếp.
<!-- [NOTE HÌNH ẢNH 6: Chụp màn hình giao diện danh sách phim Admin tại http://localhost:8080/admin/movies. Bảng hiển thị đầy đủ các cột: Tên phim, Thể loại, Thời lượng, Khởi chiếu, Độ tuổi, Trạng thái và các nút Sửa, Xoá] -->

Chụp hình kết quả: Lọc phim theo thể loại (ví dụ: "Hoạt hình") và lọc theo trạng thái (ví dụ: "STOPPED").
<!-- [NOTE HÌNH ẢNH 7: Chụp màn hình trang /admin/movies khi lọc thể loại "Hoạt hình" -> chỉ hiện các phim hoạt hình (Chú Mèo Phiêu Lưu, Huyền Thoại Cũ). Tiếp theo chụp khi lọc trạng thái STOPPED -> chỉ hiện Huyền Thoại Cũ] -->

Chụp hình kết quả: Tìm kiếm phim theo từ khoá kết hợp sắp xếp theo Tên (A→Z) và phân trang dữ liệu.
<!-- [NOTE HÌNH ẢNH 8: Chụp màn hình tìm kiếm từ khoá "bi" và chọn sắp xếp Tên A->Z, kết quả hiển thị phim tương ứng, URL giữ nguyên các tham số tìm kiếm và bộ lọc khi chuyển trang] -->

Commit: 0017be1 feat(movie): add movie search with genre and status filters (Refs #9)
Commit: 00e97ff feat(movie): add admin movie list with pagination and sorting (Refs #9)

Checklist đã hoàn thành:
[x] Xây dựng custom query JPQL `Q2` trong `MovieRepository` hỗ trợ tìm kiếm theo tên phim (`:kw IS NULL OR LOWER(m.title) LIKE ...`), lọc theo `genreId` tuỳ chọn và danh sách trạng thái `status IN :statuses`.
[x] Tối ưu hóa truy vấn bằng `JOIN FETCH m.genre` chống lỗi N+1 Hibernate và khai báo `countQuery` riêng biệt phục vụ phân trang Spring Data JPA.
[x] Triển khai phương thức `searchForAdmin()` trong `MovieServiceImpl`: chuẩn hoá từ khoá rỗng về `null`, bảo vệ sắp xếp bằng whitelist các trường hợp lệ (`title`, `releaseDate`, `duration`) chống lỗi SQL injection / PropertyReferenceException.
[x] Xây dựng `AdminMovieController.list()` xử lý tham số GET (`kw`, `genreId`, `status`, `sort`, `page`), truyền dữ liệu danh sách phim, danh mục thể loại và trạng thái sang model.
[x] Tích hợp component phân trang `PageUtils.currentUrlWithoutPage()` giúp duy trì các tham số tìm kiếm và bộ lọc khi người dùng chuyển trang.
[x] Thiết kế giao diện danh sách phim `admin/movies/list.html` với badge màu sắc trực quan cho trạng thái và nhãn độ tuổi.

---

## 2.2. TODO 20: Thêm và chỉnh sửa thông tin phim, xác thực dữ liệu (Validation)

Chụp hình kết quả: Form thêm phim mới tại `/admin/movies/new` với các trường thông tin đầy đủ và các dropdown nạp sẵn dữ liệu.
<!-- [NOTE HÌNH ẢNH 9: Chụp màn hình form thêm phim /admin/movies/new hiển thị: Tên phim, Thể loại (dropdown), Đạo diễn, Thời lượng, Ngày khởi chiếu, Độ tuổi (P, T13, T16, T18), Trạng thái, Poster URL, Mô tả] -->

Chụp hình kết quả: Form chỉnh sửa phim tại `/admin/movies/{id}/edit` hiển thị đầy đủ thông tin phim cần cập nhật.
<!-- [NOTE HÌNH ẢNH 10: Chụp màn hình form sửa phim /admin/movies/1/edit của phim "Biệt Đội Siêu Tốc" với dữ liệu cũ được điền sẵn trong các trường] -->

Chụp hình kết quả: Kiểm tra validation thất bại khi gửi form có trường trống hoặc dữ liệu không hợp lệ.
<!-- [NOTE HÌNH ẢNH 11: Chụp màn hình form thêm phim khi nhập Thời lượng = 10 (vi phạm @Min(30)), để trống thể loại, để trống tên phim. Form hiển thị các dòng thông báo lỗi đỏ ngay cạnh trường nhập: "Thời lượng tối thiểu 30 phút", "Vui lòng chọn thể loại", "Tên phim không được để trống", và giữ lại các giá trị hợp lệ đã nhập] -->

Commit: d832ab1 feat(movie): add movie form with validation (Refs #9)

Checklist đã hoàn thành:
[x] Xây dựng DTO `MovieForm` với đầy đủ các annotation Bean Validation: `@NotBlank` cho title, director, description, posterUrl; `@Min(30) @Max(300)` cho duration; `@NotNull` cho releaseDate, ageRating, genreId, status.
[x] Cài đặt phương thức `create()` và `update()` trong `MovieServiceImpl` có gắn annotation `@Transactional`, đồng bộ liên kết với entity `Genre` thông qua `genreRepository.findById()`.
[x] Xây dựng các endpoint GET/POST `/admin/movies/new` và GET/POST `/admin/movies/{id}/edit` trong `AdminMovieController`.
[x] Triển khai hàm phụ trợ `showForm()` nạp lại danh sách thể loại, enum `AgeRating`, enum `MovieStatus` khi form gặp lỗi binding (`br.hasErrors()`) giúp người dùng không bị mất dữ liệu đã điền.
[x] Xây dựng giao diện `admin/movies/form.html` dùng chung cho cả 2 thao tác Thêm và Sửa, định dạng ngày tháng tương thích chuẩn HTML5 `input type="date"`.

---

## 2.3. TODO 21: Xoá phim và cơ chế xoá mềm (Soft Delete) theo quy tắc BR-06

Chụp hình kết quả: Thông báo xoá thành công phim chưa có suất chiếu nào (Hard delete).
<!-- [NOTE HÌNH ẢNH 12: Chụp màn hình danh sách /admin/movies sau khi xoá phim "Mùa Hè Năm Ấy" (phim chưa có suất chiếu). Hiển thị flash message thông báo: "Đã xoá phim"] -->

Chụp hình kết quả: Thực thi quy tắc BR-06 xoá mềm khi xoá phim đã có suất chiếu — chuyển trạng thái sang `STOPPED`.
<!-- [NOTE HÌNH 8: [NOTE HÌNH ẢNH 13: Chụp màn hình sau khi bấm Xoá phim "Biệt Đội Siêu Tốc" (đã có suất chiếu trong hệ thống). Hiển thị flash message màu xanh: "Phim đã có suất chiếu nên được chuyển sang trạng thái STOPPED". Phim vẫn lưu trong DB nhưng trạng thái hiển thị đổi thành STOPPED] -->

Commit: e7d24d2 feat(movie): soft delete movie that has showtimes (Refs #9)

Checklist đã hoàn thành:
[x] Triển khai logic xoá phim trong `MovieServiceImpl.delete()` tuân thủ nghiêm ngặt **BR-06**:
    - Kiểm tra phim đã có suất chiếu hay chưa thông qua `showtimeRepository.existsByMovie_MovieId(movieId)`.
    - Nếu đã có suất chiếu: thực hiện Soft Delete bằng cách gán `movie.setStatus(MovieStatus.STOPPED)` và trả về `false`.
    - Nếu chưa có suất chiếu: thực hiện Hard Delete bằng `movieRepository.delete(movie)` và trả về `true`.
[x] Xử lý tại `AdminMovieController.delete()` hiển thị thông báo flash message tương ứng rõ ràng với kết quả xoá cứng hay xoá mềm.
[x] Bảo đảm toàn vẹn dữ liệu cho các phân hệ khác: các phim bị soft delete (`STOPPED`) vẫn giữ nguyên lịch sử đơn đặt vé và báo cáo doanh thu, nhưng tự động bị ẩn khỏi trang chủ khách hàng và danh sách chọn phim tạo suất chiếu (`findByStatusNotOrderByTitleAsc`).
[x] Nút xoá trên giao diện được bảo vệ bằng hộp thoại JavaScript `confirm()` và gửi qua HTTP POST.

---

# III. Trang chủ và Chi tiết phim cho Khách hàng (Sprint 2: Home & Movie Detail – Issue #13)

## 3.1. TODO 27: Trang chủ hiển thị danh sách phim đang chiếu / sắp chiếu và tìm kiếm

Chụp hình kết quả: Trang chủ `/` hiển thị danh sách các phim đang chiếu và sắp chiếu dưới dạng lưới (card poster).
<!-- [NOTE HÌNH ẢNH 14: Chụp màn hình trang chủ http://localhost:8080/ hoặc /movies khi chưa đăng nhập. Hiển thị danh sách card phim có poster, tên phim, nhãn độ tuổi (P, T13, T16, T18), nhãn trạng thái (Đang chiếu / Sắp chiếu), thể loại và thời lượng] -->

Chụp hình kết quả: Trang chủ ẩn hoàn toàn các phim có trạng thái STOPPED (ví dụ phim "Huyền Thoại Cũ" không xuất hiện).
<!-- [NOTE HÌNH ẢNH 15: Chụp màn hình trang chủ cho thấy phim STOPPED ("Huyền Thoại Cũ") không hề hiển thị trên giao diện theo đúng quy tắc BR-06] -->

Chụp hình kết quả: Khách hàng tìm kiếm phim theo tên và lọc theo thể loại tại trang chủ.
<!-- [NOTE HÌNH ẢNH 16: Chụp màn hình khi nhập từ khoá "mèo" hoặc chọn lọc thể loại "Hoạt hình" tại trang chủ -> hiển thị chính xác phim "Chú Mèo Phiêu Lưu"] -->

Commit: 49203ac feat(movie): add home page with showing and coming soon movies (Refs #13)

Checklist đã hoàn thành:
[x] Cài đặt phương thức `searchForCustomer()` trong `MovieServiceImpl`: tự động giới hạn chỉ lấy các phim có trạng thái `SHOWING` hoặc `COMING_SOON`, loại bỏ hoàn toàn phim `STOPPED` (BR-06).
[x] Sắp xếp danh sách phim trang chủ ưu tiên theo ngày khởi chiếu mới nhất (`releaseDate DESC`).
[x] Xây dựng `HomeController.home()` map cả 2 đường dẫn `/` và `/movies`, hỗ trợ phân trang 12 phim/trang phù hợp với bố cục lưới 4 cột trên màn hình desktop.
[x] Thiết kế giao diện `index.html` thân thiện, hiện đại với responsive Bootstrap grid, thanh tìm kiếm từ khoá kết hợp dropdown lọc thể loại và thanh phân trang.

---

## 3.2. TODO 27: Trang chi tiết phim, thông tin độ tuổi, đạo diễn và lịch chiếu theo ngày

Chụp hình kết quả: Trang chi tiết phim `/movies/{id}` hiển thị đầy đủ thông tin phim, poster, đạo diễn, mô tả, nhãn độ tuổi và điểm đánh giá trung bình.
<!-- [NOTE HÌNH ẢNH 17: Chụp màn hình trang chi tiết phim http://localhost:8080/movies/1 (Biệt Đội Siêu Tốc) với đầy đủ thông tin giới thiệu, đạo diễn Lê Minh, 120 phút, nhãn T13, mô tả nội dung] -->

Chụp hình kết quả: Phần lịch chiếu của phim được gom nhóm trực quan theo từng ngày (chỉ hiển thị các suất chiếu OPEN sắp tới).
<!-- [NOTE HÌNH ẢNH 18: Chụp màn hình phần "Lịch chiếu" trên trang chi tiết phim: hiển thị tiêu đề các ngày (Ví dụ: Thứ Bảy, 17/10/2026), bên dưới là các nút giờ chiếu màu xanh (Ví dụ: 09:00 · Room 01, 18:30 · Room 02). Các suất chiếu trong quá khứ hoặc suất đã CANCELLED không xuất hiện] -->

Chụp hình kết quả: Khách vãng lai chưa đăng nhập bấm vào nút giờ chiếu được chuyển hướng đến trang đăng nhập `/login`.
<!-- [NOTE HÌNH ẢNH 19: Chụp màn hình khi bấm vào nút giờ chiếu "09:00 · Room 01" khi chưa đăng nhập -> AuthInterceptor chặn lại và chuyển hướng trình duyệt đến trang http://localhost:8080/login] -->

Commit: a8acb79 feat(movie): add movie detail with upcoming showtimes (Refs #13)

Checklist đã hoàn thành:
[x] Xây dựng phương thức `findUpcomingByMovie()` trong `ShowtimeService` sử dụng derived query kèm `@EntityGraph(attributePaths = "room")` để lấy các suất chiếu `OPEN` có `startTime > now()`.
[x] Triển khai gom nhóm suất chiếu trong `HomeController.movieDetail()` bằng Java Stream `Collectors.groupingBy` kết hợp `LinkedHashMap` bảo toàn thứ tự ngày chiếu tăng dần.
[x] Hiển thị điểm số đánh giá trung bình và số lượt đánh giá tổng hợp từ hệ thống review.
[x] Cấu hình đường link nút suất chiếu trỏ đến `/customer/showtimes/{id}/seats` nhằm phối hợp nhịp nhàng với `AuthInterceptor` bảo vệ luồng đặt vé: yêu cầu người dùng đăng nhập trước khi chọn ghế.
[x] Thiết kế giao diện `movie-detail.html` rõ ràng, trực quan, hỗ trợ xem chi tiết phim mà không phát sinh lỗi lazy loading (`open-in-view=false`).

---

# IV. Đánh giá phim (Bonus) (Sprint 2: Movie Review – Issue #18)

## 4.1. TODO 31: Đánh giá phim (1–5 sao), nhận xét của khán giả đã xem phim theo BR-16

Chụp hình kết quả: Khách hàng đã xem phim (như tài khoản An xem suất N-2) thấy form gửi đánh giá và nhận xét trên trang chi tiết phim.
<!-- [NOTE HÌNH ẢNH 20: Chụp màn hình khi đăng nhập tài khoản an.nv@gmail.com và vào chi tiết phim "Biệt Đội Siêu Tốc" (An đã có vé CONFIRMED cho suất đã chiếu xong). Trang hiển thị khung form đánh giá gồm ô chọn số sao 1..5 ⭐, ô nhập nhận xét và nút "Gửi đánh giá"] -->

Chụp hình kết quả: Gửi đánh giá thành công, điểm trung bình và bình luận hiển thị ngay dưới phần đánh giá của khán giả.
<!-- [NOTE HÌNH ẢNH 21: Chụp màn hình sau khi gửi đánh giá 5 sao kèm nhận xét "Phim hành động kịch tính, kỹ xảo đỉnh cao!". Hiển thị thông báo flash xanh: "Cảm ơn bạn đã đánh giá!", điểm phim cập nhật thành 5.0/5 ⭐ (1 lượt), bình luận của An xuất hiện đầu danh sách] -->

Chụp hình kết quả: Khách hàng đánh giá lại phim thì hệ thống tự động cập nhật (Upsert) chứ không tạo mới bản ghi trùng lặp.
<!-- [NOTE HÌNH ẢNH 22: Chụp màn hình sau khi An gửi lại đánh giá 4 sao và sửa nhận xét. Tổng số lượt đánh giá vẫn giữ nguyên là 1 lượt, điểm trung bình cập nhật thành 4.0/5 ⭐, nhận xét được cập nhật mới] -->

Chụp hình kết quả: Khách hàng chưa xem phim hoặc chỉ có đơn huỷ (như tài khoản Bình) không nhìn thấy form gửi đánh giá (chỉ xem được nhận xét).
<!-- [NOTE HÌNH ẢNH 23: Chụp màn hình khi đăng nhập tài khoản binh.tt@gmail.com vào xem phim "Biệt Đội Siêu Tốc" (Bình chưa xem suất chiếu nào đã hoàn tất). Form đánh giá bị ẩn hoàn toàn, chỉ hiển thị danh sách nhận xét của các khán giả khác theo đúng quy tắc BR-16] -->

Commit: 8fbfffc feat(review): add rating summary query (Refs #18)
Commit: 4d94eb7 feat(review): allow customers who watched to review movie (Refs #18)

Checklist đã hoàn thành:
[x] Thiết kế entity `Review` với khoá chính kép `@EmbeddedId ReviewId` (gồm `customerId` và `movieId`), đảm bảo toàn vẹn dữ liệu ở mức DB: mỗi khách hàng chỉ có duy nhất 1 review cho mỗi phim.
[x] Viết query `Q10` trong `ReviewRepository` sử dụng Interface Projection `RatingSummary` tính điểm trung bình (`AVG(r.rating)`) và tổng số đánh giá (`COUNT(r)`).
[x] Viết query `Q12` trong `BookingRepository.countWatched()` kiểm tra xem khách hàng đã xem phim hay chưa (đơn `CONFIRMED` và suất chiếu có `endTime < now()`).
[x] Triển khai `ReviewServiceImpl`:
    - `canReview()`: kiểm tra điều kiện đăng nhập và thoả mãn quy tắc **BR-16**.
    - `save()`: kiểm tra bảo mật, nếu đã có review thì cập nhật (rating, comment, reviewDate), nếu chưa thì tạo mới bằng `getReferenceById` tối ưu hiệu năng.
[x] Xây dựng `CustomerReviewController` nhận POST `/customer/movies/{movieId}/review`, validate `ReviewForm` (1–5 sao, comment tối đa 1000 ký tự).
[x] Tích hợp giao diện đánh giá mượt mà vào `movie-detail.html`: hiển thị form khi `canReview == true`, hiển thị điểm số và danh sách nhận xét sắp xếp theo ngày đánh giá mới nhất.
