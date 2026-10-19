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

### GIAI ĐOẠN 1: CHUẨN HÓA DTO & NETWORK LAYER (SAU KHI FREEZE CONTRACT)
*Mục tiêu: Xây dựng các Data Transfer Objects khớp chính xác 100% với JSON schema của Backend.*

#### Danh sách công việc:
1. **Tạo Model Response Envelope (`ApiResponse.kt`):**
   ```kotlin
   data class ApiResponse<T>(
       val ok: Boolean,
       val data: T? = null,
       val error: ApiError? = null
   )

   data class ApiError(
       val code: String,
       val message: String,
       val details: Any? = null
   )
   ```
2. **Khai báo DTOs (`com.example.data.api.dto`):**
   - **Auth:** `LoginRequest`, `LoginResponse`, `RegisterRequest`, `RegisterResponse`, `OtpSendRequest`, `OtpVerifyRequest`.
   - **Verification:** `WorkerVerificationRequest`, `WorkerVerificationResponse`, `VerificationStatusResponse`.
   - **Jobs:** `JobItemResponse`, `JobsListResponse`, `JobApplyRequest`.
   - **Attendance:** `CheckInRequest` (chứa `action: CHECK_IN | CHECK_OUT`, `workDate`, `gpsLatitude`, `gpsLongitude`, `gpsAccuracyMeters`, `capturedAt`, `idempotencyKey`), `AttendanceHistoryResponse`.
   - **Feedback:** `FeedbackSubmitRequest`, `FeedbackItemResponse`.
   - **Referral:** `ReferralProgramResponse` (chứa `isEnabled`, `policyVersion`, `title`, `eligibilityCriteria`, `successEventDescription`, `rewardPolicyDescription`, `termsAndConditions`), `ReferralSubmitRequest`, `ReferralItemResponse`.
3. **Cập nhật Retrofit Interface (`HrpApiService.kt`):**
   Khai báo đủ 17 hàm API theo đúng bảng 14.3 trong `web-android-contract.md`.

---

### GIAI ĐOẠN 2: LUỒNG AUTH, OTP & XÁC THỰC CÔNG NHÂN HRP
*Mục tiêu: Đảm bảo đăng ký, đăng nhập và quy trình xác thực danh tính diễn ra thông suốt, an toàn.*

#### Danh sách công việc:
1. **Màn hình Đăng nhập (`LoginScreen.kt`):**
   - Input: Số điện thoại + Mật khẩu.
   - Xử lý phản hồi: Lưu Bearer token vào `TokenManager`. Đồng thời lưu thông tin `phoneVerified` và `verificationStatus`.
2. **Màn hình Đăng ký (`RegisterScreen.kt`):**
   - Input: Số điện thoại + Mật khẩu + Họ tên.
   - Gọi `POST /api/auth/register`. Tài khoản tạo xong ở trạng thái `phoneVerified = false`.
   - Chuyển tiếp sang Hộp thoại OTP.
3. **Hộp thoại OTP (`OtpVerificationDialog.kt`):**
   - Nhập mã 6 số -> gọi `POST /api/auth/phone-otp/verify`.
   - Xử lý mã lỗi `503 OTP_SERVICE_UNAVAILABLE`: Hiển thị thông báo *"Hệ thống gửi mã OTP đang bảo trì, vui lòng thử lại sau"*. Tuyệt đối không tự động gán `verified = true`.
4. **Màn hình Nộp hồ sơ Xác thực Công nhân (`WorkerVerificationScreen.kt`):**
   - Dropdown chọn Dự án/Nhà máy: Lấy danh sách từ `GET /api/public/projects`.
   - Input: Họ tên, Số CCCD, Ngày sinh, Mã công nhân tại dự án.
   - Bấm gửi -> gọi `POST /api/worker-verifications`.
   - Xóa dữ liệu nhạy cảm (CCCD, ngày sinh) khỏi form ngay sau khi gửi thành công.
5. **Thẻ trạng thái xác thực (`VerificationStatusCard.kt` trên HomeScreen):**
   - Hiển thị theo 4 trạng thái:
     - `PENDING_REVIEW`: Màu vàng, thông báo *"Hồ sơ đang chờ HR đối soát với danh sách nhà máy"*.
     - `NEEDS_INFO`: Màu cam, hiển thị ghi chú của HR và nút *"Cập nhật lại hồ sơ"*.
     - `APPROVED`: Màu xanh lá, huy hiệu *"Đã xác thực công nhân HRP"* (Mở khóa Chấm công).
     - `REJECTED`: Màu đỏ, thông báo từ chối kèm lý do.

---

### GIAI ĐOẠN 3: VIỆC LÀM PUBLIC & CHẤM CÔNG GPS PHẦN CỨNG THẬT
*Mục tiêu: Người tìm việc dễ dàng ứng tuyển; Công nhân đã duyệt chấm công chính xác bằng GPS.*

#### Danh sách công việc:
1. **Mục Việc làm (`JobBoardScreen.kt`):**
   - Gọi `GET /api/jobs`: Hiển thị danh sách tin tuyển dụng, phân trang, tìm kiếm vị trí/địa điểm.
   - Nút *"Ứng tuyển nhanh"*: Mở modal ứng tuyển.
   - Nếu chưa đăng nhập: Yêu cầu nhập Họ tên + Số điện thoại.
   - Nếu đã đăng nhập: Tự động điền Số điện thoại từ session tài khoản, khóa ô nhập để chống gửi sai lệch.
   - Gửi hồ sơ lên `POST /api/public/jobs/{slug}/applications`.
