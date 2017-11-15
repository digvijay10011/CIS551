import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ValidatorTest {

    @Test
    public void testValidateFileNameAlpha() {
        assertTrue(Validator.validateFileName("file"));
    }

    @Test
    public void testValidateFileNameSlash() {
        assertFalse(Validator.validateFileName("abc/def"));
        assertFalse(Validator.validateFileName("abc\\def"));
    }

    @Test
    public void testValidateFileNameExtra() {
        assertTrue(Validator.validateFileName("f.i123l_90e-"));
    }

    @Test
    public void testValidateFileNameUpper() {
        assertFalse(Validator.validateFileName("f.i123L_90e-"));
    }

    @Test
    public void testValidateFileNameSpecial() {
        assertFalse(Validator.validateFileName("f.i123L%90e-"));
    }

    @Test
    public void testValidateFileNameDots() {
        assertFalse(Validator.validateFileName("."));
    }

    @Test
    public void testValidateFileNameTwoDots() {
        assertFalse(Validator.validateFileName(".."));
    }

    @Test
    public void testValidateFileNameLength() {
        String filename = "";
        for (int i = 0; i < 205; i++) {
            filename += "0";
        }
        assertTrue(Validator.validateFileName(filename));
        filename += "a";
        assertFalse(Validator.validateFileName(filename));
    }

    @Test
    public void testValidateAccountLength() {
        String filename = "";
        for (int i = 0; i < 200; i++) {
            filename += "0";
        }
        assertTrue(Validator.validateAccountName(filename));
        filename += "a";
        assertFalse(Validator.validateAccountName(filename));
    }
    @Test
    public void testValidateAccountDots() {
        assertTrue(Validator.validateAccountName("."));
    }

    @Test
    public void testValidateAccountTwoDots() {
        assertTrue(Validator.validateAccountName(".."));
    }

    @Test
    public void testValidateAccountNameAlpha() {
        assertTrue(Validator.validateAccountName("file"));
    }

    @Test
    public void testValidateAccountNameExtra() {
        assertTrue(Validator.validateAccountName("f.i123l_90e-"));
    }

    @Test
    public void testValidateAccountNameUpper() {
        assertFalse(Validator.validateAccountName("f.i123L_90e-"));
    }

    @Test
    public void testValidateAccountNameSpecial() {
        assertFalse(Validator.validateAccountName("f.i123L%90e-"));
    }

    @Test
    public void testValidateNumber() {
        assertTrue(Validator.validateNumber("200"));
    }

    @Test
    public void testValidateNumberLeading() {
        assertFalse(Validator.validateNumber("0200"));
    }

    @Test
    public void testValidateBalance() {
        assertFalse(Validator.validateBalance("200"));
    }

    @Test
    public void testValidateBalanceDecimal() {
        assertTrue(Validator.validateBalance("331.06"));
    }

    @Test
    public void testValidateBalanceMoreDecimal() {
        assertFalse(Validator.validateBalance("331.461"));
    }

    @Test
    public void testValidateBalanceLessDecimal() {
        assertFalse(Validator.validateBalance("331.4"));
    }

    @Test
    public void testValidateBalanceDot() {
        assertFalse(Validator.validateBalance("12."));
    }

    @Test
    public void testValidateBalanceDotRegression() {
        assertFalse(Validator.validateBalance("12.9"));
    }

    @Test
    public void testValidateBalanceLeadingZeros() {
        assertFalse(Validator.validateBalance("0331"));
    }
    
    @Test
    public void testValidateBalanceLeadingZerosDecimal() {
        assertFalse(Validator.validateBalance("002331.42"));
    }

    @Test
    public void testValidateBalanceLetters() {
        assertFalse(Validator.validateBalance("a23.31"));
    }

    @Test
    public void testValidateBalanceLettersMiddle() {
        assertFalse(Validator.validateBalance("20a0"));
    }
    
    @Test
    public void testValidateBalanceNegative() {
        assertFalse(Validator.validateBalance("-0.03"));
    }

    @Test
    public void testValidateBalanceLarge() {
        assertFalse(Validator.validateBalance("4294967296.78"));
    }

    @Test
    public void testIPLocal() {
        assertTrue(Validator.validateIPAddress("127.0.0.1"));
    }

    @Test
    public void testIPArb() {
        assertTrue(Validator.validateIPAddress("50.225.89.131"));
    }

    @Test
    public void testIPSmall() {
        assertFalse(Validator.validateIPAddress("50.225.89"));
    }

    @Test
    public void testIPBig() {
        assertFalse(Validator.validateIPAddress("50.225.89.131.127"));
    }

    @Test
    public void testIPNonNum() {
        assertFalse(Validator.validateIPAddress("50.22b.89.-22"));
    }

    @Test
    public void testIPOutofRange() {
        assertFalse(Validator.validateIPAddress("50.22.2289.-22"));
    }
}
