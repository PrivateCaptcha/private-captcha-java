package com.privatecaptcha;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
    private static final Gson GSON = new Gson();

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
        this.connectTimeoutMillis = (int) configuration.getConnectTimeout().toMillis();
        this.readTimeoutMillis = (int) configuration.getReadTimeout().toMillis();

        String domain = configuration.getDomain();
        if (domain == null || domain.isEmpty()) {
            domain = Domains.GLOBAL;
        }

        if (domain.startsWith("https://")) {
            domain = domain.substring(8);
        } else if (domain.startsWith("http://")) {
            domain = domain.substring(7);
        }

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
     * Verifies a captcha solution from an HTTP request.
     * This is a helper method for server-side verification that extracts
     * the solution from a form parameter.
     *
     * @param formParameterExtractor function to extract form parameter by name
     * @return the verification result
     * @throws IllegalArgumentException if the extractor is null or solution is empty
     * @throws PrivateCaptchaHttpException if the API returns a non-retriable error
     * @throws VerificationFailedException if verification fails after all retry attempts
     */
    public VerifyOutput verifyRequest(FormParameterExtractor formParameterExtractor)
            throws PrivateCaptchaHttpException, VerificationFailedException {
        if (formParameterExtractor == null) {
            throw new IllegalArgumentException("Form parameter extractor cannot be null");
        }
        String solution = formParameterExtractor.getParameter(formField);
        return verify(new VerifyInput().setSolution(solution != null ? solution : ""));
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

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Starting verification: maxAttempts={0}, maxBackoffMs={1}",
                    new Object[]{maxAttempts, maxBackoffMillis});
        }

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            if (attempt > 0) {
                long delay = backoffDelay;

                if (lastException instanceof PrivateCaptchaHttpException) {
                    Integer retryAfter = ((PrivateCaptchaHttpException) lastException).getRetryAfterSeconds();
                    if (retryAfter != null && retryAfter * 1000L > delay) {
                        delay = Math.min(retryAfter * 1000L, maxBackoffMillis);
                    }
                }

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Attempt {0} failed: {1}, backoff={2}ms",
                            new Object[]{attempt, lastException != null ? lastException.getMessage() : "unknown", delay});
                }

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new VerificationFailedException(attempt + 1, e);
                }

                backoffDelay = Math.min(backoffDelay * 2 + RANDOM.nextInt((int) Math.max(1, backoffDelay / 4)),
                        maxBackoffMillis);
            }

            try {
                VerifyOutput response = doVerify(input.getSolution(), input.getSitekey());
                response = response.withAttempts(attempt + 1);

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Verification completed on attempt {0}", attempt + 1);
                }
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
                throw e;
            }
        }

        throw new VerificationFailedException(maxAttempts, lastException);
    }

    private static boolean isRetriableStatusCode(int statusCode) {
        switch (statusCode) {
            case 429:
            case 500:
            case 502:
            case 503:
            case 504:
            case 408:
                return true;
            default:
                return false;
        }
    }

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

            connection.setRequestProperty(Constants.HEADER_API_KEY, apiKey);
            connection.setRequestProperty(Constants.HEADER_USER_AGENT, Constants.USER_AGENT);
            connection.setRequestProperty(Constants.HEADER_CONTENT_TYPE, "text/plain");

            if (sitekey != null && !sitekey.isEmpty()) {
                connection.setRequestProperty(Constants.HEADER_SITEKEY, sitekey);
            }

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

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "HTTP response: code={0}, traceID={1}", new Object[]{statusCode, traceId});
            }

            if (statusCode >= 300) {
                Integer retryAfter = null;
                if (statusCode == 429) {
                    String retryAfterHeader = connection.getHeaderField(Constants.HEADER_RETRY_AFTER);
                    if (retryAfterHeader != null && !retryAfterHeader.isEmpty()) {
                        try {
                            retryAfter = Integer.parseInt(retryAfterHeader);
                            if (LOGGER.isLoggable(Level.FINE)) {
                                LOGGER.log(Level.FINE, "Rate limited, retry after {0}s", retryAfter);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                throw new PrivateCaptchaHttpException(statusCode, retryAfter, traceId);
            }

            VerifyOutput output = parseJsonResponse(connection);
            return output.withTraceId(traceId);

        } catch (SocketTimeoutException | UnknownHostException e) {
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static VerifyOutput parseJsonResponse(HttpURLConnection connection) throws IOException {
        try (JsonReader reader = new JsonReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

            boolean success = false;
            VerifyCode code = VerifyCode.NO_ERROR;
            String origin = "";
            String timestamp = "";

            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "success":
                        success = reader.nextBoolean();
                        break;
                    case "code":
                        code = VerifyCode.fromCode(reader.nextInt());
                        break;
                    case "origin":
                        origin = reader.nextString();
                        break;
                    case "timestamp":
                        timestamp = reader.nextString();
                        break;
                    default:
                        reader.skipValue();
                        break;
                }
            }
            reader.endObject();

            return new VerifyOutput(success, code, origin, timestamp, "", 0);
        }
    }

    /**
     * Functional interface for extracting form parameters from HTTP requests.
     * This allows integration with any HTTP server framework (Servlet, Spring, etc.)
     */
    @FunctionalInterface
    public interface FormParameterExtractor {
        /**
         * Gets a form parameter value by name.
         *
         * @param name the parameter name
         * @return the parameter value, or null if not present
         */
        String getParameter(String name);
    }
}
