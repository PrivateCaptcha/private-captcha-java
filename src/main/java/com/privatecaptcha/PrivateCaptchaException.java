package com.privatecaptcha;

/**
 * Base exception for Private Captcha client errors.
 */
public class PrivateCaptchaException extends Exception {

    /**
     * Creates a new exception with the specified message.
     *
     * @param message the error message
     */
    public PrivateCaptchaException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the specified message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public PrivateCaptchaException(String message, Throwable cause) {
        super(message, cause);
    }
}
