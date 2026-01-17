package com.privatecaptcha;

/**
 * Enumeration of verification result codes returned by the Private Captcha API.
 */
public enum VerifyCode {
    /** No error - verification successful. */
    NO_ERROR(0, ""),
    /** An unspecified error occurred. */
    ERROR_OTHER(1, "error-other"),
    /** Duplicate solutions were detected. */
    DUPLICATE_SOLUTIONS(2, "solution-duplicates"),
    /** The solution is invalid. */
    INVALID_SOLUTION(3, "solution-invalid"),
    /** Failed to parse the response. */
    PARSE_RESPONSE(4, "solution-bad-format"),
    /** The puzzle has expired. */
    PUZZLE_EXPIRED(5, "puzzle-expired"),
    /** Invalid property configuration. */
    INVALID_PROPERTY(6, "property-invalid"),
    /** Property owner mismatch. */
    WRONG_OWNER(7, "property-owner-mismatch"),
    /** Solution has already been verified. */
    VERIFIED_BEFORE(8, "solution-verified-before"),
    /** Service is in maintenance mode. */
    MAINTENANCE_MODE(9, "maintenance-mode"),
    /** Test property was used. */
    TEST_PROPERTY(10, "property-test"),
    /** Integrity check failed. */
    INTEGRITY(11, "integrity-error"),
    /** Organization scope error. */
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
        for (VerifyCode vc : values()) {
            if (vc.code == code) {
                return vc;
            }
        }
        return ERROR_OTHER;
    }
}
