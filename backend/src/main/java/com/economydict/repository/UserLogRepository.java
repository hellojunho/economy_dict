package com.economydict.repository;

import com.economydict.entity.User;
import com.economydict.entity.UserLog;
import com.economydict.entity.UserLogAction;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserLogRepository extends JpaRepository<UserLog, Long> {
    long countByActionAndCreatedAtBetween(UserLogAction action, Instant start, Instant end);

    @Query("select count(distinct l.user.id) from UserLog l where l.createdAt >= :start and l.createdAt < :end")
    long countDistinctUsersBetween(Instant start, Instant end);

    List<UserLog> findByUserOrderByCreatedAtDesc(User user);
}
