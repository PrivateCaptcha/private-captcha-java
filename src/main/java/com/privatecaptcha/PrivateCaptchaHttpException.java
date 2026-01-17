package com.privatecaptcha;

/**
 * Exception thrown when an HTTP error occurs during API communication.
 */
public class PrivateCaptchaHttpException extends PrivateCaptchaException {

    private final int statusCode;
    private final Integer retryAfterSeconds;
    private final String traceId;

    /**
     * Creates a new HTTP exception.
     *
     * @param statusCode the HTTP status code
     */
    public PrivateCaptchaHttpException(int statusCode) {
        this(statusCode, null, "");
    }

    /**
     * Creates a new HTTP exception with retry information.
     *
     * @param statusCode the HTTP status code
     * @param retryAfterSeconds optional retry-after time in seconds
     * @param traceId the trace ID from the response
     */
    public PrivateCaptchaHttpException(int statusCode, Integer retryAfterSeconds, String traceId) {
        super("HTTP error " + statusCode);
        this.statusCode = statusCode;
        this.retryAfterSeconds = retryAfterSeconds;
        this.traceId = traceId != null ? traceId : "";
    }

    /**
     * Returns the HTTP status code.
     *
     * @return the status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the suggested retry delay in seconds, if provided by the server.
     *
     * @return the retry-after seconds, or null if not provided
     */
    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    /**
     * Returns the trace ID for debugging.
     *
     * @return the trace ID, or empty string if not available
     */
    public String getTraceId() {
        return traceId;
    }
}
