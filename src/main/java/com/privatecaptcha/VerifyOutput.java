package com.privatecaptcha;

/**
 * Output from a captcha verification request.
 */
public class VerifyOutput {

    private boolean success;
    private VerifyCode code;
    private String origin;
    private String timestamp;
    private String traceId;
    private int attempts;

    /**
     * Creates a new VerifyOutput with default values.
     */
    public VerifyOutput() {
        this.success = false;
        this.code = VerifyCode.NO_ERROR;
        this.origin = "";
        this.timestamp = "";
        this.traceId = "";
        this.attempts = 0;
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
     * Sets the success flag.
     *
     * @param success the success value
     */
    void setSuccess(boolean success) {
        this.success = success;
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
     * Sets the verification result code.
     *
     * @param code the verify code
     */
    void setCode(VerifyCode code) {
        this.code = code;
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
     * Sets the origin.
     *
     * @param origin the origin string
     */
    void setOrigin(String origin) {
        this.origin = origin;
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
     * Sets the timestamp.
     *
     * @param timestamp the timestamp string
     */
    void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
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
     * Sets the trace ID.
     *
     * @param traceId the trace ID
     */
    void setTraceId(String traceId) {
        this.traceId = traceId;
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
     * Sets the number of attempts.
     *
     * @param attempts the number of attempts
     */
    void setAttempts(int attempts) {
        this.attempts = attempts;
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
