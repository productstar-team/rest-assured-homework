package org.example;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.example.utils.Constants.BASE_URL;

public class NegativeVisitTest {

    static RequestSpecification requestSpecification;

    @BeforeAll
    static void setUp() {
        requestSpecification = RestAssured.given()
                .baseUri(BASE_URL)
                .basePath("/petclinic/api")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    @Test
    void getVisitsForNonExistingPetTest() {

        // 1. Создаём владельца
        int ownerId = createOwner();

        // 2. Создаём тип питомца
        int petTypeId = createPetType();

        // 3. Создаём настоящего питомца
        int petId = createPet(ownerId, petTypeId);

        // 4. Создаём визит для настоящего питомца
        createVisit(ownerId, petId);

        // 5. Берём ID питомца, которого точно не существует
        int nonExistingPetId = 999999999;

        // 6. Пытаемся получить несуществующего питомца.
        // Вместе с питомцем API возвращает и его visits.
        // Поэтому для несуществующего pet ожидаем ошибку 404.
        requestSpecification
                .when()
                .get("/owners/" + ownerId + "/pets/" + nonExistingPetId)
                .then()
                .statusCode(404);
    }

    private int createOwner() {

        String body = """
                {
                  "firstName": "Galina",
                  "lastName": "Boiko",
                  "address": "Negative Test Street",
                  "city": "London",
                  "telephone": "3333333333"
                }
                """;

        Response response = requestSpecification
                .body(body)
                .when()
                .post("/owners")
                .then()
                .statusCode(201)
                .extract()
                .response();

        return response.jsonPath().getInt("id");
    }

    private int createPetType() {

        String unique = String.valueOf(System.currentTimeMillis() % 1_000_000);

        String body = """
                {
                  "name": "NegativeCat%s"
                }
                """.formatted(unique);

        Response response = requestSpecification
                .body(body)
                .when()
                .post("/pettypes")
                .then()
                .statusCode(201)
                .extract()
                .response();

        return response.jsonPath().getInt("id");
    }

    private int createPet(int ownerId, int petTypeId) {

        String body = """
                {
                  "name": "Murka",
                  "birthDate": "2021-06-15",
                  "type": {
                    "id": %d,
                    "name": "NegativeCat"
                  }
                }
                """.formatted(petTypeId);

        Response response = requestSpecification
                .body(body)
                .when()
                .post("/owners/" + ownerId + "/pets")
                .then()
                .statusCode(201)
                .extract()
                .response();

        return response.jsonPath().getInt("id");
    }

    private void createVisit(int ownerId, int petId) {

        String visitDate = LocalDate.now().toString();

        String body = """
                {
                  "date": "%s",
                  "description": "Negative test visit"
                }
                """.formatted(visitDate);

        requestSpecification
                .body(body)
                .when()
                .post("/owners/" + ownerId + "/pets/" + petId + "/visits")
                .then()
                .statusCode(201);
    }
}