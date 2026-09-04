package com.example.shortener.function;

import com.example.shortener.UrlShortenerApplication;
import com.example.shortener.analytics.application.AnalyticsService;
import com.example.shortener.configuration.RuntimeProperties;
import com.example.shortener.domain.analytics.ClickEvent;
import com.example.shortener.domain.analytics.AnalyticsRepository;
import com.example.shortener.domain.util.PrivacyUtils;
import com.example.shortener.link.api.CreateLinkRequest;
import com.example.shortener.link.api.LinkApiProperties;
import com.example.shortener.link.api.LinkResponse;
import com.example.shortener.link.api.UpdateLinkRequest;
import com.example.shortener.link.application.CreateLinkCommand;
import com.example.shortener.link.application.CreateLinkService;
import com.example.shortener.link.application.CreationRateLimiter;
import com.example.shortener.link.application.ManageLinkService;
import com.example.shortener.link.error.AliasConflictException;
import com.example.shortener.link.error.CodeGenerationException;
import com.example.shortener.link.error.ConcurrentLinkUpdateException;
import com.example.shortener.link.error.InvalidDestinationUrlException;
import com.example.shortener.link.error.InvalidExpirationException;
import com.example.shortener.link.error.InvalidLinkUpdateException;
import com.example.shortener.link.error.LinkAccessDeniedException;
import com.example.shortener.link.error.LinkNotFoundException;
import com.example.shortener.link.error.LinkUnavailableException;
import com.example.shortener.link.error.RateLimitExceededException;
import com.example.shortener.redirect.application.ResolveLinkService;
import com.example.shortener.system.SystemInfoResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Azure Functions HTTP boundary for the existing Spring application services.
 *
 * <p>The function deliberately does not emulate a servlet container. It keeps the
 * public HTTP contract explicit while reusing the domain, persistence, validation,
 * cache, and analytics components from the Spring Boot application.</p>
 */
public class UrlShortenerHttpFunction {
    private static final Logger log = LoggerFactory.getLogger(UrlShortenerHttpFunction.class);
    private static final ObjectMapper RESPONSE_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static volatile FunctionBeans functionBeans;
    private static final Pattern LINK_PATH = Pattern.compile("^/api/v1/links/([A-Za-z0-9_-]{4,32})$");
    private static final Pattern ANALYTICS_PATH = Pattern.compile(
            "^/api/v1/links/([A-Za-z0-9_-]{4,32})/analytics$");
    private static final Pattern REDIRECT_PATH = Pattern.compile("^/([A-Za-z0-9_-]{4,32})$");
    private static final String OWNER_TOKEN_HEADER = "X-Link-Owner-Token";

