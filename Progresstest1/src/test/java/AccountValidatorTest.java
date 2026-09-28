import com.fudn.AccountValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountValidatorTest {

    // ==================== USERNAME TESTS ====================

    @ParameterizedTest(name = "[{index}] username hợp lệ: {0}")
    @ValueSource(strings = {"alice", "Alice_01", "Z____"})
    @DisplayName("isValidUsername trả về true với username hợp lệ")
    void isValidUsername_ValidUsernames_ReturnsTrue(String username) {
        // Arrange & Act
        boolean actual = AccountValidator.isValidUsername(username);

        // Assert
        assertTrue(actual);
    }

    @ParameterizedTest(name = "[{index}] username không hợp lệ: \"{0}\"")
    @ValueSource(strings = {"ab_1", "1alice", "_alice", "ali ce", "alice!", "alice-01"})
    @DisplayName("isValidUsername trả về false với format hoặc ký tự không hợp lệ")
    void isValidUsername_InvalidUsernames_ReturnsFalse(String username) {
        // Arrange & Act
        boolean actual = AccountValidator.isValidUsername(username);

        // Assert
        assertFalse(actual);
    }

    @ParameterizedTest(name = "[{index}] username null/empty/blank: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("isValidUsername trả về false với username null, rỗng hoặc khoảng trắng")
    void isValidUsername_NullAndEmptyAndBlank_ReturnsFalse(String username) {
        // Arrange & Act
        boolean actual = AccountValidator.isValidUsername(username);

        // Assert
        assertFalse(actual);
    }

    @ParameterizedTest(name = "[{index}] username độ dài {0} -> {1}")
    @MethodSource("usernameLengths")
    @DisplayName("isValidUsername kiểm tra giá trị biên độ dài 4, 5, 6, 19, 20, 21")
    void isValidUsername_BoundaryLength_ReturnsExpected(int length, boolean expected) {
        // Arrange
        String username = "a".repeat(length);

        // Act
        boolean actual = AccountValidator.isValidUsername(username);

        // Assert
        assertEquals(expected, actual);
    }

    static Stream<Arguments> usernameLengths() {
        return Stream.of(
                Arguments.of(4, false),
                Arguments.of(5, true),
                Arguments.of(6, true),
                Arguments.of(19, true),
                Arguments.of(20, true),
                Arguments.of(21, false)
        );
    }

    // ==================== EMAIL TESTS ====================

    @ParameterizedTest(name = "[{index}] email \"{0}\" -> {1}")
    @CsvSource({
            "alice@example.com, true",
            "alice.smith@sub.domain.org, true",
            "user_123+tag@mail.co, true",
            "plainaddress, false",
            "@missinglocal.com, false",
            "missingatsign.com, false",
            "user@.missingdomain.com, false",
            "user@domain.c, false",
            "user@domain..com, false"
    })
    @DisplayName("isValidEmail kiểm tra các phân vùng tương đương hợp lệ và không hợp lệ")
    void isValidEmail_Partitions_ReturnsExpected(String email, boolean expected) {
        // Arrange & Act
        boolean actual = AccountValidator.isValidEmail(email);

        // Assert
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] email null/empty/blank: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("isValidEmail trả về false với email null, rỗng hoặc khoảng trắng")
    void isValidEmail_NullAndEmptyAndBlank_ReturnsFalse(String email) {
        // Arrange & Act
        boolean actual = AccountValidator.isValidEmail(email);

        // Assert
        assertFalse(actual);
    }

    @ParameterizedTest(name = "[{index}] email độ dài {0} -> {1}")
    @CsvSource({
            "99, true",
            "100, true",
            "101, false"
    })
    @DisplayName("isValidEmail kiểm tra biên độ dài 99, 100, 101")
    void isValidEmail_BoundaryLength_ReturnsExpected(int length, boolean expected) {
        // Arrange: domain "@example.com" dài 12 ký tự
        String email = "a".repeat(length - 12) + "@example.com";

        // Act
        boolean actual = AccountValidator.isValidEmail(email);

        // Assert
        assertEquals(expected, actual);
    }

    // ==================== PASSWORD TESTS ====================

    @ParameterizedTest(name = "[{index}] {3}")
    @CsvSource(delimiter = '|', value = {
            "Secret@123    | alice_01 | true  | hợp lệ",
            "secret@123    | alice_01 | false | thiếu chữ hoa",
            "SECRET@123    | alice_01 | false | thiếu chữ thường",
            "Secret@abc    | alice_01 | false | thiếu chữ số",
            "Secret1234    | alice_01 | false | thiếu ký tự đặc biệt",
            "'Secret @123' | alice_01 | false | chứa khoảng trắng",
            "Secret#123~   | alice_01 | false | chứa ký tự lạ ngoài danh sách cho phép",
            "Xalice_01@1   | alice_01 | false | chứa username",
            "Xalice_01@1   | ALICE_01 | false | chứa username không phân biệt hoa thường",
            "Xalice_01@1   |          | true  | username null -> bỏ qua",
            "Xalice_01@1   | '   '    | true  | username blank -> bỏ qua"
    })
    @DisplayName("isValidPassword kiểm tra các phân vùng mật khẩu và username")
    void isValidPassword_Partitions_ReturnsExpected(String pw, String user, boolean expected, String desc) {
        // Arrange & Act
        boolean actual = AccountValidator.isValidPassword(pw, user);

        // Assert
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] password độ dài {0} -> {1}")
    @CsvSource({
            "7, false",
            "8, true",
            "32, true",
            "33, false"
    })
    @DisplayName("isValidPassword kiểm tra biên độ dài 7, 8, 32, 33")
    void isValidPassword_BoundaryLength_ReturnsExpected(int length, boolean expected) {
        // Arrange: prefix "Ab1!" có đủ 4 nhóm, bù thêm 'a'
        String password = "Ab1!" + "a".repeat(length - 4);

        // Act
        boolean actual = AccountValidator.isValidPassword(password, "someone");

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("isValidPassword trả về false khi password null")
    void isValidPassword_NullPassword_ReturnsFalse() {
        // Arrange & Act
        boolean actual = AccountValidator.isValidPassword(null, "alice");

        // Assert
        assertFalse(actual);
    }

    // ==================== PHONE TESTS ====================

    @ParameterizedTest(name = "[{index}] đầu số hợp lệ: {0}")
    @ValueSource(strings = {"0312345678", "0512345678", "0712345678", "0812345678", "0912345678"})
    @DisplayName("isValidPhone trả về true với 5 đầu số hợp lệ (03, 05, 07, 08, 09)")
    void isValidPhone_ValidPrefixes_ReturnsTrue(String phone) {
        // Arrange & Act
        boolean actual = AccountValidator.isValidPhone(phone);

        // Assert
        assertTrue(actual);
    }

    @ParameterizedTest(name = "[{index}] phone không hợp lệ: \"{0}\"")
    @ValueSource(strings = {
            "0112345678", "0212345678", "0412345678", "0612345678",
            "091234567", "09123456789", "091234567a", "09 1234567"
    })
    @DisplayName("isValidPhone trả về false với đầu số sai, thừa/thiếu ký tự, hoặc ký tự chữ")
    void isValidPhone_InvalidPhones_ReturnsFalse(String phone) {
        // Arrange & Act
        boolean actual = AccountValidator.isValidPhone(phone);

        // Assert
        assertFalse(actual);
    }

    @ParameterizedTest(name = "[{index}] phone null/empty/blank: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("isValidPhone trả về false khi phone null, rỗng hoặc khoảng trắng")
    void isValidPhone_NullAndEmptyAndBlank_ReturnsFalse(String phone) {
        // Arrange & Act
        boolean actual = AccountValidator.isValidPhone(phone);

        // Assert
        assertFalse(actual);
    }

    // ==================== AGE TESTS ====================

    @ParameterizedTest(name = "[{index}] sinh {0}, hôm nay {1} -> {2} tuổi")
    @CsvSource({
            "2008-09-28, 2026-09-28, 18",
            "2008-09-29, 2026-09-28, 17",
            "2008-02-29, 2026-02-28, 17",
            "2008-02-29, 2026-03-01, 18"
    })
    @DisplayName("calculateAge kiểm tra các mốc biên tuổi với ngày cố định")
    void calculateAge_Boundaries_ReturnsExpected(LocalDate dob, LocalDate today, int expected) {
        // Arrange & Act
        int actual = AccountValidator.calculateAge(dob, today);

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("calculateAge trả về 0 khi dob hoặc today null")
    void calculateAge_NullInputs_ReturnsZero() {
        // Arrange & Act & Assert
        assertEquals(0, AccountValidator.calculateAge(null, LocalDate.now()));
        assertEquals(0, AccountValidator.calculateAge(LocalDate.now(), null));
    }
}