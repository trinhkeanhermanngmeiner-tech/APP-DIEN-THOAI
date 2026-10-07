# Thành phần của bên thứ ba

Ứng dụng **Sổ Tay Lịch Việt** (tác giả: BS. Trịnh Kế An) dùng các thành phần sau.

## Đóng gói bên trong ứng dụng

| Thành phần | Giấy phép | Dùng cho |
|---|---|---|
| Phông chữ **Be Vietnam Pro**, © 2021 The Be Vietnam Pro Project Authors | SIL Open Font License 1.1 | Chữ nội dung |
| Phông chữ **Bricolage Grotesque**, © 2022 The Bricolage Grotesque Project Authors | SIL Open Font License 1.1 | Chữ tiêu đề, con số |

Bản APK/AAB có kèm toàn văn giấy phép ở `assets/www/fonts/*-OFL.txt`. Bản web tải hai phông chữ này từ Google Fonts.
SIL OFL 1.1 cho phép dùng trong ứng dụng thương mại, chỉ cấm bán riêng phông chữ.

## Thuật toán

Phần tính âm lịch được viết riêng cho ứng dụng này, dựa trên các **công thức thiên văn đã công bố** trong sách
*Astronomical Algorithms* (Jean Meeus, Willmann-Bell, 2nd ed., 1998): thời điểm sóc (chương 49), kinh độ Mặt Trời (chương 25);
ΔT theo đa thức của F. Espenak và J. Meeus (NASA). Công thức và quy tắc lập lịch không phải đối tượng của quyền tác giả;
ứng dụng không chứa mã nguồn sao chép từ thư viện âm lịch nào khác.

## Chỉ dùng khi đóng gói (không nằm trong ứng dụng)

| Công cụ | Giấy phép |
|---|---|
| aapt2, dx (dalvik-dx), apksig, bundletool | Apache License 2.0 |
| android.jar (Android SDK Platform 36), chỉ dùng để biên dịch | Android Software Development Kit License |
