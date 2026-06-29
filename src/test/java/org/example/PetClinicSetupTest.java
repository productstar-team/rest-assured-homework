package org.example;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PetClinicSetupTest {

    private static final String BASE_URL = "http://localhost:9966/petclinic";
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin";

    @BeforeAll
    static void setupAll() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        addUser();
        int[] ownerIds = addOwners();
        int[] petTypeIds = addPetTypes();
        addPets(ownerIds, petTypeIds);
        Map<Integer, String> specialties = addSpecialties();
        addVets(specialties);
    }

    // --- 1. Один пользователь с ролью admin (идемпотентно: пропускаем если уже существует) ---
    private static void addUser() {
        Map<String, Object> user = Map.of(
                "username", "testadmin",
                "password", "testadmin123",
                "enabled", true,
                "roles", List.of(Map.of("name", "ROLE_ADMIN"))
        );

        int status = given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .contentType(ContentType.JSON)
                .body(user)
                .when()
                .post("/api/users")
                .then()
                .extract().statusCode();

        if (status == 201) {
            System.out.println("User 'testadmin' with role ROLE_ADMIN created.");
        } else if (status == 404) {
            // Server maps DataIntegrityViolationException to 404; user already exists — skip
            System.out.println("User 'testadmin' already exists, skipping creation.");
        } else {
            throw new AssertionError("Unexpected status " + status + " when creating user");
        }
    }

    // --- 2. Два владельца питомцев (всегда создаём — нет unique-ограничения) ---
    private static int[] addOwners() {
        int[] ids = new int[2];

        List<Map<String, String>> owners = List.of(
                Map.of("firstName", "Ivan", "lastName", "Petrov",
                        "address", "ул. Ленина, 1", "city", "Москва", "telephone", "1234567890"),
                Map.of("firstName", "Maria", "lastName", "Sidorova",
                        "address", "пр. Мира, 25", "city", "Санкт-Петербург", "telephone", "0987654321")
        );

        for (int i = 0; i < owners.size(); i++) {
            Response response = given()
                    .auth().basic(ADMIN_USER, ADMIN_PASS)
                    .contentType(ContentType.JSON)
                    .body(owners.get(i))
                    .when()
                    .post("/api/owners")
                    .then()
                    .statusCode(201)
                    .extract().response();

            ids[i] = response.jsonPath().getInt("id");
            System.out.println("Owner created: " + owners.get(i).get("firstName")
                    + " " + owners.get(i).get("lastName") + " (id=" + ids[i] + ")");
        }

        return ids;
    }

    // --- 3. Три вида питомцев (идемпотентно: используем существующий ID если тип уже есть) ---
    private static int[] addPetTypes() {
        List<Map<String, Object>> existing = given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get("/api/pettypes")
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("$");

        Map<String, Integer> existingByName = new HashMap<>();
        for (Map<String, Object> type : existing) {
            existingByName.put((String) type.get("name"), ((Number) type.get("id")).intValue());
        }

        String[] types = {"cat", "dog", "bird"};
        int[] ids = new int[types.length];

        for (int i = 0; i < types.length; i++) {
            if (existingByName.containsKey(types[i])) {
                ids[i] = existingByName.get(types[i]);
                System.out.println("Pet type already exists: " + types[i] + " (id=" + ids[i] + ")");
            } else {
                Response response = given()
                        .auth().basic(ADMIN_USER, ADMIN_PASS)
                        .contentType(ContentType.JSON)
                        .body(Map.of("name", types[i]))
                        .when()
                        .post("/api/pettypes")
                        .then()
                        .statusCode(201)
                        .extract().response();

                ids[i] = response.jsonPath().getInt("id");
                System.out.println("Pet type created: " + types[i] + " (id=" + ids[i] + ")");
            }
        }

        return ids;
    }

    // --- 4. Пять питомцев (всегда создаём под владельцами из текущего запуска) ---
    // Endpoint: POST /api/owners/{ownerId}/pets (не /api/pets — такого маршрута нет)
    // type требует и id, и name
    private static void addPets(int[] ownerIds, int[] petTypeIds) {
        // {name, birthDate, ownerIndex, typeIndex, typeName}
        Object[][] petData = {
                {"Барсик", "2021-03-15", 0, 0, "cat"},
                {"Шарик",  "2020-07-10", 0, 1, "dog"},
                {"Мурка",  "2022-01-05", 1, 0, "cat"},
                {"Кеша",   "2019-11-20", 1, 2, "bird"},
                {"Бобик",  "2023-04-01", 0, 1, "dog"},
        };

        for (Object[] petDatum : petData) {
            String petName   = (String) petDatum[0];
            String birthDate = (String) petDatum[1];
            int ownerIdx     = (int) petDatum[2];
            int typeIdx      = (int) petDatum[3];
            String typeName  = (String) petDatum[4];

            int ownerId = ownerIds[ownerIdx];
            int typeId  = petTypeIds[typeIdx];

            Map<String, Object> body = Map.of(
                    "name", petName,
                    "birthDate", birthDate,
                    "type", Map.of("id", typeId, "name", typeName)
            );

            Response response = given()
                    .auth().basic(ADMIN_USER, ADMIN_PASS)
                    .contentType(ContentType.JSON)
                    .body(body)
                    .when()
                    .post("/api/owners/{ownerId}/pets", ownerId)
                    .then()
                    .statusCode(201)
                    .extract().response();

            System.out.println("Pet created: " + petName + " (id=" + response.jsonPath().getInt("id") + ")");
        }
    }

    // --- 5. Две специализации ветеринаров (идемпотентно: используем существующий ID если есть) ---
    // Возвращает Map<id, name> — имена нужны при создании ветеринаров (specialties требует name)
    private static Map<Integer, String> addSpecialties() {
        List<Map<String, Object>> existing = given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get("/api/specialties")
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("$");

        Map<String, Integer> existingByName = new HashMap<>();
        for (Map<String, Object> spec : existing) {
            existingByName.put((String) spec.get("name"), ((Number) spec.get("id")).intValue());
        }

        Map<Integer, String> result = new java.util.LinkedHashMap<>();
        String[] names = {"surgery", "dentistry"};

        for (String name : names) {
            if (existingByName.containsKey(name)) {
                int id = existingByName.get(name);
                result.put(id, name);
                System.out.println("Specialty already exists: " + name + " (id=" + id + ")");
            } else {
                Response response = given()
                        .auth().basic(ADMIN_USER, ADMIN_PASS)
                        .contentType(ContentType.JSON)
                        .body(Map.of("name", name))
                        .when()
                        .post("/api/specialties")
                        .then()
                        .statusCode(201)
                        .extract().response();

                int id = response.jsonPath().getInt("id");
                result.put(id, name);
                System.out.println("Specialty created: " + name + " (id=" + id + ")");
            }
        }

        return result;
    }

    // --- 6. Три ветеринара (всегда создаём — нет unique-ограничения) ---
    // specialties требует {id, name} — только id вызывает 400
    private static void addVets(Map<Integer, String> specialties) {
        List<Integer> specIds = new java.util.ArrayList<>(specialties.keySet());
        int s0 = specIds.get(0);
        int s1 = specIds.get(1);

        List<Map<String, Object>> vets = List.of(
                Map.of("firstName", "Алексей", "lastName", "Иванов",
                        "specialties", List.of(Map.of("id", s0, "name", specialties.get(s0)))),
                Map.of("firstName", "Елена", "lastName", "Козлова",
                        "specialties", List.of(Map.of("id", s1, "name", specialties.get(s1)))),
                Map.of("firstName", "Дмитрий", "lastName", "Смирнов",
                        "specialties", List.of(
                                Map.of("id", s0, "name", specialties.get(s0)),
                                Map.of("id", s1, "name", specialties.get(s1))))
        );

        for (Map<String, Object> vet : vets) {
            Response response = given()
                    .auth().basic(ADMIN_USER, ADMIN_PASS)
                    .contentType(ContentType.JSON)
                    .body(vet)
                    .when()
                    .post("/api/vets")
                    .then()
                    .statusCode(201)
                    .extract().response();

            System.out.println("Vet created: " + vet.get("firstName") + " " + vet.get("lastName")
                    + " (id=" + response.jsonPath().getInt("id") + ")");
        }
    }

    // --- Проверочный тест ---
    @Test
    void verifySetupCompleted() {
        int ownerCount = given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get("/api/owners")
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("$").size();

        int petTypeCount = given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get("/api/pettypes")
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("$").size();

        int petCount = given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get("/api/pets")
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("$").size();

        int specialtyCount = given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get("/api/specialties")
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("$").size();

        int vetCount = given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get("/api/vets")
                .then()
                .statusCode(200)
                .extract().jsonPath().getList("$").size();

        assertTrue(ownerCount >= 2, "Expected at least 2 owners");
        assertTrue(petTypeCount >= 3, "Expected at least 3 pet types");
        assertTrue(petCount >= 5, "Expected at least 5 pets");
        assertTrue(specialtyCount >= 2, "Expected at least 2 specialties");
        assertTrue(vetCount >= 3, "Expected at least 3 vets");

        System.out.println("Setup verification passed:");
        System.out.println("  Owners: " + ownerCount);
        System.out.println("  Pet types: " + petTypeCount);
        System.out.println("  Pets: " + petCount);
        System.out.println("  Specialties: " + specialtyCount);
        System.out.println("  Vets: " + vetCount);
    }
}
