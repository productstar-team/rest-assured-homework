package org.example.negative;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.*;

import static io.restassured.RestAssured.*;
import static org.example.utils.Constants.*;
import static org.hamcrest.Matchers.*;

/**
 * Негативный сценарий: создаём корректный визит (baseline), затем убеждаемся,
 * что API корректно обрабатывает ошибочные запросы (404, 400).
 *
 * Порядок: step3-negative (после PetClinicSetupTest и PositiveVisitTest).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NegativeVisitTest {

    private static final int NON_EXIST_ID = 99999;

    private static int ownerId;
    private static int petId;

    @BeforeAll
    static void configure() {
        RestAssured.baseURI  = BASE_URL;
        RestAssured.basePath = BASE_PATH;
        RestAssured.authentication = preemptive().basic(ADMIN_USER, ADMIN_PASS);

        // Получаем реальные IDs из API
        JsonPath owners = given()
            .queryParam("lastName", "Smith")
        .when()
            .get("/api/owners")
        .then()
            .statusCode(SUCCESS_CODE)
            .body("size()", greaterThan(0))
            .extract().jsonPath();

        ownerId = owners.getInt("[0].id");

        petId = given()
        .when()
            .get("/api/owners/" + ownerId)
        .then()
            .statusCode(SUCCESS_CODE)
            .extract().jsonPath().getInt("pets[0].id");
    }

    // -----------------------------------------------------------------------
    // Предусловие: корректный визит создаётся успешно
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("Предусловие: создание корректного визита")
    void createValidVisitAsBaseline() {
        Map<String, String> body = new HashMap<>();
        body.put("date",        LocalDate.now().plusDays(14).toString());
        body.put("description", "Routine checkup baseline");

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/owners/" + ownerId + "/pets/" + petId + "/visits")
        .then()
            .statusCode(CREATED_CODE)
            .body("date",        notNullValue())
            .body("description", equalTo("Routine checkup baseline"));
    }

    // -----------------------------------------------------------------------
    // Негативный сценарий 1: GET визитов для несуществующего питомца → 404
    // -----------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("GET визитов для несуществующего питомца → 404")
    void getVisitsForNonExistentPet() {
        given()
        .when()
            .get("/api/owners/" + ownerId + "/pets/" + NON_EXIST_ID + "/visits")
        .then()
            .statusCode(NOT_FOUND_CODE);
    }

    // -----------------------------------------------------------------------
    // Негативный сценарий 2: GET несуществующего владельца → 404
    // -----------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("GET несуществующего владельца → 404")
    void getNonExistentOwner() {
        given()
        .when()
            .get("/api/owners/" + NON_EXIST_ID)
        .then()
            .statusCode(NOT_FOUND_CODE);
    }

    // -----------------------------------------------------------------------
    // Негативный сценарий 3: POST визита для несуществующего владельца → 404
    // -----------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("POST визита для несуществующего владельца → 404")
    void createVisitForNonExistentOwner() {
        Map<String, String> body = new HashMap<>();
        body.put("date",        LocalDate.now().plusDays(3).toString());
        body.put("description", "Should not be created");

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/owners/" + NON_EXIST_ID + "/pets/" + petId + "/visits")
        .then()
            .statusCode(NOT_FOUND_CODE);
    }

    // -----------------------------------------------------------------------
    // Негативный сценарий 4: POST визита для несуществующего питомца → 404
    // -----------------------------------------------------------------------

    @Test
    @Order(5)
    @DisplayName("POST визита для несуществующего питомца → 404")
    void createVisitForNonExistentPet() {
        Map<String, String> body = new HashMap<>();
        body.put("date",        LocalDate.now().plusDays(3).toString());
        body.put("description", "Should not be created");

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/owners/" + ownerId + "/pets/" + NON_EXIST_ID + "/visits")
        .then()
            .statusCode(NOT_FOUND_CODE);
    }

    // -----------------------------------------------------------------------
    // Негативный сценарий 5: POST визита с пустым телом → 400
    // -----------------------------------------------------------------------

    @Test
    @Order(6)
    @DisplayName("POST визита с пустым телом → 400")
    void createVisitWithEmptyBody() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post("/api/owners/" + ownerId + "/pets/" + petId + "/visits")
        .then()
            .statusCode(BAD_REQUEST_CODE);
    }

    // -----------------------------------------------------------------------
    // Негативный сценарий 6: POST визита без поля date → 400
    // -----------------------------------------------------------------------

    @Test
    @Order(7)
    @DisplayName("POST визита без поля date → 400")
    void createVisitWithoutDate() {
        Map<String, String> body = new HashMap<>();
        body.put("description", "Missing date field");

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/owners/" + ownerId + "/pets/" + petId + "/visits")
        .then()
            .statusCode(BAD_REQUEST_CODE);
    }

    // -----------------------------------------------------------------------
    // Негативный сценарий 7: POST визита без поля description → 400
    // -----------------------------------------------------------------------

    @Test
    @Order(8)
    @DisplayName("POST визита без поля description → 400")
    void createVisitWithoutDescription() {
        Map<String, String> body = new HashMap<>();
        body.put("date", LocalDate.now().plusDays(1).toString());

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/owners/" + ownerId + "/pets/" + petId + "/visits")
        .then()
            .statusCode(BAD_REQUEST_CODE);
    }
}
