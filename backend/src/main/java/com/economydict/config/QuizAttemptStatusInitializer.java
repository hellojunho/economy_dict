package com.economydict.config;

import com.economydict.entity.UserQuestionAttempt;
import com.economydict.entity.UserQuestionStatus;
import com.economydict.repository.UserQuestionAttemptRepository;
import com.economydict.repository.UserQuestionStatusRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuizAttemptStatusInitializer {
    private static final Logger log = LoggerFactory.getLogger(QuizAttemptStatusInitializer.class);

    @Bean
    public ApplicationRunner normalizeQuizAttemptStatuses(UserQuestionStatusRepository statusRepository,
                                                          UserQuestionAttemptRepository attemptRepository) {
        return args -> {
            List<UserQuestionStatus> statuses = statusRepository.findAll();
            if (statuses.isEmpty()) {
                return;
            }

            List<Long> statusIds = statuses.stream()
                    .map(UserQuestionStatus::getId)
                    .toList();
            List<UserQuestionAttempt> attempts = attemptRepository.findByStatus_IdInOrderByAttemptedAtAsc(statusIds);

            Map<Long, UserQuestionAttempt> firstAttempts = new LinkedHashMap<>();
            Map<Long, UserQuestionAttempt> lastAttempts = new LinkedHashMap<>();
            Map<Long, Integer> attemptCounts = new LinkedHashMap<>();

            for (UserQuestionAttempt attempt : attempts) {
                Long statusId = attempt.getStatus().getId();
                firstAttempts.putIfAbsent(statusId, attempt);
                lastAttempts.put(statusId, attempt);
                attemptCounts.merge(statusId, 1, Integer::sum);
            }

            List<UserQuestionStatus> changed = new ArrayList<>();
            for (UserQuestionStatus status : statuses) {
                UserQuestionAttempt firstAttempt = firstAttempts.get(status.getId());
                if (firstAttempt == null) {
                    continue;
                }

                UserQuestionAttempt lastAttempt = lastAttempts.get(status.getId());
                boolean normalizedCorrect = firstAttempt.isCorrect();
                var normalizedCorrectAt = normalizedCorrect ? firstAttempt.getAttemptedAt() : null;
                var normalizedLastAttemptAt = lastAttempt == null ? status.getLastAttemptAt() : lastAttempt.getAttemptedAt();
                Boolean normalizedLatestRetryCorrect = attemptCounts.getOrDefault(status.getId(), 0) > 1 && lastAttempt != null
                        ? lastAttempt.isCorrect()
                        : null;

                boolean changedStatus = status.isCorrect() != normalizedCorrect
                        || !java.util.Objects.equals(status.getCorrectAt(), normalizedCorrectAt)
                        || !java.util.Objects.equals(status.getLastAttemptAt(), normalizedLastAttemptAt)
                        || !java.util.Objects.equals(status.getLatestRetryCorrect(), normalizedLatestRetryCorrect);

                if (!changedStatus) {
                    continue;
                }

                status.setCorrect(normalizedCorrect);
                status.setCorrectAt(normalizedCorrectAt);
                status.setLastAttemptAt(normalizedLastAttemptAt);
                status.setLatestRetryCorrect(normalizedLatestRetryCorrect);
                changed.add(status);
            }

            if (!changed.isEmpty()) {
                statusRepository.saveAll(changed);
                log.info("Normalized {} quiz question statuses to first-attempt correctness.", changed.size());
            }
        };
    }
}
