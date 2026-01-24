PC_API_KEY ?=

.PHONY: build test package clean

build:
	mvn -B compile

test:
	@env PC_API_KEY=$(PC_API_KEY) mvn -B test

package:
	mvn -B package -DskipTests

clean:
	mvn -B clean
