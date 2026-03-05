package com.economydict.repository;

import com.economydict.entity.DictionaryEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DictionaryEntryRepository extends JpaRepository<DictionaryEntry, Long> {
    List<DictionaryEntry> findByWordContainingIgnoreCase(String word);
}
