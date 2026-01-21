# Private Captcha Java Client

[![CI](https://github.com/PrivateCaptcha/private-captcha-java/actions/workflows/ci.yml/badge.svg)](https://github.com/PrivateCaptcha/private-captcha-java/actions/workflows/ci.yml)

Official Java client for the [Private Captcha](https://privatecaptcha.com) API.

## Requirements

- Java 8 or higher

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.privatecaptcha</groupId>
    <artifactId>private-captcha-java</artifactId>
    <version>0.0.1</version>
</dependency>
```

### Gradle

Add the following to your `build.gradle`:

```groovy
implementation 'com.privatecaptcha:private-captcha-java:0.0.1'
```

### Manual Installation

Download the JAR file from the [releases page](https://github.com/PrivateCaptcha/private-captcha-java/releases) and add it to your classpath.

## Quick Start

```java
import com.privatecaptcha.*;

PrivateCaptchaClient client = new PrivateCaptchaClient(
    new PrivateCaptchaConfiguration()
        .setApiKey("pc_your_api_key")
);

try {
    VerifyOutput output = client.verify(new VerifyInput()
        .setSolution(solution));
    
    if (output.ok()) {
        System.out.println("Verification successful!");
    } else {
        System.out.println("Verification failed: " + output.getErrorMessage());
    }
} catch (PrivateCaptchaHttpException e) {
    System.err.println("HTTP error: " + e.getStatusCode());
} catch (VerificationFailedException e) {
    System.err.println("Verification failed after " + e.getAttempts() + " attempts");
}
```

## Configuration Options

```java
import java.time.Duration;

PrivateCaptchaConfiguration config = new PrivateCaptchaConfiguration()
    .setApiKey("pc_your_api_key")
    .setDomain(Domains.GLOBAL)
    .setFormField("private-captcha-solution")
    .setFailedStatusCode(403)
    .setConnectTimeout(Duration.ofSeconds(10))
    .setReadTimeout(Duration.ofSeconds(30));

PrivateCaptchaClient client = new PrivateCaptchaClient(config);
```

## Verification Input Options

```java
VerifyInput input = new VerifyInput()
    .setSolution(solution)
    .setSitekey("your-sitekey")
    .setMaxBackoffSeconds(20)
    .setMaxAttempts(5);

VerifyOutput output = client.verify(input);
```

## Verification Output

```java
VerifyOutput output = client.verify(input);

if (output.ok()) {
    // Success
}

boolean success = output.isSuccess();
VerifyCode code = output.getCode();
String error = output.getErrorMessage();
String origin = output.getOrigin();
String timestamp = output.getTimestamp();
String traceId = output.getTraceId();
int attempts = output.getAttempts();
```

## Error Handling

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
- HTTP 500, 502, 503, 504 (Server errors)
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
        try {
            VerifyOutput output = captchaClient.verifyRequest(request::getParameter);
            
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
git clone https://github.com/PrivateCaptcha/private-captcha-java.git
cd private-captcha-java
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
mvn clean deploy -P release
mvn nexus-staging:release
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- For issues with this Java client, please [open an issue](https://github.com/PrivateCaptcha/private-captcha-java/issues) on GitHub.
- For Private Captcha service questions, visit [privatecaptcha.com](https://privatecaptcha.com)
- For documentation, visit [docs.privatecaptcha.com](https://docs.privatecaptcha.com)