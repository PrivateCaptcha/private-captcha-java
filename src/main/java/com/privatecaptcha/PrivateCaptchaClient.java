package com.privatecaptcha;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
 *     new PrivateCaptchaConfiguration("pc_your_api_key")
 * );
 *
 * VerifyOutput output = client.verify(new VerifyInput(solution));
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
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final HttpClient httpClient;

    /**
     * Creates a new Private Captcha client with the specified configuration.
     *
     * @param configuration the client configuration
     */
    public PrivateCaptchaClient(PrivateCaptchaConfiguration configuration) {
        this.apiKey = configuration.getApiKey();
        this.formField = configuration.getFormField();
        this.failedStatusCode = configuration.getFailedStatusCode();
        this.connectTimeout = configuration.getConnectTimeout();
        this.readTimeout = configuration.getReadTimeout();

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
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
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
        if (solution == null || solution.isEmpty()) {
            throw new IllegalArgumentException("Solution cannot be empty");
        }
        return verify(new VerifyInput(solution));
    }

    /**
     * Verifies a captcha solution.
     *
     * @param input the verification input parameters
     * @return the verification output
     * @throws PrivateCaptchaHttpException if the API returns an error that should not be retried
     * @throws VerificationFailedException if verification fails after all retry attempts
     */
    public VerifyOutput verify(VerifyInput input)
            throws PrivateCaptchaHttpException, VerificationFailedException {

        int maxAttempts = input.getMaxAttempts() > 0 ? input.getMaxAttempts() : Constants.DEFAULT_MAX_ATTEMPTS;
        Duration maxBackoff = Duration.ofSeconds(
                input.getMaxBackoffSeconds() > 0 ? input.getMaxBackoffSeconds() : Constants.DEFAULT_MAX_BACKOFF_SECONDS
        );

        Duration backoffDelay = Duration.ofMillis(Constants.MIN_BACKOFF_MILLIS);
        Exception lastException = null;

        LOGGER.log(Level.FINE, "Starting verification: maxAttempts={0}, maxBackoff={1}",
                new Object[]{maxAttempts, maxBackoff});

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            if (attempt > 0) {
                Duration delay = backoffDelay;

                if (lastException instanceof PrivateCaptchaHttpException) {
                    Integer retryAfter = ((PrivateCaptchaHttpException) lastException).getRetryAfterSeconds();
                    if (retryAfter != null) {
                        Duration retryAfterDuration = Duration.ofSeconds(retryAfter);
                        if (retryAfterDuration.compareTo(delay) > 0) {
                            delay = retryAfterDuration.compareTo(maxBackoff) < 0 ? retryAfterDuration : maxBackoff;
                        }
                    }
                }

                LOGGER.log(Level.FINE, "Attempt {0} failed: {1}, backoff={2}",
                        new Object[]{attempt, lastException != null ? lastException.getMessage() : "unknown", delay});

                try {
                    Thread.sleep(delay.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new VerificationFailedException(attempt + 1, e);
                }

                long currentBackoffMs = backoffDelay.toMillis();
                long jitter = RANDOM.nextInt((int) Math.max(1, currentBackoffMs / 4));
                long newBackoffMs = Math.min(currentBackoffMs * 2 + jitter, maxBackoff.toMillis());
                backoffDelay = Duration.ofMillis(newBackoffMs);
            }

            try {
                VerifyOutput response = doVerify(input.getSolution(), input.getSitekey());
                response = response.withAttempts(attempt + 1);

                LOGGER.log(Level.FINE, "Verification completed on attempt {0}", attempt + 1);
                return response;
            } catch (IOException e) {
                lastException = e;
                LOGGER.log(Level.FINE, "Request failed with IOException", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new VerificationFailedException(attempt + 1, e);
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
            case 408:
            case 429:
            case 500:
            case 502:
            case 503:
            case 504:
                return true;
            default:
                return false;
        }
    }

    private VerifyOutput doVerify(String solution, String sitekey)
            throws IOException, InterruptedException, PrivateCaptchaHttpException {

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(readTimeout)
                .header(Constants.HEADER_API_KEY, apiKey)
                .header(Constants.HEADER_USER_AGENT, Constants.USER_AGENT)
                .header(Constants.HEADER_CONTENT_TYPE, "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(solution));

        if (sitekey != null && !sitekey.isEmpty()) {
            requestBuilder.header(Constants.HEADER_SITEKEY, sitekey);
        }

        HttpResponse<String> response = httpClient.send(
                requestBuilder.build(),
                HttpResponse.BodyHandlers.ofString()
        );

        int statusCode = response.statusCode();
        String traceId = response.headers().firstValue(Constants.HEADER_TRACE_ID).orElse("");

        LOGGER.log(Level.FINE, "HTTP response: code={0}, traceID={1}", new Object[]{statusCode, traceId});

        if (statusCode >= 300) {
            Integer retryAfter = null;
            if (statusCode == 429) {
                String retryAfterHeader = response.headers().firstValue(Constants.HEADER_RETRY_AFTER).orElse(null);
                if (retryAfterHeader != null && !retryAfterHeader.isEmpty()) {
                    try {
                        retryAfter = Integer.parseInt(retryAfterHeader);
                        LOGGER.log(Level.FINE, "Rate limited, retry after {0}s", retryAfter);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            throw new PrivateCaptchaHttpException(statusCode, retryAfter, traceId);
        }

        VerifyOutput output = parseJsonResponse(response.body());
        return output.withTraceId(traceId);
    }

    private static VerifyOutput parseJsonResponse(String body) throws IOException {
        try (JsonReader reader = new JsonReader(new StringReader(body))) {

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
     * This allows integration with any HTTP server framework.
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>Servlet: {@code client.verifyRequest(request::getParameter)}</li>
     *   <li>Spring WebFlux: {@code client.verifyRequest(params::getFirst)}</li>
     *   <li>Vert.x: {@code client.verifyRequest(request::getParam)}</li>
     *   <li>Javalin: {@code client.verifyRequest(ctx::formParam)}</li>
     * </ul>
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
