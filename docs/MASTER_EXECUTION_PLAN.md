# KẾ HOẠCH THỰC THI CHI TIẾT (MASTER EXECUTION PLAN)
## DỰ ÁN ANDROID HRP (MOBILE CLIENT)

> **Mã định danh hợp đồng:** `web-android-contract.md`  
> **Hash SHA-256 Hợp đồng:** `300C6F107F5C08823AA47B049A560C169393F2D021BFA03B40B2AAC90999F57A`  
> **Git Repository Android:** `https://github.com/nobita6986/hrp-android` (Thư mục: `D:\CodeApp\hr-partner`)  
> **Backend Repository:** `D:\CodeApp\HRP`  
> **Thời điểm cập nhật:** 19/10/2026  
> **Đối tượng áp dụng:** Mọi Coding Agent và Lập trình viên phụ trách Android App HRP qua các phiên làm việc.

> **Ghi chú kỹ thuật trước Giai đoạn 1:**
> 1. Vị trí enum điều hướng hiện hữu nằm ở `com.example.ui.viewmodel.NavigationTab` trong `HrpViewModel.kt` (không dùng `Screen.kt`).
> 2. DTO ở Giai đoạn 1 tuân thủ Mixed Response: các route Web hiện hữu giữ root JSON (`/api/auth/login`, `GET /api/jobs`, `POST /api/public/jobs/{slug}/applications`, `GET /api/worker/attendance`); các route mới cho Mobile dùng envelope `{ ok, data, error }`.
> 3. Idempotency key gửi qua HTTP Header `Idempotency-Key: <UUID>` thay vì body.
> 4. Ngưỡng GPS và bán kính phụ thuộc cấu hình Site/Project trên server; nếu chưa cấu hình thì fail-closed với `SITE_GEOFENCE_NOT_CONFIGURED`.
> 5. Referral chỉ dành cho Người lao động đã xác thực và đủ điều kiện (`canSubmit: true`).

---

## 1. NGUYÊN TẮC BẤT BIẾN (NON-NEGOTIABLE ARCHITECTURAL RULES)

1. **Phạm vi đối tượng:** Ứng dụng Android **CHỈ DÀNH RIÊNG** cho Người lao động (Worker) và Người tìm việc (Job Seeker).
   - Tuyệt đối không có vai trò HR, HR Staff, Quản lý, Ban Giám Đốc (BoD) hay CTV/Recruiter nội bộ trong app.
   - Nghiệp vụ quản trị, đối soát, phê duyệt hồ sơ, duyệt hoa hồng và chi trả thuộc về Web Admin Panel HRP.
2. **Cấu trúc điều hướng:** Đúng **5 Tabs cố định** theo thứ tự:
   `1. Tổng quan` -> `2. Việc làm` -> `3. Chấm công` -> `4. Góp ý` -> `5. Giới thiệu người làm`.
3. **Tạm ẩn chức năng ngoài phạm vi:**
   - Tạm ẩn Phiếu lương / Ứng lương (`PayrollScreen`).
   - Tạm ẩn module Tuyển dụng / CTV nội bộ (Referral Người làm là tính năng riêng của NLĐ, không cấp quyền CTV).
   - Xóa bỏ hoàn toàn hộp thoại chuyển vai trò (`RoleSwitcherDialog`) và các nút giả lập duyệt đơn trên app.
4. **Nguyên tắc Zero Mock:**
   - Không dùng dữ liệu giả lập (mock data), không bypass đăng nhập, không trả về kết quả thành công giả khi gọi API ghi dữ liệu.
   - Mọi lỗi mạng (HTTP 401, 403, 404, 422, 500, 503) phải được bắt và hiển thị trung thực lên giao diện (Snackbar, Error Banner).
5. **Chấm công GPS phần cứng thật:**
   - Bắt buộc kích hoạt chip GPS phần cứng (`FusedLocationProviderClient`) để lấy tọa độ thực tế, thời gian thực và sai số (accuracy).
   - Chặn tuyệt đối Mock GPS trên môi trường Production/Debug. Không tự điền tọa độ mặc định khi GPS null.
