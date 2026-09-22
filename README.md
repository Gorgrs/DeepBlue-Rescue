# DeepBlue Rescue

## 1. Nombre del proyecto

**deepblue-rescue**

## 2. Descripción breve

API backend construida con Spring Boot para gestionar el proceso de rescate y
rehabilitación de fauna marina en centros de rescate: casos de rescate, animales
atendidos, sus historias clínicas, especialistas veterinarios, sus áreas de
expertise y los tratamientos que aplican.

## 3. Modelo de datos

| Entidad | Descripción | Campos principales |
|---|---|---|
| `RescueCenter` | Centro físico de rescate | `code`, `name`, `city` |
| `RescueCase` | Caso de rescate abierto para un animal | `caseCode`, `rescueDate`, `rescueLocation`, `status` |
| `Animal` | Animal atendido dentro de un caso | `animalCode`, `commonName`, `scientificName`, `sex` |
| `MedicalRecord` | Historia clínica inicial de un animal | `initialWeight`, `initialCondition`, `injuries`, `observations` |
| `Specialist` | Veterinario/especialista | `professionalCode`, `firstName`, `lastName`, `email`, `active` |
| `Expertise` | Área de especialidad (catálogo) | `name` |
| `Treatment` | Tratamiento aplicado a un animal | `performedAt`, `type`, `description` |

Enums: `RescueStatus` (ADMITTED, UNDER_EVALUATION, IN_REHABILITATION,
READY_FOR_RELEASE, RELEASED, CLOSED), `AnimalSex` (MALE, FEMALE, UNKNOWN),
`TreatmentType` (WOUND_CARE, HYDRATION, MEDICATION, SURGERY, NUTRITION,
PHYSIOTHERAPY, OBSERVATION).

## 4. Relaciones

- `RescueCenter` **1 — N** `RescueCase` (un centro tiene muchos casos).
- `RescueCase` **1 — 1** `Animal` (un caso corresponde a un único animal).
- `Animal` **1 — 1** `MedicalRecord` (historia clínica única por animal).
- `Animal` **1 — N** `Treatment` (un animal recibe muchos tratamientos).
- `Specialist` **1 — N** `Treatment` (un especialista aplica muchos tratamientos).
- `Specialist` **N — M** `Expertise` (tabla asociativa `specialist_expertise`).

## 5. Instrucciones para ejecutar

Requisitos: JDK 21+, Docker (para el contenedor de PostgreSQL en desarrollo).

```bash
./mvnw spring-boot:run
```

Gracias al soporte de **Testcontainers at development time**, no necesitas
levantar PostgreSQL manualmente: Spring Boot lo arranca en un contenedor
Docker automáticamente al ejecutar en modo desarrollo.

Si prefieres usar tu propio PostgreSQL, define las variables de entorno antes
de ejecutar:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/deepblue
export DB_USER=postgres
export DB_PASSWORD=postgres
```

## 6. Instrucciones para ejecutar tests

```bash
./mvnw test
```

Los tests de persistencia (`PersistenceIntegrationTest`) usan `@DataJpaTest`
junto con `TestcontainersConfiguration`, por lo que Docker debe estar
corriendo para que puedan levantar el contenedor de PostgreSQL de prueba.

## 7. Explicación de Flyway

Flyway gestiona el versionado del esquema de base de datos mediante scripts
SQL ubicados en `src/main/resources/db/migration`, nombrados con el patrón
`V<version>__<descripcion>.sql`:

- `V1__create_schema.sql`: crea todas las tablas, llaves foráneas, checks e
  índices.
- `V2__insert_expertise_catalog.sql`: carga el catálogo inicial de áreas de
  expertise.

Al arrancar la aplicación, Flyway compara la versión aplicada contra los
scripts disponibles y ejecuta únicamente los pendientes, en orden. Por eso en
`application.yaml` se usa `hibernate.ddl-auto: validate`: Hibernate **no**
crea ni modifica el esquema, solo valida que las entidades coincidan con lo
que Flyway ya creó. Esto evita que dos herramientas distintas compitan por
gestionar la misma base de datos.

## 8. Explicación de Testcontainers

Testcontainers permite levantar un contenedor Docker real de PostgreSQL de
forma automática y desechable, tanto en desarrollo como en pruebas, sin
depender de una instancia instalada manualmente:

- `TestcontainersConfiguration` (en `src/test/java`) define un bean
  `PostgreSQLContainer` anotado con `@ServiceConnection`, que le indica a
  Spring Boot que configure automáticamente el `DataSource` apuntando a ese
  contenedor.
- `TestDeepblueRescueApplication` reutiliza esa configuración para poder
  arrancar la app completa en modo desarrollo con la base de datos
  "de mentira" ya lista (feature de *Testcontainers at development time*).
- En los tests (`PersistenceIntegrationTest`), el mismo contenedor se importa
  vía `@Import(TestcontainersConfiguration.class)`, garantizando que las
  pruebas corran contra un PostgreSQL real y no contra una base en memoria,
  evitando incompatibilidades de tipos/SQL entre entornos.

## 9. Listado de Query Methods implementados

| Repositorio | Query Method |
|---|---|
| `RescueCenterRepository` | `findByCode(String code)` |
| `RescueCaseRepository` | `findByCaseCode(String caseCode)` |
| `RescueCaseRepository` | `findByStatus(RescueStatus status)` |
| `RescueCaseRepository` | `findByRescueCenterId(Long rescueCenterId)` |
| `RescueCaseRepository` | `findByRescueDateBetween(LocalDate startDate, LocalDate endDate)` |
| `AnimalRepository` | `findByAnimalCode(String animalCode)` |
| `AnimalRepository` | `findByRescueCaseId(Long rescueCaseId)` |
| `MedicalRecordRepository` | `findByAnimalId(Long animalId)` |
| `SpecialistRepository` | `findByProfessionalCode(String professionalCode)` |
| `SpecialistRepository` | `findByEmail(String email)` |
| `SpecialistRepository` | `findByActiveTrue()` |
| `ExpertiseRepository` | `findByName(String name)` |
| `TreatmentRepository` | `findByAnimalIdOrderByPerformedAtDesc(Long animalId)` |
| `TreatmentRepository` | `findBySpecialistIdOrderByPerformedAtDesc(Long specialistId)` |

## 10. Listado de consultas JPQL implementadas

| Repositorio | Método | JPQL |
|---|---|---|
| `RescueCaseRepository` | `findByStatusFetchingCenter` | `SELECT rc FROM RescueCase rc JOIN FETCH rc.rescueCenter WHERE rc.status = :status` |
| `AnimalRepository` | `findAnimalsWithoutMedicalRecord` | `SELECT a FROM Animal a WHERE a.medicalRecord IS NULL` |
| `SpecialistRepository` | `findByExpertiseName` | `SELECT DISTINCT s FROM Specialist s JOIN s.expertiseAreas e WHERE e.name = :expertiseName` |
| `TreatmentRepository` | `countTreatmentsByAnimalId` | `SELECT COUNT(t) FROM Treatment t WHERE t.animal.id = :animalId` |
| `RescueCenterRepository` | `findCentersWithMoreCasesThan` | `SELECT rc FROM RescueCenter rc WHERE SIZE(rc.rescueCases) > :minCases` |
