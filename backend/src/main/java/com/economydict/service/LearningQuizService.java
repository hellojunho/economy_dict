package com.economydict.service;

import com.economydict.dto.DailyQuizOptionResponse;
import com.economydict.dto.DailyQuizQuestionResponse;
import com.economydict.dto.DailyQuizResponse;
import com.economydict.dto.IncorrectQuizQuestionResponse;
import com.economydict.dto.TopIncorrectWordResponse;
import com.economydict.entity.Quiz;
import com.economydict.entity.QuizOption;
import com.economydict.entity.QuizQuestion;
import com.economydict.entity.User;
import com.economydict.entity.UserQuestionAttempt;
import com.economydict.entity.UserQuestionStatus;
import com.economydict.repository.QuizRepository;
import com.economydict.repository.UserQuestionAttemptRepository;
import com.economydict.repository.UserQuestionStatusRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.security.SecureRandom;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class LearningQuizService {
    private final QuizRepository quizRepository;
    private final UserQuestionStatusRepository userQuestionStatusRepository;
    private final UserQuestionAttemptRepository userQuestionAttemptRepository;
    private final UserService userService;
    private final AnalyticsService analyticsService;
    private final SecureRandom random = new SecureRandom();

    public LearningQuizService(QuizRepository quizRepository,
                               UserQuestionStatusRepository userQuestionStatusRepository,
                               UserQuestionAttemptRepository userQuestionAttemptRepository,
                               UserService userService,
                               AnalyticsService analyticsService) {
        this.quizRepository = quizRepository;
        this.userQuestionStatusRepository = userQuestionStatusRepository;
        this.userQuestionAttemptRepository = userQuestionAttemptRepository;
        this.userService = userService;
        this.analyticsService = analyticsService;
    }

    public Optional<DailyQuizResponse> getDailyQuiz() {
        Optional<Quiz> quizOptional = quizRepository.findFirstByOrderByCreatedAtDesc();
        if (quizOptional.isEmpty()) {
            return Optional.empty();
        }
        User user = userService.getCurrentUser();
        Quiz quiz = quizOptional.get();

        List<DailyQuizQuestionResponse> questions = quiz.getQuestions().stream()
                .sorted(Comparator.comparing(QuizQuestion::getId))
                .map(this::toDailyQuestion)
                .collect(Collectors.toList());

        DailyQuizResponse response = new DailyQuizResponse();
        response.setQuizId(quiz.getQuizId());
        response.setTitle(quiz.getTitle());
        response.setQuestions(questions);
        List<UserQuestionStatus> statuses = userQuestionStatusRepository.findByUserAndQuestion_Quiz_Id(user, quiz.getId());
        response.setSolvedQuestionIds(statuses.stream()
                .filter(UserQuestionStatus::isCorrect)
                .map(status -> status.getQuestion().getId())
                .sorted()
                .collect(Collectors.toList()));
        QuizRecordSummary summary = summarizeStatuses(statuses);
        response.setRecordedCorrectCount(summary.recordedCorrectCount());
        response.setRecordedIncorrectCount(summary.recordedIncorrectCount());
        return Optional.of(response);
    }

    public List<IncorrectQuizQuestionResponse> getIncorrectQuestions() {
        User user = userService.getCurrentUser();
        List<UserQuestionStatus> statuses = userQuestionStatusRepository.findByUser(user);
        Map<Long, UserQuestionStatus> statusById = statuses.stream()
                .collect(Collectors.toMap(UserQuestionStatus::getId, status -> status));
        Map<Long, UserQuestionAttempt> firstAttempts = findFirstAttempts(statusById);
        Map<Long, UserQuestionAttempt> attemptsByQuestionId = new LinkedHashMap<>();

        List<IncorrectQuizQuestionResponse> responses = new ArrayList<>();
        for (UserQuestionAttempt attempt : firstAttempts.values()) {
            if (attempt.isCorrect()) {
                continue;
            }
            UserQuestionStatus status = statusById.get(attempt.getStatus().getId());
            QuizQuestion question = status.getQuestion();
            IncorrectQuizQuestionResponse response = new IncorrectQuizQuestionResponse();
            response.setQuestionId(question.getId());
            response.setQuizId(question.getQuiz().getQuizId());
            response.setQuizTitle(question.getQuiz().getTitle());
            response.setQuestionText(question.getQuestionText());
            response.setOptions(toShuffledDailyOptions(question));
            responses.add(response);
            attemptsByQuestionId.put(question.getId(), attempt);
        }
        responses.sort(Comparator
                .comparing((IncorrectQuizQuestionResponse item) -> attemptsByQuestionId.get(item.getQuestionId()).getAttemptedAt())
                .reversed()
                .thenComparing(IncorrectQuizQuestionResponse::getQuestionId));
        return responses;
    }

    public List<TopIncorrectWordResponse> getTop100() {
        return analyticsService.getTopIncorrectWords();
    }

    private DailyQuizQuestionResponse toDailyQuestion(QuizQuestion question) {
        DailyQuizQuestionResponse response = new DailyQuizQuestionResponse();
        response.setQuestionId(question.getId());
        response.setQuestionText(question.getQuestionText());
        response.setOptions(toShuffledDailyOptions(question));
        return response;
    }

    private DailyQuizOptionResponse toDailyOption(QuizOption option) {
        DailyQuizOptionResponse response = new DailyQuizOptionResponse();
        response.setOptionId(option.getId());
        response.setOptionText(option.getOptionText());
        return response;
    }

    private List<DailyQuizOptionResponse> toShuffledDailyOptions(QuizQuestion question) {
        List<DailyQuizOptionResponse> options = question.getOptions().stream()
                .sorted(Comparator.comparingInt(QuizOption::getOptionOrder))
                .map(this::toDailyOption)
                .collect(Collectors.toCollection(ArrayList::new));
        for (int index = options.size() - 1; index > 0; index -= 1) {
            Collections.swap(options, index, random.nextInt(index + 1));
        }
        return options;
    }

    private QuizRecordSummary summarizeStatuses(List<UserQuestionStatus> statuses) {
        Map<Long, UserQuestionStatus> statusById = statuses.stream()
                .collect(Collectors.toMap(UserQuestionStatus::getId, status -> status));
        Map<Long, UserQuestionAttempt> firstAttempts = findFirstAttempts(statusById);
        long recordedCorrectCount = firstAttempts.values().stream()
                .filter(UserQuestionAttempt::isCorrect)
                .count();
        long recordedIncorrectCount = firstAttempts.values().stream()
                .filter(attempt -> !attempt.isCorrect())
                .count();
        return new QuizRecordSummary(recordedCorrectCount, recordedIncorrectCount);
    }

    private Map<Long, UserQuestionAttempt> findFirstAttempts(Map<Long, UserQuestionStatus> statusById) {
        if (statusById.isEmpty()) {
            return Map.of();
        }
        Map<Long, UserQuestionAttempt> firstAttempts = new LinkedHashMap<>();
        List<Long> statusIds = new ArrayList<>(statusById.keySet());
        for (UserQuestionAttempt attempt : userQuestionAttemptRepository.findByStatus_IdInOrderByAttemptedAtAsc(statusIds)) {
            Long statusId = attempt.getStatus().getId();
            if (firstAttempts.containsKey(statusId)) {
                continue;
            }
            firstAttempts.put(statusId, attempt);
        }
        return firstAttempts;
    }

    private record QuizRecordSummary(long recordedCorrectCount, long recordedIncorrectCount) {
    }
}
