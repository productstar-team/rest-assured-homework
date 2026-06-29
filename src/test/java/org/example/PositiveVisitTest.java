package org.example;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PositiveVisitTest {

    private static final String BASE_URL = "http://localhost:9966/petclinic";
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin";

    // Shared state between ordered tests
    private static int ownerId;
    private static int petId;
    private static int vetId;
    private static String vetSpecialtyName;
    private static String visitDate;
    private static String visitDescription;
    private static int createdVisitId;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Resolve first available owner and their first pet
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
        ownerId = (int) firstOwner.get("id");

        List<Map<String, Object>> pets = (List<Map<String, Object>>) firstOwner.get("pets");
        assertFalse(pets == null || pets.isEmpty(), "Owner " + ownerId + " has no pets");
        petId = (int) pets.get(0).get("id");

        // Resolve first available vet and their first specialty
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
        vetId = (int) firstVet.get("id");

        List<Map<String, Object>> specialties = (List<Map<String, Object>>) firstVet.get("specialties");
        vetSpecialtyName = (specialties != null && !specialties.isEmpty())
                ? (String) specialties.get(0).get("name")
                : "general";

        // Compose visit data
        visitDate = LocalDate.now().toString();
        visitDescription = vetSpecialtyName + " " + vetId;

        System.out.println("=== Test context resolved ===");
        System.out.println("  ownerId=" + ownerId + ", petId=" + petId);
        System.out.println("  vetId=" + vetId + ", specialty=" + vetSpecialtyName);
        System.out.println("  visitDate=" + visitDate + ", description=\"" + visitDescription + "\"");
    }

    // --- 1. GET /owners/{ownerId} ---
    @Test
    @Order(1)
    void testGetOwnerById() {
        given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get("/api/owners/{ownerId}", ownerId)
                .then()
                .statusCode(200)
                .body("id", equalTo(ownerId))
                .body("firstName", not(emptyOrNullString()))
                .body("lastName", not(emptyOrNullString()))
                .body("address", not(emptyOrNullString()))
                .body("city", not(emptyOrNullString()))
                .body("telephone", not(emptyOrNullString()))
                .body("pets", not(empty()));

        System.out.println("Owner " + ownerId + " fetched successfully with pets.");
    }

    // --- 2. GET /vets ---
    @Test
    @Order(2)
    void testGetVets() {
        given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get("/api/vets")
                .then()
                .statusCode(200)
                .body("$", not(empty()))
                .body("size()", greaterThanOrEqualTo(3))
                .body("[0].id", notNullValue())
                .body("[0].firstName", not(emptyOrNullString()))
                .body("[0].lastName", not(emptyOrNullString()))
                // проверяем что хотя бы у одного ветеринара есть специализация
                // (первый vet в demo-данных может не иметь specialties)
                .body("specialties.flatten()", not(empty()));

        System.out.println("Vets list fetched successfully, at least 3 vets present.");
    }

    // --- 3. POST /owners/{ownerId}/pets/{petId}/visits ---
    @Test
    @Order(3)
    void testCreateVisit() {
        Map<String, String> visitBody = Map.of(
                "date", visitDate,
                "description", visitDescription
        );

        Response response = given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .contentType(ContentType.JSON)
                .body(visitBody)
                .when()
                .post("/api/owners/{ownerId}/pets/{petId}/visits", ownerId, petId)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("date", equalTo(visitDate))
                .body("description", equalTo(visitDescription))
                .body("petId", equalTo(petId))
                .extract().response();

        createdVisitId = response.jsonPath().getInt("id");
        System.out.println("Visit created: id=" + createdVisitId
                + ", date=" + visitDate + ", description=\"" + visitDescription + "\"");
    }

    // --- 4. Проверка существования visit с заданными description и date ---
    // GET /api/owners/{id}/pets/{id}/visits не поддерживает метод GET (500 от сервера).
    // Используем GET /api/visits (все визиты) и фильтруем по petId в Java.
    @Test
    @Order(4)
    void testVerifyVisitExists() {
        // Получаем все визиты и ищем наш по petId + description + date
        List<Map<String, Object>> allVisits = given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get("/api/visits")
                .then()
                .statusCode(200)
                .body("$", not(empty()))
                .extract().jsonPath().getList("$");

        boolean foundInList = allVisits.stream()
                .filter(v -> petId == ((Number) v.get("petId")).intValue())
                .anyMatch(v ->
                        visitDescription.equals(v.get("description")) &&
                        visitDate.equals(v.get("date")));

        assertTrue(foundInList,
                "Visit with petId=" + petId + ", description=\"" + visitDescription
                + "\", date=" + visitDate + " not found in GET /api/visits");

        // Дополнительно — прямая проверка по visit ID
        given()
                .auth().basic(ADMIN_USER, ADMIN_PASS)
                .when()
                .get("/api/visits/{visitId}", createdVisitId)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdVisitId))
                .body("date", equalTo(visitDate))
                .body("description", equalTo(visitDescription))
                .body("petId", equalTo(petId));

        System.out.println("Visit " + createdVisitId + " verified: description=\""
                + visitDescription + "\", date=" + visitDate);
    }
}
