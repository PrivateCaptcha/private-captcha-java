package com.privatecaptcha;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of verification result codes returned by the Private Captcha API.
 */
public enum VerifyCode {
    NO_ERROR(0, ""),
    ERROR_OTHER(1, "error-other"),
    DUPLICATE_SOLUTIONS(2, "solution-duplicates"),
    INVALID_SOLUTION(3, "solution-invalid"),
    PARSE_RESPONSE(4, "solution-bad-format"),
    PUZZLE_EXPIRED(5, "puzzle-expired"),
    INVALID_PROPERTY(6, "property-invalid"),
    WRONG_OWNER(7, "property-owner-mismatch"),
    VERIFIED_BEFORE(8, "solution-verified-before"),
    MAINTENANCE_MODE(9, "maintenance-mode"),
    TEST_PROPERTY(10, "property-test"),
    INTEGRITY(11, "integrity-error"),
    ORG_SCOPE(12, "org-scope-error");

    private static final Map<Integer, VerifyCode> CODE_MAP = new HashMap<>();

    static {
        for (VerifyCode vc : values()) {
            CODE_MAP.put(vc.code, vc);
        }
    }

    private final int code;
    private final String errorString;

    VerifyCode(int code, String errorString) {
        this.code = code;
        this.errorString = errorString;
    }

    /**
     * Returns the numeric code value.
     *
     * @return the code value
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the error string representation.
     *
     * @return the error string, or empty string for NO_ERROR
     */
    public String getErrorString() {
        return errorString;
    }

    /**
     * Converts a numeric code to its corresponding VerifyCode enum value.
     *
     * @param code the numeric code
     * @return the corresponding VerifyCode, or ERROR_OTHER if not found
     */
    public static VerifyCode fromCode(int code) {
        return CODE_MAP.getOrDefault(code, ERROR_OTHER);
    }
}
