package com.privatecaptcha;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client for the Private Captcha API.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * PrivateCaptchaClient client = new PrivateCaptchaClient(
 *     new PrivateCaptchaConfiguration()
 *         .setApiKey("pc_your_api_key")
 * );
 *
 * VerifyOutput output = client.verify(new VerifyInput()
 *     .setSolution(solution));
 *
 * if (output.ok()) {
 *     // Captcha verified successfully
 * } else {
 *     // Verification failed: output.getErrorMessage()
 * }
 * }</pre>
 */
public class PrivateCaptchaClient {

    private static final Logger LOGGER = Logger.getLogger(PrivateCaptchaClient.class.getName());
    private static final Random RANDOM = new Random();

    private final String endpoint;
    private final String apiKey;
    private final String formField;
    private final int failedStatusCode;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    /**
     * Creates a new Private Captcha client with the specified configuration.
     *
     * @param configuration the client configuration
     * @throws IllegalArgumentException if the API key is empty
     */
    public PrivateCaptchaClient(PrivateCaptchaConfiguration configuration) {
        if (configuration.getApiKey() == null || configuration.getApiKey().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be empty");
        }

        this.apiKey = configuration.getApiKey();
        this.formField = configuration.getFormField();
        this.failedStatusCode = configuration.getFailedStatusCode();
        this.connectTimeoutMillis = configuration.getConnectTimeoutMillis();
        this.readTimeoutMillis = configuration.getReadTimeoutMillis();

        // Build endpoint URL
        String domain = configuration.getDomain();
        if (domain == null || domain.isEmpty()) {
            domain = Domains.GLOBAL;
        }

        // Strip protocol prefix if present
        if (domain.startsWith("https://")) {
            domain = domain.substring(8);
        } else if (domain.startsWith("http://")) {
            domain = domain.substring(7);
        }

        // Remove trailing slashes
        while (domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }

        this.endpoint = "https://" + domain + "/verify";
    }

    /**
     * Gets the form field name configured for this client.
     *
     * @return the form field name
     */
    public String getFormField() {
        return formField;
    }

    /**
     * Gets the HTTP status code used for failed verifications.
     *
     * @return the failed status code
     */
    public int getFailedStatusCode() {
        return failedStatusCode;
    }

    /**
     * Verifies a captcha solution.
     *
     * @param input the verification input parameters
     * @return the verification output
     * @throws IllegalArgumentException if the solution is empty
     * @throws PrivateCaptchaHttpException if the API returns an error that should not be retried
     * @throws VerificationFailedException if verification fails after all retry attempts
     */
    public VerifyOutput verify(VerifyInput input)
            throws PrivateCaptchaHttpException, VerificationFailedException {
        if (input.getSolution() == null || input.getSolution().isEmpty()) {
            throw new IllegalArgumentException("Solution cannot be empty");
        }

        int maxAttempts = input.getMaxAttempts() > 0 ? input.getMaxAttempts() : Constants.DEFAULT_MAX_ATTEMPTS;
        int maxBackoffMillis = (input.getMaxBackoffSeconds() > 0
                ? input.getMaxBackoffSeconds()
                : Constants.DEFAULT_MAX_BACKOFF_SECONDS) * 1000;

        long backoffDelay = Constants.MIN_BACKOFF_MILLIS;
        Exception lastException = null;

        LOGGER.fine("Starting verification with max " + maxAttempts + " attempts, max backoff "
                + maxBackoffMillis + "ms");

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            if (attempt > 0) {
                long delay = backoffDelay;

                // Check if we got a Retry-After hint from the server
                if (lastException instanceof PrivateCaptchaHttpException) {
                    Integer retryAfter = ((PrivateCaptchaHttpException) lastException).getRetryAfterSeconds();
                    if (retryAfter != null && retryAfter * 1000L > delay) {
                        delay = Math.min(retryAfter * 1000L, maxBackoffMillis);
                    }
                }

                LOGGER.fine("Attempt " + attempt + " failed with error: " +
                        (lastException != null ? lastException.getMessage() : "unknown") +
                        ", backoff: " + delay + "ms");

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new VerificationFailedException(attempt + 1, e);
                }

                // Exponential backoff with jitter
                backoffDelay = Math.min(backoffDelay * 2 + RANDOM.nextInt((int) Math.max(1, backoffDelay / 4)),
                        maxBackoffMillis);
            }

