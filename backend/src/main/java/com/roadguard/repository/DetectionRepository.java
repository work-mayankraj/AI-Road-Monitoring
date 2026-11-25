package com.roadguard.repository;

import com.roadguard.entity.Detection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DetectionRepository extends JpaRepository<Detection, Long> {

    @Query("select d from Detection d where d.upload.user.id = :userId order by d.id")
    List<Detection> findByOwner(@Param("userId") Long userId);

    List<Detection> findAllByOrderByIdAsc();
}
