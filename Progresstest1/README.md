# SWT301 – ProgressTest 1: Unit Testing với JUnit 5 & JaCoCo

- **Môn học:** SWT301 – Software Testing
- **Sinh viên:** Hoàng Khánh Hòa
- **MSSV:** DE190162
- **Dự án:** Module Account Management (`com.fudn`)

---

## 1. Hướng dẫn chạy kiểm thử và sinh báo cáo

### 1.1 Chạy toàn bộ test
```bash
mvn clean test
```

### 1.2 Sinh báo cáo JaCoCo
```bash
mvn test jacoco:report
```
Báo cáo HTML chi tiết được lưu tại: `target/site/jacoco/index.html`

---

## 2. Thống kê kết quả kiểm thử

- **Tổng số test methods / cases:** 138 lượt chạy (invocations)
- **Số lượng Parameterized Test:** 14 phương thức `@ParameterizedTest` với đa dạng nguồn dữ liệu (`@ValueSource`, `@CsvSource`, `@MethodSource`, `@NullAndEmptySource`)
- **Kết quả thực thi:**
  - Tests run: **138**
  - Failures: **0**
  - Errors: **0**
  - Skipped: **0**
  - BUILD SUCCESS

### Bảng độ phủ JaCoCo (Coverage Report)

| Class | Line Coverage | Branch Coverage | Trạng thái |
|---|---|---|---|
| `AccountValidator` | **100%** (34/34 lines) | **100%** (52/52 branches) | Đạt chuẩn vượt mức |
| `AccountService` | **94.6%** (70/74 lines) | **97.0%** (64/66 branches) | Đạt chuẩn vượt mức |
| `Account` | **81.1%** (30/37 lines) | - | Đạt chuẩn |
| `PasswordHasher` | **71.4%** (10/14 lines) | **50.0%** (5/10 branches) | Đạt chuẩn |
| **Toàn bộ dự án** | **~89.7%** (>= 80%) | **~93.1%** (>= 70%) | **VƯỢT CHỈ TIÊU ĐỀ BÀI** |

---

## 3. Kiểm thử đột biến (Mutation Testing) – 3 Lỗi giả lập

| # | Vị trí chèn lỗi | Lỗi chèn | Test bắt được (Fail) | Đã hoàn tác |
|---|---|---|---|---|
| **M1** | `AccountService.login()` | Sửa `>= MAX_FAILED_ATTEMPTS` thành `> MAX_FAILED_ATTEMPTS` (khóa sai ở lần 6 thay vì lần 5) | `Login.login_WrongPassword5thTime_LocksAccount`<br>`Login.login_CorrectPasswordAfterNFailures[2]` | [x] |
| **M2** | `AccountService.login()` | Bỏ qua kiểm tra `if (acc.isLocked())` ở đầu phương thức `login()` | `Login.login_AlreadyLocked_ReturnsAccountLockedWithoutIncrementingCounter`<br>`Login.login_CorrectPasswordAfterNFailures[2, 3]` | [x] |
| **M3** | `AccountValidator.isValidUsername()` | Sửa regex `{4,19}` thành `{3,19}` (cho phép username 4 ký tự thay vì tối thiểu 5) | `AccountValidatorTest.isValidUsername_BoundaryLength_ReturnsExpected`<br>`AccountValidatorTest.isValidUsername_InvalidUsernames_ReturnsFalse` | [x] |

---

## 4. Ma trận truy vết rút gọn (Traceability Matrix)

