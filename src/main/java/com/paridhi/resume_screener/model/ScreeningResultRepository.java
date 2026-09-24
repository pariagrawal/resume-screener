package com.paridhi.resume_screener.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScreeningResultRepository extends JpaRepository<ScreeningResult, Long> {
    List<ScreeningResult> findAllByOrderByCreatedAtDesc();
}