6. **Chính sách Giới thiệu người làm (Referral) động:**
   - Không hard-code số tiền hoa hồng, tỷ lệ hoặc quy tắc trong app.
   - Mọi thông tin (điều kiện, sự kiện thành công, cách tính hoa hồng) phải lấy từ API `GET /api/referrals/program`.
   - Nếu chính sách chưa được Owner/Admin publish (`isEnabled == false`), app hiển thị trạng thái **“Chưa mở”** và vô hiệu hóa form gửi.
   - Bắt buộc có xác nhận người được giới thiệu đồng ý chia sẻ thông tin. Không tự động nộp hồ sơ ứng tuyển thay họ.

---

## 2. MA TRẬN 5 TABS & PHÂN QUYỀN TRUY CẬP

| Tab | Tên Màn hình | Route Enum | Khách (Chưa Login) | Đã Login (Chưa duyệt HRP) | Công nhân HRP (Đã duyệt HRP) |
| :---: | :--- | :--- | :--- | :--- | :--- |
| **1** | **Tổng quan** | `Screen.HOME` | Xem thông tin giới thiệu HRP, nút Đăng nhập / Đăng ký | Xem thông tin tài khoản, Thẻ trạng thái xác thực hồ sơ (`PENDING_REVIEW`, `NEEDS_INFO`, `REJECTED`) | Xem ca làm việc hiện tại, nút tắt Chấm công nhanh, số ngày công trong tháng |
| **2** | **Việc làm** | `Screen.JOBS` | Xem danh sách việc làm, tìm kiếm, nộp hồ sơ ứng tuyển nhanh (nhập Họ tên + SĐT) | Xem và ứng tuyển; Form tự động lấy SĐT từ tài khoản (khóa trường SĐT) | Xem và ứng tuyển; tự động lấy SĐT từ tài khoản |
| **3** | **Chấm công** | `Screen.ATTENDANCE` | Bị khóa (Hiển thị thông báo yêu cầu Đăng nhập) | Bị khóa (Hiển thị Banner yêu cầu gửi / chờ duyệt hồ sơ xác thực công nhân HRP) | Mở đầy đủ: Nút Chấm công Vào/Ra ca bằng GPS thật; Bảng lịch sử công theo tháng |
| **4** | **Góp ý** | `Screen.FEEDBACK` | Yêu cầu đăng nhập hoặc xác thực OTP SĐT | Cho phép gửi góp ý và tra cứu phản hồi từ HR (chỉ cần SĐT đã xác thực OTP) | Cho phép gửi góp ý và tra cứu phản hồi từ HR |
| **5** | **Giới thiệu người làm** | `Screen.REFERRAL` | Xem chính sách giới thiệu công khai; yêu cầu Đăng nhập để gửi | Nếu policy bật: Cho phép gửi giới thiệu; xem danh sách người mình đã giới thiệu | Nếu policy bật: Cho phép gửi giới thiệu; xem danh sách người mình đã giới thiệu |

---

## 3. LỘ TRÌNH 5 GIAI ĐOẠN THỰC THI (PHASED EXECUTION ROADMAP)

### GIAI ĐOẠN 0: DỌN DẸP MOCK & KHÓA AN TOÀN (ĐỘC LẬP MẠNG) - [HOÀN TẤT]
*Mục tiêu: Đưa ứng dụng về trạng thái giao diện sạch, đúng 5 tabs, không còn code giả lập role HR/Admin/BoD/CTV.*

#### Danh sách công việc đã hoàn thành:
1. **Điều hướng (`HrpViewModel.kt`, `HrpBottomBar.kt`, `MainActivity.kt`):**
   - Đã cập nhật enum `NavigationTab`: `HOME`, `JOBS`, `ATTENDANCE`, `FEEDBACK`, `REFERRAL`.
   - Cập nhật `HrpBottomBar` hiển thị chuẩn xác 5 tabs trên theo đúng thứ tự.
   - Thêm `ReferralScreen.kt` hiển thị trạng thái "Chưa mở" minh bạch, không mock dữ liệu hay hard-code hoa hồng.
