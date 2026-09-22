package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.RescueCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RescueCenterRepository extends JpaRepository<RescueCenter, Long> {

    Optional<RescueCenter> findByCode(String code);

    @Query("SELECT rc FROM RescueCenter rc WHERE SIZE(rc.rescueCases) > :minCases")
    List<RescueCenter> findCentersWithMoreCasesThan(@Param("minCases") int minCases);
}
