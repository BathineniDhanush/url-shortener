package com.example.shortener.link.domain;

import java.util.Optional;

public interface LinkRepository {
    void insert(OwnedLink link);
    Optional<Link> findByCode(String code);
    Optional<OwnedLink> findOwnedByCode(String code);
    boolean update(OwnedLink link, long expectedVersion);
}
