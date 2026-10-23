# Cài Horizon lên tivi Android

English version: [INSTALL.md](INSTALL.md)

Horizon là một màn hình chính (launcher). Cài gồm hai bước:

1. **Cài app** (khoảng 5 phút, đa số tivi không cần máy tính).
2. **Đặt làm màn hình chính mặc định**, để bấm nút Home là mở Horizon (cần máy tính một lần).

Làm bước 1 trước, thấy chạy ổn rồi mới làm bước 2.

Dùng được trên: tivi Android TV 5.0 trở lên có remote và danh sách ứng dụng bình thường. Không dùng
cho Fire TV, Apple TV, Samsung Tizen, LG webOS, Roku. Mới thử trên một tivi Android 8, nên nếu gặp
lỗi gì xin báo lại.

---

## Bước 1 – Cài app

Tải file `Horizon-1.0.0.apk` ở trang **Releases** của dự án trước.

### Cách A – App Downloader (dễ nhất, tivi có Google Play)

1. Trên tivi, mở Google Play, cài app **Downloader** (của AFTVnews).
2. Mở Downloader, nhập link tải trực tiếp file APK (copy ở trang Releases), bấm **Go**.
3. Khi tivi hỏi, cho phép Downloader **cài ứng dụng không rõ nguồn**
   (Cài đặt → Ứng dụng → Bảo mật → Nguồn không xác định → Downloader → Bật).
4. Bấm **Install**, rồi **Open**.

### Cách B – USB

1. Chép `Horizon-1.0.0.apk` vào USB (định dạng FAT32 hoặc exFAT).
2. Cắm vào tivi, mở một app quản lý file trên tivi (ví dụ *File Commander*, *X-plore*).
3. Mở file APK, bấm **Install**. Cho phép app quản lý file cài ứng dụng không rõ nguồn nếu được hỏi.

### Cách C – adb từ máy tính (bước 2 cũng cần cách này)

1. Trên tivi: **Cài đặt → Tùy chọn thiết bị → Giới thiệu**, bấm vào **Bản dựng (Build)** 7 lần để bật
   Tùy chọn nhà phát triển. Sau đó vào **Cài đặt → Tùy chọn thiết bị → Tùy chọn nhà phát triển**,
   bật **Gỡ lỗi USB** (một số tivi còn có **Gỡ lỗi mạng / ADB debugging**).
2. Xem địa chỉ IP của tivi: **Cài đặt → Mạng → mạng đang dùng**.
3. Cài adb trên máy tính:
   - macOS: `brew install --cask android-platform-tools`
   - Windows: tải "SDK Platform-Tools" ở developer.android.com rồi giải nén
   - Linux: `sudo apt install adb`
4. Kết nối và cài (máy tính và tivi cùng mạng):

```
adb connect <IP-tivi>:5555
adb install -r Horizon-1.0.0.apk
```

   Lần đầu tivi hiện "Cho phép gỡ lỗi USB?". Chọn **Luôn cho phép**. Thấy `Success` là đã cài xong.

   Tivi Android 11 trở lên có **Gỡ lỗi không dây**: dùng `adb pair <ip>:<cổng-ghép-đôi>` với mã hiện
   trên tivi, rồi `adb connect <ip>:<cổng>`.

### Kiểm tra app chạy đúng

Mở **Horizon** từ danh sách ứng dụng. Bạn sẽ thấy:

- đồng hồ, ngày, dòng lịch âm ở góc trên bên phải, chữ có bóng đổ,
- một hàng ứng dụng sát đáy màn hình (trái/phải để di chuyển, OK để mở),
- sau 10–20 giây có ảnh phong cảnh, tên người chụp ở góc trên bên trái.

Ảnh cần có internet. Sau 12 giây không bấm gì, hàng app mờ đi để chỉ còn ảnh; bấm phím bất kỳ để hiện lại.

---

## Bước 2 – Đặt Horizon làm màn hình chính mặc định

