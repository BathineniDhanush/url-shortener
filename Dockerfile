FROM maven:3-eclipse-temurin-26-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B package -Dmaven.test.skip=true

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN apk upgrade --no-cache \
    && addgroup -S app \
    && adduser -S app -G app
COPY --from=build /workspace/target/url-shortener-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
