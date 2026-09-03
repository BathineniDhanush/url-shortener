package com.example.shortener.link.domain;

import java.util.Optional;

public interface LinkRepository {
    void insert(Link link);
    Optional<Link> findByCode(String code);
}
