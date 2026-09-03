package com.example.shortener.system;

import com.example.shortener.configuration.RuntimeRole;

public record SystemInfoResponse(String service, String version, RuntimeRole runtimeRole) {
}
