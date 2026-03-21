package com.economydict.repository;

import com.economydict.entity.DailyUserStat;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyUserStatRepository extends JpaRepository<DailyUserStat, Long> {
    Optional<DailyUserStat> findByTargetDate(LocalDate targetDate);
    List<DailyUserStat> findTop14ByOrderByTargetDateDesc();
}
