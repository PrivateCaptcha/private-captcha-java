package com.privatecaptcha;

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
        switch (code) {
            case 0: return NO_ERROR;
            case 1: return ERROR_OTHER;
            case 2: return DUPLICATE_SOLUTIONS;
            case 3: return INVALID_SOLUTION;
            case 4: return PARSE_RESPONSE;
            case 5: return PUZZLE_EXPIRED;
            case 6: return INVALID_PROPERTY;
            case 7: return WRONG_OWNER;
            case 8: return VERIFIED_BEFORE;
            case 9: return MAINTENANCE_MODE;
            case 10: return TEST_PROPERTY;
            case 11: return INTEGRITY;
            case 12: return ORG_SCOPE;
            default: return ERROR_OTHER;
        }
    }
}
