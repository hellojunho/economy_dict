package com.economydict.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

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
import org.springframework.boot.test.mock.mockito.MockBean;
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

    @MockBean
    private OpenAiService openAiService;

    @Test
    void importsJsonDictionaryEntriesAndSkipsDuplicates() {
        OpenAiService.DefinitionResult firstSummary = new OpenAiService.DefinitionResult();
        firstSummary.setWord("테스트");
        firstSummary.setMeaning("""
                "쉽게 말해 테스트용으로 확인하는 개념"

                테스트는 시스템이나 개념이 의도한 대로 동작하는지 확인하기 위한 기준 사례를 뜻합니다.

                **핵심 정리**
                - 동작 여부를 빠르게 확인하기 위한 기준입니다.
                - 실제 처리 흐름을 검증할 때 자주 사용됩니다.
                - 예시 데이터나 점검 절차와 함께 쓰입니다.
                """);
        firstSummary.setEnglishWord("Test");
        firstSummary.setEnglishMeaning("A baseline case used to verify intended behavior.");
        given(openAiService.summarizeUploadedTerm(eq("테스트"), eq("테스트입니다."))).willReturn(firstSummary);

        OpenAiService.DefinitionResult duplicateSummary = new OpenAiService.DefinitionResult();
        duplicateSummary.setWord("테스트");
        duplicateSummary.setMeaning("""
                "중복 저장 방지 확인용 설명"

                테스트는 중복 저장이 되지 않아야 하는 검증용 사례입니다.

                **핵심 정리**
                - 중복 저장 방지 검증에 사용됩니다.
                - 기존 데이터 유지 여부를 확인합니다.
                - 동일 단어 재업로드 시 skip 되어야 합니다.
                """);
        duplicateSummary.setEnglishWord("Test");
        duplicateSummary.setEnglishMeaning("A duplicate-check validation case.");
        given(openAiService.summarizeUploadedTerm(eq("테스트"), eq("다른 뜻이어도 중복 저장되면 안 됩니다."))).willReturn(duplicateSummary);

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

        WordUploadStatusResponse firstResponse = pdfImportJobService.submit(firstFile, null, null);
        ImportTask firstTask = importTaskRepository.findById(firstResponse.getFileId()).orElseThrow();
        DictionaryEntry saved = dictionaryEntryRepository.findByWordIgnoreCase("테스트").orElseThrow();

        assertThat(firstTask.getState()).isEqualTo(ImportTaskState.FINISHED);
        assertThat(firstTask.getTotalUnits()).isEqualTo(1);
        assertThat(firstTask.getProcessedUnits()).isEqualTo(1);
        assertThat(saved.getMeaning()).contains("\"쉽게 말해 테스트용으로 확인하는 개념\"");
        assertThat(saved.getEnglishWord()).isEqualTo("Test");
        assertThat(saved.getEnglishMeaning()).isEqualTo("A baseline case used to verify intended behavior.");
        assertThat(saved.getFileType()).isNotNull();
        assertThat(saved.getFileType().getCode()).isEqualTo("JSON_IMPORT");

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

        WordUploadStatusResponse secondResponse = pdfImportJobService.submit(duplicateFile, null, null);
        ImportTask secondTask = importTaskRepository.findById(secondResponse.getFileId()).orElseThrow();

        assertThat(secondTask.getState()).isEqualTo(ImportTaskState.FINISHED);
        assertThat(dictionaryEntryRepository.count()).isEqualTo(1);
        assertThat(dictionaryEntryRepository.findByWordIgnoreCase("테스트").orElseThrow().getMeaning())
                .contains("\"쉽게 말해 테스트용으로 확인하는 개념\"");
    }
}
