package com.economydict.config;

import com.economydict.entity.DictionaryEntry;
import com.economydict.repository.DictionaryEntryRepository;
import com.economydict.service.DictionaryMeaningFormatService;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;

@Configuration
public class DictionaryMeaningInitializer {
    private static final Logger log = LoggerFactory.getLogger(DictionaryMeaningInitializer.class);

    @Bean
    public ApplicationRunner reformatDictionaryMeanings(DictionaryEntryRepository dictionaryEntryRepository,
                                                        DictionaryMeaningFormatService dictionaryMeaningFormatService) {
        return args -> {
            List<DictionaryEntry> changed = new ArrayList<>();
            for (DictionaryEntry entry : dictionaryEntryRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))) {
                if (dictionaryMeaningFormatService.applyFormat(entry)) {
                    changed.add(entry);
                }
            }

            if (!changed.isEmpty()) {
                dictionaryEntryRepository.saveAll(changed);
                log.info("Reformatted {} dictionary meanings to the structured glossary format.", changed.size());
            }
        };
    }
}
