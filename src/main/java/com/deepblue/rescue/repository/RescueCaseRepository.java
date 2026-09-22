package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RescueCaseRepository extends JpaRepository<RescueCase, Long> {

    Optional<RescueCase> findByCaseCode(String caseCode);

    List<RescueCase> findByStatus(RescueStatus status);

    List<RescueCase> findByRescueCenterId(Long rescueCenterId);

    List<RescueCase> findByRescueDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT rc FROM RescueCase rc JOIN FETCH rc.rescueCenter WHERE rc.status = :status")
    List<RescueCase> findByStatusFetchingCenter(@Param("status") RescueStatus status);
}
