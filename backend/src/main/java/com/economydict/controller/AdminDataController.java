package com.economydict.controller;

import com.economydict.dto.AdminOptionDto;
import com.economydict.dto.AdminOptionRequest;
import com.economydict.dto.AdminQuestionDto;
import com.economydict.dto.AdminQuestionRequest;
import com.economydict.dto.AdminQuizDto;
import com.economydict.dto.AdminQuizRequest;
import com.economydict.dto.AdminUserDto;
import com.economydict.dto.AdminUserRequest;
import com.economydict.dto.DictionaryEntryDto;
import com.economydict.dto.PdfImportResponse;
import com.economydict.entity.DictionaryEntry;
import com.economydict.entity.Quiz;
import com.economydict.entity.QuizOption;
import com.economydict.entity.QuizQuestion;
import com.economydict.entity.User;
import com.economydict.entity.UserStatus;
import com.economydict.repository.DictionaryEntryRepository;
import com.economydict.repository.QuizOptionRepository;
import com.economydict.repository.QuizQuestionRepository;
import com.economydict.repository.QuizRepository;
import com.economydict.repository.UserRepository;
import com.economydict.service.PdfImportService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDataController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DictionaryEntryRepository dictionaryEntryRepository;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final PdfImportService pdfImportService;

    public AdminDataController(UserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               DictionaryEntryRepository dictionaryEntryRepository,
                               QuizRepository quizRepository,
                               QuizQuestionRepository quizQuestionRepository,
                               QuizOptionRepository quizOptionRepository,
                               PdfImportService pdfImportService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.dictionaryEntryRepository = dictionaryEntryRepository;
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizOptionRepository = quizOptionRepository;
        this.pdfImportService = pdfImportService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> listUsers() {
        return ResponseEntity.ok(userRepository.findAll().stream().map(this::toAdminUserDto).collect(Collectors.toList()));
    }

    @PostMapping("/users")
    public ResponseEntity<AdminUserDto> createUser(@Valid @RequestBody AdminUserRequest request) {
        if (userRepository.existsByUserId(request.getUserId())) {
            return ResponseEntity.badRequest().build();
        }
        User user = new User();
        user.setUserId(request.getUserId());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword() == null ? "changeme123" : request.getPassword()));
        user.setEmail(request.getEmail());
        user.setProfilePicture(request.getProfilePicture());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus() == null ? UserStatus.ACTIVE : request.getStatus());
        if (user.getStatus() == UserStatus.ACTIVE) {
            user.setActivatedAt(Instant.now());
        }
        return ResponseEntity.ok(toAdminUserDto(userRepository.save(user)));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<AdminUserDto> updateUser(@PathVariable Long id, @Valid @RequestBody AdminUserRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setUserId(request.getUserId());
        user.setUsername(request.getUsername());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setEmail(request.getEmail());
        user.setProfilePicture(request.getProfilePicture());
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        return ResponseEntity.ok(toAdminUserDto(userRepository.save(user)));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dictionary")
    public ResponseEntity<List<DictionaryEntryDto>> listDictionary() {
        return ResponseEntity.ok(dictionaryEntryRepository.findAll().stream().map(this::toDictionaryDto).collect(Collectors.toList()));
    }

    @PostMapping("/dictionary")
    public ResponseEntity<DictionaryEntryDto> createDictionary(@Valid @RequestBody DictionaryEntryDto dto) {
        DictionaryEntry entry = new DictionaryEntry();
        entry.setWord(dto.getWord());
        entry.setMeaning(dto.getMeaning());
        entry.setEnglishWord(dto.getEnglishWord());
        entry.setEnglishMeaning(dto.getEnglishMeaning());
        return ResponseEntity.ok(toDictionaryDto(dictionaryEntryRepository.save(entry)));
    }

    @PostMapping("/dictionary/import-pdf")
    public ResponseEntity<PdfImportResponse> importDictionaryPdf(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(pdfImportService.importPdf(file));
    }

    @PutMapping("/dictionary/{id}")
    public ResponseEntity<DictionaryEntryDto> updateDictionary(@PathVariable Long id, @Valid @RequestBody DictionaryEntryDto dto) {
        DictionaryEntry entry = dictionaryEntryRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Dictionary not found"));
        entry.setWord(dto.getWord());
        entry.setMeaning(dto.getMeaning());
        entry.setEnglishWord(dto.getEnglishWord());
        entry.setEnglishMeaning(dto.getEnglishMeaning());
        return ResponseEntity.ok(toDictionaryDto(dictionaryEntryRepository.save(entry)));
    }

    @DeleteMapping("/dictionary/{id}")
    public ResponseEntity<Void> deleteDictionary(@PathVariable Long id) {
        dictionaryEntryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/quizzes")
    public ResponseEntity<List<AdminQuizDto>> listQuizzes() {
        return ResponseEntity.ok(quizRepository.findAll().stream().map(this::toQuizDto).collect(Collectors.toList()));
    }

    @PostMapping("/quizzes")
    public ResponseEntity<AdminQuizDto> createQuiz(@Valid @RequestBody AdminQuizRequest request) {
        Quiz quiz = new Quiz();
        quiz.setTitle(request.getTitle());
        return ResponseEntity.ok(toQuizDto(quizRepository.save(quiz)));
    }

    @PutMapping("/quizzes/{id}")
    public ResponseEntity<AdminQuizDto> updateQuiz(@PathVariable Long id, @Valid @RequestBody AdminQuizRequest request) {
        Quiz quiz = quizRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Quiz not found"));
        quiz.setTitle(request.getTitle());
        return ResponseEntity.ok(toQuizDto(quizRepository.save(quiz)));
    }

    @DeleteMapping("/quizzes/{id}")
    public ResponseEntity<Void> deleteQuiz(@PathVariable Long id) {
        quizRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/questions")
    public ResponseEntity<List<AdminQuestionDto>> listQuestions() {
        return ResponseEntity.ok(quizQuestionRepository.findAll().stream().map(this::toQuestionDto).collect(Collectors.toList()));
    }

    @PostMapping("/questions")
    public ResponseEntity<AdminQuestionDto> createQuestion(@Valid @RequestBody AdminQuestionRequest request) {
        Quiz quiz = quizRepository.findById(request.getQuizId()).orElseThrow(() -> new IllegalArgumentException("Quiz not found"));
        QuizQuestion question = new QuizQuestion();
        question.setQuiz(quiz);
        question.setQuestionText(request.getQuestionText());
        return ResponseEntity.ok(toQuestionDto(quizQuestionRepository.save(question)));
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<AdminQuestionDto> updateQuestion(@PathVariable Long id, @Valid @RequestBody AdminQuestionRequest request) {
        QuizQuestion question = quizQuestionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Question not found"));
        Quiz quiz = quizRepository.findById(request.getQuizId()).orElseThrow(() -> new IllegalArgumentException("Quiz not found"));
        question.setQuiz(quiz);
        question.setQuestionText(request.getQuestionText());
        return ResponseEntity.ok(toQuestionDto(quizQuestionRepository.save(question)));
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        quizQuestionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/options")
    public ResponseEntity<List<AdminOptionDto>> listOptions() {
        return ResponseEntity.ok(quizOptionRepository.findAll().stream().map(this::toOptionDto).collect(Collectors.toList()));
    }

    @PostMapping("/options")
    public ResponseEntity<AdminOptionDto> createOption(@Valid @RequestBody AdminOptionRequest request) {
        QuizQuestion question = quizQuestionRepository.findById(request.getQuestionId()).orElseThrow(() -> new IllegalArgumentException("Question not found"));
        QuizOption option = new QuizOption();
        option.setQuestion(question);
        option.setOptionText(request.getOptionText());
        option.setOptionOrder(request.getOptionOrder());
        option.setCorrect(request.isCorrect());
        return ResponseEntity.ok(toOptionDto(quizOptionRepository.save(option)));
    }

    @PutMapping("/options/{id}")
    public ResponseEntity<AdminOptionDto> updateOption(@PathVariable Long id, @Valid @RequestBody AdminOptionRequest request) {
        QuizOption option = quizOptionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Option not found"));
        QuizQuestion question = quizQuestionRepository.findById(request.getQuestionId()).orElseThrow(() -> new IllegalArgumentException("Question not found"));
        option.setQuestion(question);
        option.setOptionText(request.getOptionText());
        option.setOptionOrder(request.getOptionOrder());
        option.setCorrect(request.isCorrect());
        return ResponseEntity.ok(toOptionDto(quizOptionRepository.save(option)));
    }

    @DeleteMapping("/options/{id}")
    public ResponseEntity<Void> deleteOption(@PathVariable Long id) {
        quizOptionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private AdminUserDto toAdminUserDto(User user) {
        AdminUserDto dto = new AdminUserDto();
        dto.setId(user.getId());
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setProfilePicture(user.getProfilePicture());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setActivatedAt(user.getActivatedAt());
        dto.setDeactivatedAt(user.getDeactivatedAt());
        return dto;
    }

    private DictionaryEntryDto toDictionaryDto(DictionaryEntry entry) {
        DictionaryEntryDto dto = new DictionaryEntryDto();
        dto.setId(entry.getId());
        dto.setWord(entry.getWord());
        dto.setMeaning(entry.getMeaning());
        dto.setEnglishWord(entry.getEnglishWord());
        dto.setEnglishMeaning(entry.getEnglishMeaning());
        return dto;
    }

    private AdminQuizDto toQuizDto(Quiz quiz) {
        AdminQuizDto dto = new AdminQuizDto();
        dto.setId(quiz.getId());
        dto.setQuizId(quiz.getQuizId());
        dto.setTitle(quiz.getTitle());
        return dto;
    }

    private AdminQuestionDto toQuestionDto(QuizQuestion question) {
        AdminQuestionDto dto = new AdminQuestionDto();
        dto.setId(question.getId());
        dto.setQuizId(question.getQuiz().getId());
        dto.setQuestionText(question.getQuestionText());
        return dto;
    }

    private AdminOptionDto toOptionDto(QuizOption option) {
        AdminOptionDto dto = new AdminOptionDto();
        dto.setId(option.getId());
        dto.setQuestionId(option.getQuestion().getId());
        dto.setOptionText(option.getOptionText());
        dto.setOptionOrder(option.getOptionOrder());
        dto.setCorrect(option.isCorrect());
        return dto;
    }
}
