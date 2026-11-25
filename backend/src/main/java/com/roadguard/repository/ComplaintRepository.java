package com.roadguard.repository;

import com.roadguard.entity.Complaint;
import com.roadguard.entity.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByUserIdOrderByIdDesc(Long userId);
    List<Complaint> findAllByOrderByIdDesc();
    List<Complaint> findByStatusOrderByIdDesc(ComplaintStatus status);
    boolean existsByDetections_Id(Long detectionId);
}
