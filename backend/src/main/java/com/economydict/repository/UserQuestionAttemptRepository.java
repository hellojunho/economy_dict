package com.economydict.repository;

import com.economydict.entity.UserQuestionAttempt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserQuestionAttemptRepository extends JpaRepository<UserQuestionAttempt, Long> {
    List<UserQuestionAttempt> findByStatus_IdInOrderByAttemptedAtAsc(List<Long> statusIds);
}
