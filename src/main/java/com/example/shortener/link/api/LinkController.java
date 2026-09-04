package com.example.shortener.link.api;

import com.example.shortener.link.application.CreateLinkCommand;
import com.example.shortener.link.application.CreateLinkService;
import com.example.shortener.link.application.ManageLinkService;
import com.example.shortener.link.application.CreationRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/links")
@ConditionalOnProperty(name = "app.runtime.role", havingValue = "api", matchIfMissing = true)
public class LinkController {
    private final CreateLinkService createLinkService;
    private final LinkApiProperties properties;
    private final ManageLinkService manageLinkService;
    private final CreationRateLimiter creationRateLimiter;

    public LinkController(CreateLinkService createLinkService, LinkApiProperties properties,
                          ManageLinkService manageLinkService, CreationRateLimiter creationRateLimiter) {
        this.createLinkService = createLinkService;
        this.properties = properties;
        this.manageLinkService = manageLinkService;
        this.creationRateLimiter = creationRateLimiter;
    }

    @PostMapping
    public ResponseEntity<LinkResponse> create(@Valid @RequestBody CreateLinkRequest request,
                                               HttpServletRequest servletRequest) {
        creationRateLimiter.check(servletRequest.getRemoteAddr());
        var command = new CreateLinkCommand(request.destinationUrl(), request.customAlias(), request.expiresAt());
        var created = createLinkService.create(command);
        var response = LinkResponse.from(created.link(), properties.publicBaseUrl(), created.version(), created.ownerToken());
        return ResponseEntity.created(response.shortUrl()).header("Cache-Control", "no-store").body(response);
    }

    @GetMapping("/{code:[A-Za-z0-9_-]{4,32}}")
    public LinkResponse get(@PathVariable String code,
                            @RequestHeader("X-Link-Owner-Token") String ownerToken) {
        var owned = manageLinkService.get(code, ownerToken);
        return LinkResponse.from(owned.link(), properties.publicBaseUrl(), owned.version(), null);
    }

    @PatchMapping("/{code:[A-Za-z0-9_-]{4,32}}")
    public LinkResponse update(@PathVariable String code,
                               @RequestHeader("X-Link-Owner-Token") String ownerToken,
                               @Valid @RequestBody UpdateLinkRequest request) {
        var owned = manageLinkService.update(code, ownerToken, request.expectedVersion(),
                request.destinationUrl(), request.status(), request.expiresAt());
        return LinkResponse.from(owned.link(), properties.publicBaseUrl(), owned.version(), null);
    }

    @DeleteMapping("/{code:[A-Za-z0-9_-]{4,32}}")
    public ResponseEntity<Void> delete(@PathVariable String code,
                                       @RequestHeader("X-Link-Owner-Token") String ownerToken,
                                       @RequestParam long expectedVersion) {
        manageLinkService.delete(code, ownerToken, expectedVersion);
        return ResponseEntity.noContent().build();
    }
}