            try {
                VerifyOutput response = doVerify(input.getSolution(), input.getSitekey());
                response.setAttempts(attempt + 1);

                LOGGER.fine("Verification request completed successfully on attempt " + (attempt + 1));
                return response;
            } catch (IOException e) {
                lastException = e;
                LOGGER.log(Level.FINE, "Request failed with IOException", e);
                continue;
            } catch (PrivateCaptchaHttpException e) {
                if (isRetriableStatusCode(e.getStatusCode())) {
                    lastException = e;
                    continue;
                }
                // Non-retriable HTTP error - rethrow immediately
                throw e;
            }
        }

        throw new VerificationFailedException(maxAttempts, lastException);
    }

    /**
     * Checks if an HTTP status code indicates a retriable error.
     */
    private static boolean isRetriableStatusCode(int statusCode) {
        switch (statusCode) {
            case 429: // Too Many Requests
            case 500: // Internal Server Error
            case 502: // Bad Gateway
            case 503: // Service Unavailable
            case 504: // Gateway Timeout
            case 408: // Request Timeout
                return true;
            default:
                return false;
        }
    }

    /**
     * Performs the actual HTTP request to verify the solution.
     */
    private VerifyOutput doVerify(String solution, String sitekey)
            throws IOException, PrivateCaptchaHttpException {

        HttpURLConnection connection = null;
        try {
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(connectTimeoutMillis);
            connection.setReadTimeout(readTimeoutMillis);

            // Set headers
            connection.setRequestProperty(Constants.HEADER_API_KEY, apiKey);
            connection.setRequestProperty(Constants.HEADER_USER_AGENT, Constants.USER_AGENT);
            connection.setRequestProperty(Constants.HEADER_CONTENT_TYPE, "text/plain");

            if (sitekey != null && !sitekey.isEmpty()) {
                connection.setRequestProperty(Constants.HEADER_SITEKEY, sitekey);
            }

            // Write solution to request body
            byte[] requestBody = solution.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(requestBody.length);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBody);
            }

            int statusCode = connection.getResponseCode();
            String traceId = connection.getHeaderField(Constants.HEADER_TRACE_ID);
            if (traceId == null) {
                traceId = "";
            }

            LOGGER.fine("Received HTTP response. code=" + statusCode + " traceID=" + traceId);

            if (statusCode >= 300) {
                Integer retryAfter = null;
                if (statusCode == 429) {
                    String retryAfterHeader = connection.getHeaderField(Constants.HEADER_RETRY_AFTER);
                    if (retryAfterHeader != null && !retryAfterHeader.isEmpty()) {
                        try {
                            retryAfter = Integer.parseInt(retryAfterHeader);
                            LOGGER.fine("Rate limited, retry after " + retryAfter + "s");
                        } catch (NumberFormatException e) {
                            // Ignore invalid header
                        }
                    }
                }
                throw new PrivateCaptchaHttpException(statusCode, retryAfter, traceId);
            }

            // Read response body
            String responseBody = readResponseBody(connection);

            // Parse JSON response
            VerifyOutput output = parseJsonResponse(responseBody);
            output.setTraceId(traceId);

            return output;

        } catch (SocketTimeoutException | UnknownHostException e) {
            // Wrap network errors as IOException to be retried
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Reads the response body from the connection.
     */
    private static String readResponseBody(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    /**
     * Simple JSON parser for the verification response.
     * This avoids requiring external JSON libraries.
     */
    private static VerifyOutput parseJsonResponse(String json) {
        VerifyOutput output = new VerifyOutput();

        if (json == null || json.isEmpty()) {
            output.setCode(VerifyCode.PARSE_RESPONSE);
            return output;
        }

        // Parse success field
        output.setSuccess(extractBooleanField(json, "success"));

        // Parse code field
        Integer codeValue = extractIntField(json, "code");
        if (codeValue != null) {
            output.setCode(VerifyCode.fromCode(codeValue));
        }

        // Parse origin field
        String origin = extractStringField(json, "origin");
        if (origin != null) {
            output.setOrigin(origin);
        }

        // Parse timestamp field
        String timestamp = extractStringField(json, "timestamp");
        if (timestamp != null) {
            output.setTimestamp(timestamp);
        }

        return output;
    }

    /**
     * Extracts a boolean field from JSON.
     */
    private static boolean extractBooleanField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\"";
        int startIndex = json.indexOf(pattern);
        if (startIndex == -1) {
            return false;
        }

        int colonIndex = json.indexOf(':', startIndex + pattern.length());
        if (colonIndex == -1) {
            return false;
        }

        // Find the value after the colon
        String remaining = json.substring(colonIndex + 1).trim();
        return remaining.startsWith("true");
    }

    /**
     * Extracts an integer field from JSON.
     */
    private static Integer extractIntField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\"";
        int startIndex = json.indexOf(pattern);
        if (startIndex == -1) {
            return null;
        }

        int colonIndex = json.indexOf(':', startIndex + pattern.length());
        if (colonIndex == -1) {
            return null;
        }

        // Find the numeric value
        StringBuilder sb = new StringBuilder();
        boolean started = false;
        for (int i = colonIndex + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (Character.isDigit(c) || (c == '-' && !started)) {
                sb.append(c);
                started = true;
            } else if (started) {
                break;
            } else if (!Character.isWhitespace(c)) {
                break;
            }
        }

        if (sb.length() == 0) {
            return null;
        }

        try {
            return Integer.parseInt(sb.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Extracts a string field from JSON.
     */
    private static String extractStringField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\"";
        int startIndex = json.indexOf(pattern);
        if (startIndex == -1) {
            return null;
        }

        int colonIndex = json.indexOf(':', startIndex + pattern.length());
        if (colonIndex == -1) {
            return null;
        }

        // Find the opening quote
        int openQuote = json.indexOf('"', colonIndex + 1);
        if (openQuote == -1) {
            return null;
        }

        // Find the closing quote (handling escaped quotes)
        int closeQuote = -1;
        for (int i = openQuote + 1; i < json.length(); i++) {
            if (json.charAt(i) == '"' && json.charAt(i - 1) != '\\') {
                closeQuote = i;
                break;
            }
        }

        if (closeQuote == -1) {
            return null;
        }

        // Unescape the string
        return json.substring(openQuote + 1, closeQuote)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}
