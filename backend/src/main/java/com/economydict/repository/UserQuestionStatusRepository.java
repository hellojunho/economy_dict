package com.economydict.repository;

import com.economydict.entity.QuizQuestion;
import com.economydict.entity.User;
import com.economydict.entity.UserQuestionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserQuestionStatusRepository extends JpaRepository<UserQuestionStatus, Long> {
    Optional<UserQuestionStatus> findByUserAndQuestion(User user, QuizQuestion question);
    List<UserQuestionStatus> findByUser(User user);
    long countByUserAndQuestion_Quiz_IdAndCorrectTrue(User user, Long quizId);
}
