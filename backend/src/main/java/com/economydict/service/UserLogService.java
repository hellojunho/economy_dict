package com.economydict.service;

import com.economydict.entity.User;
import com.economydict.entity.UserLog;
import com.economydict.entity.UserLogAction;
import com.economydict.repository.UserLogRepository;
import org.springframework.stereotype.Service;

@Service
public class UserLogService {
    private final UserLogRepository userLogRepository;

    public UserLogService(UserLogRepository userLogRepository) {
        this.userLogRepository = userLogRepository;
    }

    public void log(User user, UserLogAction action, String description) {
        if (user == null) {
            return;
        }
        UserLog log = new UserLog();
        log.setUser(user);
        log.setAction(action);
        log.setDescription(description);
        userLogRepository.save(log);
    }
}
