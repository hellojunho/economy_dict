package com.economydict.service;

import com.economydict.dto.DailyQuizQuestionResponse;
import com.economydict.dto.DailyQuizResponse;
import com.economydict.dto.IncorrectWordResponse;
import com.economydict.dto.QuizAnswerItemRequest;
import com.economydict.dto.QuizSubmitAnswersRequest;
import com.economydict.dto.QuizSubmitResultResponse;
import com.economydict.dto.TopIncorrectWordResponse;
import com.economydict.entity.DictionaryEntry;
import com.economydict.entity.QuizHistory;
import com.economydict.entity.User;
import com.economydict.entity.UserLogAction;
import com.economydict.repository.DictionaryEntryRepository;
import com.economydict.repository.QuizHistoryRepository;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LearningQuizService {
    private static final int DAILY_QUIZ_SIZE = 10;
    private final DictionaryEntryRepository dictionaryEntryRepository;
    private final QuizHistoryRepository quizHistoryRepository;
    private final UserService userService;
    private final UserLogService userLogService;
    private final AnalyticsService analyticsService;
    private final SecureRandom random = new SecureRandom();

    public LearningQuizService(DictionaryEntryRepository dictionaryEntryRepository,
                               QuizHistoryRepository quizHistoryRepository,
                               UserService userService,
                               UserLogService userLogService,
                               AnalyticsService analyticsService) {
        this.dictionaryEntryRepository = dictionaryEntryRepository;
        this.quizHistoryRepository = quizHistoryRepository;
        this.userService = userService;
        this.userLogService = userLogService;
        this.analyticsService = analyticsService;
    }

    public DailyQuizResponse getDailyQuiz() {
        List<DictionaryEntry> words = dictionaryEntryRepository.findAll();
        Collections.shuffle(words, random);
        List<DictionaryEntry> selected = words.stream().limit(Math.min(DAILY_QUIZ_SIZE, words.size())).collect(Collectors.toList());
        List<DailyQuizQuestionResponse> questions = new ArrayList<>();
        for (DictionaryEntry word : selected) {
            DailyQuizQuestionResponse item = new DailyQuizQuestionResponse();
            item.setWordId(word.getId());
            item.setTerm(word.getWord());
            item.setOptions(buildOptions(word, words));
            questions.add(item);
        }
        DailyQuizResponse response = new DailyQuizResponse();
        response.setQuestions(questions);
        return response;
    }

    @Transactional
    public QuizSubmitResultResponse submit(QuizSubmitAnswersRequest request) {
        User user = userService.getCurrentUser();
        int correctCount = 0;
        for (QuizAnswerItemRequest answer : request.getAnswers()) {
            DictionaryEntry word = dictionaryEntryRepository.findById(answer.getWordId())
                    .orElseThrow(() -> new IllegalArgumentException("Word not found"));
            boolean correct = word.getMeaning().equals(answer.getSelectedAnswer());
            if (correct) {
                correctCount++;
            }
            QuizHistory history = new QuizHistory();
            history.setUser(user);
            history.setWord(word);
            history.setSelectedAnswer(answer.getSelectedAnswer());
            history.setCorrectAnswer(word.getMeaning());
            history.setCorrect(correct);
            quizHistoryRepository.save(history);
        }
        userLogService.log(user, UserLogAction.QUIZ_SUBMIT, "Submitted daily quiz");
        return new QuizSubmitResultResponse(request.getAnswers().size(), correctCount);
    }

    public List<IncorrectWordResponse> getIncorrectWords() {
        User user = userService.getCurrentUser();
        Map<Long, IncorrectWordResponse> deduplicated = new LinkedHashMap<>();
        for (QuizHistory history : quizHistoryRepository.findByUserAndCorrectFalseOrderByCreatedAtDesc(user)) {
            deduplicated.computeIfAbsent(history.getWord().getId(), id -> {
                IncorrectWordResponse response = new IncorrectWordResponse();
                response.setWordId(history.getWord().getId());
                response.setTerm(history.getWord().getWord());
                response.setDefinition(history.getWord().getMeaning());
                return response;
            });
        }
        return new ArrayList<>(deduplicated.values());
    }

    public List<TopIncorrectWordResponse> getTop100() {
        return analyticsService.getTopIncorrectWords();
    }

    private List<String> buildOptions(DictionaryEntry correctWord, List<DictionaryEntry> allWords) {
        List<String> options = new ArrayList<>();
        options.add(correctWord.getMeaning());
        List<DictionaryEntry> pool = new ArrayList<>(allWords);
        Collections.shuffle(pool, random);
        for (DictionaryEntry candidate : pool) {
            if (options.size() >= 4) {
                break;
            }
            if (candidate.getId().equals(correctWord.getId())) {
                continue;
            }
            if (candidate.getMeaning() == null || candidate.getMeaning().isBlank()) {
                continue;
            }
            if (!options.contains(candidate.getMeaning())) {
                options.add(candidate.getMeaning());
            }
        }
        Collections.shuffle(options, random);
        return options;
    }
}