Android TV giữ launcher riêng của hãng cho nút Home. Có ba cách, từ dễ đến chắc chắn nhất. Sau mỗi
cách, bấm **Home** trên remote để thử.

### Cách 1 – Để tivi tự hỏi (không cần máy tính)

Một số tivi hỏi "Chọn ứng dụng màn hình chính" khi có nhiều hơn một launcher. Nếu tivi của bạn hỏi,
bấm Home, chọn **Horizon**, rồi bấm **Luôn luôn**. Nếu không thấy hỏi, sang cách 2.

### Cách 2 – Đặt bằng adb

```
adb shell cmd package set-home-activity app.horizontv.home/.MainActivity
```

Bấm Home. Nếu mở ra Horizon là xong. Nhiều tivi vẫn ưu tiên launcher của hãng, bấm Home vẫn ra
màn hình cũ. Khi đó làm cách 3.

### Cách 3 – Tắt launcher gốc (chắc chắn, đảo ngược được)

1. Liệt kê các launcher, ghi lại tên gói:

```
adb shell pm list packages | grep -iE "launcher|home"
```

   Tivi Android TV 8 thường có `com.google.android.tvlauncher` và
   `com.google.android.leanbacklauncher`. Hãng khác có tên riêng. **Tuyệt đối không tắt
   `app.horizontv.home`.** Chỉ chọn launcher gốc, không phải Horizon.

2. Tắt chúng (thay bằng tên gói trên tivi của bạn):

```
adb shell pm disable-user --user 0 com.google.android.tvlauncher
adb shell pm disable-user --user 0 com.google.android.leanbacklauncher
```

3. Đặt Horizon làm màn hình chính và khởi động lại:

```
adb shell cmd package set-home-activity app.horizontv.home/.MainActivity
adb reboot
```

An toàn: cài Horizon và thấy nó chạy được (bước 1) **trước khi** tắt bất cứ gì. Nếu có lúc màn hình
không có launcher nào, kết nối adb và chạy các lệnh trong phần "Quay về như cũ" bên dưới.

---

## Quay về như cũ

```
adb shell pm enable com.google.android.tvlauncher
adb shell pm enable com.google.android.leanbacklauncher
adb uninstall app.horizontv.home
adb reboot
```

Dùng đúng tên gói bạn đã tắt. Muốn giữ Horizon nhưng không để nó làm mặc định thì chỉ chạy các dòng
`pm enable`.

---

## Gặp lỗi

| Vấn đề | Cách xử lý |
|---|---|
| `adb: command not found` | Cài adb (bước 1, cách C, mục 3). |
| `failed to connect` / `connection refused` | Bật gỡ lỗi USB/mạng, khởi động lại tivi, kiểm tra IP, đảm bảo cùng mạng. |
| `failed to authenticate` | Nhìn màn hình tivi và chấp nhận "Cho phép gỡ lỗi USB?". |
| `INSTALL_FAILED_...` khi cài | Gỡ bản cũ trước: `adb uninstall app.horizontv.home`, rồi cài lại. |
| Bấm Home vẫn ra launcher cũ | Dùng cách 3 và khởi động lại tivi. |
| Không có ảnh, chỉ nền tối | Kiểm tra tivi có internet. Đợi một phút. Ảnh tải từ Wikimedia, đôi khi chậm; Horizon sẽ thử lại và lưu ảnh đã tải. |
| Đồng hồ sai giờ | Cài đặt → Ngày & giờ → Tự động. |
| Hàng app biến mất | Cố ý mờ đi sau 12 giây. Bấm phím bất kỳ. |
| Thiếu app trong hàng | Horizon chỉ liệt kê app có biểu tượng launcher cho tivi hoặc thường. App cài ngoài không có biểu tượng sẽ không hiện. |

## Quyền riêng tư

Horizon chỉ xin một quyền là internet, để tải ảnh từ Wikimedia Commons. Không có tài khoản, quảng cáo
hay theo dõi.
