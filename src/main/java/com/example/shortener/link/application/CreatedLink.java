package com.example.shortener.link.application;

import com.example.shortener.link.domain.Link;

public record CreatedLink(Link link, String ownerToken, long version) {
}