    @FunctionName("UrlShortenerApi")
    public HttpResponseMessage handle(
            @HttpTrigger(
                    name = "request",
                    methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PATCH},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "{*path}") HttpRequestMessage<Optional<String>> request,
            ExecutionContext executionContext) {
        try {
            return route(request);
        } catch (Exception exception) {
            executionContext.getLogger().warning(
                    "Request failed: " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
            return errorResponse(request, exception);
        }
    }

    private HttpResponseMessage route(HttpRequestMessage<Optional<String>> request) throws IOException {
        String path = normalizePath(request.getUri().getPath());
        HttpMethod method = request.getHttpMethod();

        if (method == HttpMethod.POST && path.equals("/api/v1/links")) {
            return createLink(request);
        }
        if (method == HttpMethod.GET && path.equals("/api/v1/system/info")) {
            return systemInfo(request);
        }
        if (method == HttpMethod.GET && path.equals("/openapi.yaml")) {
            return openApi(request);
        }
        if (method == HttpMethod.GET && path.equals("/actuator/health")) {
            return health(request);
        }

        Matcher analyticsMatcher = ANALYTICS_PATH.matcher(path);
        if (method == HttpMethod.GET && analyticsMatcher.matches()) {
            return analytics(request, analyticsMatcher.group(1));
        }

        Matcher linkMatcher = LINK_PATH.matcher(path);
        if (linkMatcher.matches()) {
            if (method == HttpMethod.GET) {
                return getLink(request, linkMatcher.group(1));
            }
            if (method == HttpMethod.PATCH) {
                return updateLink(request, linkMatcher.group(1));
            }
        }

        Matcher redirectMatcher = REDIRECT_PATH.matcher(path);
        if (method == HttpMethod.GET && redirectMatcher.matches()) {
            return redirect(request, redirectMatcher.group(1));
        }

        return problem(request, HttpStatus.NOT_FOUND, "Route not found",
                "No API route matches " + method + " " + path, List.of(), null);
    }

    private HttpResponseMessage createLink(HttpRequestMessage<Optional<String>> request) throws JsonProcessingException {
        CreateLinkRequest body = readBody(request, CreateLinkRequest.class);
        validate(body);
        beans().creationRateLimiter.check(clientIp(request));
        var created = beans().createLinkService.create(
                new CreateLinkCommand(body.destinationUrl(), body.customAlias(), body.expiresAt()));
        LinkResponse response = LinkResponse.from(created.link(), beans().linkProperties.publicBaseUrl(),
                created.version(), created.ownerToken());
        return request.createResponseBuilder(HttpStatus.CREATED)
                .header("Content-Type", "application/json")
                .header("Location", response.shortUrl().toString())
                .header("Cache-Control", "no-store")
                .body(writeJson(response))
                .build();
    }

    private HttpResponseMessage getLink(HttpRequestMessage<Optional<String>> request, String code) {
        var owned = beans().manageLinkService.get(code, ownerToken(request));
        return json(request, HttpStatus.OK, LinkResponse.from(
                owned.link(), beans().linkProperties.publicBaseUrl(), owned.version(), null));
    }

    private HttpResponseMessage updateLink(HttpRequestMessage<Optional<String>> request, String code)
            throws JsonProcessingException {
        UpdateLinkRequest body = readBody(request, UpdateLinkRequest.class);
        validate(body);
        var owned = beans().manageLinkService.update(code, ownerToken(request), body.expectedVersion(),
                body.destinationUrl(), body.status(), body.expiresAt());
        return json(request, HttpStatus.OK, LinkResponse.from(
                owned.link(), beans().linkProperties.publicBaseUrl(), owned.version(), null));
    }

    private HttpResponseMessage analytics(HttpRequestMessage<Optional<String>> request, String code) {
        var link = beans().manageLinkService.get(code, ownerToken(request)).link();
        long totalClicks = beans().analyticsService.getTotalClicks(code, link.id());
        return json(request, HttpStatus.OK, Map.of("code", code, "totalClicks", totalClicks));
    }

    private HttpResponseMessage redirect(HttpRequestMessage<Optional<String>> request, String code) {
        var link = beans().resolveLinkService.resolve(code);
        ClickEvent event = new ClickEvent(
                UUID.randomUUID(), link.id(), Instant.now(),
                PrivacyUtils.anonymizeIp(clientIp(request)),
                PrivacyUtils.filterUserAgent(header(request, "User-Agent").orElse(null)));
        try {
            beans().analyticsRepository.save(event);
        } catch (Exception exception) {
            log.warn("Failed to persist click analytics: {}", exception.getMessage());
        }
        return request.createResponseBuilder(HttpStatus.FOUND)
                .header("Location", URI.create(link.destinationUrl()).toString())
                .header("Cache-Control", "no-store")
                .header("Referrer-Policy", "no-referrer")
                .build();
    }

    private HttpResponseMessage systemInfo(HttpRequestMessage<Optional<String>> request) {
        String version = beans().buildProperties.map(BuildProperties::getVersion).orElse("development");
        return json(request, HttpStatus.OK, new SystemInfoResponse(
                "url-shortener", version, beans().runtimeProperties.role()));
    }

    private HttpResponseMessage openApi(HttpRequestMessage<Optional<String>> request) throws IOException {
        try (InputStream stream = UrlShortenerHttpFunction.class.getResourceAsStream("/static/openapi.yaml")) {
            if (stream == null) {
                throw new IllegalStateException("OpenAPI document is missing from the application package");
            }
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/yaml; charset=utf-8")
                    .body(new String(stream.readAllBytes(), StandardCharsets.UTF_8))
                    .build();
        }
    }

    private HttpResponseMessage health(HttpRequestMessage<Optional<String>> request) {
        try (Connection ignored = beans().dataSource.getConnection()) {
            return json(request, HttpStatus.OK, Map.of("status", "UP"));
        } catch (Exception exception) {
            return json(request, HttpStatus.SERVICE_UNAVAILABLE, Map.of("status", "DOWN"));
        }
    }

    private <T> T readBody(HttpRequestMessage<Optional<String>> request, Class<T> bodyType)
            throws JsonProcessingException {
        String body = request.getBody().filter(value -> !value.isBlank())
                .orElseThrow(() -> new InvalidRequestException("A JSON request body is required"));
        return beans().objectMapper.readValue(body, bodyType);
    }

    private <T> void validate(T body) {
        List<String> errors = beans().validator.validate(body).stream()
                .map(this::validationMessage)
                .sorted()
                .toList();
        if (!errors.isEmpty()) {
            throw new RequestValidationException(errors);
        }
    }

    private String validationMessage(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + ": " + violation.getMessage();
    }

    private String ownerToken(HttpRequestMessage<Optional<String>> request) {
        return header(request, OWNER_TOKEN_HEADER)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new MissingOwnerTokenException());
    }

    private String clientIp(HttpRequestMessage<Optional<String>> request) {
        String forwarded = header(request, "X-Forwarded-For").orElse(null);
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",", 2)[0].trim();
        }
        return header(request, "X-Azure-ClientIP").orElse("unknown");
    }

    private Optional<String> header(HttpRequestMessage<?> request, String name) {
        return request.getHeaders().entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private HttpResponseMessage errorResponse(HttpRequestMessage<Optional<String>> request, Exception exception) {
        if (exception instanceof RequestValidationException validation) {
            return problem(request, HttpStatus.BAD_REQUEST, "Request validation failed",
                    "One or more request fields are invalid", validation.errors, null);
        }
        if (exception instanceof JsonProcessingException || exception instanceof InvalidRequestException) {
            return problem(request, HttpStatus.BAD_REQUEST, "Invalid request",
                    "The request body must contain valid JSON", List.of(), null);
        }
        if (exception instanceof InvalidDestinationUrlException
                || exception instanceof InvalidExpirationException
                || exception instanceof InvalidLinkUpdateException) {
            return problem(request, HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage(), List.of(), null);
        }
        if (exception instanceof MissingOwnerTokenException) {
            return problem(request, HttpStatus.FORBIDDEN, "Link access denied",
                    "A valid link owner token is required", List.of(), null);
        }
        if (exception instanceof LinkAccessDeniedException) {
            return problem(request, HttpStatus.FORBIDDEN, "Link access denied", exception.getMessage(), List.of(), null);
        }
        if (exception instanceof AliasConflictException) {
            return problem(request, HttpStatus.CONFLICT, "Short code conflict", exception.getMessage(), List.of(), null);
        }
        if (exception instanceof ConcurrentLinkUpdateException) {
            return problem(request, HttpStatus.CONFLICT, "Concurrent link update", exception.getMessage(), List.of(), null);
        }
        if (exception instanceof RateLimitExceededException rateLimit) {
            return problem(request, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded",
                    exception.getMessage(), List.of(), Long.toString(rateLimit.retryAfterSeconds()));
        }
        if (exception instanceof CodeGenerationException) {
            return problem(request, HttpStatus.SERVICE_UNAVAILABLE, "Code allocation unavailable",
                    exception.getMessage(), List.of(), null);
        }
        if (exception instanceof LinkNotFoundException) {
            return problem(request, HttpStatus.NOT_FOUND, "Short link not found", exception.getMessage(), List.of(), null);
        }
        if (exception instanceof LinkUnavailableException) {
            return problem(request, HttpStatus.GONE, "Short link unavailable", exception.getMessage(), List.of(), null);
        }
        return problem(request, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "The request could not be completed", List.of(), null);
    }

    private HttpResponseMessage problem(HttpRequestMessage<Optional<String>> request, HttpStatus status,
                                        String title, String detail, List<String> errors, String retryAfter) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://url-shortener.example/problems/" + status.value());
        body.put("title", title);
        body.put("status", status.value());
        body.put("detail", detail);
        if (!errors.isEmpty()) {
            body.put("errors", errors);
        }
        HttpResponseMessage.Builder builder = request.createResponseBuilder(status)
                .header("Content-Type", "application/problem+json")
                .body(writeJson(body));
        if (retryAfter != null) {
            builder.header("Retry-After", retryAfter);
        }
        return builder.build();
    }

    private HttpResponseMessage json(HttpRequestMessage<Optional<String>> request, HttpStatus status, Object body) {
        return request.createResponseBuilder(status)
                .header("Content-Type", "application/json")
                .body(writeJson(body))
                .build();
    }

    private String writeJson(Object body) {
        try {
            return RESPONSE_MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize the response", exception);
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private FunctionBeans beans() {
        FunctionBeans current = functionBeans;
        if (current == null) {
            synchronized (UrlShortenerHttpFunction.class) {
                current = functionBeans;
                if (current == null) {
                    ConfigurableApplicationContext context = new SpringApplicationBuilder(
                            UrlShortenerApplication.class)
                            .web(WebApplicationType.NONE)
                            .properties(
                                    "app.runtime.role=api",
                                    "spring.autoconfigure.exclude="
                                            + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                                            + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration")
                            .run();
                    current = new FunctionBeans(context);
                    functionBeans = current;
                }
            }
        }
        return current;
    }

    private static final class FunctionBeans {
        private final CreateLinkService createLinkService;
        private final ManageLinkService manageLinkService;
        private final ResolveLinkService resolveLinkService;
        private final AnalyticsService analyticsService;
        private final CreationRateLimiter creationRateLimiter;
        private final AnalyticsRepository analyticsRepository;
        private final LinkApiProperties linkProperties;
        private final RuntimeProperties runtimeProperties;
        private final Optional<BuildProperties> buildProperties;
        private final ObjectMapper objectMapper;
        private final Validator validator;
        private final DataSource dataSource;

        private FunctionBeans(ConfigurableApplicationContext context) {
            createLinkService = context.getBean(CreateLinkService.class);
            manageLinkService = context.getBean(ManageLinkService.class);
            resolveLinkService = context.getBean(ResolveLinkService.class);
            analyticsService = context.getBean(AnalyticsService.class);
            creationRateLimiter = context.getBean(CreationRateLimiter.class);
            analyticsRepository = context.getBean(AnalyticsRepository.class);
            linkProperties = context.getBean(LinkApiProperties.class);
            runtimeProperties = context.getBean(RuntimeProperties.class);
            buildProperties = context.getBeanProvider(BuildProperties.class).stream().findFirst();
            objectMapper = context.getBean(ObjectMapper.class);
            validator = context.getBean(Validator.class);
            dataSource = context.getBean(DataSource.class);
        }
    }

    private static final class MissingOwnerTokenException extends RuntimeException {
    }

    private static final class InvalidRequestException extends RuntimeException {
        private InvalidRequestException(String message) {
            super(message);
        }
    }

    private static final class RequestValidationException extends RuntimeException {
        private final List<String> errors;

        private RequestValidationException(List<String> errors) {
            this.errors = List.copyOf(errors);
        }
    }
}
