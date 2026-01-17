# Private Captcha Java Client

[![CI](https://github.com/PrivateCaptcha/private-captcha-java/actions/workflows/ci.yml/badge.svg)](https://github.com/PrivateCaptcha/private-captcha-java/actions/workflows/ci.yml)

Official Java client for the [Private Captcha](https://privatecaptcha.com) API.

## Requirements

- Java 8 or higher
- No external dependencies required

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.privatecaptcha</groupId>
    <artifactId>private-captcha-java</artifactId>
    <version>0.0.5</version>
</dependency>
```

### Gradle

Add the following to your `build.gradle`:

```groovy
implementation 'com.privatecaptcha:private-captcha-java:0.0.5'
```

### Manual Installation

Download the JAR file from the [releases page](https://github.com/PrivateCaptcha/private-captcha-java/releases) and add it to your classpath.

## Quick Start

```java
import com.privatecaptcha.*;

// Create a client with your API key
PrivateCaptchaClient client = new PrivateCaptchaClient(
    new PrivateCaptchaConfiguration()
        .setApiKey("pc_your_api_key")
);

// Verify a captcha solution
try {
    VerifyOutput output = client.verify(new VerifyInput()
        .setSolution(solution));
    
    if (output.ok()) {
        // Captcha verified successfully
        System.out.println("Verification successful!");
    } else {
        // Verification failed
        System.out.println("Verification failed: " + output.getErrorMessage());
    }
} catch (PrivateCaptchaHttpException e) {
    // HTTP error from the API
    System.err.println("HTTP error: " + e.getStatusCode());
} catch (VerificationFailedException e) {
    // Verification failed after all retry attempts
    System.err.println("Verification failed after " + e.getAttempts() + " attempts");
}
```

## Configuration Options

```java
PrivateCaptchaConfiguration config = new PrivateCaptchaConfiguration()
    // Required: Your API key from Private Captcha account settings
    .setApiKey("pc_your_api_key")
    
    // Optional: Custom domain for self-hosted instances
    .setDomain(Domains.GLOBAL)  // or Domains.EU for EU region
    
    // Optional: Form field name to read captcha solution from
    .setFormField("private-captcha-solution")
    
    // Optional: HTTP status code to return for failed verifications
    .setFailedStatusCode(403)
    
    // Optional: Connection timeout in milliseconds
    .setConnectTimeoutMillis(10000)
    
    // Optional: Read timeout in milliseconds
    .setReadTimeoutMillis(30000);

PrivateCaptchaClient client = new PrivateCaptchaClient(config);
```

## Verification Input Options

```java
VerifyInput input = new VerifyInput()
    // Required: The captcha solution from the client
    .setSolution(solution)
    
    // Optional: Sitekey to verify solution against
    .setSitekey("your-sitekey")
    
    // Optional: Maximum backoff time in seconds (default: 20)
    .setMaxBackoffSeconds(20)
    
    // Optional: Maximum retry attempts (default: 5)
    .setMaxAttempts(5);

VerifyOutput output = client.verify(input);
```

## Verification Output

```java
VerifyOutput output = client.verify(input);

// Check if verification was fully successful
if (output.ok()) {
    // Success - captcha verified
}

// Check individual fields
boolean success = output.isSuccess();      // API reported success
VerifyCode code = output.getCode();        // Verification code
String error = output.getErrorMessage();   // Error message if any
String origin = output.getOrigin();        // Request origin
String timestamp = output.getTimestamp();  // Verification timestamp
String traceId = output.getTraceId();      // Trace ID for debugging
int attempts = output.getAttempts();       // Number of attempts made
```

## Error Handling

The client uses three exception types:

- `IllegalArgumentException` - Invalid input (empty API key, empty solution)
- `PrivateCaptchaHttpException` - Non-retriable HTTP errors from the API
- `VerificationFailedException` - Verification failed after all retry attempts

```java
try {
    VerifyOutput output = client.verify(input);
} catch (IllegalArgumentException e) {
    // Invalid input parameters
} catch (PrivateCaptchaHttpException e) {
    // HTTP error (e.g., 400 Bad Request, 401 Unauthorized)
    int statusCode = e.getStatusCode();
    String traceId = e.getTraceId();
} catch (VerificationFailedException e) {
    // All retry attempts exhausted
    int attempts = e.getAttempts();
    Throwable cause = e.getCause();
}
```

## Retry Behavior

The client automatically retries on:
- Network errors (connection timeouts, unknown hosts)
- HTTP 429 (Too Many Requests)
- HTTP 500 (Internal Server Error)
- HTTP 502 (Bad Gateway)
- HTTP 503 (Service Unavailable)
- HTTP 504 (Gateway Timeout)
- HTTP 408 (Request Timeout)

Non-retriable errors (e.g., 400, 401, 403) are thrown immediately.

## Servlet Integration Example

```java
@WebServlet("/submit-form")
public class FormServlet extends HttpServlet {
    
    private final PrivateCaptchaClient captchaClient;
    
    public FormServlet() {
        captchaClient = new PrivateCaptchaClient(
            new PrivateCaptchaConfiguration()
                .setApiKey(System.getenv("PC_API_KEY"))
        );
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String solution = request.getParameter(captchaClient.getFormField());
        
        try {
            VerifyOutput output = captchaClient.verify(
                new VerifyInput().setSolution(solution)
            );
            
            if (!output.ok()) {
                response.sendError(captchaClient.getFailedStatusCode(), 
                    "Captcha verification failed: " + output.getErrorMessage());
                return;
            }
            
            // Process the form...
            response.getWriter().println("Form submitted successfully!");
            
        } catch (Exception e) {
            response.sendError(captchaClient.getFailedStatusCode(), 
                "Captcha verification error");
        }
    }
}
```

## Building from Source

```bash
# Clone the repository
git clone https://github.com/PrivateCaptcha/private-captcha-java.git
cd private-captcha-java

# Build
mvn clean package

# Run tests (requires PC_API_KEY environment variable for integration tests)
PC_API_KEY=your_api_key mvn test
```

## Publishing to Maven Central

### Prerequisites

1. Create a Sonatype OSSRH account at https://issues.sonatype.org
2. Create a new project ticket for `com.privatecaptcha` group ID
3. Set up GPG key for signing artifacts

### Configuration

Add to your `~/.m2/settings.xml`:

```xml
<settings>
    <servers>
        <server>
            <id>ossrh</id>
            <username>your-sonatype-username</username>
            <password>your-sonatype-password</password>
        </server>
    </servers>
    <profiles>
        <profile>
            <id>ossrh</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <gpg.executable>gpg</gpg.executable>
                <gpg.keyname>your-key-id</gpg.keyname>
            </properties>
        </profile>
    </profiles>
</settings>
```

### Publishing

```bash
# Deploy a snapshot version
mvn clean deploy

# Deploy a release version
mvn clean deploy -P release

# For automated releases, use the nexus-staging-maven-plugin
mvn nexus-staging:release
```

### GitHub Actions Publishing

Add the following secrets to your repository:
- `MAVEN_USERNAME` - Sonatype OSSRH username
- `MAVEN_PASSWORD` - Sonatype OSSRH password
- `GPG_PRIVATE_KEY` - GPG private key (armor format)
- `GPG_PASSPHRASE` - GPG passphrase

Then create a release workflow or use the manual publish process.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- For issues with this Java client, please [open an issue](https://github.com/PrivateCaptcha/private-captcha-java/issues) on GitHub.
- For Private Captcha service questions, visit [privatecaptcha.com](https://privatecaptcha.com)
- For documentation, visit [docs.privatecaptcha.com](https://docs.privatecaptcha.com)