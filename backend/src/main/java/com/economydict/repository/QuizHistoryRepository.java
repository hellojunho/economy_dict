package com.economydict.repository;

import com.economydict.entity.DictionaryEntry;
import com.economydict.entity.QuizHistory;
import com.economydict.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuizHistoryRepository extends JpaRepository<QuizHistory, Long> {
    List<QuizHistory> findByUserAndCorrectFalseOrderByCreatedAtDesc(User user);

    @Query("select q.word as word, count(q.id) as incorrectCount from QuizHistory q where q.correct = false group by q.word order by count(q.id) desc")
    List<Object[]> aggregateIncorrectWords();

    long countByUser(User user);

    long countByUserAndCorrectTrue(User user);

    long countByWord(DictionaryEntry word);
}
