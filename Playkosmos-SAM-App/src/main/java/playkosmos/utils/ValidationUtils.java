package playkosmos.utils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.time.LocalDate;
import java.time.Period;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class ValidationUtils {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern STRONG_PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Z])(?=.*[!@#$%^&*])(?=.*\\d).{8,}$");

    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^\\+[0-9]{13}$");

    private static final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    public static CompletableFuture<ValidationResult> validateUsername(String username) {
        return CompletableFuture.supplyAsync(() -> {
            if (username == null || username.length() <= 3) {
                return new ValidationResult(false, "Username must be at least 3 characters long.\n");
            }
            return new ValidationResult(true, "Username is valid.");
        });
    }

//    public static CompletableFuture<ValidationResult> validateContact(String email, String phoneNumber) {
//        return CompletableFuture.supplyAsync(() -> {
//            if(email == null && phoneNumber == null){
//                return new ValidationResult(false, "Either email or phone number must be provided.");
//            }
//
//            if(email != null && !EMAIL_PATTERN.matcher(email).matches()){
//                return new ValidationResult(false, "Invalid email format");
//            }else if(phoneNumber != null && !PHONE_NUMBER_PATTERN.matcher(phoneNumber).matches()){
//                return new ValidationResult(false, "Phone number must start with '+' and be 13 digits long");
//            }
//
//            return new ValidationResult(true, "Contact information is valid");
//        });
//    }

    public static CompletableFuture<ValidationResult> validateContact(String email, String phoneNumber, String countryCode) {
        return CompletableFuture.supplyAsync(() -> {
            if (email == null && phoneNumber == null) {
                return new ValidationResult(false, "Either email or phone number must be provided.");
            }

            if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
                return new ValidationResult(false, "Invalid email format.");
            } else if (phoneNumber != null && !isValidPhoneNumber(phoneNumber, countryCode)) {
                return new ValidationResult(false, "Invalid phone number format.");
            }

            return new ValidationResult(true, "Contact information is valid.");
        });
    }

    public static CompletableFuture<ValidationResult> validateDateOfBirth(LocalDate dateOfBirth) {

        return CompletableFuture.supplyAsync(() -> {
            if (dateOfBirth == null) {
                return new ValidationResult(false, "Date of Birth is required.");
            }

            if (Period.between(dateOfBirth, LocalDate.now()).getYears() < 16) {
                return new ValidationResult(false, "You must be at least 16 years old.");
            }
            return new ValidationResult(true, "Date of Birth is valid.");
        });
    }

    public static CompletableFuture<ValidationResult> validatePassword(String password) {
        return CompletableFuture.supplyAsync(() -> {
            if (password == null || !STRONG_PASSWORD_PATTERN.matcher(password).matches()) {
                return new ValidationResult(false, "Password must be at least 8 characters long, contain an uppercase letter, a special character, and a digit.\n");
            }
            return new ValidationResult(true, "Password is valid.");
        });
    }

    private static boolean isValidPhoneNumber(String phoneNumber, String countryCode) {
        try {
            Phonenumber.PhoneNumber parsedNumber = phoneNumberUtil.parse(phoneNumber, countryCode);
            return phoneNumberUtil.isValidNumber(parsedNumber);
        } catch (NumberParseException e) {
            return false;
        }
    }
}
