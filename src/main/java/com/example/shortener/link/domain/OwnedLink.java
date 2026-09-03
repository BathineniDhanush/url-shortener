package com.example.shortener.link.domain;

public record OwnedLink(Link link, String ownerTokenHash, long version) {
}
