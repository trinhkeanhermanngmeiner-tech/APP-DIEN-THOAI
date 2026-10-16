# Hướng dẫn thiết lập bán "Bản Đầy Đủ" 20.000đ trên Google Play

Mô hình: tải miễn phí, chỉ có lịch âm dương. Mua một lần **20.000đ** để mở khóa phần còn lại.
Tác giả: BS. Trịnh Kế An · bstrinhkean@gmail.com

> Các bước và tên mục dưới đây theo hiểu biết tại thời điểm viết; Play Console có thể đổi tên hoặc vị trí. Nếu khác, làm theo những gì Console hiện.

## Phần tính năng

| Miễn phí | Bản Đầy Đủ (20.000đ) |
|---|---|
| Lịch tháng âm dương, can chi, ngày lễ, mùng 1 và rằm | Nhắc ngày giỗ, sinh nhật, kỷ niệm + thông báo |
| Đổi ngày âm ↔ dương | Chi tiêu, ngân sách, sổ hiếu hỷ, khóa PIN/vân tay |
| Tiện ích "Tờ lịch (gọn)" và "Lịch tháng" (chỉ xem lịch âm dương) | Ghi chú, tiện ích Ghi chú, ghi chú theo ngày |
| Chế độ chữ to, giao diện sáng/tối | Xem lịch cả năm, 7 theme màu, tự lưu Google Drive, sao lưu |

Muốn đổi ranh giới này, sửa các điểm kiểm tra `isPro()` trong `so-tay/index.html` và `Pro.isPro()` trong `android/src`.

## Hai loại file khác nhau, đừng nhầm

| File | Dùng để | Thanh toán |
|---|---|---|
| `dist/SoTayLichViet-dung-thu.apk` | Cài trực tiếp để thử trên điện thoại | Không có Play. Mở khóa bằng **mã dùng thử** (nằm trong `android/.dev-code`, không đưa lên GitHub) |
| `.aab` từ GitHub Actions | **Chỉ nộp lên Google Play** | Thanh toán thật qua Google Play |

**Không phát tán file `.apk` dùng thử cho người lạ**: ai có mã sẽ mở khóa không mất tiền. File `.aab` không có ô nhập mã.

## Bước 1: Build file .aab bằng GitHub Actions
1. Đưa nhánh có thư mục `.github/workflows/build-play.yml` lên nhánh chính (main) của repo (GitHub chỉ cho chạy workflow từ nhánh chính).
2. GitHub → **Actions** → **Build Play AAB** → **Run workflow** → nhập **mã phiên bản** (9, sau đó 10, 11…) → chạy.
3. Chờ khoảng 5–10 phút, tải file ở mục **Artifacts** (`SoTayLichViet-play-aab`).
4. Lần đầu có thể lỗi do phiên bản công cụ; xem nhật ký (log) và báo lại để sửa. Bản build này chưa được thử trong môi trường phát triển vì bị chặn kho thư viện của Google.
5. (Khuyến nghị) Tạo khóa tải lên riêng: Settings → Secrets → thêm `KEYSTORE_BASE64` (nội dung `base64` của file .p12) và `KEYSTORE_PASS`. Nếu bỏ qua, workflow dùng khóa đang nằm trong repo (`android/sotay-release.p12`); nên để repo ở chế độ riêng tư.

## Bước 2: Tạo ứng dụng và tải bản đầu tiên
Làm theo hướng dẫn đăng app (tạo app, khai báo nội dung, trang cửa hàng) rồi tải file `.aab` lên **Kiểm thử nội bộ** hoặc **Kiểm thử kín**. Phải có một bản chứa thư viện thanh toán trên Play Console thì mới tạo được sản phẩm mua trong ứng dụng.

## Bước 3: Hồ sơ thanh toán (người bán)
**Cài đặt → Hồ sơ thanh toán** (Payments profile): điền thông tin người bán và tài khoản nhận tiền. Kiểm tra xem tài khoản của bạn ở Việt Nam có được mở không.

## Bước 4: Tạo sản phẩm "Bản Đầy Đủ"
**Kiếm tiền (Monetize) → Sản phẩm → Sản phẩm trong ứng dụng → Tạo sản phẩm**:
| Ô | Giá trị |
|---|---|
| Mã sản phẩm (ID) | `ban_day_du` (**phải đúng từng chữ**, không đổi được sau này) |
| Tên | Bản Đầy Đủ |
| Mô tả | Mở khóa nhắc ngày giỗ, chi tiêu, sổ hiếu hỷ, ghi chú, tiện ích, Google Drive. Mua một lần, dùng mãi. |
| Giá | 20.000 ₫ (Việt Nam); các nước khác để Google quy đổi hoặc bỏ không bán |
| Trạng thái | **Kích hoạt** |

## Bước 5: Thử mua với tài khoản kiểm thử
1. **Cài đặt → Kiểm thử giấy phép** (License testing): thêm Gmail của bạn và của 12 người thử. Họ mua thử **không bị trừ tiền** (thẻ thử nghiệm), giao dịch tự hủy.
2. Người thử vào đường link tham gia kiểm thử, cài app từ Google Play (không cài APK dùng thử).
3. Mở app → Mở khóa → mua thử → kiểm tra các tính năng mở ra; thử **Cài đặt → Bản Đầy Đủ**. Gỡ app, cài lại, bấm "Khôi phục giao dịch đã mua" để thử khôi phục.
4. Thử hoàn tiền / hủy giao dịch thử: lần mở app kế tiếp tính năng phải bị khóa lại.

## Bước 6: Phát hành
Sau 14 ngày thử kín với 12 người (nếu tài khoản cá nhân), xin phát hành chính thức. Ghi rõ trong mô tả cửa hàng phần miễn phí và phần trả phí (đã có sẵn trong `store/noi-dung-chplay.md`).

## Những điều cần biết
- **Google giữ phí dịch vụ** (thường 15% với nhà phát triển nhỏ; kiểm tra mức hiện hành) và có thể khấu trừ thuế. Bạn tự kê khai thuế thu nhập cá nhân theo quy định.
- **Hoàn tiền:** Google cho người mua hoàn tiền trong một thời gian ngắn sau khi mua. App kiểm tra lại giao dịch mỗi lần mở; hoàn tiền xong, tính năng sẽ khóa lại.
- **Bản quyền "mua một lần" chỉ kiểm tra trên máy**, không có máy chủ riêng. Người có kỹ thuật vẫn có thể bẻ khóa app; với mức giá 20.000đ, đây là cách đơn giản và chấp nhận được.
- Khi Google yêu cầu nâng phiên bản thư viện thanh toán, sửa số phiên bản trong `android/gradle/app/build.gradle` rồi build lại.
