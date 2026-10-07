# Sổ Tay Lịch Việt

Ứng dụng điện thoại gồm **lịch âm dương, nhắc sinh nhật – đám giỗ – ngày lễ hằng năm, quản lý chi tiêu và ghi chú**.

**Tác giả:** BS. Trịnh Kế An · bstrinhkean@gmail.com
**Bản quyền:** © 2026 BS. Trịnh Kế An. Mọi quyền được bảo lưu (xem `LICENSE`).

## Tính năng
- Lịch tháng có ngày âm, can chi, mặt trăng theo ngày âm; đổi ngày âm ↔ dương
- Nhắc sinh nhật, đám giỗ theo âm lịch hoặc dương lịch, tự đổi ngày mỗi năm, báo trước nhiều ngày
- Các ngày lễ, Tết Việt Nam; nhắc mùng 1 và rằm
- Sổ thu chi theo tháng, theo nhóm, có ngân sách
- Ghi chú có ghim, màu giấy, tìm kiếm
- Bản Android báo thông báo mỗi sáng, chạy không cần mạng, không thu thập dữ liệu

## Cấu trúc
| Thư mục | Nội dung |
|---|---|
| `so-tay/index.html` | Toàn bộ giao diện và logic (một tệp HTML) |
| `android/` | Vỏ Android (Java), tài nguyên, script đóng gói `build.sh` |
| `dist/` | `SoTayLichViet.apk` (cài trực tiếp), `SoTayLichViet.aab` (nộp Google Play) |
| `docs/` | Chính sách quyền riêng tư |
| `store/` | Biểu tượng, ảnh bìa, ảnh chụp màn hình cho trang Google Play |

## Đóng gói
```bash
android/build.sh            # tạo dist/SoTayLichViet.apk và dist/SoTayLichViet.aab
KEYSTORE=/duong/dan/upload.p12 KEYSTORE_PASS=... android/build.sh   # ký bằng khóa riêng
```
Không cần Android Studio; script tự tải aapt2, dx, apksig, bundletool và android.jar.

> Tệp `index.html` ở thư mục gốc là ứng dụng *Anesthesia Assist* (hỗ trợ gây mê hồi sức), độc lập với Sổ Tay Lịch Việt.
