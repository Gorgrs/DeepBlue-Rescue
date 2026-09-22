package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Specialist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpecialistRepository extends JpaRepository<Specialist, Long> {

    Optional<Specialist> findByProfessionalCode(String professionalCode);

    Optional<Specialist> findByEmail(String email);

    List<Specialist> findByActiveTrue();

    @Query("SELECT DISTINCT s FROM Specialist s JOIN s.expertiseAreas e WHERE e.name = :expertiseName")
    List<Specialist> findByExpertiseName(@Param("expertiseName") String expertiseName);
}
