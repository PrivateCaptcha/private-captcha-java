package com.privatecaptcha;

/**
 * Input parameters for a captcha verification request.
 */
public class VerifyInput {

    private final String solution;
    private String sitekey;
    private int maxBackoffSeconds;
    private int maxAttempts;

    /**
     * Creates a new VerifyInput with the required solution.
     *
     * @param solution the captcha solution obtained from the client-side
     * @throws IllegalArgumentException if solution is null or empty
     */
    public VerifyInput(String solution) {
        if (solution == null || solution.isEmpty()) {
            throw new IllegalArgumentException("Solution cannot be null or empty");
        }
        this.solution = solution;
        this.sitekey = "";
        this.maxBackoffSeconds = Constants.DEFAULT_MAX_BACKOFF_SECONDS;
        this.maxAttempts = Constants.DEFAULT_MAX_ATTEMPTS;
    }

    /**
     * Gets the captcha solution obtained from the client-side.
     *
     * @return the solution string
     */
    public String getSolution() {
        return solution;
    }

    /**
     * Gets the optional sitekey to verify solution against.
     *
     * @return the sitekey, or empty string if not set
     */
    public String getSitekey() {
        return sitekey;
    }

    /**
     * Sets the optional sitekey to verify solution against.
     *
     * @param sitekey the sitekey
     * @return this instance for method chaining
     */
    public VerifyInput setSitekey(String sitekey) {
        this.sitekey = sitekey;
        return this;
    }

    /**
     * Gets the maximum backoff time in seconds between retries.
     *
     * @return the maximum backoff seconds (default: 20)
     */
    public int getMaxBackoffSeconds() {
        return maxBackoffSeconds;
    }

    /**
     * Sets the maximum backoff time in seconds between retries.
     *
     * @param maxBackoffSeconds the maximum backoff seconds
     * @return this instance for method chaining
     */
    public VerifyInput setMaxBackoffSeconds(int maxBackoffSeconds) {
        this.maxBackoffSeconds = maxBackoffSeconds;
        return this;
    }

    /**
     * Gets the maximum number of retry attempts.
     *
     * @return the maximum attempts (default: 5)
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * Sets the maximum number of retry attempts.
     *
     * @param maxAttempts the maximum attempts
     * @return this instance for method chaining
     */
    public VerifyInput setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
        return this;
    }
}