2. **Loại bỏ vai trò giả lập (`HrpTopBar.kt`, `HomeScreen.kt`, `HrpViewModel.kt`):**
   - Xóa bỏ `RoleSwitcherDialog` và nút chuyển role khỏi `HrpTopBar`, thay bằng nút Đăng xuất an toàn.
   - Xóa bỏ các nhánh HR/BoD/CTV trong `HomeScreen`, `TicketScreen`, `AttendanceScreen`, `JobBoardScreen`.
   - Giữ lại vai trò duy nhất `WORKER` (NLĐ / Người tìm việc).
   - Nếu đăng nhập trả về role khác `WORKER`, session bị từ chối ngay lập tức và xóa token.
3. **Tạm ẩn màn hình không dùng:**
   - Ẩn điều hướng đến `PayrollScreen` và xóa bỏ toàn bộ card theo dõi ứng viên CTV / quản lý tuyển dụng nội bộ.
4. **Loại bỏ Mock Data Fallback trong `HrpRepository.kt`:**
   - Xóa bỏ toàn bộ mock datasets ban đầu.
   - Các API methods trả về `Result.failure()` thật khi mạng lỗi, loại bỏ hoàn toàn fallback mock success / fake login.
5. **Kiểm thử biên dịch:**
   - Đã sinh `gradlew.bat` và chạy `./gradlew compileDebugKotlin` thành công 100% (0 errors).

---

### GIAI ĐOẠN 1: CHUẨN HÓA DTO & NETWORK LAYER (SAU KHI FREEZE CONTRACT) - [HOÀN TẤT]
*Mục tiêu: Xây dựng các Data Transfer Objects khớp chính xác 100% với JSON schema của Backend.*

#### Danh sách công việc đã hoàn thành:
1. **Tạo Model Response Envelope & Error Parser (`ApiModels.kt`):**
   - Đã cài đặt `ApiResponse<T>`, `ApiError`, `HrpApiException` và `ApiErrorParser` dịch chuẩn xác các mã lỗi hợp đồng (`OTP_SERVICE_UNAVAILABLE`, `FORBIDDEN_NOT_VERIFIED`, `WORKER_VERIFICATION_PENDING`, `REFERRAL_PROGRAM_DISABLED`, `REFERRAL_CONSENT_REQUIRED`, `GEOFENCE_OUT_OF_RANGE`, `GPS_ACCURACY_TOO_LOW`, `SITE_GEOFENCE_NOT_CONFIGURED`, `RATE_LIMIT_EXCEEDED`).
2. **Khai báo đầy đủ DTOs hỗn hợp (`ApiModels.kt`):**
   - Hỗ trợ đồng thời Root JSON cho 4 endpoints Web hiện hữu (`/api/auth/login`, `GET /api/jobs`, `POST /api/public/jobs/{slug}/applications`, `GET /api/worker/attendance`) và Mobile Envelope `{ ok, data, error }` cho 13 endpoints mới.
   - Gửi `Idempotency-Key: <UUID>` qua HTTP Header cho các request ghi dữ liệu (`applyPublicJob`, `submitCheckInEvent`, `submitReferral`).
3. **Cập nhật Retrofit Interface (`HrpApiService.kt`) & Repository (`HrpRepository.kt`):**
   - Khai báo và kết nối đủ 17 hàm API theo đúng bảng 14.3.B trong `web-android-contract.md`.

---

### GIAI ĐOẠN 2: LUỒNG AUTH, OTP & XÁC THỰC CÔNG NHÂN HRP - [HOÀN TẤT]
*Mục tiêu: Đảm bảo đăng ký, đăng nhập và quy trình xác thực danh tính diễn ra thông suốt, an toàn.*

#### Danh sách công việc đã hoàn thành:
1. **Màn hình Đăng nhập & Đăng ký (`LoginScreen.kt`):**
   - Hỗ trợ chuyển đổi giữa Đăng nhập (`POST /api/auth/login`) và Đăng ký tài khoản mới (`POST /api/auth/register`).
   - Lưu Bearer token, `phoneVerified`, `verificationStatus` và `hasActiveAssignment` vào `TokenManager`. Từ chối mọi tài khoản có role khác `WORKER`.
