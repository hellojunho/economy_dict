package com.economydict.service;

import com.economydict.dto.DailyUserStatResponse;
import com.economydict.dto.TopIncorrectWordResponse;
import com.economydict.entity.DailyUserStat;
import com.economydict.entity.DictionaryEntry;
import com.economydict.entity.TopIncorrectWord;
import com.economydict.entity.User;
import com.economydict.entity.UserLogAction;
import com.economydict.repository.DailyUserStatRepository;
import com.economydict.repository.QuizHistoryRepository;
import com.economydict.repository.TopIncorrectWordRepository;
import com.economydict.repository.UserLogRepository;
import com.economydict.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {
    private final UserRepository userRepository;
    private final UserLogRepository userLogRepository;
    private final DailyUserStatRepository dailyUserStatRepository;
    private final QuizHistoryRepository quizHistoryRepository;
    private final TopIncorrectWordRepository topIncorrectWordRepository;

    public AnalyticsService(UserRepository userRepository,
                            UserLogRepository userLogRepository,
                            DailyUserStatRepository dailyUserStatRepository,
                            QuizHistoryRepository quizHistoryRepository,
                            TopIncorrectWordRepository topIncorrectWordRepository) {
        this.userRepository = userRepository;
        this.userLogRepository = userLogRepository;
        this.dailyUserStatRepository = dailyUserStatRepository;
        this.quizHistoryRepository = quizHistoryRepository;
        this.topIncorrectWordRepository = topIncorrectWordRepository;
    }

    public List<DailyUserStatResponse> getDailyStats() {
        return dailyUserStatRepository.findTop14ByOrderByTargetDateDesc().stream()
                .map(this::toDailyResponse)
                .collect(Collectors.toList());
    }

    public List<TopIncorrectWordResponse> getTopIncorrectWords() {
        LocalDate latest = topIncorrectWordRepository.findTop100ByOrderByTargetDateDescRankAsc().stream()
                .findFirst()
                .map(TopIncorrectWord::getTargetDate)
                .orElse(null);
        if (latest == null) {
            refreshTopIncorrectWords();
            latest = LocalDate.now(ZoneId.of("Asia/Seoul"));
        }
        return topIncorrectWordRepository.findByTargetDateOrderByRankAsc(latest).stream()
                .map(this::toTopWordResponse)
                .collect(Collectors.toList());
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    @Transactional
    public void refreshDailyStats() {
        LocalDate targetDate = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
        Instant start = targetDate.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
        Instant end = targetDate.plusDays(1).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();

        DailyUserStat stat = dailyUserStatRepository.findByTargetDate(targetDate).orElseGet(DailyUserStat::new);
        stat.setTargetDate(targetDate);
        stat.setNewUsersCount((int) userRepository.findAll().stream()
                .map(User::getCreatedAt)
                .filter(createdAt -> createdAt != null && !createdAt.isBefore(start) && createdAt.isBefore(end))
                .count());
        stat.setLoginCount((int) userLogRepository.countByActionAndCreatedAtBetween(UserLogAction.LOGIN, start, end));
        stat.setActiveUsersCount((int) userLogRepository.countDistinctUsersBetween(start, end));
        stat.setCreatedAt(Instant.now());
        dailyUserStatRepository.save(stat);
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    @Transactional
    public void refreshTopIncorrectWords() {
        LocalDate targetDate = LocalDate.now(ZoneId.of("Asia/Seoul"));
        topIncorrectWordRepository.deleteByTargetDate(targetDate);
        List<Object[]> rows = quizHistoryRepository.aggregateIncorrectWords();
        List<TopIncorrectWord> entities = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            if (rank > 100) {
                break;
            }
            DictionaryEntry word = (DictionaryEntry) row[0];
            Long incorrectCount = (Long) row[1];
            TopIncorrectWord entity = new TopIncorrectWord();
            entity.setWord(word);
            entity.setIncorrectCount(incorrectCount.intValue());
            entity.setRank(rank++);
            entity.setTargetDate(targetDate);
            entities.add(entity);
        }
        topIncorrectWordRepository.saveAll(entities);
    }

    private DailyUserStatResponse toDailyResponse(DailyUserStat stat) {
        DailyUserStatResponse response = new DailyUserStatResponse();
        response.setTargetDate(stat.getTargetDate());
        response.setNewUsersCount(stat.getNewUsersCount());
        response.setLoginCount(stat.getLoginCount());
        response.setActiveUsersCount(stat.getActiveUsersCount());
        response.setCreatedAt(stat.getCreatedAt());
        return response;
    }

    private TopIncorrectWordResponse toTopWordResponse(TopIncorrectWord word) {
        TopIncorrectWordResponse response = new TopIncorrectWordResponse();
        response.setRank(word.getRank());
        response.setWordId(word.getWord().getId());
        response.setTerm(word.getWord().getWord());
        response.setIncorrectCount(word.getIncorrectCount());
        response.setDefinition(word.getWord().getMeaning());
        return response;
    }
}
