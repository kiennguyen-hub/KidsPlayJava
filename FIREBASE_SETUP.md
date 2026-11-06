# Thiết lập Firebase cho KidsPlay

Project Firebase: `fir-653a6` (file `app/google-services.json` đã có sẵn trong project).
`applicationId` của app đã đặt là `com.company.KidsPlay_BackFix` để khớp với `package_name`
trong file đó. (Nếu muốn dùng package khác, vào Firebase Console > Project settings > Your apps,
thêm app Android mới với package mong muốn rồi thay file `google-services.json`.)

## 1. Bật các dịch vụ trong Firebase Console
1. **Authentication** > Sign-in method > bật **Email/Password**.
2. **Firestore Database** > Create database (chọn vị trí gần nhất, ví dụ `asia-southeast1`).
3. **Storage** > Get started. Từ 3/2/2026 Storage yêu cầu gói **Blaze** (liên kết thanh toán);
   nếu chưa nâng, đăng nhập và Firestore vẫn chạy nhưng video/ảnh truyện tranh sẽ không tải lên được.
   Nên đặt cảnh báo ngân sách thấp trong Google Cloud Billing.

## 2. Dán Security Rules (rất quan trọng, mỗi phụ huynh chỉ truy cập dữ liệu của mình)

**Firestore** (tab Rules):
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid}/contents/{contentId} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```

**Storage** (tab Rules):
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /users/{uid}/content/{allPaths=**} {
      allow read, delete: if request.auth != null && request.auth.uid == uid;
      allow create, update: if request.auth != null && request.auth.uid == uid
                            && request.resource.size < 300 * 1024 * 1024;
    }
  }
}
```

## 3. Cấu trúc dữ liệu trên cloud
- Firestore: `users/{uid}/contents/{cloudId}` gồm title, type (VIDEO/STORY/COMIC), minAge,
  durationMs, pageCount, body (truyện chữ), files (danh sách tên file), createdAt.
- Storage: `users/{uid}/content/{cloudId}/{tên file}` (video hoặc 0001.jpg, 0002.jpg...).
- Hồ sơ bé, mã PIN và nhật ký giờ dùng vẫn chỉ lưu trong máy (không đưa lên cloud).

## 4. Lỗi thường gặp
| Thông báo trong app | Nguyên nhân / cách xử lý |
|---|---|
| Chưa bật đăng nhập Email/Password | Làm bước 1.1 |
| Firestore từ chối quyền | Chưa dán Rules Firestore (bước 2) |
| Storage từ chối quyền | Chưa dán Rules Storage hoặc chưa đăng nhập |
| Chưa bật Cloud Storage cho project | Làm bước 1.3 (cần gói Blaze) |
| Build lỗi "package name not found in google-services.json" | `applicationId` không khớp file JSON |
