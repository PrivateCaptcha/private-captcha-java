package com.privatecaptcha;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.junit.Assert.*;

/**
 * Tests for the Private Captcha client.
 * 
 * Note: Some tests require a valid API key set in the PC_API_KEY environment variable.
 * Tests that require the API key will be skipped if it's not set.
 */
public class PrivateCaptchaClientTest {

    private static final int SOLUTIONS_COUNT = 16;
    private static final int SOLUTION_LENGTH = 8;

    private static String testPuzzle;

    private static synchronized String fetchTestPuzzle() throws Exception {
        if (testPuzzle != null) {
            return testPuzzle;
        }

        URL url = new URL("https://api.privatecaptcha.com/puzzle?sitekey=aaaaaaaabbbbccccddddeeeeeeeeeeee");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Origin", "not.empty");

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            testPuzzle = response.toString();
            return testPuzzle;
        } finally {
            connection.disconnect();
        }
    }

    private static String getApiKey() {
        String apiKey = System.getenv("PC_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            return null;
        }
        return apiKey;
    }

    @Test
    public void testVerifyWithTestPuzzle() throws Exception {
        String apiKey = getApiKey();
        if (apiKey == null) {
            System.out.println("Skipping testVerifyWithTestPuzzle: PC_API_KEY not set");
            return;
        }

        String puzzle = fetchTestPuzzle();
        PrivateCaptchaClient client = new PrivateCaptchaClient(
                new PrivateCaptchaConfiguration().setApiKey(apiKey)
        );

        byte[] emptySolutions = new byte[SOLUTIONS_COUNT * SOLUTION_LENGTH];
        String solutionsStr = Base64.getEncoder().encodeToString(emptySolutions);
        String payload = solutionsStr + "." + puzzle;

        VerifyOutput output = client.verify(new VerifyInput().setSolution(payload));

        assertTrue("Expected success flag to be true", output.isSuccess());
        assertFalse("Expected ok() to be false for test property", output.ok());
        assertEquals("Expected TEST_PROPERTY code", VerifyCode.TEST_PROPERTY, output.getCode());
    }

    @Test
    public void testVerifyWithInvalidSolution() throws Exception {
        String apiKey = getApiKey();
        if (apiKey == null) {
            System.out.println("Skipping testVerifyWithInvalidSolution: PC_API_KEY not set");
            return;
        }

        String puzzle = fetchTestPuzzle();
        PrivateCaptchaClient client = new PrivateCaptchaClient(
                new PrivateCaptchaConfiguration().setApiKey(apiKey)
        );

        byte[] emptySolutions = new byte[(SOLUTIONS_COUNT * SOLUTION_LENGTH) / 2];
        String solutionsStr = Base64.getEncoder().encodeToString(emptySolutions);
        String payload = solutionsStr + "." + puzzle;

        try {
            client.verify(new VerifyInput().setSolution(payload));
            fail("Expected PrivateCaptchaHttpException");
        } catch (PrivateCaptchaHttpException e) {
            assertEquals("Expected HTTP 400", 400, e.getStatusCode());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVerifyEmptySolution() throws Exception {
        PrivateCaptchaClient client = new PrivateCaptchaClient(
                new PrivateCaptchaConfiguration().setApiKey("test-key")
        );

        client.verify(new VerifyInput());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithEmptyApiKey() {
        new PrivateCaptchaClient(new PrivateCaptchaConfiguration());
    }

    @Test
    public void testRetryBackoff() throws Exception {
        PrivateCaptchaClient client = new PrivateCaptchaClient(
                new PrivateCaptchaConfiguration()
                        .setApiKey("test-key")
                        .setDomain("does-not-exist.qwerty12345-asdfjkl.net")
                        .setConnectTimeout(Duration.ofSeconds(1))
                        .setReadTimeout(Duration.ofSeconds(1))
        );

        VerifyInput input = new VerifyInput()
                .setSolution("asdf")
                .setMaxBackoffSeconds(1)
                .setMaxAttempts(4);

        try {
            client.verify(input);
            fail("Expected VerificationFailedException");
        } catch (VerificationFailedException e) {
            assertEquals("Expected all attempts to be used", input.getMaxAttempts(), e.getAttempts());
        }
    }

    @Test
    public void testCustomFormField() {
        String customFieldName = "my-custom-captcha-field";
        PrivateCaptchaClient client = new PrivateCaptchaClient(
                new PrivateCaptchaConfiguration()
                        .setApiKey("test-key")
                        .setFormField(customFieldName)
        );

        assertEquals("Expected custom form field", customFieldName, client.getFormField());
    }

    @Test
    public void testDefaultFormField() {
        PrivateCaptchaClient client = new PrivateCaptchaClient(
                new PrivateCaptchaConfiguration().setApiKey("test-key")
        );

        assertEquals("Expected default form field", "private-captcha-solution", client.getFormField());
    }

    @Test
    public void testCustomFailedStatusCode() {
        int customStatusCode = 418;
        PrivateCaptchaClient client = new PrivateCaptchaClient(
                new PrivateCaptchaConfiguration()
                        .setApiKey("test-key")
                        .setFailedStatusCode(customStatusCode)
        );

        assertEquals("Expected custom failed status code", customStatusCode, client.getFailedStatusCode());
    }

    @Test
    public void testDefaultFailedStatusCode() {
        PrivateCaptchaClient client = new PrivateCaptchaClient(
                new PrivateCaptchaConfiguration().setApiKey("test-key")
        );

        assertEquals("Expected default 403 status code", 403, client.getFailedStatusCode());
    }

    @Test
    public void testDomainNormalization() {
        PrivateCaptchaClient client1 = new PrivateCaptchaClient(
                new PrivateCaptchaConfiguration()
                        .setApiKey("test-key")
                        .setDomain("https://example.com/")
        );
        assertNotNull(client1);

        PrivateCaptchaClient client2 = new PrivateCaptchaClient(
                new PrivateCaptchaConfiguration()
                        .setApiKey("test-key")
                        .setDomain("http://example.com")
        );
        assertNotNull(client2);

        PrivateCaptchaClient client3 = new PrivateCaptchaClient(
                new PrivateCaptchaConfiguration()
                        .setApiKey("test-key")
                        .setDomain("example.com///")
        );
        assertNotNull(client3);
    }

    @Test
    public void testVerifyCodeEnum() {
        assertEquals(VerifyCode.NO_ERROR, VerifyCode.fromCode(0));
        assertEquals(VerifyCode.ERROR_OTHER, VerifyCode.fromCode(1));
        assertEquals(VerifyCode.DUPLICATE_SOLUTIONS, VerifyCode.fromCode(2));
        assertEquals(VerifyCode.INVALID_SOLUTION, VerifyCode.fromCode(3));
        assertEquals(VerifyCode.PARSE_RESPONSE, VerifyCode.fromCode(4));
        assertEquals(VerifyCode.PUZZLE_EXPIRED, VerifyCode.fromCode(5));
        assertEquals(VerifyCode.INVALID_PROPERTY, VerifyCode.fromCode(6));
        assertEquals(VerifyCode.WRONG_OWNER, VerifyCode.fromCode(7));
        assertEquals(VerifyCode.VERIFIED_BEFORE, VerifyCode.fromCode(8));
        assertEquals(VerifyCode.MAINTENANCE_MODE, VerifyCode.fromCode(9));
        assertEquals(VerifyCode.TEST_PROPERTY, VerifyCode.fromCode(10));
        assertEquals(VerifyCode.INTEGRITY, VerifyCode.fromCode(11));
        assertEquals(VerifyCode.ORG_SCOPE, VerifyCode.fromCode(12));

        assertEquals(VerifyCode.ERROR_OTHER, VerifyCode.fromCode(999));
    }

    @Test
    public void testVerifyCodeErrorStrings() {
        assertEquals("", VerifyCode.NO_ERROR.getErrorString());
        assertEquals("error-other", VerifyCode.ERROR_OTHER.getErrorString());
        assertEquals("solution-duplicates", VerifyCode.DUPLICATE_SOLUTIONS.getErrorString());
        assertEquals("solution-invalid", VerifyCode.INVALID_SOLUTION.getErrorString());
        assertEquals("solution-bad-format", VerifyCode.PARSE_RESPONSE.getErrorString());
        assertEquals("puzzle-expired", VerifyCode.PUZZLE_EXPIRED.getErrorString());
        assertEquals("property-invalid", VerifyCode.INVALID_PROPERTY.getErrorString());
        assertEquals("property-owner-mismatch", VerifyCode.WRONG_OWNER.getErrorString());
        assertEquals("solution-verified-before", VerifyCode.VERIFIED_BEFORE.getErrorString());
        assertEquals("maintenance-mode", VerifyCode.MAINTENANCE_MODE.getErrorString());
        assertEquals("property-test", VerifyCode.TEST_PROPERTY.getErrorString());
        assertEquals("integrity-error", VerifyCode.INTEGRITY.getErrorString());
        assertEquals("org-scope-error", VerifyCode.ORG_SCOPE.getErrorString());
    }

    @Test
    public void testVerifyOutput() {
        VerifyOutput output = new VerifyOutput();

        assertFalse(output.isSuccess());
        assertFalse(output.ok());
        assertEquals(VerifyCode.NO_ERROR, output.getCode());
        assertEquals("", output.getOrigin());
        assertEquals("", output.getTimestamp());
        assertEquals("", output.getTraceId());
        assertEquals(0, output.getAttempts());

        VerifyOutput successOutput = new VerifyOutput(true, VerifyCode.NO_ERROR, "origin", "timestamp", "trace", 1);
        assertTrue(successOutput.ok());
        assertTrue(successOutput.isSuccess());
        assertEquals("origin", successOutput.getOrigin());
        assertEquals("timestamp", successOutput.getTimestamp());
        assertEquals("trace", successOutput.getTraceId());
        assertEquals(1, successOutput.getAttempts());

        VerifyOutput testPropertyOutput = new VerifyOutput(true, VerifyCode.TEST_PROPERTY, "", "", "", 0);
        assertFalse(testPropertyOutput.ok());
        assertEquals("property-test", testPropertyOutput.getErrorMessage());
    }

    @Test
    public void testVerifyInput() {
        VerifyInput input = new VerifyInput();

        assertEquals("", input.getSolution());
        assertEquals("", input.getSitekey());
        assertEquals(20, input.getMaxBackoffSeconds());
        assertEquals(5, input.getMaxAttempts());

        VerifyInput same = input
                .setSolution("solution")
                .setSitekey("sitekey")
                .setMaxBackoffSeconds(30)
                .setMaxAttempts(10);

        assertSame(input, same);
        assertEquals("solution", input.getSolution());
        assertEquals("sitekey", input.getSitekey());
        assertEquals(30, input.getMaxBackoffSeconds());
        assertEquals(10, input.getMaxAttempts());
    }

    @Test
    public void testPrivateCaptchaConfiguration() {
        PrivateCaptchaConfiguration config = new PrivateCaptchaConfiguration();

        assertEquals(Domains.GLOBAL, config.getDomain());
        assertEquals("", config.getApiKey());
        assertEquals("private-captcha-solution", config.getFormField());
        assertEquals(403, config.getFailedStatusCode());
        assertEquals(Duration.ofSeconds(10), config.getConnectTimeout());
        assertEquals(Duration.ofSeconds(30), config.getReadTimeout());

        PrivateCaptchaConfiguration same = config
                .setDomain("example.com")
                .setApiKey("key")
                .setFormField("field")
                .setFailedStatusCode(401)
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(15));

        assertSame(config, same);
        assertEquals("example.com", config.getDomain());
        assertEquals("key", config.getApiKey());
        assertEquals("field", config.getFormField());
        assertEquals(401, config.getFailedStatusCode());
        assertEquals(Duration.ofSeconds(5), config.getConnectTimeout());
        assertEquals(Duration.ofSeconds(15), config.getReadTimeout());
    }

    @Test
    public void testDomains() {
        assertEquals("api.privatecaptcha.com", Domains.GLOBAL);
        assertEquals("api.eu.privatecaptcha.com", Domains.EU);
    }

    @Test
    public void testPrivateCaptchaHttpException() {
        PrivateCaptchaHttpException ex1 = new PrivateCaptchaHttpException(429);
        assertEquals(429, ex1.getStatusCode());
        assertNull(ex1.getRetryAfterSeconds());
        assertEquals("", ex1.getTraceId());
        assertEquals("HTTP error 429", ex1.getMessage());

        PrivateCaptchaHttpException ex2 = new PrivateCaptchaHttpException(500, 30, "trace-123");
        assertEquals(500, ex2.getStatusCode());
        assertEquals(Integer.valueOf(30), ex2.getRetryAfterSeconds());
        assertEquals("trace-123", ex2.getTraceId());
    }

    @Test
    public void testVerificationFailedException() {
        Exception cause = new RuntimeException("network error");
        VerificationFailedException ex = new VerificationFailedException(5, cause);

        assertEquals(5, ex.getAttempts());
        assertEquals("Captcha verification failed after 5 attempts", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void testVerifyRequest() throws Exception {
        PrivateCaptchaClient client = new PrivateCaptchaClient(
                new PrivateCaptchaConfiguration()
                        .setApiKey("test-key")
                        .setFormField("my-field")
        );

        try {
            client.verifyRequest(name -> null);
            fail("Expected IllegalArgumentException for null solution");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("empty"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVerifyRequestNullExtractor() throws Exception {
        PrivateCaptchaClient client = new PrivateCaptchaClient(
                new PrivateCaptchaConfiguration().setApiKey("test-key")
        );

        client.verifyRequest(null);
    }
}
