package org.petclinic.setup;

import io.restassured.http.ContentType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.petclinic.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.restassured.RestAssured.given;

@Slf4j
@Getter
public class DataInitializer {
    private static volatile boolean initialized = false;

    private static final Map<Long, Owner> owners = new ConcurrentHashMap<>();
    private static final Map<Long, PetType> petTypes = new ConcurrentHashMap<>();
    private static final Map<Long, Pet> pets = new ConcurrentHashMap<>();
    private static final Map<Long, Specialty> specialties = new ConcurrentHashMap<>();
    private static final Map<Long, Vet> vets = new ConcurrentHashMap<>();

    public static synchronized void initialize() {
        if (initialized) {
            log.info("[DataInitializer] Data already initialized, skipping");
            return;
        }
        log.info("[DataInitializer] Starting data initialization...");

        // 1. Admin user (1)
        createAdmin("admin", "admin123", new String[]{"ROLE_ADMIN", "ROLE_USER"});

        // 2. Owners (2)
        var owner1 = createOwner("Василия", "Самойлов","ул. Победы, 45","Москва", "89161234567");
        var owner2 = createOwner("Светлана", "Некрасова","ул. Гагарина, 25","Казань", "89223334455");

        // 3. Pet types (3)
        var dogType = createPetType("Собака");
        var catType = createPetType("Кошка");
        var birdType = createPetType("Попугай");

        // 4. Pets (5)
        createPet(owner1, "Шарик", dogType, "2020-01-15");
        createPet(owner1, "Мурка", catType, "2019-05-20");
        createPet(owner1, "Рыжик", catType, "2022-03-25");
        createPet(owner2, "Кеша", birdType, "2021-08-10");
        createPet(owner2, "Бобик", dogType, "2018-11-02");

        // 5. Specialties (2)
        var surgery = createSpecialty("Хирургия");
        var therapy = createSpecialty("Терапия");

        // 6. Vets (3)
        createVet("Алексей", "Смирнов", surgery);
        createVet("Елена", "Кузнецова", therapy);
        createVet("Дмитрий", "Иванов", surgery);

        initialized = true;
        log.info("[DataInitializer] Data initialization completed");
    }

    public static Optional<Owner> getFirstOwner() {
        return owners.values().stream().findFirst();
    }

    public static Optional<Pet> getFirstPet() {
        return pets.values().stream().findFirst();
    }

    public static Optional<Vet> getFirstVet() {
        return vets.values().stream().findFirst();
    }

    public static List<Vet> getAllVets() {
        return new ArrayList<>(vets.values());
    }

    public static List<Pet> getPetsByOwnerId(Long ownerId) {
        return pets.values().stream()
                .filter(pet -> pet.owner().id().equals(ownerId))
                .toList();
    }

    private static void createAdmin(String username, String password, String[] roles) {
        var admin = new Admin(username, password, roles);
        given()
                .contentType(ContentType.JSON)
                .body(admin)
                .post("/users")
                .then().statusCode(201);
        log.info("[DataInitializer] Admin user created: {}", admin);
    }

    private static Owner createOwner(String firstName, String lastName, String address, String city, String telephone) {
        var body = Map.of(
                "firstName", firstName,
                "lastName", lastName,
                "address", address,
                "city", city,
                "telephone", telephone
        );
        var id = given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/owners")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");
        var owner = new Owner(id, firstName, lastName, address, city, telephone, null);
        owners.put(id, owner);

        log.info("[DataInitializer] Owner created: {}", owner);
        return owner;
    }

    private static PetType createPetType(String name) {
        var petTypeId = given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
                .post("/pettypes")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");
        var petType = new PetType(petTypeId, name);
        petTypes.put(petTypeId, petType);

        log.info("[DataInitializer] Pet type created: {}", petType);
        return petType;
    }

    private static Pet createPet(Owner owner, String name, PetType type, String birthDate) {
        var body = Map.of(
                "name", name,
                "type", Map.of("id", type.id()),
                "birthDate", birthDate
        );
        var id = given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/owners/{ownerId}/pets", owner.id())
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");
        var pet = new Pet(id, name, birthDate, type, owner);
        pets.put(id, pet);

        log.info("[DataInitializer] Pet created: {}", pet);
        return pet;
    }

    private static Specialty createSpecialty(String name) {
        var id = given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
                .post("/specialties")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");
        var specialty = new Specialty(id, name);
        specialties.put(id, specialty);

        log.info("[DataInitializer] Specialty created: {}", specialty);
        return specialty;
    }

    private static Vet createVet(String firstName, String lastName, Specialty specialty) {
        var body = Map.of(
                "firstName", firstName,
                "lastName", lastName,
                "specialties", new Object[]{Map.of("id", specialty.id())}
        );
        var id = given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/vets")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        var vet = new Vet(id, firstName, lastName, List.of(specialty));
        vets.put(id, vet);

        log.info("[DataInitializer] Vet created: {}", vet);
        return vet;
    }
}
