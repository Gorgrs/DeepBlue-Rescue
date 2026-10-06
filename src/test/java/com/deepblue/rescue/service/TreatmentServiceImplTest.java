package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private TreatmentMapper mapper;

    @InjectMocks
    private TreatmentServiceImpl service;

    // TEST 5 — Tratamiento valido -> save()
    @Test
    void shouldRegisterTreatmentSuccessfully() {

        RescueCase rescueCase = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 20),
                "Bahia Solano",
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = new Animal(
                "AN-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        rescueCase.assignAnimal(animal);

        Specialist specialist = new Specialist(
                "SPEC-001",
                "Ana",
                "Gomez",
                "ana.gomez@deepblue.org",
                true
        );

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001",
                "SPEC-001",
                LocalDateTime.of(2026, 8, 22, 10, 0),
                TreatmentType.WOUND_CARE,
                "Cleaned flipper wound"
        );

        Treatment savedTreatment = new Treatment(
                animal,
                specialist,
                request.performedAt(),
                request.type(),
                request.description()
        );

        TreatmentResponse response = new TreatmentResponse(
                1L,
                "AN-001",
                "SPEC-001",
                request.performedAt(),
                TreatmentType.WOUND_CARE,
                "Cleaned flipper wound"
        );

        when(
            animalRepository.findByAnimalCode("AN-001")
        ).thenReturn(Optional.of(animal));

        when(
            specialistRepository.findByProfessionalCode("SPEC-001")
        ).thenReturn(Optional.of(specialist));

        when(
            treatmentRepository.save(any(Treatment.class))
        ).thenReturn(savedTreatment);

        when(
            mapper.toResponse(savedTreatment)
        ).thenReturn(response);

        TreatmentResponse result = service.register(request);

        assertThat(result).isEqualTo(response);

        verify(treatmentRepository).save(any(Treatment.class));
    }

    // TEST 6 — Especialista inactivo -> BusinessRuleException, nunca save()
    @Test
    void shouldThrowWhenSpecialistIsInactive() {

        RescueCase rescueCase = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 20),
                "Bahia Solano",
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = new Animal(
                "AN-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        rescueCase.assignAnimal(animal);

        Specialist specialist = new Specialist(
                "SPEC-001",
                "Ana",
                "Gomez",
                "ana.gomez@deepblue.org",
                false
        );

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001",
                "SPEC-001",
                LocalDateTime.of(2026, 8, 22, 10, 0),
                TreatmentType.WOUND_CARE,
                "Cleaned flipper wound"
        );

        when(
            animalRepository.findByAnimalCode("AN-001")
        ).thenReturn(Optional.of(animal));

        when(
            specialistRepository.findByProfessionalCode("SPEC-001")
        ).thenReturn(Optional.of(specialist));

        assertThatThrownBy(
            () -> service.register(request)
        ).isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }

    // TEST 7 — Caso RELEASED -> BusinessRuleException, nunca save()
    @Test
    void shouldThrowWhenRescueCaseIsReleased() {

        RescueCase rescueCase = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 20),
                "Bahia Solano",
                RescueStatus.RELEASED
        );

        Animal animal = new Animal(
                "AN-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        rescueCase.assignAnimal(animal);

        Specialist specialist = new Specialist(
                "SPEC-001",
                "Ana",
                "Gomez",
                "ana.gomez@deepblue.org",
                true
        );

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001",
                "SPEC-001",
                LocalDateTime.of(2026, 8, 22, 10, 0),
                TreatmentType.OBSERVATION,
                "Routine check"
        );

        when(
            animalRepository.findByAnimalCode("AN-001")
        ).thenReturn(Optional.of(animal));

        when(
            specialistRepository.findByProfessionalCode("SPEC-001")
        ).thenReturn(Optional.of(specialist));

        assertThatThrownBy(
            () -> service.register(request)
        ).isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }

    // TEST 8 — Regla 5: fecha del tratamiento anterior a la fecha de rescate
    //          -> BusinessRuleException, nunca save()
    @Test
    void shouldThrowWhenTreatmentDateIsBeforeRescueDate() {

        RescueCase rescueCase = new RescueCase(
                "RES-2026-100",
                LocalDate.of(2026, 8, 20),
                "Bahia Solano",
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = new Animal(
                "AN-2026-100",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        rescueCase.assignAnimal(animal);

        Specialist specialist = new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena.vargas@deepblue.org",
                true
        );

        // Rescate: 2026-08-20  |  Tratamiento: 2026-08-15  -> invalido
        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-2026-100",
                "SPEC-001",
                LocalDateTime.of(2026, 8, 15, 9, 0),
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper injury."
        );

        when(
            animalRepository.findByAnimalCode("AN-2026-100")
        ).thenReturn(Optional.of(animal));

        when(
            specialistRepository.findByProfessionalCode("SPEC-001")
        ).thenReturn(Optional.of(specialist));

        assertThatThrownBy(
            () -> service.register(request)
        ).isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }
}