2. **Hộp thoại OTP (`PhoneOtpVerificationModal` trong `HrpModals.kt`):**
   - Hỗ trợ gửi mã OTP (`POST /api/auth/phone-otp/send`) kèm bộ đếm ngược `cooldownSeconds` và xác thực mã (`POST /api/auth/phone-otp/verify`).
   - Xử lý trung thực mã lỗi `503 OTP_SERVICE_UNAVAILABLE`, tuyệt đối không giả lập thành công.
3. **Hộp thoại Nộp hồ sơ Xác thực Công nhân (`WorkerVerificationModal` trong `HrpModals.kt`):**
   - Tải danh sách dự án đang mở từ `GET /api/public/projects`.
   - Gửi hồ sơ xác thực (`fullName`, `cccdNumber`, `dob`, `projectId`, `employeeCode`) qua `POST /api/worker-verifications` và đồng bộ trạng thái qua `GET /api/worker-verifications/me`.
4. **Thẻ trạng thái xác thực trên `HomeScreen.kt`:**
   - Hiển thị rõ ràng theo từng trạng thái (`NONE`, `PENDING_REVIEW`, `NEEDS_INFO`, `APPROVED`, `REJECTED`) kèm `reviewNote` từ HR.

---

### GIAI ĐOẠN 3: VIỆC LÀM PUBLIC & CHẤM CÔNG GPS PHẦN CỨNG THẬT - [HOÀN TẤT]
*Mục tiêu: Người tìm việc dễ dàng ứng tuyển; Công nhân đã duyệt chấm công chính xác bằng GPS.*

#### Danh sách công việc đã hoàn thành:
1. **Mục Việc làm (`JobBoardScreen.kt` & `ApplyJobModal`):**
   - Gọi `GET /api/jobs` và nộp hồ sơ ứng tuyển qua `POST /api/public/jobs/{slug}/applications` kèm header `Idempotency-Key` và xác nhận `consent`.
2. **Mục Chấm công (`AttendanceScreen.kt` & `LocationHelper.kt`):**
   - Khóa nút Vào Ca / Tan Ca khi `verificationStatus != APPROVED`, hiển thị nút yêu cầu hoàn tất Xác thực công nhân HRP.
   - Lấy tọa độ GPS phần cứng thực tế (`latitude`, `longitude`, `accuracyMeters`) cho cả Vào Ca (`CHECK_IN`) và Tan Ca (`CHECK_OUT`), gửi lên `POST /api/worker/checkins/events` kèm header `Idempotency-Key`.
   - Hiển thị lịch sử chấm công thực tế từ `GET /api/worker/attendance` kèm trạng thái `geofenceResult`.

---

### GIAI ĐOẠN 4: GÓP Ý & GIỚI THIỆU NGƯỜI LÀM (DYNAMIC REFERRAL) - [HOÀN TẤT]
*Mục tiêu: Đóng góp ý kiến và triển khai chính sách giới thiệu người làm tuân thủ chính sách Owner duyệt.*

#### Danh sách công việc đã hoàn thành:
1. **Mục Góp ý (`TicketScreen.kt` & `CreateTicketModal`):**
   - Hỗ trợ 5 danh mục chuẩn của contract (`WORK_ENVIRONMENT`, `SALARY_BENEFITS`, `SUGGESTION`, `COMPLAINT`, `OTHER`).
   - Gửi góp ý lên `POST /api/feedback` và tải lịch sử kèm phản hồi HR từ `GET /api/feedback/mine`.
2. **Mục Giới thiệu người làm (`ReferralScreen.kt`):**
   - Tải chính sách động từ `GET /api/referrals/program`, tuyệt đối không hard-code mức thưởng hay quy tắc.
   - Khi `isEnabled == false`: Hiển thị huy hiệu **“CHƯA MỞ”** và ẩn biểu mẫu gửi.
   - Khi `isEnabled == true && !canSubmit`: Hiển thị lý do `blockedReason` và nút dẫn tới Xác thực công nhân HRP.
   - Khi `isEnabled == true && canSubmit`: Hiển thị biểu mẫu giới thiệu với checkbox bắt buộc `consentToContact` (không tự ứng tuyển thay ứng viên) và gửi qua `POST /api/referrals` kèm header `Idempotency-Key`.
   - Hiển thị danh sách giới thiệu tối thiểu đã che thông tin nhạy cảm từ `GET /api/referrals/mine`.

