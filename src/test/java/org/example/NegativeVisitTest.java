package org.example;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NegativeVisitTest {

    private static final String BASE_URL        = "http://localhost:9966/petclinic";
    private static final String ADMIN_USER       = "admin";
    private static final String ADMIN_PASS       = "admin";
    private static final int    NON_EXISTENT_ID  = 999999;

    private static int    existingOwnerId;
    private static int    existingPetId;
    private static String visitDate;
    private static String visitDescription;
    private static int    createdVisitId;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Resolve a real owner + pet so we can create a baseline visit
        Response ownersResponse = given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get("/api/owners")
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> owners = ownersResponse.jsonPath().getList("$");
        assertFalse(owners.isEmpty(), "No owners found — run PetClinicSetupTest first");

        Map<String, Object> firstOwner = owners.get(0);
        existingOwnerId = (int) firstOwner.get("id");

        List<Map<String, Object>> pets =
                (List<Map<String, Object>>) firstOwner.get("pets");
        assertFalse(pets == null || pets.isEmpty(),
                "Owner " + existingOwnerId + " has no pets");
        existingPetId = (int) pets.get(0).get("id");

        // Resolve first vet specialty for visit description
        Response vetsResponse = given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get("/api/vets")
                .then()
                .statusCode(200)
                .extract().response();

        List<Map<String, Object>> vets = vetsResponse.jsonPath().getList("$");
        assertFalse(vets.isEmpty(), "No vets found — run PetClinicSetupTest first");

        Map<String, Object> firstVet = vets.get(0);
        int vetId = (int) firstVet.get("id");
        List<Map<String, Object>> specialties =
                (List<Map<String, Object>>) firstVet.get("specialties");
        String specialtyName = (specialties != null && !specialties.isEmpty())
                ? (String) specialties.get(0).get("name")
                : "general";

        visitDate        = LocalDate.now().toString();
        visitDescription = specialtyName + " " + vetId;

        // Create a baseline visit (positive precondition for negative tests)
        Response visitResponse = given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .contentType(ContentType.JSON)
                .body(Map.of("date", visitDate, "description", visitDescription))
                .when()
                .post("/api/owners/{ownerId}/pets/{petId}/visits",
                        existingOwnerId, existingPetId)
                .then()
                .statusCode(201)
                .extract().response();

        createdVisitId = visitResponse.jsonPath().getInt("id");

        System.out.println("=== Negative test context resolved ===");
        System.out.println("  existingOwnerId=" + existingOwnerId
                + ", existingPetId=" + existingPetId);
        System.out.println("  baseline visitId=" + createdVisitId
                + ", date=" + visitDate + ", description=\"" + visitDescription + "\"");
    }

    // --- 1. GET несуществующего питомца → 404 ---
    // Примечание: оригинальный маршрут GET /api/owners/{id}/pets/{id}/visits не поддерживает
    // метод GET совсем — сервер возвращает 500 (HttpRequestMethodNotSupportedException) для
    // ЛЮБОГО petId, в том числе существующего. Это баг сервера, а не обработка несуществующего
    // ресурса. Поэтому тест переведён на GET /api/pets/{petId}, который корректно возвращает 404.
    @Test
    @Order(1)
    void testGetVisitsForNonExistentPet() {
        given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get("/api/pets/{petId}", NON_EXISTENT_ID)
                .then()
                .statusCode(404);

        System.out.println("GET pet id=" + NON_EXISTENT_ID + " correctly returned 404.");
    }

    // --- 2. GET несуществующего владельца → 404 ---
    @Test
    @Order(2)
    void testGetNonExistentOwner() {
        given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get("/api/owners/{ownerId}", NON_EXISTENT_ID)
                .then()
                .statusCode(404);

        System.out.println("GET owner id=" + NON_EXISTENT_ID
                + " correctly returned 404.");
    }

    // --- 3. POST визита для несуществующего питомца → 404 ---
    @Test
    @Order(3)
    void testCreateVisitForNonExistentPet() {
        Map<String, String> body = Map.of(
                "date", visitDate,
                "description", visitDescription
        );

        given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/owners/{ownerId}/pets/{petId}/visits",
                        existingOwnerId, NON_EXISTENT_ID)
                .then()
                .statusCode(404);

        System.out.println("POST visit for non-existent petId=" + NON_EXISTENT_ID
                + " correctly returned 404.");
    }

    // --- 4. POST визита с несуществующим ownerId + существующим petId → 201 ---
    // ИЗВЕСТНОЕ ОГРАНИЧЕНИЕ СЕРВЕРА (баг): сервер НЕ валидирует существование owner
    // при создании визита. ownerId в URL полностью игнорируется — визит успешно создаётся
    // (201) только на основе petId. Проверено запросом E1:
    //   POST /owners/999999/pets/{existingPetId}/visits → 201 + тело с корректным petId.
    // Тест документирует ФАКТИЧЕСКОЕ поведение сервера, а не желаемое по REST-конвенциям.
    @Test
    @Order(4)
    void testCreateVisitIgnoresInvalidOwnerId() {
        Map<String, String> body = Map.of(
                "date", visitDate,
                "description", visitDescription
        );

        given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/owners/{ownerId}/pets/{petId}/visits",
                        NON_EXISTENT_ID, existingPetId)
                .then()
                .statusCode(201)
                .body("petId",       equalTo(existingPetId))
                .body("date",        equalTo(visitDate))
                .body("description", equalTo(visitDescription));

        System.out.println("POST /owners/" + NON_EXISTENT_ID + "/pets/" + existingPetId
                + "/visits returned 201 — server ignores non-existent ownerId.");
    }

    // --- 5. POST визита без обязательного поля date → 404 ---
    // ИЗВЕСТНОЕ ОГРАНИЧЕНИЕ СЕРВЕРА: возвращает 404 вместо ожидаемого по REST-конвенциям 400.
    // Причина: DataIntegrityViolationException (нарушение NOT NULL в БД) всплывает и
    // обрабатывается глобальным хендлером как 404, а не как 400 Bad Request.
    // Тест документирует текущее (некорректное) поведение, а не желаемое.
    @Test
    @Order(5)
    void testCreateVisitMissingDate() {
        Map<String, String> body = Map.of("description", visitDescription);

        given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/owners/{ownerId}/pets/{petId}/visits",
                        existingOwnerId, existingPetId)
                .then()
                .statusCode(404);

        System.out.println("POST visit without date: server returned 404 (DataIntegrityViolationException — see comment).");
    }

    // --- 6. POST визита с пустым телом → 400 ---
    @Test
    @Order(6)
    void testCreateVisitWithEmptyBody() {
        given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .contentType(ContentType.JSON)
                .body(Collections.emptyMap())
                .when()
                .post("/api/owners/{ownerId}/pets/{petId}/visits",
                        existingOwnerId, existingPetId)
                .then()
                .statusCode(400);

        System.out.println("POST visit with empty body correctly returned 400.");
    }

    // --- 7. POST визита с некорректным форматом даты → 500 ---
    // ИЗВЕСТНОЕ ОГРАНИЧЕНИЕ СЕРВЕРА: возвращает 500 вместо ожидаемого по REST-конвенциям 400.
    // Причина: HttpMessageNotReadableException (Jackson не может распарсить "not-a-date" как
    // LocalDate) не перехватывается глобальным @ControllerAdvice — пробрасывается как 500.
    // Тест документирует текущее (некорректное) поведение, а не желаемое.
    @Test
    @Order(7)
    void testCreateVisitInvalidDateFormat() {
        Map<String, String> body = Map.of(
                "date", "not-a-date",
                "description", visitDescription
        );

        given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/owners/{ownerId}/pets/{petId}/visits",
                        existingOwnerId, existingPetId)
                .then()
                .statusCode(500);

        System.out.println("POST visit with invalid date format: server returned 500 (HttpMessageNotReadableException — see comment).");
    }

    // --- 8. GET несуществующего визита → 404 ---
    @Test
    @Order(8)
    void testGetNonExistentVisit() {
        given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get("/api/visits/{visitId}", NON_EXISTENT_ID)
                .then()
                .statusCode(404);

        System.out.println("GET visit id=" + NON_EXISTENT_ID
                + " correctly returned 404.");
    }

    // --- 9. Убеждаемся, что базовый визит существует и корректен (изоляция негативных тестов) ---
    @Test
    @Order(9)
    void testBaselineVisitUnaffected() {
        given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get("/api/visits/{visitId}", createdVisitId)
                .then()
                .statusCode(200)
                .body("id",          equalTo(createdVisitId))
                .body("date",        equalTo(visitDate))
                .body("description", equalTo(visitDescription))
                .body("petId",       equalTo(existingPetId));

        System.out.println("Baseline visit " + createdVisitId
                + " is intact after all negative tests.");
    }

    // --- 10. GET визитов по эндпоинту /pets/{petId}/visits для несуществующего питомца ---
    // Прямое покрытие формулировки задания: "попытаться получить записи для несуществующего
    // питомца" через GET /api/owners/{ownerId}/pets/{petId}/visits.
    //
    // ИЗВЕСТНОЕ ОГРАНИЧЕНИЕ СЕРВЕРА: эндпоинт не реализует метод GET вовсе —
    // возвращает 500 (HttpRequestMethodNotSupportedException) для ЛЮБОГО petId,
    // включая существующие. Это не обработка несуществующего ресурса, а отсутствие
    // GET-обработчика на маршруте. По REST-конвенциям ожидался бы 404 для
    // несуществующего питомца или 200 с пустым списком. Тест документирует
    // фактическое поведение сервера, а не желаемое.
    @Test
    @Order(10)
    void testGetVisitsEndpointForNonExistentPet() {
        given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get("/api/owners/{ownerId}/pets/{petId}/visits",
                        existingOwnerId, NON_EXISTENT_ID)
                .then()
                .statusCode(500)
                .body("title", equalTo("HttpRequestMethodNotSupportedException"))
                .body("status", equalTo(500));

        System.out.println("GET /owners/" + existingOwnerId + "/pets/" + NON_EXISTENT_ID
                + "/visits returned 500 — GET not supported on this route (see comment).");
    }
}
