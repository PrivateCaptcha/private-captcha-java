package com.privatecaptcha;

/**
 * Internal constants used by the Private Captcha client.
 */
final class Constants {

    private Constants() {
        // Utility class - prevent instantiation
    }

    /** Default form field name for captcha solutions. */
    static final String DEFAULT_FORM_FIELD = "private-captcha-solution";

    /** Library version. */
    static final String VERSION = "0.0.5";

    /** User agent string for HTTP requests. */
    static final String USER_AGENT = "private-captcha-java/" + VERSION;

    /** Minimum backoff time in milliseconds between retries. */
    static final int MIN_BACKOFF_MILLIS = 500;

    /** Default maximum backoff time in seconds. */
    static final int DEFAULT_MAX_BACKOFF_SECONDS = 20;

    /** Default number of retry attempts. */
    static final int DEFAULT_MAX_ATTEMPTS = 5;

    // HTTP Headers
    static final String HEADER_API_KEY = "X-Api-Key";
    static final String HEADER_TRACE_ID = "X-Trace-ID";
    static final String HEADER_USER_AGENT = "User-Agent";
    static final String HEADER_SITEKEY = "X-PC-Sitekey";
    static final String HEADER_CONTENT_TYPE = "Content-Type";
    static final String HEADER_RETRY_AFTER = "Retry-After";
}