| Mã quy tắc | Yêu cầu nghiệp vụ | Tên phương thức Test đại diện |
|---|---|---|
| **BR-REG-01** | Input null / rỗng / sai kiểu ngày | `Register.register_InvalidInputs_ReturnsExpectedErrorCode`<br>`Register.register_NullUsernameOrPassword_ThrowsIllegalArgumentException` |
| **BR-REG-02** | Định dạng username (5-20 ký tự, bắt đầu chữ cái) | `AccountValidatorTest.isValidUsername_*`<br>`Register.register_InvalidInputs_ReturnsExpectedErrorCode` |
| **BR-REG-03** | Username trùng lặp (không phân biệt hoa thường) | `Register.register_DuplicateUsernameCaseInsensitive_ReturnsDuplicateUsername`<br>`Register.register_DuplicateUsernameAndEmail_ReturnsDuplicateUsernameFirst` |
| **BR-REG-04** | Định dạng email (RFC 5322 giản lược, max 100 ký tự) | `AccountValidatorTest.isValidEmail_*`<br>`Register.register_InvalidInputs_ReturnsExpectedErrorCode` |
| **BR-REG-05** | Email trùng lặp (không phân biệt hoa thường) | `Register.register_DuplicateEmailCaseInsensitive_ReturnsDuplicateEmail` |
| **BR-REG-06** | Mật khẩu mạnh (>= 8 ký tự, hoa, thường, số, ký tự đặc biệt) | `AccountValidatorTest.isValidPassword_*`<br>`Register.register_InvalidInputs_ReturnsExpectedErrorCode` |
| **BR-REG-07** | Xác nhận mật khẩu khớp | `Register.register_InvalidInputs_ReturnsExpectedErrorCode` |
| **BR-REG-08** | Độ tuổi >= 18 tính đến ngày đăng ký | `AccountValidatorTest.calculateAge_*`<br>`Register.register_AgeBoundaries_ReturnsExpectedResult` |
| **BR-REG-09** | Số điện thoại VN hợp lệ (10 chữ số, đầu số hợp lệ) | `AccountValidatorTest.isValidPhone_*`<br>`Register.register_InvalidInputs_ReturnsExpectedErrorCode` |
| **BR-REG-10** | Thứ tự kiểm tra hợp lệ khi có nhiều lỗi đồng thời | `Register.register_InvalidInputs_ReturnsExpectedErrorCode` (5 test priority) |
| **BR-LOG-01** | Đăng nhập tài khoản không tồn tại -> `INVALID_CREDENTIALS` | `Login.login_NonExistentUser_ReturnsInvalidCredentials` |
| **BR-LOG-02** | Đăng nhập tài khoản `DISABLED` -> `ACCOUNT_DISABLED` | `Login.login_DisabledAccount_ReturnsAccountDisabled` |
| **BR-LOG-03** | Đăng nhập tài khoản đang bị khóa -> `ACCOUNT_LOCKED`, không tăng đếm | `Login.login_AlreadyLocked_ReturnsAccountLockedWithoutIncrementingCounter` |
| **BR-LOG-04** | Sai mật khẩu 1..4 lần -> tăng đếm, chưa khóa | `Login.login_WrongPasswordUpTo4Times_IncrementsCounter` |
| **BR-LOG-05** | Sai mật khẩu lần thứ 5 -> khóa tài khoản (`ACCOUNT_LOCKED`) | `Login.login_WrongPassword5thTime_LocksAccount`<br>`Login.login_CorrectPasswordAfterNFailures` |
| **BR-LOG-06** | Đăng nhập thành công -> `SUCCESS`, reset bộ đếm thất bại về 0 | `Login.login_ValidCredentials_ReturnsSuccessAndResetsCounter` |
| **ADMIN** | Admin mở khóa tài khoản, reset bộ đếm về 0 | `Login.login_AfterAdminUnlock_CounterRestartsAndCanLogin`<br>`Admin.adminOperations_UserNotFound_ReturnsUserNotFound` |

---

## 5. Checklist tự đánh giá trước khi nộp

### A. Mã production
- [x] **A1** `mvn clean compile` thành công
- [x] **A2** `AccountValidator` đủ 5 hàm, null trả `false`, không ném exception ngoài ý muốn
- [x] **A3** Mật khẩu băm SHA-256 + salt riêng 16 bytes ngẫu nhiên, không lưu bản rõ
- [x] **A4** `register()` đủ BR-REG-01..10, đúng thứ tự ưu tiên
- [x] **A5** `login()`: sai 5 lần thì khóa; đang khóa không tăng bộ đếm; thành công đặt bộ đếm về 0
- [x] **A6** `unlockAccount()` mở khóa và đặt `failedAttempts = 0`
- [x] **A7** Username/email không phân biệt hoa thường (`toLowerCase(Locale.ROOT)`), mật khẩu phân biệt
- [x] **A8** Không dùng `Clock`; không `System.out`, không biến static giữ trạng thái

### B. Mã test
- [x] **B1** Đạt 138 test invocations (vượt xa yêu cầu >= 20 methods, >= 12 parameterized tests, >= 60 invocations)
- [x] **B2** Dùng đủ `@ValueSource`, `@NullAndEmptySource`, `@CsvSource`, `@MethodSource`
- [x] **B3** Kiểm thử biên (BVA): username 4/5/20/21, mật khẩu 7/8/32/33, email 100/101, tuổi 17/18
- [x] **B4** Biên số lần đăng nhập sai 4/5 và test mở khóa admin
- [x] **B5** Đầy đủ test thứ tự ưu tiên trong `register()`
- [x] **B6** Cấu trúc `@Nested` rõ ràng, `@BeforeEach` tạo service mới cách ly hoàn toàn
- [x] **B7** Assert chuẩn theo trạng thái và đối tượng, không `assertTrue(true)`, không `Thread.sleep`
- [x] **B8** Tên test chuẩn `method_TinhHuong_KetQua`, cấu trúc AAA (Arrange - Act - Assert)

### C. Chất lượng và nộp bài
- [x] **C1** `mvn clean test`: 0 failures / 0 errors / 0 skipped (138/138 Passed)
- [x] **C2** JaCoCo Line: ~89.7% (>= 80%), Branch: ~93.1% (>= 70%)
- [x] **C3** Đã thử nghiệm và ghi nhận 3 lỗi giả lập (Mutation Testing)
- [x] **C4** Lịch sử git đầy đủ các bước commit theo Conventional Commits
- [x] **C5** Đóng gói file zip đúng cấu trúc, không chứa `target/` hay file IDE rác