2. **Mục Chấm công (`AttendanceScreen.kt`):**
   - **Kiểm tra phân quyền (Gating Check):**
     - Nếu `verificationStatus != "APPROVED"` -> Hiển thị màn hình khóa kèm giải thích: *"Bạn cần hoàn thành Xác thực công nhân HRP để mở khóa tính năng chấm công"*.
   - **Xử lý GPS thực tế (`LocationHelper.kt`):**
     - Xin quyền `ACCESS_FINE_LOCATION` tại runtime.
     - Kiểm tra thiết bị đã bật GPS chưa (nếu chưa, mở dialog nhắc bật Location).
     - Gọi `fusedLocationClient.getCurrentLocation()` với `PRIORITY_HIGH_ACCURACY`.
     - Kiểm tra `location.isMock`: Nếu phát hiện giả lập vị trí trên bản thật -> Chặn ngay và báo lỗi.
     - Lấy tọa độ `latitude`, `longitude`, `accuracy`. Nếu `accuracy > 50m` -> cảnh báo sóng yếu, yêu cầu ra chỗ thoáng.
   - **Gửi sự kiện chấm công:**
     - Nút *"Vào Ca"* -> gửi `{ action: "CHECK_IN", ... }`.
     - Nút *"Tan Ca"* -> gửi `{ action: "CHECK_OUT", ... }`.
     - Lịch sử công nhật -> gọi `GET /api/worker/attendance?month=...` hiển thị thẻ chấm công từng ngày.

---

### GIAI ĐOẠN 4: GÓP Ý & GIỚI THIỆU NGƯỜI LÀM (DYNAMIC REFERRAL)
*Mục tiêu: Đóng góp ý kiến và triển khai chính sách giới thiệu người làm tuân thủ chính sách Owner duyệt.*

#### Danh sách công việc:
1. **Mục Góp ý (`FeedbackScreen.kt`):**
   - Kiểm tra điều kiện: Yêu cầu số điện thoại đã xác thực OTP (`phoneVerified == true`).
   - Danh mục: Môi trường làm việc, Tiền lương & Chế độ, Đề xuất cải tiến, Khiếu nại, Khác.
   - Gửi văn bản góp ý lên `POST /api/feedback`.
   - Tab Lịch sử: Gọi `GET /api/feedback/mine` để xem danh sách phản ánh và câu trả lời từ HR/Ban Quản lý.
2. **Mục Giới thiệu người làm (`ReferralScreen.kt`):**
   - **Tải chính sách từ backend (`GET /api/referrals/program`):**
     - Nếu `isEnabled == false` hoặc API trả về 404/chưa có chính sách:
       Hiển thị thẻ trạng thái **“Chương trình giới thiệu người làm hiện chưa mở”**, nêu lý do chính sách đang được hoàn thiện. Vô hiệu hóa form nhập liệu.
     - Nếu `isEnabled == true`:
       Render toàn bộ nội dung chính sách lấy từ API:
       - *Điều kiện tham gia* (`eligibilityCriteria`).
       - *Sự kiện được tính là thành công* (`successEventDescription`).
       - *Chính sách & mức hoa hồng* (`rewardPolicyDescription`).
       - *Điều khoản & quy trình chi trả* (`termsAndConditions`).
   - **Form gửi giới thiệu:**
     - Họ tên người được giới thiệu.
     - Số điện thoại người được giới thiệu (validate định dạng SĐT Việt Nam).
     - Vị trí / Khu vực quan tâm.
     - **Checkbox bắt buộc:** *"Tôi xác nhận người này đã đồng ý chia sẻ thông tin cá nhân để HRP liên hệ cơ hội việc làm"*. Nút gửi chỉ sáng lên khi đã tích chọn.
     - Gửi lên `POST /api/referrals` kèm `idempotencyKey`.
   - **Danh sách giới thiệu của tôi (`GET /api/referrals/mine`):**
     - Hiển thị danh sách các lượt giới thiệu: Tên (che một phần), Số điện thoại (dạng `0987***321`), Thời gian gửi.
     - Trạng thái giới thiệu: `Đã ghi nhận`, `Đang đối soát`, `Đủ điều kiện`, `Đã duyệt quyền lợi`, `Đã chi trả`, `Từ chối`.
     - Tuyệt đối không hiển thị chi tiết hồ sơ hay lịch sử tuyển dụng của ứng viên.

---

### GIAI ĐOẠN 5: KIỂM THỬ, HARDENING & BÀN GIAO (DELIVERY)
*Mục tiêu: Kiểm tra toàn diện, đảm bảo build thành công, không rò rỉ mã bảo mật.*

#### Danh sách công việc:
1. **Kiểm tra an toàn bảo mật:**
   - Chạy lệnh `git status` và `git check-ignore` đảm bảo không có file `.env`, Keystore hoặc file log nào lọt vào git.
   - Kiểm tra build release: Đảm bảo HTTP Logging đã tắt hoàn toàn body/token.
2. **Build Verification:**
   - Chạy lệnh: `./gradlew assembleDebug` và `./gradlew assembleRelease`.
   - Đảm bảo mã nguồn compile sạch lỗi, không còn cảnh báo nghiêm trọng.
3. **Biên bản bàn giao chuẩn (Handover Report):**
   - Commit Git với thông điệp chuẩn mực.
   - Ghi lại commit SHA mới nhất.
   - Đính kèm bảng log Request / Response mẫu đã kiểm thử.
   - Nêu rõ các phần còn phụ thuộc backend (nếu có).

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
