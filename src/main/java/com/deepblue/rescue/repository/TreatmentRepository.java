package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TreatmentRepository extends JpaRepository<Treatment, Long> {

    List<Treatment> findByAnimalIdOrderByPerformedAtDesc(Long animalId);

    List<Treatment> findBySpecialistIdOrderByPerformedAtDesc(Long specialistId);

    @Query("SELECT COUNT(t) FROM Treatment t WHERE t.animal.id = :animalId")
    long countTreatmentsByAnimalId(@Param("animalId") Long animalId);
}
