package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Animal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

    Optional<Animal> findByAnimalCode(String animalCode);

    Optional<Animal> findByRescueCaseId(Long rescueCaseId);

    @Query("SELECT a FROM Animal a WHERE a.medicalRecord IS NULL")
    List<Animal> findAnimalsWithoutMedicalRecord();
}
