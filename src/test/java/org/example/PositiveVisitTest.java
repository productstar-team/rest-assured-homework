package org.example;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.example.utils.Constants.BASE_URL;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class PositiveVisitTest {

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
    void createVisitPositiveTest() {

        // 1. Создаём владельца
        int ownerId = createOwner();

        // 2. Создаём тип питомца
        int petTypeId = createPetType();

        // 3. Создаём питомца для владельца
        int petId = createPet(ownerId, petTypeId);

        // 4. Создаём специализацию ветеринара
        int specialtyId = createSpecialty();

        // 5. Создаём ветеринара с этой специализацией
        int vetId = createVet(specialtyId);

        // 6. GET /owners/{ownerId}
        requestSpecification
                .when()
                .get("/owners/" + ownerId)
                .then()
                .statusCode(200)
                .body("id", equalTo(ownerId));

        // 7. GET /vets
        Response vetsResponse = requestSpecification
                .when()
                .get("/vets")
                .then()
                .statusCode(200)
                .extract()
                .response();

        // Проверяем, что список ветеринаров не пустой
        assertFalse(vetsResponse.jsonPath().getList("$").isEmpty());

        // 8. Формируем дату и description
        String visitDate = LocalDate.now().toString();
        String description = "Surgery " + vetId;

        // 9. Создаём визит
        Response visitResponse = requestSpecification
                .body("""
                        {
                          "date": "%s",
                          "description": "%s"
                        }
                        """.formatted(visitDate, description))
                .when()
                .post("/owners/" + ownerId + "/pets/" + petId + "/visits")
                .then()
                .statusCode(201)
                .extract()
                .response();

        int visitId = visitResponse.jsonPath().getInt("id");

        // 10. Получаем созданный визит и проверяем его
        requestSpecification
                .when()
                .get("/visits/" + visitId)
                .then()
                .statusCode(200)
                .body("date", equalTo(visitDate))
                .body("description", equalTo(description));
    }

    private int createOwner() {

        String body = """
                {
                  "firstName": "Galina",
                  "lastName": "Boiko",
                  "address": "Test Street 10",
                  "city": "London",
                  "telephone": "1234567890"
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
                  "name": "VisitCat%s"
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
                  "name": "Barsik",
                  "birthDate": "2020-05-10",
                  "type": {
                    "id": %d,
                    "name": "VisitCat"
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

    private int createSpecialty() {

        String unique = String.valueOf(System.currentTimeMillis() % 1_000_000);

        String body = """
                {
                  "name": "Surgery%s"
                }
                """.formatted(unique);

        Response response = requestSpecification
                .body(body)
                .when()
                .post("/specialties")
                .then()
                .statusCode(201)
                .extract()
                .response();

        return response.jsonPath().getInt("id");
    }

    private int createVet(int specialtyId) {

        String body = """
                {
                  "firstName": "Alice",
                  "lastName": "Brown",
                  "specialties": [
                    {
                      "id": %d,
                      "name": "Surgery"
                    }
                  ]
                }
                """.formatted(specialtyId);

        Response response = requestSpecification
                .body(body)
                .when()
                .post("/vets")
                .then()
                .statusCode(201)
                .extract()
                .response();

        return response.jsonPath().getInt("id");
    }
}