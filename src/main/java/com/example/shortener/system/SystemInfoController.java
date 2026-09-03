package com.example.shortener.system;

import com.example.shortener.configuration.RuntimeProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping(path = "/api/v1/system", produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(name = "app.runtime.role", havingValue = "api", matchIfMissing = true)
public class SystemInfoController {

    private final RuntimeProperties runtimeProperties;
    private final Optional<BuildProperties> buildProperties;

    public SystemInfoController(RuntimeProperties runtimeProperties, Optional<BuildProperties> buildProperties) {
        this.runtimeProperties = runtimeProperties;
        this.buildProperties = buildProperties;
    }

    @GetMapping("/info")
    public SystemInfoResponse info() {
        String version = buildProperties.map(BuildProperties::getVersion).orElse("development");
        return new SystemInfoResponse("url-shortener", version, runtimeProperties.role());
    }
}
