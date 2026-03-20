package com.economydict.repository;

import com.economydict.entity.DictionaryEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DictionaryEntryRepository extends JpaRepository<DictionaryEntry, Long> {
    List<DictionaryEntry> findByWordContainingIgnoreCase(String word);
    boolean existsByWordIgnoreCase(String word);
    Optional<DictionaryEntry> findByWordIgnoreCase(String word);
    Page<DictionaryEntry> findByWordContainingIgnoreCaseOrMeaningContainingIgnoreCase(String word, String meaning, Pageable pageable);
}
