# Private Captcha Java Client

[![CI](https://github.com/PrivateCaptcha/private-captcha-java/actions/workflows/ci.yml/badge.svg)](https://github.com/PrivateCaptcha/private-captcha-java/actions/workflows/ci.yml)

[![Maven Central](https://img.shields.io/maven-central/v/com.privatecaptcha/private-captcha-java)](
https://central.sonatype.com/artifact/com.privatecaptcha/private-captcha-java
)

Official Java client for the [Private Captcha](https://privatecaptcha.com) API.

<mark>Please check the [official documentation](https://docs.privatecaptcha.com/docs/integrations/java/) for the in-depth and up-to-date information.</mark>

## Quick Start

- Install `private-captcha-java` using Maven (in `pom.xml`) or Gradle (in `build.gradle`)
- Instantiate `PrivateCaptchaClient` and call `verify()` method
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

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues with this Java client, please open an issue on GitHub.

For Private Captcha service questions, visit [privatecaptcha.com](https://privatecaptcha.com).
