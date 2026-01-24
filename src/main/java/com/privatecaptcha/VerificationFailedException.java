package com.privatecaptcha;

/**
 * Exception thrown when verification fails after exhausting all retry attempts.
 */
public class VerificationFailedException extends PrivateCaptchaException {

    private final int attempts;

    /**
     * Creates a new exception indicating verification failure.
     *
     * @param attempts the number of attempts made
     * @param cause the underlying cause of the failure
     */
    public VerificationFailedException(int attempts, Throwable cause) {
        super("Captcha verification failed after " + attempts + " attempts", cause);
        this.attempts = attempts;
    }

    /**
     * Returns the number of attempts made before giving up.
     *
     * @return the number of attempts
     */
    public int getAttempts() {
        return attempts;
    }
}
