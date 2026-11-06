# Ứng dụng KidsPlay - Ngôn ngữ Java (Android Nâng Cao)

Mã nguồn ứng dụng KidsPlay dựa trên Phân tích Yêu cầu Chương 2, viết hoàn toàn bằng **Java**.

## Kiến trúc & Công nghệ:
- **Kiến trúc:** MVVM (Model-View-ViewModel) + Room Database (Java) + Repository Pattern
- **Luồng bất đồng bộ:** `ExecutorService` & `LiveData`
- **Chống thoát Kid Mode:** Android Lock Task Mode (`startLockTask()`) + Chặn phím cứng Back
- **Cảm biến (Sensors):**
  - **Accelerometer (Gia tốc - Lắc máy):** Lật trang truyện tự động (`ShakeDetector.java`)
  - **Light Sensor (Ánh sáng):** Tự động làm tối màn hình bảo vệ mắt khi phòng thiếu sáng (`LightSensorManager.java`)
- **Tác vụ Nền (WorkManager):** Đếm thời gian cộng dồn và tự bật màn hình khóa khi hết giờ

## Hướng dẫn mở dự án:
1. Giải nén tập tin ZIP.
2. Mở Android Studio -> **Open...** -> Chọn thư mục `KidsPlay_Java`.
3. Chờ Gradle Sync hoàn tất và nhấn **Run** (`Shift + F10`).
