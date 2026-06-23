package org.example.petclinic;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class PetClinicApiTest {

    private static int ownerId;
    private static int petId;

    private static final String VISIT_DATE = LocalDate.now().plusDays(1).toString();
    private static final String VISIT_DESCRIPTION = "dentistry vet 1";

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = "http://localhost:9966/petclinic/api";

        createAdmin();
        createPetTypes();
        createOwners();
        createPets();
        createSpecialties();
        createVets();
    }

    private static void createAdmin() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "username": "testAdmin",
                          "password": "password",
                          "enabled": true,
                          "roles": [
                            {
                              "name": "OWNER_ADMIN"
                            }
                          ]
                        }
                        """)
                .when()
                .post("/users")
                .then()
                .statusCode(anyOf(is(200), is(201), is(400), is(404)));
    }

    private static void createPetTypes() {
        createPetType("cat");
        createPetType("dog");
        createPetType("hamster");
    }

    private static void createPetType(String name) {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "name": "%s"
                        }
                        """.formatted(name))
                .when()
                .post("/pettypes")
                .then()
                .statusCode(anyOf(is(200), is(201), is(400)));
    }

    private static void createOwners() {
        ownerId = createOwner("Ivan", "Petrov", "Moscow Street 1", "Moscow", "1234567890");
        createOwner("Anna", "Sidorova", "Nevsky 10", "Saint Petersburg", "0987654321");
    }

    private static int createOwner(String firstName, String lastName, String address, String city, String telephone) {
        return given()
                .contentType("application/json")
                .body("""
                        {
                          "firstName": "%s",
                          "lastName": "%s",
                          "address": "%s",
                          "city": "%s",
                          "telephone": "%s"
                        }
                        """.formatted(firstName, lastName, address, city, telephone))
                .when()
                .post("/owners")
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .extract()
                .path("id");
    }

    private static void createPets() {
        petId = createPet(ownerId, "Barsik", "2020-01-01", 1, "cat");
        createPet(ownerId, "Sharik", "2021-02-02", 2, "dog");
        createPet(ownerId, "Homa", "2022-03-03", 3, "hamster");
        createPet(ownerId, "Murzik", "2023-04-04", 1, "cat");
        createPet(ownerId, "Rex", "2024-05-05", 2, "dog");
    }

    private static int createPet(int ownerId, String name, String birthDate, int typeId, String typeName) {
    return given()
            .contentType("application/json")
            .body("""
                    {
                      "name": "%s",
                      "birthDate": "%s",
                      "type": {
                        "id": %d,
                        "name": "%s"
                      }
                    }
                    """.formatted(name, birthDate, typeId, typeName))
            .when()
            .post("/owners/" + ownerId + "/pets")
            .then()
            .statusCode(anyOf(is(200), is(201)))
            .extract()
            .path("id");
    }

    private static void createSpecialties() {
        createSpecialty("dentistry");
        createSpecialty("surgery");
    }

    private static void createSpecialty(String name) {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "name": "%s"
                        }
                        """.formatted(name))
                .when()
                .post("/specialties")
                .then()
                .statusCode(anyOf(is(200), is(201), is(400)));
    }

    private static void createVets() {
        createVet("John", "Smith", 1);
        createVet("Bob", "Brown", 2);
        createVet("Alice", "White", 1);
    }

    private static void createVet(String firstName, String lastName, int specialtyId) {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "firstName": "%s",
                          "lastName": "%s",
                          "specialties": [
                            {
                              "id": %d
                            }
                          ]
                        }
                        """.formatted(firstName, lastName, specialtyId))
                .when()
                .post("/vets")
                .then()
                .statusCode(anyOf(is(200), is(201), is(400)));
    }

    @Test
    void positiveCreateVisitTest() {
        given()
                .when()
                .get("/owners/" + ownerId)
                .then()
                .statusCode(200)
                .body("id", equalTo(ownerId));

        given()
                .when()
                .get("/vets")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0));

        given()
                .contentType("application/json")
                .body("""
                        {
                          "date": "%s",
                          "description": "%s"
                        }
                        """.formatted(VISIT_DATE, VISIT_DESCRIPTION))
                .when()
                .post("/owners/" + ownerId + "/pets/" + petId + "/visits")
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .body("date", equalTo(VISIT_DATE))
                .body("description", equalTo(VISIT_DESCRIPTION));
    }

    @Test
    void negativeGetVisitsForNotExistingPetTest() {
        int notExistingPetId = 999999;

        given()
                .when()
                .get("/owners/" + ownerId + "/pets/" + notExistingPetId + "/visits")
                .then()
                .statusCode(500);
    }
}
