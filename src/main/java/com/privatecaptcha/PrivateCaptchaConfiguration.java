package com.privatecaptcha;

import java.net.HttpURLConnection;
import java.time.Duration;

/**
 * Configuration options for the Private Captcha client.
 */
public class PrivateCaptchaConfiguration {

    private final String apiKey;
    private String domain;
    private String formField;
    private int failedStatusCode;
    private Duration connectTimeout;
    private Duration readTimeout;

    /**
     * Creates a new configuration with the required API key.
     *
     * @param apiKey the API key from Private Captcha account settings
     * @throws IllegalArgumentException if apiKey is null or empty
     */
    public PrivateCaptchaConfiguration(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        this.apiKey = apiKey;
        this.domain = Domains.GLOBAL;
        this.formField = Constants.DEFAULT_FORM_FIELD;
        this.failedStatusCode = HttpURLConnection.HTTP_FORBIDDEN;
        this.connectTimeout = Duration.ofSeconds(10);
        this.readTimeout = Duration.ofSeconds(30);
    }

    /**
     * Gets the API key for authentication.
     *
     * @return the API key
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Gets the domain for the Private Captcha API.
     * Use for self-hosted versions.
     *
     * @return the domain (default: {@link Domains#GLOBAL})
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Sets the domain for the Private Captcha API.
     *
     * @param domain the domain (e.g., "api.privatecaptcha.com")
     * @return this instance for method chaining
     */
    public PrivateCaptchaConfiguration setDomain(String domain) {
        this.domain = domain;
        return this;
    }

    /**
     * Gets the form field name to read puzzle solution from.
     *
     * @return the form field name (default: "private-captcha-solution")
     */
    public String getFormField() {
        return formField;
    }

    /**
     * Sets the custom form field to read puzzle solution from.
     *
     * @param formField the form field name
     * @return this instance for method chaining
     */
    public PrivateCaptchaConfiguration setFormField(String formField) {
        this.formField = formField;
        return this;
    }

    /**
     * Gets the HTTP status code to return for failed verifications.
     *
     * @return the status code (default: 403 Forbidden)
     */
    public int getFailedStatusCode() {
        return failedStatusCode;
    }

    /**
     * Sets the HTTP status code to return for failed verifications.
     *
     * @param failedStatusCode the HTTP status code
     * @return this instance for method chaining
     */
    public PrivateCaptchaConfiguration setFailedStatusCode(int failedStatusCode) {
        this.failedStatusCode = failedStatusCode;
        return this;
    }

    /**
     * Gets the connection timeout.
     *
     * @return the connection timeout (default: 10 seconds)
     */
    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Sets the connection timeout.
     *
     * @param connectTimeout the connection timeout
     * @return this instance for method chaining
     */
    public PrivateCaptchaConfiguration setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * Gets the read timeout.
     *
     * @return the read timeout (default: 30 seconds)
     */
    public Duration getReadTimeout() {
        return readTimeout;
    }

    /**
     * Sets the read timeout.
     *
     * @param readTimeout the read timeout
     * @return this instance for method chaining
     */
    public PrivateCaptchaConfiguration setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }
}
