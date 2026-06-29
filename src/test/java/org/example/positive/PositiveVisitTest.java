package org.example.positive;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.*;

import static io.restassured.RestAssured.*;
import static org.example.utils.Constants.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Позитивный сценарий: пользователь выбирает владельца, питомца и ветеринара,
 * создаёт запись на приём и проверяет её наличие в системе.
 *
 * Порядок: step2-positive (после PetClinicSetupTest).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PositiveVisitTest {

    private static int    ownerId;
    private static int    petId;
    private static int    vetId;
    private static String vetSpecialtyName;
    private static String visitDate;
    private static String visitDescription;

    @BeforeAll
    static void configure() {
        RestAssured.baseURI  = BASE_URL;
        RestAssured.basePath = BASE_PATH;
        RestAssured.authentication = preemptive().basic(ADMIN_USER, ADMIN_PASS);
        visitDate = LocalDate.now().plusDays(7).toString();
    }

    // -----------------------------------------------------------------------
    // Шаг 1 — GET /api/owners/{ownerId}: выбор владельца и питомца
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("Шаг 1: выбор владельца и питомца")
    void selectOwnerAndPet() {
        JsonPath owners = given()
            .queryParam("lastName", "Smith")
        .when()
            .get("/api/owners")
        .then()
            .statusCode(SUCCESS_CODE)
            .body("size()", greaterThan(0))
            .extract().jsonPath();

        ownerId = owners.getInt("[0].id");
        assertNotEquals(0, ownerId, "ID владельца должен быть ненулевым");

        JsonPath ownerDetails = given()
        .when()
            .get("/api/owners/" + ownerId)
        .then()
            .statusCode(SUCCESS_CODE)
            .body("id",        equalTo(ownerId))
            .body("firstName", equalTo("John"))
            .body("lastName",  equalTo("Smith"))
            .body("pets",      not(empty()))
            .extract().jsonPath();

        petId = ownerDetails.getInt("pets[0].id");
        assertNotEquals(0, petId, "ID питомца должен быть ненулевым");
    }

    // -----------------------------------------------------------------------
    // Шаг 2 — GET /api/vets: выбор ветеринара
    // -----------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("Шаг 2: получение списка ветеринаров и выбор ветеринара")
    void selectVet() {
        List<Map<String, Object>> vets = given()
        .when()
            .get("/api/vets")
        .then()
            .statusCode(SUCCESS_CODE)
            .body("size()", greaterThan(0))
            .extract().jsonPath().getList("$");

        for (Map<String, Object> vet : vets) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> specialties =
                (List<Map<String, Object>>) vet.get("specialties");

            if (specialties == null || specialties.isEmpty()) continue;

            if ("Alice".equals(vet.get("firstName")) && "Johnson".equals(vet.get("lastName"))) {
                vetId = (int) vet.get("id");
                vetSpecialtyName = (String) specialties.get(0).get("name");
                break;
            }
        }

        assertNotEquals(0, vetId,
            "Ветеринар Alice Johnson должен присутствовать в списке");
        assertNotNull(vetSpecialtyName,
            "Специализация ветеринара не должна быть null");

        visitDescription = vetSpecialtyName + " " + vetId;
    }

    // -----------------------------------------------------------------------
    // Шаг 3 — POST /api/owners/{ownerId}/pets/{petId}/visits: создание визита
    // -----------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("Шаг 3: создание записи на приём")
    void createVisit() {
        Map<String, String> body = new HashMap<>();
        body.put("date",        visitDate);
        body.put("description", visitDescription);

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/owners/" + ownerId + "/pets/" + petId + "/visits")
        .then()
            .statusCode(CREATED_CODE)
            .body("date",        equalTo(visitDate))
            .body("description", equalTo(visitDescription));
    }

    // -----------------------------------------------------------------------
    // Шаг 4 — GET /api/owners/{ownerId}: проверка существования визита
    // -----------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("Шаг 4: проверка существования созданного визита")
    void verifyVisitExists() {
        JsonPath ownerJson = given()
        .when()
            .get("/api/owners/" + ownerId)
        .then()
            .statusCode(SUCCESS_CODE)
            .extract().jsonPath();

        List<Map<String, Object>> pets = ownerJson.getList("pets");
        @SuppressWarnings("unchecked")
        Map<String, Object> targetPet = pets.stream()
            .filter(p -> petId == (int) p.get("id"))
            .findFirst()
            .orElseThrow(() ->
                new AssertionError("Питомец id=" + petId + " не найден у владельца id=" + ownerId));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> visits =
            (List<Map<String, Object>>) targetPet.get("visits");

        assertNotNull(visits,  "Список визитов не должен быть null");
        assertFalse(visits.isEmpty(), "Список визитов не должен быть пустым");

        boolean found = visits.stream().anyMatch(v ->
            visitDate.equals(v.get("date")) &&
            visitDescription.equals(v.get("description"))
        );

        assertTrue(found,
            String.format("Визит с date='%s' и description='%s' не найден у питомца id=%d",
                visitDate, visitDescription, petId));
    }
}
