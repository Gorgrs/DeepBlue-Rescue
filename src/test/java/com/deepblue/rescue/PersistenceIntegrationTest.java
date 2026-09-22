package com.deepblue.rescue;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class PersistenceIntegrationTest {

    @Autowired
    private RescueCenterRepository rescueCenterRepository;

    @Autowired
    private RescueCaseRepository rescueCaseRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private ExpertiseRepository expertiseRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Test
    void shouldPersistAndFindRescueCenterByCode() {

        RescueCenter center = new RescueCenter("SM-01", "Centro de Rescate Santa Marta", "Santa Marta");
        rescueCenterRepository.save(center);

        Optional<RescueCenter> found = rescueCenterRepository.findByCode("SM-01");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Centro de Rescate Santa Marta");
    }

    @Test
    void shouldPersistRescueCaseWithAnimalAndMedicalRecord() {

        RescueCenter center = new RescueCenter("SM-02", "Centro Tayrona", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase(
                "CASE-001", LocalDate.now(), "Playa Rodadero", RescueStatus.ADMITTED
        );
        center.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal("AN-001", "Tortuga Carey", "Eretmochelys imbricata", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);

        MedicalRecord medicalRecord = new MedicalRecord(
                new BigDecimal("12.50"), "Deshidratada", "Fractura en aleta", "Encontrada varada"
        );
        animal.assignMedicalRecord(medicalRecord);

        animalRepository.save(animal);

        Optional<RescueCase> foundCase = rescueCaseRepository.findByCaseCode("CASE-001");
        assertThat(foundCase).isPresent();
        assertThat(foundCase.get().getAnimal()).isNotNull();
        assertThat(foundCase.get().getAnimal().getMedicalRecord()).isNotNull();
        assertThat(foundCase.get().getAnimal().getMedicalRecord().getInjuries())
                .isEqualTo("Fractura en aleta");

        Optional<MedicalRecord> foundRecord = medicalRecordRepository.findByAnimalId(animal.getId());
        assertThat(foundRecord).isPresent();
    }

    @Test
    void shouldFilterRescueCasesByStatus() {

        RescueCenter center = new RescueCenter("SM-03", "Centro Bahia", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase admitted = new RescueCase("CASE-100", LocalDate.now(), "Bahia Concha", RescueStatus.ADMITTED);
        RescueCase released = new RescueCase("CASE-101", LocalDate.now(), "Bahia Concha", RescueStatus.RELEASED);

        center.addCase(admitted);
        center.addCase(released);

        rescueCaseRepository.save(admitted);
        rescueCaseRepository.save(released);

        List<RescueCase> admittedCases = rescueCaseRepository.findByStatus(RescueStatus.ADMITTED);

        assertThat(admittedCases).hasSize(1);
        assertThat(admittedCases.get(0).getCaseCode()).isEqualTo("CASE-100");

        List<RescueCase> admittedWithCenter = rescueCaseRepository.findByStatusFetchingCenter(RescueStatus.ADMITTED);
        assertThat(admittedWithCenter).hasSize(1);
        assertThat(admittedWithCenter.get(0).getRescueCenter().getCode()).isEqualTo("SM-03");
    }

    @Test
    void shouldPersistSpecialistWithExpertiseAreas() {

        Expertise trauma = expertiseRepository.findByName("Trauma")
                .orElseGet(() -> expertiseRepository.save(new Expertise("Trauma")));

        Specialist specialist = new Specialist(
                "VET-001", "Laura", "Gomez", "laura.gomez@deepblue.org", true
        );
        specialist.addExpertise(trauma);

        specialistRepository.save(specialist);

        Optional<Specialist> found = specialistRepository.findByEmail("laura.gomez@deepblue.org");

        assertThat(found).isPresent();
        assertThat(found.get().getExpertiseAreas()).extracting(Expertise::getName)
                .contains("Trauma");
        assertThat(specialistRepository.findByActiveTrue()).contains(found.get());
        assertThat(specialistRepository.findByExpertiseName("Trauma")).contains(found.get());
    }

    @Test
    void shouldPersistTreatmentsForAnimalOrderedByMostRecent() {

        RescueCenter center = new RescueCenter("SM-04", "Centro Rodadero", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase(
                "CASE-200", LocalDate.now(), "Rodadero", RescueStatus.IN_REHABILITATION
        );
        center.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal("AN-200", "Delfin Nariz de Botella", "Tursiops truncatus", AnimalSex.MALE);
        rescueCase.assignAnimal(animal);
        animalRepository.save(animal);

        Specialist specialist = new Specialist(
                "VET-002", "Carlos", "Perez", "carlos.perez@deepblue.org", true
        );
        specialistRepository.save(specialist);

        Treatment first = new Treatment(
                animal, specialist, LocalDateTime.now().minusDays(2), TreatmentType.HYDRATION, "Suero inicial"
        );
        Treatment second = new Treatment(
                animal, specialist, LocalDateTime.now().minusDays(1), TreatmentType.MEDICATION, "Antibiotico"
        );

        treatmentRepository.save(first);
        treatmentRepository.save(second);

        List<Treatment> treatments = treatmentRepository.findByAnimalIdOrderByPerformedAtDesc(animal.getId());

        assertThat(treatments).hasSize(2);
        assertThat(treatments.get(0).getType()).isEqualTo(TreatmentType.MEDICATION);
        assertThat(treatments.get(1).getType()).isEqualTo(TreatmentType.HYDRATION);
        assertThat(treatmentRepository.countTreatmentsByAnimalId(animal.getId())).isEqualTo(2L);
    }

    @Test
    void shouldFindAnimalsWithoutMedicalRecordAndCentersWithManyCases() {

        RescueCenter center = new RescueCenter("SM-05", "Centro Taganga", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase caseA = new RescueCase("CASE-300", LocalDate.now(), "Taganga", RescueStatus.ADMITTED);
        RescueCase caseB = new RescueCase("CASE-301", LocalDate.now(), "Taganga", RescueStatus.ADMITTED);
        center.addCase(caseA);
        center.addCase(caseB);
        rescueCaseRepository.save(caseA);
        rescueCaseRepository.save(caseB);

        Animal animalWithoutRecord = new Animal("AN-300", "Iguana Marina", "Amblyrhynchus cristatus", AnimalSex.UNKNOWN);
        caseA.assignAnimal(animalWithoutRecord);
        animalRepository.save(animalWithoutRecord);

        assertThat(animalRepository.findAnimalsWithoutMedicalRecord())
                .extracting(Animal::getAnimalCode)
                .contains("AN-300");

        assertThat(rescueCenterRepository.findCentersWithMoreCasesThan(1))
                .extracting(RescueCenter::getCode)
                .contains("SM-05");
    }
}
