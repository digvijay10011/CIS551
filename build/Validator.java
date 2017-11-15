import java.io.File;
import java.util.regex.*;

class Validator {
    static boolean validateFileName(String fileName) {
        Pattern p = Pattern.compile("[-_\\.0-9a-z]");
        for (int i = 0; i < fileName.length(); i++) {
            char c = fileName.charAt(i);
            if (!p.matcher(c+"").find()) {
                return false;
            }
        }
        Matcher m = p.matcher(fileName);
        return !fileName.equals(".") && !fileName.equals("..") && m.find() &&
                fileName != null && fileName.length() <= 205;
    }

    static boolean validateFile(String fileName) {
        if (!validateFileName(fileName)) {
            return false;
        }

        File f = new File(fileName);
        return f.exists();
    }

    static boolean validateIPAddress(String ipAddress) {
        if (ipAddress == null) {
            return false;
        }

        String[] parts = ipAddress.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        try {
            for (int i = 0; i < 4; i++) {
                if (!validateNumber(parts[i])) {
                    return false;
                }
                int part = Integer.valueOf(parts[i]);
                if (part < 0 || part > 255) {
                    return false;
                }
            }
        } catch(NumberFormatException e) {
            return false;
        }

        return true;
    }
    
    static boolean validateAccountName(String account) {
        Pattern p = Pattern.compile("[_\\-\\.0-9a-z]");
        for (int i = 0; i < account.length(); i++) {
            char c = account.charAt(i);
            if (!p.matcher(c+"").find()) {
                return false;
            }
        }
        return account != null && account.length() <= 200;
    }

    static boolean validateNumber(String num) {
        if (num == null || num.length() == 0) {
            return false;
        }
        try {
            if (Double.valueOf(num) < 0 || Double.valueOf(num) > 4294967295.99) {
                return false;
            }
        } catch(NumberFormatException e) {
            return false;
        }

        String[] parts = num.split("\\.");
        if (parts.length > 2 || parts.length == 0 ||
                                (parts.length == 2 && parts[1].length() != 2)) {
            return false;
        } 
        return !(num.charAt(0) == '0' && parts[0].length() > 1);
    }
}
