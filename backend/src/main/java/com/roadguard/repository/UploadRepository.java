package com.roadguard.repository;

import com.roadguard.entity.Upload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UploadRepository extends JpaRepository<Upload, Long> {
    List<Upload> findByUserIdOrderByIdDesc(Long userId);
    List<Upload> findAllByOrderByIdDesc();
}
