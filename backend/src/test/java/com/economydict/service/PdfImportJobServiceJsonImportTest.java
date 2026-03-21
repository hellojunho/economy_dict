package com.economydict.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.economydict.dto.WordUploadStatusResponse;
import com.economydict.entity.DictionaryEntry;
import com.economydict.entity.ImportTask;
import com.economydict.entity.ImportTaskState;
import com.economydict.repository.DictionaryEntryRepository;
import com.economydict.repository.ImportTaskRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:json-import-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "openai.api.base-url=http://localhost",
        "openai.api.key=test-key",
        "openai.api.model=test-model"
})
class PdfImportJobServiceJsonImportTest {

    @Autowired
    private PdfImportJobService pdfImportJobService;

    @Autowired
    private DictionaryEntryRepository dictionaryEntryRepository;

    @Autowired
    private ImportTaskRepository importTaskRepository;

    @Test
    void importsJsonDictionaryEntriesAndSkipsDuplicates() {
        MockMultipartFile firstFile = new MockMultipartFile(
                "file",
                "chunk_1.json",
                "application/json",
                """
                {
                  "테스트": "테스트입니다."
                }
                """.getBytes(StandardCharsets.UTF_8)
        );

        WordUploadStatusResponse firstResponse = pdfImportJobService.submit(firstFile);
        ImportTask firstTask = importTaskRepository.findById(firstResponse.getFileId()).orElseThrow();
        DictionaryEntry saved = dictionaryEntryRepository.findByWordIgnoreCase("테스트").orElseThrow();

        assertThat(firstTask.getState()).isEqualTo(ImportTaskState.FINISHED);
        assertThat(firstTask.getTotalUnits()).isEqualTo(1);
        assertThat(firstTask.getProcessedUnits()).isEqualTo(1);
        assertThat(saved.getMeaning()).isEqualTo("테스트입니다.");
        assertThat(saved.getSource()).isEqualTo("JSON_IMPORT");

        MockMultipartFile duplicateFile = new MockMultipartFile(
                "file",
                "chunk_2.json",
                "application/json",
                """
                {
                  "테스트": "다른 뜻이어도 중복 저장되면 안 됩니다."
                }
                """.getBytes(StandardCharsets.UTF_8)
        );

        WordUploadStatusResponse secondResponse = pdfImportJobService.submit(duplicateFile);
        ImportTask secondTask = importTaskRepository.findById(secondResponse.getFileId()).orElseThrow();

        assertThat(secondTask.getState()).isEqualTo(ImportTaskState.FINISHED);
        assertThat(dictionaryEntryRepository.count()).isEqualTo(1);
        assertThat(dictionaryEntryRepository.findByWordIgnoreCase("테스트"))
                .get()
                .extracting(DictionaryEntry::getMeaning)
                .isEqualTo("테스트입니다.");
    }
}
