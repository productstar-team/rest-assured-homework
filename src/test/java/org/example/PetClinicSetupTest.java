package org.example;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.example.utils.Constants.BASE_URL;

public class PetClinicSetupTest {

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
    void createTestData() {

        String unique = String.valueOf(System.currentTimeMillis() % 1_000_000);

        // 1. Создаём пользователя с ролью admin
        createAdmin("galina" + unique);

        // 2. Создаём двух владельцев
        int owner1Id = createOwner(
                "Galina",
                "Boiko",
                "Test Street 1",
                "London",
                "1111111111"
        );

        int owner2Id = createOwner(
                "Anna",
                "Petrova",
                "Test Street 2",
                "London",
                "2222222222"
        );

        // 3. Создаём три вида питомцев
        int catTypeId = createPetType("Cat" + unique);
        int dogTypeId = createPetType("Dog" + unique);
        int birdTypeId = createPetType("Bird" + unique);

        // 4. Создаём пять питомцев
        createPet(
                owner1Id,
                "Barsik",
                "2020-05-10",
                catTypeId,
                "Cat" + unique
        );

        createPet(
                owner1Id,
                "Rex",
                "2019-03-15",
                dogTypeId,
                "Dog" + unique
        );

        createPet(
                owner1Id,
                "Kesha",
                "2021-07-20",
                birdTypeId,
                "Bird" + unique
        );

        createPet(
                owner2Id,
                "Murka",
                "2022-01-12",
                catTypeId,
                "Cat" + unique
        );

        createPet(
                owner2Id,
                "Bim",
                "2018-11-05",
                dogTypeId,
                "Dog" + unique
        );

        // 5. Создаём две специализации ветеринаров
        int surgeryId = createSpecialty("Surgery" + unique);
        int dentistryId = createSpecialty("Dentistry" + unique);

        // 6. Создаём трёх ветеринаров
        createVet(
                "Alice",
                "Brown",
                surgeryId,
                "Surgery" + unique
        );

        createVet(
                "James",
                "Wilson",
                dentistryId,
                "Dentistry" + unique
        );

        createVet(
                "Emily",
                "Taylor",
                surgeryId,
                "Surgery" + unique
        );
    }

    private void createAdmin(String username) {

        String body = """
                {
                  "username": "%s",
                  "password": "admin123",
                  "enabled": true,
                  "roles": [
                    {
                      "name": "admin"
                    }
                  ]
                }
                """.formatted(username);

        requestSpecification
                .body(body)
                .when()
                .post("/users")
                .then()
                .statusCode(201);
    }

    private int createOwner(
            String firstName,
            String lastName,
            String address,
            String city,
            String telephone
    ) {

        String body = """
                {
                  "firstName": "%s",
                  "lastName": "%s",
                  "address": "%s",
                  "city": "%s",
                  "telephone": "%s"
                }
                """.formatted(
                firstName,
                lastName,
                address,
                city,
                telephone
        );

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

    private int createPetType(String name) {

        String body = """
                {
                  "name": "%s"
                }
                """.formatted(name);

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

    private int createPet(
            int ownerId,
            String name,
            String birthDate,
            int petTypeId,
            String petTypeName
    ) {

        String body = """
                {
                  "name": "%s",
                  "birthDate": "%s",
                  "type": {
                    "id": %d,
                    "name": "%s"
                  }
                }
                """.formatted(
                name,
                birthDate,
                petTypeId,
                petTypeName
        );

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

    private int createSpecialty(String name) {

        String body = """
                {
                  "name": "%s"
                }
                """.formatted(name);

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

    private int createVet(
            String firstName,
            String lastName,
            int specialtyId,
            String specialtyName
    ) {

        String body = """
                {
                  "firstName": "%s",
                  "lastName": "%s",
                  "specialties": [
                    {
                      "id": %d,
                      "name": "%s"
                    }
                  ]
                }
                """.formatted(
                firstName,
                lastName,
                specialtyId,
                specialtyName
        );

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