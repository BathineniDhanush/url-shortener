package com.example.shortener.link.api;

import com.example.shortener.link.application.CreateLinkCommand;
import com.example.shortener.link.application.CreateLinkService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/links")
@ConditionalOnProperty(name = "app.runtime.role", havingValue = "api", matchIfMissing = true)
public class LinkController {
    private final CreateLinkService createLinkService;
    private final LinkApiProperties properties;

    public LinkController(CreateLinkService createLinkService, LinkApiProperties properties) {
        this.createLinkService = createLinkService;
        this.properties = properties;
    }

    @PostMapping
    public ResponseEntity<LinkResponse> create(@Valid @RequestBody CreateLinkRequest request) {
        var command = new CreateLinkCommand(request.destinationUrl(), request.customAlias(), request.expiresAt());
        var response = LinkResponse.from(createLinkService.create(command), properties.publicBaseUrl());
        return ResponseEntity.created(response.shortUrl()).body(response);
    }
}
