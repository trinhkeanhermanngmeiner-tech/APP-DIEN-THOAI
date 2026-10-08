# Aurora Launcher

Launcher Android nhiều hiệu ứng, viết bằng Kotlin + Jetpack Compose. Cần Android 12 trở lên; shader cực quang cần Android 13 trở lên.

## Hiệu ứng đã có

| Hiệu ứng | File |
|---|---|
| Nền cực quang động (shader AGSL): dải sáng uốn lượn, sao lấp lánh | `effects/Aurora.kt` |
| Parallax theo cảm biến: nền, icon, đồng hồ, đom đóm xê dịch khi nghiêng máy | `effects/Tilt.kt` |
| Dock kính mờ (frosted glass) có viền sáng và vệt phản chiếu chạy theo góc nghiêng | `effects/Glass.kt` |
| Đom đóm phát sáng bay lên | `effects/Fireflies.kt` |
| Lật trang kiểu khối lập phương 3D | `effects/PageTransitions.kt` |
| Icon nảy kiểu lò xo khi chạm, rung phản hồi | `ui/AppIcon.kt` |
| App phóng to từ icon khi mở | `ui/LauncherScreen.kt` |
| Ngăn kéo ứng dụng: vuốt lên, màn hình chính thu nhỏ và mờ dần, icon hiện lần lượt từng hàng | `ui/AppDrawer.kt` |
| Đồng hồ lớn chữ chuyển màu, phát sáng, ngày tháng tiếng Việt | `ui/ClockHeader.kt` |

Chức năng: liệt kê mọi ứng dụng (kể cả hồ sơ công việc), tự cập nhật khi cài hoặc gỡ app, tìm kiếm không dấu ("tin nhan" ra "Tin nhắn"), dock tự chọn app Điện thoại, Tin nhắn, Trình duyệt, Camera, nhấn giữ icon để xem thông tin hoặc gỡ cài đặt.

## Cài đặt trong app
Nhấn giữ vào chỗ trống trên màn hình chính (hoặc bấm **Cài đặt** trong ngăn kéo ứng dụng) để:
- **Ẩn thanh điều hướng** 3 nút ở màn hình chính (vuốt từ cạnh dưới lên để hiện tạm).
- **Vuốt xuống** mở thanh thông báo, **chạm 2 lần** khóa màn hình (cần bật "Aurora Launcher – cử chỉ" trong Trợ năng).
- Bật/tắt từng hiệu ứng: nền cực quang, đom đóm, chuyển động theo độ nghiêng, lật trang 3D, dock kính mờ.
- Bố cục: hiện/ẩn tên app, 4 hoặc 5 cột, kích thước icon.

**Giới hạn:** app thường không đổi được chế độ điều hướng của hệ thống. Muốn bỏ 3 nút trong *mọi* ứng dụng, chuyển HyperOS sang cử chỉ toàn màn hình: **Cài đặt → Màn hình chính → Điều hướng hệ thống → Cử chỉ**. Một số bản HyperOS khóa mục này khi dùng launcher bên thứ ba.

## Cách lấy file APK

### Cách 1: tải từ GitHub Actions (không cần máy tính)
Mỗi lần đẩy code lên, GitHub tự build. Vào tab **Actions** của repo, chọn lần chạy mới nhất của **Build Aurora Launcher**, tải mục **aurora-launcher-apk** ở cuối trang. Nên cài `app-release.apk` vì chạy mượt hơn bản debug nhiều.

### Cách 2: Android Studio
1. Mở thư mục `launcher/` bằng Android Studio.
2. Bật **Gỡ lỗi USB** trên điện thoại rồi cắm cáp.
3. Bấm Run. Để đánh giá độ mượt, chọn biến thể build `release`.

## Đặt làm launcher mặc định
- Mở Aurora. Khi chưa là mặc định, phía trên màn hình có thanh **"Đặt mặc định"**. Bấm vào để hiện hộp chọn launcher của hệ thống, hoặc mở trang cài đặt nếu HyperOS không hiện hộp đó.
- Cách thủ công trên HyperOS: **Cài đặt → Ứng dụng → Ứng dụng mặc định → Màn hình chính**, chọn Aurora Launcher. Nếu không tìm thấy, gõ "mặc định" hoặc "màn hình chính" vào ô tìm kiếm của Cài đặt.

## Lưu ý với HyperOS (Xiaomi / POCO)
- Một số bản HyperOS/MIUI **chặn cử chỉ toàn màn hình khi dùng launcher bên thứ ba** (tự chuyển về 3 nút điều hướng). Đây là giới hạn của hệ thống, app không vượt qua được.
- Hiệu ứng mở app có thể bị HyperOS thay bằng animation riêng của hệ thống.
- Nếu launcher bị tắt khi chạy nền: vào thông tin ứng dụng, đặt **Tiết kiệm pin → Không giới hạn** và bật **Tự khởi động**.

## Bước tiếp theo nên làm
- Màn hình cài đặt: bật/tắt từng hiệu ứng, chọn bảng màu cực quang
- Kéo thả sắp xếp icon, tạo thư mục
- Widget (`AppWidgetHost`)
- Thêm kiểu chuyển trang (carousel, cover flow), hạt tuyết/mưa
- Icon pack
