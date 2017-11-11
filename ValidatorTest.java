import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ValidatorTest {

    @Test
    public void testValidateFileNameAlpha() {
        assertTrue(Validator.validateFileName("file"));
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
    public void testValidateNumberDecimal() {
        assertTrue(Validator.validateNumber("331.06"));
    }

    @Test
    public void testValidateNumberMoreDecimal() {
        assertFalse(Validator.validateNumber("331.461"));
    }

    @Test
    public void testValidateNumberLessDecimal() {
        assertFalse(Validator.validateNumber("331.4"));
    }

    @Test
    public void testValidateNumberLeadingZeros() {
        assertFalse(Validator.validateNumber("0331"));
    }
    
    @Test
    public void testValidateNumberLeadingZerosDecimal() {
        assertFalse(Validator.validateNumber("002331.42"));
    }

    @Test
    public void testValidateNumberLetters() {
        assertFalse(Validator.validateNumber("a23.31"));
    }

    @Test
    public void testValidateNumberLettersMiddle() {
        assertFalse(Validator.validateNumber("20a0"));
    }
    
    @Test
    public void testValidateNumberNegative() {
        assertFalse(Validator.validateNumber("-0.03"));
    }

    @Test
    public void testValidateNumberLarge() {
        assertFalse(Validator.validateNumber("4294967296.78"));
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
}
