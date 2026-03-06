package com.economydict.repository;

import com.economydict.entity.ImportTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportTaskRepository extends JpaRepository<ImportTask, String> {
}
