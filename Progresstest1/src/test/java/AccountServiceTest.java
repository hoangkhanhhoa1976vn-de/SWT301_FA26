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