import com.fudn.Account;
import com.fudn.AccountService;
import com.fudn.AccountStatus;
import com.fudn.PasswordHasher;
import com.fudn.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountServiceTest {

    static final String USER = "alice";
    static final String EMAIL = "alice@example.com";
    static final String PASS = "Secret@123";
    static final String PHONE = "0912345678";
    static final LocalDate DOB = LocalDate.now().minusYears(20);

    AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService();
    }

    // ========================================================
    // @Nested Register
    // ========================================================
    @Nested
    @DisplayName("Tests for register()")
    class Register {

        @Test
        @DisplayName("register thành công: trả về SUCCESS và khởi tạo đầy đủ trạng thái tài khoản")
        void register_ValidAccount_ReturnsSuccessAndInitializesState() {
            // Arrange
            String inputEmail = "Alice@Example.COM";

            // Act
            ResultCode result = service.register(USER, inputEmail, PASS, PASS, DOB, PHONE);

            // Assert
            assertEquals(ResultCode.SUCCESS, result);
            Optional<Account> opt = service.findByUsername(USER);
            assertTrue(opt.isPresent());

            Account acc = opt.get();
            assertEquals("alice", acc.getUsername());
            assertEquals("alice@example.com", acc.getEmail(), "Email phải được lưu ở dạng lowercase");
            assertEquals(AccountStatus.ACTIVE, acc.getStatus());
            assertEquals(0, acc.getFailedAttempts());
            assertFalse(acc.isLocked());
            assertNotEquals(PASS, acc.getCurrentPasswordHash(), "Mật khẩu không được lưu ở dạng rõ");
            assertTrue(PasswordHasher.matches(acc.getSalt(), PASS, acc.getCurrentPasswordHash()));
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("AccountServiceTest#invalidRegisterInputs")
        @DisplayName("register trả về mã lỗi đúng theo từng quy tắc và thứ tự ưu tiên")
        void register_InvalidInputs_ReturnsExpectedResultCode(String desc, String u, String e, String p,
                                                             String c, LocalDate dob, String phone,
                                                             ResultCode expected) {
            // Act
            ResultCode result = service.register(u, e, p, c, dob, phone);

            // Assert
            assertEquals(expected, result);
            if (u != null && !u.isBlank()) {
                assertTrue(service.findByUsername(u).isEmpty(), "Đăng ký thất bại không được lưu tài khoản");
            }
        }

        @ParameterizedTest(name = "[{index}] username null/empty/blank: \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("register trả về INVALID_INPUT khi username null hoặc rỗng hoặc khoảng trắng")
        void register_BlankUsername_ReturnsInvalidInput(String username) {
            // Act
            ResultCode result = service.register(username, EMAIL, PASS, PASS, DOB, PHONE);

            // Assert
            assertEquals(ResultCode.INVALID_INPUT, result);
        }

        @ParameterizedTest(name = "[{index}] email null/empty/blank: \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("register trả về INVALID_INPUT khi email null hoặc rỗng hoặc khoảng trắng")
        void register_BlankEmail_ReturnsInvalidInput(String email) {
            // Act
            ResultCode result = service.register(USER, email, PASS, PASS, DOB, PHONE);

            // Assert
            assertEquals(ResultCode.INVALID_INPUT, result);
        }

        @ParameterizedTest(name = "[{index}] password null/empty/blank: \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("register trả về INVALID_INPUT khi password null hoặc rỗng hoặc khoảng trắng")
        void register_BlankPassword_ReturnsInvalidInput(String password) {
            // Act
            ResultCode result = service.register(USER, EMAIL, password, password, DOB, PHONE);

            // Assert
            assertEquals(ResultCode.INVALID_INPUT, result);
        }

        @ParameterizedTest(name = "[{index}] trùng username không phân biệt hoa thường: \"{0}\"")
        @ValueSource(strings = {"alice", "ALICE", "Alice", "aLiCe"})
        @DisplayName("register trả về DUPLICATE_USERNAME khi trùng username hoa/thường")
        void register_DuplicateUsernameCaseInsensitive_ReturnsDuplicateUsername(String dupUser) {
            // Arrange
            service.register(USER, EMAIL, PASS, PASS, DOB, PHONE);

            // Act
            ResultCode result = service.register(dupUser, "other@domain.com", PASS, PASS, DOB, PHONE);

            // Assert
            assertEquals(ResultCode.DUPLICATE_USERNAME, result);
        }

        @ParameterizedTest(name = "[{index}] trùng email không phân biệt hoa thường: \"{0}\"")
        @ValueSource(strings = {"alice@example.com", "ALICE@EXAMPLE.COM", "Alice@Example.Com"})
        @DisplayName("register trả về DUPLICATE_EMAIL khi trùng email hoa/thường")
        void register_DuplicateEmailCaseInsensitive_ReturnsDuplicateEmail(String dupEmail) {
            // Arrange
            service.register(USER, EMAIL, PASS, PASS, DOB, PHONE);

            // Act
            ResultCode result = service.register("bob_01", dupEmail, PASS, PASS, DOB, PHONE);

            // Assert
            assertEquals(ResultCode.DUPLICATE_EMAIL, result);
        }

        @ParameterizedTest(name = "[{index}] today - {0} năm + {1} ngày -> {2}")
        @CsvSource({
                "18, 0, SUCCESS",
                "18, 1, UNDERAGE",
                "0, 1, INVALID_INPUT"
        })
        @DisplayName("register kiểm tra biên tuổi tương đối so với ngày hiện tại")
        void register_AgeBoundary(int yearsAgo, int plusDays, ResultCode expected) {
            // Arrange
            LocalDate dob = LocalDate.now().minusYears(yearsAgo).plusDays(plusDays);

            // Act
            ResultCode result = service.register(USER, EMAIL, PASS, PASS, dob, null);

            // Assert
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("register thành công khi phone là null hoặc rỗng (phone là tùy chọn)")
        void register_OptionalPhoneNullOrEmpty_ReturnsSuccess() {
            // Arrange & Act
            ResultCode resNullPhone = service.register("user_null", "null_phone@example.com", PASS, PASS, DOB, null);
            ResultCode resEmptyPhone = service.register("user_empty", "empty_phone@example.com", PASS, PASS, DOB, "");

            // Assert
            assertEquals(ResultCode.SUCCESS, resNullPhone);
            assertEquals(ResultCode.SUCCESS, resEmptyPhone);
        }

        @Test
        @DisplayName("register ưu tiên DUPLICATE_USERNAME trước DUPLICATE_EMAIL khi trùng cả hai")
        void register_DuplicateUsernameAndEmail_ReturnsDuplicateUsernameFirst() {
            // Arrange
            service.register(USER, EMAIL, PASS, PASS, DOB, PHONE);

            // Act: Cố tình đăng ký lại cả username và email đã tồn tại
            ResultCode result = service.register(USER, EMAIL, PASS, PASS, DOB, PHONE);

            // Assert: BR-REG-03 (DUPLICATE_USERNAME) phải được kiểm tra trước BR-REG-05 (DUPLICATE_EMAIL)
            assertEquals(ResultCode.DUPLICATE_USERNAME, result);
        }
    }

    // ========================================================
    // @Nested Login
    // ========================================================
    @Nested
    @DisplayName("Tests for login()")
    class Login {

        static final String WRONG = "Wrong@999";

        @BeforeEach
        void setUpUser() {
            service.register(USER, EMAIL, PASS, PASS, DOB, PHONE);
        }

        private Account account() {
            return service.findByUsername(USER).orElseThrow();
        }

        private void failLogin(int times) {
            for (int i = 0; i < times; i++) {
                service.login(USER, WRONG);
            }
        }

        @Test
        @DisplayName("Rule 6: Đăng nhập đúng mật khẩu -> SUCCESS, failedAttempts == 0")
        void login_CorrectPassword_ReturnsSuccessAndResetsAttempts() {
            // Act
            ResultCode result = service.login(USER, PASS);

            // Assert
            assertEquals(ResultCode.SUCCESS, result);
            assertEquals(0, account().getFailedAttempts());
            assertFalse(service.isLocked(USER));
        }

        @Test
        @DisplayName("Rule 1: User không tồn tại -> INVALID_CREDENTIALS")
        void login_NonExistentUser_ReturnsInvalidCredentials() {
            // Act
            ResultCode result = service.login("ghost_user", PASS);

            // Assert
            assertEquals(ResultCode.INVALID_CREDENTIALS, result);
        }

        @ParameterizedTest(name = "[{index}] tài khoản bị vô hiệu hóa với pass: {0}")
        @ValueSource(strings = {PASS, WRONG})
        @DisplayName("Rule 2: Tài khoản bị DISABLED nhập pass đúng hay sai đều -> ACCOUNT_DISABLED")
        void login_DisabledAccount_ReturnsAccountDisabled(String passwordToTry) {
            // Arrange
            service.disableAccount(USER);

            // Act
            ResultCode result = service.login(USER, passwordToTry);

            // Assert
            assertEquals(ResultCode.ACCOUNT_DISABLED, result);
            assertEquals(AccountStatus.DISABLED, account().getStatus());
        }

        @ParameterizedTest(name = "[{index}] sai lần thứ {0} -> INVALID_CREDENTIALS, failedAttempts={0}, locked=false")
        @ValueSource(ints = {1, 2, 3, 4})
        @DisplayName("Rule 4: Sai mật khẩu từ 1 đến 4 lần -> INVALID_CREDENTIALS và tăng bộ đếm")
        void login_WrongPasswordUpTo4Times_IncrementsCounter(int attempts) {
            // Arrange & Act
            failLogin(attempts);

            // Assert
            assertEquals(attempts, account().getFailedAttempts());
            assertFalse(service.isLocked(USER));
        }

        @Test
        @DisplayName("Rule 5: Sai mật khẩu lần thứ 5 -> ACCOUNT_LOCKED và khóa tài khoản")
        void login_WrongPassword5thTime_LocksAccount() {
            // Arrange
            failLogin(4);

            // Act
            ResultCode result = service.login(USER, WRONG);

            // Assert
            assertEquals(ResultCode.ACCOUNT_LOCKED, result);
            assertTrue(service.isLocked(USER));
            assertEquals(5, account().getFailedAttempts());
        }

        @ParameterizedTest(name = "[{index}] đang khóa nhập pass: {0} -> ACCOUNT_LOCKED, không tăng đếm")
        @ValueSource(strings = {PASS, WRONG})
        @DisplayName("Rule 3: Đang bị khóa nhập pass đúng hay sai đều ACCOUNT_LOCKED và không tăng bộ đếm")
        void login_AlreadyLocked_ReturnsAccountLockedWithoutIncrementingCounter(String passwordToTry) {
            // Arrange: Khóa tài khoản bằng 5 lần sai
            failLogin(5);
            int attemptsBefore = account().getFailedAttempts();

            // Act
            ResultCode result = service.login(USER, passwordToTry);

            // Assert
            assertEquals(ResultCode.ACCOUNT_LOCKED, result);
            assertEquals(attemptsBefore, account().getFailedAttempts(), "Không được tăng bộ đếm khi đang bị khóa");
            assertTrue(service.isLocked(USER));
        }

        @ParameterizedTest(name = "[{index}] {0} lần sai rồi nhập pass đúng -> {1}, locked={2}")
        @CsvSource({
                "4, SUCCESS,        false",
                "5, ACCOUNT_LOCKED, true",
                "6, ACCOUNT_LOCKED, true"
        })
        @DisplayName("Biên số lần đăng nhập sai: 4 lần rồi đúng -> SUCCESS; 5 lần rồi đúng -> ACCOUNT_LOCKED")
        void login_CorrectPasswordAfterNFailures(int failures, ResultCode expected, boolean locked) {
            // Arrange
            failLogin(failures);

            // Act
            ResultCode result = service.login(USER, PASS);

            // Assert
            assertEquals(expected, result);
            assertEquals(locked, service.isLocked(USER));
        }

        @Test
        @DisplayName("Mở khóa admin: sau khi mở khóa có thể đăng nhập lại và bộ đếm bắt đầu lại từ 0")
        void login_AfterAdminUnlock_CounterRestartsAndCanLogin() {
            // Arrange
            failLogin(5);
            assertTrue(service.isLocked(USER));

            // Act: Admin mở khóa
            assertEquals(ResultCode.SUCCESS, service.unlockAccount(USER));

            // Assert
            assertFalse(service.isLocked(USER));
            assertEquals(ResultCode.INVALID_CREDENTIALS, service.login(USER, WRONG));
            assertEquals(1, account().getFailedAttempts(), "Bộ đếm thất bại phải bắt đầu lại từ 0");
            assertEquals(ResultCode.SUCCESS, service.login(USER, PASS));
            assertEquals(0, account().getFailedAttempts());
        }

        @Test
        @DisplayName("Username không phân biệt hoa thường khi đăng nhập")
        void login_UsernameCaseInsensitive_ReturnsSuccess() {
            // Act
            ResultCode result = service.login("ALICE", PASS);

            // Assert
            assertEquals(ResultCode.SUCCESS, result);
        }

        @Test
        @DisplayName("Password phân biệt hoa thường khi đăng nhập")
        void login_PasswordCaseSensitive_ReturnsInvalidCredentials() {
            // Act
            ResultCode result = service.login(USER, PASS.toLowerCase());

            // Assert
            assertEquals(ResultCode.INVALID_CREDENTIALS, result);
            assertEquals(1, account().getFailedAttempts());
        }

        @ParameterizedTest(name = "[{index}] username null/empty/blank: \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("login trả về INVALID_INPUT khi username null, rỗng hoặc khoảng trắng")
        void login_BlankUsername_ReturnsInvalidInput(String u) {
            // Act
            ResultCode result = service.login(u, PASS);

            // Assert
            assertEquals(ResultCode.INVALID_INPUT, result);
        }

        @ParameterizedTest(name = "[{index}] password null/empty/blank: \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("login trả về INVALID_INPUT khi password null, rỗng hoặc khoảng trắng")
        void login_BlankPassword_ReturnsInvalidInput(String p) {
            // Act
            ResultCode result = service.login(USER, p);

            // Assert
            assertEquals(ResultCode.INVALID_INPUT, result);
        }
    }

    // ========================================================
    // @Nested Admin
    // ========================================================
    @Nested
    @DisplayName("Tests for Admin Operations")
    class Admin {

        @ParameterizedTest(name = "[{index}] user không tồn tại hoặc blank: \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "non_existing"})
        @DisplayName("disableAccount và unlockAccount trả về USER_NOT_FOUND với user không tồn tại hoặc blank")
        void adminOperations_UserNotFound_ReturnsUserNotFound(String targetUser) {
            assertEquals(ResultCode.USER_NOT_FOUND, service.disableAccount(targetUser));
            assertEquals(ResultCode.USER_NOT_FOUND, service.unlockAccount(targetUser));
        }

        @ParameterizedTest(name = "[{index}] isLocked trả về false với user không tồn tại hoặc blank: \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "non_existing"})
        @DisplayName("isLocked trả về false với user không tồn tại hoặc blank/null")
        void isLocked_UserNotFoundOrBlank_ReturnsFalse(String targetUser) {
            assertFalse(service.isLocked(targetUser));
        }
    }

    static Stream<Arguments> invalidRegisterInputs() {
        return Stream.of(
                // BR-REG-01
                Arguments.of("REG-01: confirm password null", USER, EMAIL, PASS, null, DOB, PHONE, ResultCode.INVALID_INPUT),
                Arguments.of("REG-01: dob null", USER, EMAIL, PASS, PASS, null, PHONE, ResultCode.INVALID_INPUT),
                Arguments.of("REG-01: dob ở tương lai", USER, EMAIL, PASS, PASS, LocalDate.now().plusDays(2), PHONE, ResultCode.INVALID_INPUT),

                // BR-REG-02
                Arguments.of("REG-02: username sai format", "1alice", EMAIL, PASS, PASS, DOB, PHONE, ResultCode.INVALID_USERNAME),

                // BR-REG-04
                Arguments.of("REG-04: email sai format", USER, "bad-email", PASS, PASS, DOB, PHONE, ResultCode.INVALID_EMAIL),

                // BR-REG-06
                Arguments.of("REG-06: mật khẩu yếu", USER, EMAIL, "weakpass", "weakpass", DOB, PHONE, ResultCode.WEAK_PASSWORD),

                // BR-REG-07
                Arguments.of("REG-07: mật khẩu xác nhận không khớp", USER, EMAIL, PASS, "Different@123", DOB, PHONE, ResultCode.PASSWORD_MISMATCH),

                // BR-REG-08
                Arguments.of("REG-08: chưa đủ 18 tuổi", USER, EMAIL, PASS, PASS, LocalDate.now().minusYears(17), PHONE, ResultCode.UNDERAGE),

                // BR-REG-09
                Arguments.of("REG-09: số điện thoại sai đầu số/độ dài", USER, EMAIL, PASS, PASS, DOB, "0123456789", ResultCode.INVALID_PHONE),
                Arguments.of("REG-09: số điện thoại chứa khoảng trắng", USER, EMAIL, PASS, PASS, DOB, "   ", ResultCode.INVALID_PHONE),

                // THỨ TỰ ƯU TIÊN (Priority Order):
                Arguments.of("Priority 1: username sai + email sai -> INVALID_USERNAME", "1alice", "bad-email", PASS, PASS, DOB, PHONE, ResultCode.INVALID_USERNAME),
                Arguments.of("Priority 2: email sai + mk yếu -> INVALID_EMAIL", USER, "bad-email", "weak", "weak", DOB, PHONE, ResultCode.INVALID_EMAIL),
                Arguments.of("Priority 3: mk yếu + confirm lệch -> WEAK_PASSWORD", USER, EMAIL, "weak", "different", DOB, PHONE, ResultCode.WEAK_PASSWORD),
                Arguments.of("Priority 4: confirm lệch + chưa đủ tuổi -> PASSWORD_MISMATCH", USER, EMAIL, PASS, "Secret@999", LocalDate.now().minusYears(16), PHONE, ResultCode.PASSWORD_MISMATCH),
                Arguments.of("Priority 5: chưa đủ tuổi + phone sai -> UNDERAGE", USER, EMAIL, PASS, PASS, LocalDate.now().minusYears(15), "0123456789", ResultCode.UNDERAGE)
        );
    }
}