package com.economydict.repository;

import com.economydict.entity.TopIncorrectWord;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopIncorrectWordRepository extends JpaRepository<TopIncorrectWord, Long> {
    void deleteByTargetDate(LocalDate targetDate);
    List<TopIncorrectWord> findTop100ByOrderByTargetDateDescRankAsc();
    List<TopIncorrectWord> findByTargetDateOrderByRankAsc(LocalDate targetDate);
}
