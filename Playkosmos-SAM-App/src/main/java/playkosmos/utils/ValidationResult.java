package playkosmos.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ValidationResult {

    private boolean isValid;
    private String message;

}
