package com.economydict.repository;

import com.economydict.entity.User;
import com.economydict.entity.UserQuiz;
import com.economydict.entity.Quiz;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserQuizRepository extends JpaRepository<UserQuiz, Long> {
    Optional<UserQuiz> findByUserAndQuiz(User user, Quiz quiz);
    List<UserQuiz> findByQuiz(Quiz quiz);
    List<UserQuiz> findByUser(User user);
}
