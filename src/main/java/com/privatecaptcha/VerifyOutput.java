package com.privatecaptcha;

/**
 * Output from a captcha verification request.
 */
public class VerifyOutput {

    private final boolean success;
    private final VerifyCode code;
    private final String origin;
    private final String timestamp;
    private final String traceId;
    private final int attempts;

    /**
     * Creates a new VerifyOutput with default values (for error cases).
     */
    VerifyOutput() {
        this(false, VerifyCode.NO_ERROR, "", "", "", 0);
    }

    /**
     * Creates a new VerifyOutput with the specified values.
     */
    VerifyOutput(boolean success, VerifyCode code, String origin, String timestamp, String traceId, int attempts) {
        this.success = success;
        this.code = code;
        this.origin = origin != null ? origin : "";
        this.timestamp = timestamp != null ? timestamp : "";
        this.traceId = traceId != null ? traceId : "";
        this.attempts = attempts;
    }

    /**
     * Creates a copy with updated attempts count.
     */
    VerifyOutput withAttempts(int attempts) {
        return new VerifyOutput(this.success, this.code, this.origin, this.timestamp, this.traceId, attempts);
    }

    /**
     * Creates a copy with updated traceId.
     */
    VerifyOutput withTraceId(String traceId) {
        return new VerifyOutput(this.success, this.code, this.origin, this.timestamp, traceId, this.attempts);
    }

    /**
     * Returns whether the verification was successful and had no errors.
     * This checks both the success flag and ensures the code is NO_ERROR.
     *
     * @return true if verification succeeded with no errors
     */
    public boolean ok() {
        return success && code == VerifyCode.NO_ERROR;
    }

    /**
     * Returns whether the API reported success.
     *
     * @return true if the success flag was set
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the verification result code.
     *
     * @return the verify code
     */
    public VerifyCode getCode() {
        return code;
    }

    /**
     * Returns the origin of the request as reported by the API.
     *
     * @return the origin string
     */
    public String getOrigin() {
        return origin;
    }

    /**
     * Returns the timestamp of the verification.
     *
     * @return the timestamp string
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the trace ID for this request, useful for debugging.
     *
     * @return the trace ID
     */
    public String getTraceId() {
        return traceId;
    }

    /**
     * Returns the number of attempts made for this verification.
     *
     * @return the number of attempts
     */
    public int getAttempts() {
        return attempts;
    }

    /**
     * Returns the error message for the verification code.
     *
     * @return the error message, or empty string if no error
     */
    public String getErrorMessage() {
        return code.getErrorString();
    }
}
