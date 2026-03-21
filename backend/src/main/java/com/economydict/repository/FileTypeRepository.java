package com.economydict.repository;

import com.economydict.entity.FileType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileTypeRepository extends JpaRepository<FileType, String> {
}