---

### GIAI ĐOẠN 5: KIỂM THỬ, HARDENING & BÀN GIAO (DELIVERY) - [HOÀN TẤT]
*Mục tiêu: Kiểm tra toàn diện, đảm bảo build thành công, không rò rỉ mã bảo mật.*

#### Danh sách công việc đã hoàn thành:
1. **Kiểm tra an toàn bảo mật:**
   - Đã kiểm tra `git status` và `.gitignore`, đảm bảo `.env` và `local.properties` không bị track.
2. **Unit Test & Build Verification:**
   - Đã cập nhật và chạy thành công bộ Unit Test Robolectric (`HrpNetworkAndAuthTest`, `ExampleRobolectricTest`, `ExampleUnitTest`) qua `./gradlew testDebugUnitTest` (100% PASS).
   - Biên dịch và đóng gói thành công APK Debug (`./gradlew assembleDebug`).

---

## 4. HƯỚNG DẪN CHI TIẾT THEO TỪNG TỆP TIN (FILE-BY-FILE IMPLEMENTATION GUIDE)

| Tệp tin | Vị trí | Trách nhiệm & Thay đổi cần thực hiện |
| :--- | :--- | :--- |
| `Screen.kt` | `app/src/main/java/com/example/ui/navigation/` | Định nghĩa lại 5 screens: `HOME`, `JOBS`, `ATTENDANCE`, `FEEDBACK`, `REFERRAL`. |
| `MainActivity.kt` | `app/src/main/java/com/example/` | Cập nhật `BottomNavigationBar` hiển thị đúng 5 items và xử lý chuyển màn hình. |
| `HomeScreen.kt` | `app/src/main/java/com/example/ui/screens/` | Xóa `RoleSwitcherDialog`; thêm `VerificationStatusCard`; hiển thị ca làm việc & nút chấm công nhanh. |
| `JobBoardScreen.kt` | `app/src/main/java/com/example/ui/screens/` | Hiển thị tin tuyển dụng public; Modal ứng tuyển tự điền SĐT nếu đã login. |
| `AttendanceScreen.kt` | `app/src/main/java/com/example/ui/screens/` | Khóa nếu chưa verified; tích hợp GPS FusedLocation thật; 2 nút Vào Ca / Tan Ca; Lịch sử tháng. |
| `FeedbackScreen.kt` | `app/src/main/java/com/example/ui/screens/` | Gửi góp ý (check `phoneVerified`); Xem danh sách phản hồi từ HR. |
| `ReferralScreen.kt` | `app/src/main/java/com/example/ui/screens/` | Tải policy động; nếu disabled -> hiển thị "Chưa mở"; Form giới thiệu kèm consent checkbox; Lịch sử referral. |
| `WorkerVerificationScreen.kt` | `app/src/main/java/com/example/ui/screens/` | Form nộp hồ sơ công nhân: Họ tên, CCCD, Ngày sinh, Dự án (dropdown), Mã công nhân. |
| `LoginScreen.kt` / `RegisterScreen.kt` | `app/src/main/java/com/example/ui/screens/` | Đăng nhập SĐT/Pass; Đăng ký mới SĐT/Pass -> trạng thái `PENDING_OTP`. |
| `HrpApiService.kt` | `app/src/main/java/com/example/data/api/` | Định nghĩa 17 Retrofit interfaces theo đúng contract. |
| `ApiResponse.kt` & DTOs | `app/src/main/java/com/example/data/api/dto/` | Các data class cho Request/Response bọc trong `{ ok, data, error }`. |
| `HrpRepository.kt` | `app/src/main/java/com/example/data/repository/` | Xóa toàn bộ Mock Data; gọi Retrofit suspend functions; xử lý lỗi trả về `Result<T>`. |
| `LocationHelper.kt` | `app/src/main/java/com/example/util/` | Lấy GPS phần cứng qua FusedLocationProviderClient; chặn Mock GPS; kiểm tra accuracy. |
| `TokenManager.kt` | `app/src/main/java/com/example/data/local/` | Lưu trữ token và đồng bộ session cookie an toàn qua DataStore Preferences. |
