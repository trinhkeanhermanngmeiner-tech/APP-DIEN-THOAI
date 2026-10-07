# Sổ Tay Lịch Việt

Ứng dụng điện thoại gồm **lịch âm dương, nhắc sinh nhật – đám giỗ – ngày lễ hằng năm, quản lý chi tiêu và ghi chú**.

**Tác giả:** BS. Trịnh Kế An · bstrinhkean@gmail.com
**Bản quyền:** © 2026 BS. Trịnh Kế An. Mọi quyền được bảo lưu (xem `LICENSE`).

## Tính năng
- Lịch tháng có ngày âm, can chi, mặt trăng theo ngày âm; đổi ngày âm ↔ dương
- Nhắc sinh nhật, đám giỗ theo âm lịch hoặc dương lịch, tự đổi ngày mỗi năm, báo trước nhiều ngày
- Các ngày lễ, Tết Việt Nam; nhắc mùng 1 và rằm
- Sổ thu chi theo tháng, theo nhóm, có ngân sách
- Sổ hiếu hỷ: ghi tiền mừng / phúng viếng theo từng gia đình, nhắc lần trước họ mừng mình bao nhiêu
- Khóa mục Chi tiêu bằng mã PIN hoặc vân tay (bật/tắt trong Cài đặt)
- Hai tiện ích ngoài màn hình chính (Android): "Tờ lịch (gọn)" 2×2 và "Lịch tháng" 4×4 (ngày âm dương, ngày lễ, ngày nhớ, ghi chú; đổi tháng; chạm vào ngày để ghi chú nhanh)
- Ghi chú gắn với ngày, hiện trên lịch
- Chế độ chữ to cho người lớn tuổi
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

Kiểm tra mở app trên Android 8 → 16 (Robolectric): `android/test/run.sh` (chạy sau `build.sh`).

Phiên bản: tên phiên bản luôn để **1.0**; mã phiên bản (`VERSION_CODE` trong `android/build.sh`) tăng mỗi lần phát hành để máy và Google Play nhận bản cập nhật.

> Tệp `index.html` ở thư mục gốc là ứng dụng *Anesthesia Assist* (hỗ trợ gây mê hồi sức), độc lập với Sổ Tay Lịch Việt.
