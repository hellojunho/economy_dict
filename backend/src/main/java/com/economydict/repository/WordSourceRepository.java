package com.economydict.repository;

import com.economydict.entity.WordSource;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WordSourceRepository extends JpaRepository<WordSource, Long> {
    Optional<WordSource> findByNameIgnoreCase(String name);
}
