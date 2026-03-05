package com.economydict.repository;

import com.economydict.entity.UserQuestionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserQuestionAttemptRepository extends JpaRepository<UserQuestionAttempt, Long> {
}
