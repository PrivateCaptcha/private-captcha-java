# Private Captcha Java Client

[![CI](https://github.com/PrivateCaptcha/private-captcha-java/actions/workflows/ci.yml/badge.svg)](https://github.com/PrivateCaptcha/private-captcha-java/actions/workflows/ci.yml)

Official Java client for the [Private Captcha](https://privatecaptcha.com) API.

## Requirements

- Java 11 or higher

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
    new PrivateCaptchaConfiguration("pc_your_api_key")
);

try {
    VerifyOutput output = client.verify(new VerifyInput(solution));
    
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

PrivateCaptchaConfiguration config = new PrivateCaptchaConfiguration("pc_your_api_key")
    .setDomain(Domains.GLOBAL)
    .setFormField("private-captcha-solution")
    .setFailedStatusCode(403)
    .setConnectTimeout(Duration.ofSeconds(10))
    .setReadTimeout(Duration.ofSeconds(30));

PrivateCaptchaClient client = new PrivateCaptchaClient(config);
```

## Verification Input Options

```java
VerifyInput input = new VerifyInput(solution)
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
- HTTP 408 (Request Timeout)
- HTTP 429 (Too Many Requests)
- HTTP 500, 502, 503, 504 (Server errors)

Non-retriable errors (e.g., 400, 401, 403) are thrown immediately.

## Server Integration

The `verifyRequest()` method provides easy integration with any HTTP server framework using the `FormParameterExtractor` functional interface:

### Servlet (Jakarta/Javax)

```java
@WebServlet("/submit-form")
public class FormServlet extends HttpServlet {
    
    private final PrivateCaptchaClient captchaClient = new PrivateCaptchaClient(
        new PrivateCaptchaConfiguration(System.getenv("PC_API_KEY"))
    );
    
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

### Spring MVC

```java
@RestController
public class FormController {
    
    private final PrivateCaptchaClient captchaClient = new PrivateCaptchaClient(
        new PrivateCaptchaConfiguration(System.getenv("PC_API_KEY"))
    );
    
    @PostMapping("/submit-form")
    public ResponseEntity<String> submitForm(HttpServletRequest request) {
        try {
            VerifyOutput output = captchaClient.verifyRequest(request::getParameter);
            
            if (!output.ok()) {
                return ResponseEntity.status(captchaClient.getFailedStatusCode())
                    .body("Captcha verification failed: " + output.getErrorMessage());
            }
            
            return ResponseEntity.ok("Form submitted successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(captchaClient.getFailedStatusCode())
                .body("Captcha verification error");
        }
    }
}
```

### Spring WebFlux

```java
@RestController
public class FormController {
    
    private final PrivateCaptchaClient captchaClient = new PrivateCaptchaClient(
        new PrivateCaptchaConfiguration(System.getenv("PC_API_KEY"))
    );
    
    @PostMapping("/submit-form")
    public Mono<ResponseEntity<String>> submitForm(ServerWebExchange exchange) {
        return exchange.getFormData().map(formData -> {
            try {
                VerifyOutput output = captchaClient.verifyRequest(formData::getFirst);
                
                if (!output.ok()) {
                    return ResponseEntity.status(captchaClient.getFailedStatusCode())
                        .body("Captcha verification failed");
                }
                
                return ResponseEntity.ok("Form submitted successfully!");
            } catch (Exception e) {
                return ResponseEntity.status(captchaClient.getFailedStatusCode())
                    .body("Captcha verification error");
            }
        });
    }
}
```

### Vert.x

```java
router.post("/submit-form").handler(ctx -> {
    try {
        VerifyOutput output = captchaClient.verifyRequest(ctx.request()::getParam);
        
        if (!output.ok()) {
            ctx.response()
                .setStatusCode(captchaClient.getFailedStatusCode())
                .end("Captcha verification failed");
            return;
        }
        
        ctx.response().end("Form submitted successfully!");
    } catch (Exception e) {
        ctx.response()
            .setStatusCode(captchaClient.getFailedStatusCode())
            .end("Captcha verification error");
    }
});
```

### Javalin

```java
app.post("/submit-form", ctx -> {
    try {
        VerifyOutput output = captchaClient.verifyRequest(ctx::formParam);
        
        if (!output.ok()) {
            ctx.status(captchaClient.getFailedStatusCode())
               .result("Captcha verification failed");
            return;
        }
        
        ctx.result("Form submitted successfully!");
    } catch (Exception e) {
        ctx.status(captchaClient.getFailedStatusCode())
           .result("Captcha verification error");
    }
});
```

## Building from Source

```bash
git clone https://github.com/PrivateCaptcha/private-captcha-java.git
cd private-captcha-java

# Build
make build

# Run tests (requires PC_API_KEY environment variable)
make test PC_API_KEY=your_api_key

# Package
make package

# Clean
make clean
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