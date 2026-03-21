package com.economydict.service;

import com.economydict.dto.AdminOptionDto;
import com.economydict.dto.AdminQuestionDto;
import com.economydict.dto.AdminQuizDto;
import com.economydict.dto.QuizDto;
import com.economydict.dto.QuizOptionDto;
import com.economydict.dto.QuizQuestionDto;
import com.economydict.dto.QuizSubmitRequest;
import com.economydict.dto.QuizSubmitResponse;
import com.economydict.entity.DictionaryEntry;
import com.economydict.entity.Quiz;
import com.economydict.entity.QuizOption;
import com.economydict.entity.QuizQuestion;
import com.economydict.entity.User;
import com.economydict.entity.UserQuestionAttempt;
import com.economydict.entity.UserQuestionStatus;
import com.economydict.entity.UserQuiz;
import com.economydict.entity.UserQuizStatus;
import com.economydict.repository.DictionaryEntryRepository;
import com.economydict.repository.QuizOptionRepository;
import com.economydict.repository.QuizQuestionRepository;
import com.economydict.repository.QuizRepository;
import com.economydict.repository.UserQuestionAttemptRepository;
import com.economydict.repository.UserQuestionStatusRepository;
import com.economydict.repository.UserQuizRepository;
import com.economydict.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizService {
    private static final int GENERATED_QUIZ_QUESTION_LIMIT = 10;
    private static final int DISTRACTOR_POOL_SIZE = 5;
    private static final DateTimeFormatter QUIZ_TITLE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizOptionRepository optionRepository;
    private final UserQuizRepository userQuizRepository;
    private final UserQuestionStatusRepository statusRepository;
    private final UserQuestionAttemptRepository attemptRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final DictionaryEntryRepository dictionaryEntryRepository;
    private final OpenAiService openAiService;
    private final SecureRandom random = new SecureRandom();

    public QuizService(QuizRepository quizRepository,
                       QuizQuestionRepository questionRepository,
                       QuizOptionRepository optionRepository,
                       UserQuizRepository userQuizRepository,
                       UserQuestionStatusRepository statusRepository,
                       UserQuestionAttemptRepository attemptRepository,
                       UserService userService,
                       UserRepository userRepository,
                       DictionaryEntryRepository dictionaryEntryRepository,
                       OpenAiService openAiService) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.userQuizRepository = userQuizRepository;
        this.statusRepository = statusRepository;
        this.attemptRepository = attemptRepository;
        this.userService = userService;
        this.userRepository = userRepository;
        this.dictionaryEntryRepository = dictionaryEntryRepository;
        this.openAiService = openAiService;
    }

    public List<QuizDto> getAllQuizzes() {
        return quizRepository.findAll().stream().map(this::toDtoSummary).collect(Collectors.toList());
    }

    public QuizDto getQuiz(String quizId) {
        Quiz quiz = quizRepository.findByQuizId(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));
        return toDtoDetail(quiz);
    }

    @Transactional
    public QuizSubmitResponse submit(String quizId, QuizSubmitRequest request) {
        User user = userService.getCurrentUser();
        Quiz quiz = quizRepository.findByQuizId(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));
        boolean submittedCorrect = true;

        UserQuiz userQuiz = userQuizRepository.findByUserAndQuiz(user, quiz)
                .orElseGet(() -> {
                    UserQuiz uq = new UserQuiz();
                    uq.setUser(user);
                    uq.setQuiz(quiz);
                    uq.setStatus(UserQuizStatus.STARTED);
                    uq.setStartedAt(Instant.now());
                    return userQuizRepository.save(uq);
                });

        for (var answer : request.getAnswers()) {
            QuizQuestion question = questionRepository.findById(answer.getQuestionId())
                    .orElseThrow(() -> new IllegalArgumentException("Question not found"));
            if (!question.getQuiz().getId().equals(quiz.getId())) {
                throw new IllegalArgumentException("Question does not belong to quiz");
            }
            QuizOption option = optionRepository.findById(answer.getSelectedOptionId())
                    .orElseThrow(() -> new IllegalArgumentException("Option not found"));
            if (!option.getQuestion().getId().equals(question.getId())) {
                throw new IllegalArgumentException("Option does not belong to question");
            }

            UserQuestionStatus status = statusRepository.findByUserAndQuestion(user, question)
                    .orElseGet(() -> {
                        UserQuestionStatus s = new UserQuestionStatus();
                        s.setUser(user);
                        s.setQuestion(question);
                        s.setCorrect(false);
                        return statusRepository.save(s);
                    });

            boolean isCorrect = option.isCorrect();
            if (!isCorrect) {
                submittedCorrect = false;
            }
            Instant now = Instant.now();
            boolean isRetryAttempt = status.getLastAttemptAt() != null;

            status.setLastAttemptAt(now);
            if (isRetryAttempt) {
                status.setLatestRetryCorrect(isCorrect);
            }
            if (isCorrect && !status.isCorrect()) {
                status.setCorrect(true);
                status.setCorrectAt(now);
            }
            statusRepository.save(status);

            UserQuestionAttempt attempt = new UserQuestionAttempt();
            attempt.setStatus(status);
            attempt.setSelectedOption(option);
            attempt.setCorrect(isCorrect);
            attempt.setAttemptedAt(now);
            attemptRepository.save(attempt);
        }

        List<QuizQuestion> questions = questionRepository.findByQuiz_QuizId(quizId);
        int totalQuestions = questions.size();
        long correctCount = statusRepository.countByUserAndQuestion_Quiz_IdAndCorrectTrue(user, quiz.getId());
        boolean completed = totalQuestions > 0 && correctCount == totalQuestions;
        RecordedQuizStats recordedStats = summarizeRecordedStats(statusRepository.findByUserAndQuestion_Quiz_Id(user, quiz.getId()));

        if (completed && userQuiz.getStatus() != UserQuizStatus.COMPLETED) {
            userQuiz.setStatus(UserQuizStatus.COMPLETED);
            userQuiz.setCompletedAt(Instant.now());
            userQuizRepository.save(userQuiz);
        }

        return new QuizSubmitResponse(
                totalQuestions,
                correctCount,
                completed,
                submittedCorrect,
                recordedStats.recordedCorrectCount(),
                recordedStats.recordedIncorrectCount()
        );
    }

    public List<String> getParticipants(String quizId) {
        Quiz quiz = quizRepository.findByQuizId(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));
        return userQuizRepository.findByQuiz(quiz).stream()
                .map(uq -> uq.getUser().getUserId())
                .collect(Collectors.toList());
    }

    public List<String> getMyQuizzes() {
        User user = userService.getCurrentUser();
        return userQuizRepository.findByUser(user).stream()
                .map(uq -> uq.getQuiz().getQuizId())
                .collect(Collectors.toList());
    }

    public List<String> getUserQuizzes(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return userQuizRepository.findByUser(user).stream()
                .map(uq -> uq.getQuiz().getQuizId())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AdminQuizDto> getAdminQuizzes() {
        return quizRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::toAdminQuizSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminQuizDto getAdminQuiz(Long id) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));
        return toAdminQuizDetail(quiz);
    }

    @Transactional
    public AdminQuizDto generateAdminQuiz() {
        List<DictionaryEntry> candidates = dictionaryEntryRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .filter(this::isUsableDictionaryEntry)
                .collect(Collectors.toList());

        if (candidates.size() < 4) {
            throw new IllegalArgumentException("AI 퀴즈를 생성하려면 뜻이 등록된 용어가 4개 이상 필요합니다.");
        }

        List<DictionaryEntry> selectedEntries = new ArrayList<>(candidates);
        Collections.shuffle(selectedEntries, random);
        selectedEntries = selectedEntries.subList(0, Math.min(GENERATED_QUIZ_QUESTION_LIMIT, selectedEntries.size()));

        List<OpenAiService.QuizGenerationSeed> seeds = buildQuizSeeds(selectedEntries, candidates);
        List<OpenAiService.GeneratedQuizItem> generatedItems = openAiService.generateQuizItems(seeds);
        Map<String, OpenAiService.GeneratedQuizItem> generatedByWord = generatedItems.stream()
                .filter(item -> item.getWord() != null && !item.getWord().isBlank())
                .collect(Collectors.toMap(
                        item -> item.getWord().trim(),
                        item -> item,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
        if (generatedByWord.size() < seeds.size()) {
            throw new IllegalStateException("AI가 일부 퀴즈 문항을 누락했습니다. 다시 시도하세요.");
        }

        Quiz quiz = new Quiz();
        quiz.setTitle("AI Quiz " + LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(QUIZ_TITLE_FORMATTER));

        for (OpenAiService.QuizGenerationSeed seed : seeds) {
            OpenAiService.GeneratedQuizItem item = generatedByWord.get(seed.getWord());
            String questionText = buildQuestionText(seed, item);
            List<String> options = buildOptions(seed, item);

            QuizQuestion question = new QuizQuestion();
            question.setQuiz(quiz);
            question.setQuestionText(questionText);
            quiz.getQuestions().add(question);

            for (int index = 0; index < options.size(); index++) {
                String optionText = options.get(index);
                QuizOption option = new QuizOption();
                option.setQuestion(question);
                option.setOptionText(optionText);
                option.setOptionOrder(index + 1);
                option.setCorrect(seed.getWord().equals(optionText));
                question.getOptions().add(option);
            }
        }

        Quiz saved = quizRepository.save(quiz);
        return toAdminQuizDetail(saved);
    }

    private QuizDto toDtoSummary(Quiz quiz) {
        QuizDto dto = new QuizDto();
        dto.setQuizId(quiz.getQuizId());
        dto.setTitle(quiz.getTitle());
        return dto;
    }

    private QuizDto toDtoDetail(Quiz quiz) {
        QuizDto dto = new QuizDto();
        dto.setQuizId(quiz.getQuizId());
        dto.setTitle(quiz.getTitle());
        dto.setQuestions(quiz.getQuestions().stream().map(this::toQuestionDto).collect(Collectors.toList()));
        return dto;
    }

    private QuizQuestionDto toQuestionDto(QuizQuestion question) {
        QuizQuestionDto dto = new QuizQuestionDto();
        dto.setId(question.getId());
        dto.setQuestionText(question.getQuestionText());
        dto.setOptions(question.getOptions().stream().map(this::toOptionDto).collect(Collectors.toList()));
        return dto;
    }

    private QuizOptionDto toOptionDto(QuizOption option) {
        QuizOptionDto dto = new QuizOptionDto();
        dto.setId(option.getId());
        dto.setOptionText(option.getOptionText());
        dto.setOptionOrder(option.getOptionOrder());
        return dto;
    }

    private boolean isUsableDictionaryEntry(DictionaryEntry entry) {
        return normalize(entry.getWord()) != null && normalize(entry.getMeaning()) != null;
    }

    private List<OpenAiService.QuizGenerationSeed> buildQuizSeeds(List<DictionaryEntry> selectedEntries,
                                                                  List<DictionaryEntry> allEntries) {
        List<OpenAiService.QuizGenerationSeed> seeds = new ArrayList<>();
        for (DictionaryEntry entry : selectedEntries) {
            List<DictionaryEntry> distractorPool = allEntries.stream()
                    .filter(candidate -> !candidate.getId().equals(entry.getId()))
                    .sorted(Comparator.comparing(DictionaryEntry::getId))
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.shuffle(distractorPool, random);

            OpenAiService.QuizGenerationSeed seed = new OpenAiService.QuizGenerationSeed();
            seed.setWord(entry.getWord().trim());
            seed.setMeaning(compactMeaning(entry.getMeaning()));

            List<OpenAiService.QuizCandidate> distractorCandidates = distractorPool.stream()
                    .limit(DISTRACTOR_POOL_SIZE)
                    .map(candidate -> {
                        OpenAiService.QuizCandidate quizCandidate = new OpenAiService.QuizCandidate();
                        quizCandidate.setWord(candidate.getWord().trim());
                        quizCandidate.setMeaning(compactMeaning(candidate.getMeaning()));
                        return quizCandidate;
                    })
                    .collect(Collectors.toList());
            seed.setDistractorCandidates(distractorCandidates);
            seeds.add(seed);
        }
        return seeds;
    }

    private String buildQuestionText(OpenAiService.QuizGenerationSeed seed, OpenAiService.GeneratedQuizItem item) {
        String questionText = normalize(item == null ? null : item.getQuestionText());
        if (questionText != null && !questionText.contains(seed.getWord())) {
            return questionText.length() <= 500 ? questionText : questionText.substring(0, 500);
        }
        String fallback = seed.getMeaning() + " 위 설명에 해당하는 경제 용어는 무엇인가?";
        return fallback.length() <= 500 ? fallback : fallback.substring(0, 500);
    }

    private List<String> buildOptions(OpenAiService.QuizGenerationSeed seed, OpenAiService.GeneratedQuizItem item) {
        Set<String> allowedOptions = new LinkedHashSet<>();
        allowedOptions.add(seed.getWord());
        for (OpenAiService.QuizCandidate candidate : seed.getDistractorCandidates()) {
            String candidateWord = normalize(candidate.getWord());
            if (candidateWord != null) {
                allowedOptions.add(candidateWord);
            }
        }

        LinkedHashSet<String> sanitizedOptions = new LinkedHashSet<>();
        if (item != null && item.getOptions() != null) {
            for (String option : item.getOptions()) {
                String normalizedOption = normalize(option);
                if (normalizedOption != null && allowedOptions.contains(normalizedOption)) {
                    sanitizedOptions.add(normalizedOption);
                }
            }
        }
        sanitizedOptions.add(seed.getWord());

        for (OpenAiService.QuizCandidate candidate : seed.getDistractorCandidates()) {
            if (sanitizedOptions.size() >= 4) {
                break;
            }
            String candidateWord = normalize(candidate.getWord());
            if (candidateWord != null) {
                sanitizedOptions.add(candidateWord);
            }
        }

        if (sanitizedOptions.size() < 4) {
            throw new IllegalStateException("AI가 유효한 퀴즈 보기를 생성하지 못했습니다. 다시 시도하세요.");
        }

        List<String> randomizedOptions = new ArrayList<>(new ArrayList<>(sanitizedOptions).subList(0, 4));
        Collections.shuffle(randomizedOptions, random);
        return randomizedOptions;
    }

    private AdminQuizDto toAdminQuizSummary(Quiz quiz) {
        AdminQuizDto dto = new AdminQuizDto();
        dto.setId(quiz.getId());
        dto.setQuizId(quiz.getQuizId());
        dto.setTitle(quiz.getTitle());
        dto.setQuestionCount(quiz.getQuestions().size());
        dto.setParticipantCount(userQuizRepository.findByQuiz(quiz).size());
        dto.setCreatedAt(quiz.getCreatedAt());
        return dto;
    }

    private AdminQuizDto toAdminQuizDetail(Quiz quiz) {
        AdminQuizDto dto = toAdminQuizSummary(quiz);
        List<UserQuestionStatus> statuses = statusRepository.findByQuestion_Quiz_Id(quiz.getId());
        Map<Long, List<UserQuestionStatus>> statusesByQuestionId = statuses.stream()
                .collect(Collectors.groupingBy(status -> status.getQuestion().getId()));

        dto.setQuestions(quiz.getQuestions().stream()
                .sorted(Comparator.comparing(QuizQuestion::getId))
                .map(question -> toAdminQuestionDto(question, statusesByQuestionId.getOrDefault(question.getId(), List.of())))
                .collect(Collectors.toList()));
        return dto;
    }

    private AdminQuestionDto toAdminQuestionDto(QuizQuestion question, List<UserQuestionStatus> statuses) {
        AdminQuestionDto dto = new AdminQuestionDto();
        dto.setId(question.getId());
        dto.setQuizId(question.getQuiz().getId());
        dto.setQuestionText(question.getQuestionText());
        dto.setAttemptedUsers(statuses.size());
        dto.setCorrectUsers(statuses.stream().filter(UserQuestionStatus::isCorrect).count());
        dto.setCorrectRate(statuses.isEmpty() ? 0.0 : (double) dto.getCorrectUsers() / statuses.size());
        dto.setParticipants(statuses.stream()
                .map(status -> status.getUser().getUserId())
                .sorted()
                .collect(Collectors.toList()));
        dto.setCorrectParticipants(statuses.stream()
                .filter(UserQuestionStatus::isCorrect)
                .map(status -> status.getUser().getUserId())
                .sorted()
                .collect(Collectors.toList()));
        dto.setOptions(question.getOptions().stream()
                .sorted(Comparator.comparingInt(QuizOption::getOptionOrder))
                .map(this::toAdminOptionDto)
                .collect(Collectors.toList()));
        return dto;
    }

    private AdminOptionDto toAdminOptionDto(QuizOption option) {
        AdminOptionDto dto = new AdminOptionDto();
        dto.setId(option.getId());
        dto.setQuestionId(option.getQuestion().getId());
        dto.setOptionText(option.getOptionText());
        dto.setOptionOrder(option.getOptionOrder());
        dto.setCorrect(option.isCorrect());
        dto.setSelectedCount(0L);
        return dto;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String compactMeaning(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return "";
        }
        String singleLine = normalized.replaceAll("\\s+", " ");
        return singleLine.length() <= 220 ? singleLine : singleLine.substring(0, 220);
    }

    private RecordedQuizStats summarizeRecordedStats(List<UserQuestionStatus> statuses) {
        if (statuses.isEmpty()) {
            return new RecordedQuizStats(0, 0);
        }
        Map<Long, UserQuestionAttempt> firstAttempts = new LinkedHashMap<>();
        List<Long> statusIds = statuses.stream()
                .map(UserQuestionStatus::getId)
                .collect(Collectors.toList());
        for (UserQuestionAttempt attempt : attemptRepository.findByStatus_IdInOrderByAttemptedAtAsc(statusIds)) {
            firstAttempts.putIfAbsent(attempt.getStatus().getId(), attempt);
        }
        long recordedCorrectCount = firstAttempts.values().stream()
                .filter(UserQuestionAttempt::isCorrect)
                .count();
        long recordedIncorrectCount = firstAttempts.values().stream()
                .filter(attempt -> !attempt.isCorrect())
                .count();
        return new RecordedQuizStats(recordedCorrectCount, recordedIncorrectCount);
    }

    private record RecordedQuizStats(long recordedCorrectCount, long recordedIncorrectCount) {
    }
}
