package com.economydict.service;

import com.economydict.dto.QuizDto;
import com.economydict.dto.QuizOptionDto;
import com.economydict.dto.QuizQuestionDto;
import com.economydict.dto.QuizSubmitRequest;
import com.economydict.dto.QuizSubmitResponse;
import com.economydict.entity.Quiz;
import com.economydict.entity.QuizOption;
import com.economydict.entity.QuizQuestion;
import com.economydict.entity.User;
import com.economydict.entity.UserQuestionAttempt;
import com.economydict.entity.UserQuestionStatus;
import com.economydict.entity.UserQuiz;
import com.economydict.entity.UserQuizStatus;
import com.economydict.repository.QuizOptionRepository;
import com.economydict.repository.QuizQuestionRepository;
import com.economydict.repository.QuizRepository;
import com.economydict.repository.UserQuestionAttemptRepository;
import com.economydict.repository.UserQuestionStatusRepository;
import com.economydict.repository.UserQuizRepository;
import com.economydict.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizService {
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final QuizOptionRepository optionRepository;
    private final UserQuizRepository userQuizRepository;
    private final UserQuestionStatusRepository statusRepository;
    private final UserQuestionAttemptRepository attemptRepository;
    private final UserService userService;
    private final UserRepository userRepository;

    public QuizService(QuizRepository quizRepository,
                       QuizQuestionRepository questionRepository,
                       QuizOptionRepository optionRepository,
                       UserQuizRepository userQuizRepository,
                       UserQuestionStatusRepository statusRepository,
                       UserQuestionAttemptRepository attemptRepository,
                       UserService userService,
                       UserRepository userRepository) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.userQuizRepository = userQuizRepository;
        this.statusRepository = statusRepository;
        this.attemptRepository = attemptRepository;
        this.userService = userService;
        this.userRepository = userRepository;
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
            Instant now = Instant.now();

            status.setLastAttemptAt(now);
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

        if (completed && userQuiz.getStatus() != UserQuizStatus.COMPLETED) {
            userQuiz.setStatus(UserQuizStatus.COMPLETED);
            userQuiz.setCompletedAt(Instant.now());
            userQuizRepository.save(userQuiz);
        }

        return new QuizSubmitResponse(totalQuestions, correctCount, completed);
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
}
