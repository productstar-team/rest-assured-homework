package org.example;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.hasItem;
import static org.example.utils.Constants.BASE_URL;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PetClinicSetupTest {

    static RequestSpecification requestSpecification;

    static int firstOwnerId;
    static int secondOwnerId;

    static int catTypeId;
    static int dogTypeId;
    static int hamsterTypeId;


    static int surgerySpecialtyId;
    static int dentistrySpecialtyId;


    static int barsikId;
    static int rexId;
    static int homaId;
    static int murkaId;
    static int bobikId;

    static int firstVetId;
    static int secondVetId;
    static int thirdVetId;


    @BeforeAll
    static void setup() {
        requestSpecification = RestAssured.given()
                .baseUri(BASE_URL)
                .basePath("/petclinic/api")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    @Test
    @Order(1)
    void createAdminUser() {
        String adminJson = """
                {
                    "username": "homeworkAdmin",
                    "password": "admin123",
                    "enabled": true,
                    "roles": [
                        {
                        "name": "ADMIN"
                        }
                    ]
                }
                """;

        requestSpecification
                .body(adminJson)
                .when()
                .post("/users")
                .then()
                .log().all()
                .statusCode(201);
    }

    @Test
    @Order(2)
    void createOwners() {
        String firstOwnerJson = """
                {
                  "firstName": "Danil",
                  "lastName": "Vachoskiy",
                  "address": "Test Street 1",
                  "city": "Almaty",
                  "telephone": "7700123456"
                }
                """;

        String secondOwnerJson = """
                {
                  "firstName": "Alex",
                  "lastName": "Petrov",
                  "address": "Test Street 2",
                  "city": "Astana",
                  "telephone": "7700654321"
                }
                """;

        firstOwnerId = requestSpecification
                .body(firstOwnerJson)
                .when()
                .post("/owners")
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .path("id");

        secondOwnerId= requestSpecification
                .body(secondOwnerJson)
                .when()
                .post("/owners")
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .path("id");

        System.out.println("Danil ownerId = " + firstOwnerId);
        System.out.println("Alex ownerId = " + secondOwnerId);

    }

    @Test
    @Order(3)
    void createPetTypes() {

        String catJson = """
                {
                  "name": "cat"
                }
                """;

        String dogJson = """
                {
                  "name": "dog"
                }
                """;

        String hamsterJson = """
                {
                  "name": "hamster"
                }
                """;

        catTypeId = requestSpecification
                .body(catJson)
                .when()
                .post("/pettypes")
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .path("id");

        dogTypeId = requestSpecification
                .body(dogJson)
                .when()
                .post("/pettypes")
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .path("id");

        hamsterTypeId = requestSpecification
                .body(hamsterJson)
                .when()
                .post("/pettypes")
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .path("id");

        System.out.println("catTypeId = " + catTypeId);
        System.out.println("dogTypeId = " + dogTypeId);
        System.out.println("hamsterTypeId = " + hamsterTypeId);
    }

    @Test
    @Order(4)
    void createPets() {

        String barsikJson = """
            {
              "name": "Barsik",
              "birthDate": "2022-05-10",
              "type": {
                "id": %d,
                "name": "cat"
              }
            }
            """.formatted(catTypeId);

        String rexJson = """
            {
              "name": "Rex",
              "birthDate": "2021-08-15",
              "type": {
                "id": %d,
                "name": "dog"
              }
            }
            """.formatted(dogTypeId);

        String homaJson = """
            {
              "name": "Homa",
              "birthDate": "2023-01-20",
              "type": {
                "id": %d,
                "name": "hamster"
              }
            }
            """.formatted(hamsterTypeId);

        String murkaJson = """
            {
              "name": "Murka",
              "birthDate": "2020-11-12",
              "type": {
                "id": %d,
                "name": "cat"
              }
            }
            """.formatted(catTypeId);

        String bobikJson = """
            {
              "name": "Bobik",
              "birthDate": "2022-03-25",
              "type": {
                "id": %d,
                "name": "dog"
              }
            }
            """.formatted(dogTypeId);

        barsikId = requestSpecification
                .body(barsikJson)
                .when()
                .post("/owners/" + firstOwnerId + "/pets")
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .path("id");

        rexId = requestSpecification
                .body(rexJson)
                .when()
                .post("/owners/" + firstOwnerId + "/pets")
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .path("id");

        homaId = requestSpecification
                .body(homaJson)
                .when()
                .post("/owners/" + firstOwnerId + "/pets")
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .path("id");

        murkaId = requestSpecification
                .body(murkaJson)
                .when()
                .post("/owners/" + secondOwnerId + "/pets")
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .path("id");

        bobikId = requestSpecification
                .body(bobikJson)
                .when()
                .post("/owners/" + secondOwnerId + "/pets")
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .path("id");

        System.out.println("Barsik petId = " + barsikId);
        System.out.println("Rex petId = " + rexId);
        System.out.println("Homa petId = " + homaId);
        System.out.println("Murka petId = " + murkaId);
        System.out.println("Bobik petId = " + bobikId);
    }

    @Test
    @Order(5)
    void createSpecialties(){
        String surgeonJson = """
                {
                    "name": "surgery"
                }
                """;

        String dentistJson = """
                {
                    "name": "dentistry"
                }
                """;

        surgerySpecialtyId = requestSpecification
                .body(surgeonJson)
                .when()
                .post("/specialties")
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .path("id");

        dentistrySpecialtyId = requestSpecification
                .body(dentistJson)
                .when()
                .post("/specialties")
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .path("id");

    }

    @Test
    @Order(6)
    void createVets() {

        String firstVetJson = """
        {
          "firstName": "Sergey",
          "lastName": "Ivanov",
          "specialties": [
            {
              "id": %d,
              "name": "surgery"
            }
          ]
        }
        """.formatted(surgerySpecialtyId);

        String secondVetJson = """
        {
          "firstName": "Ivan",
          "lastName": "Petrov",
          "specialties": [
            {
              "id": %d,
              "name": "dentistry"
            }
          ]
        }
        """.formatted(dentistrySpecialtyId);

        String thirdVetJson = """
        {
          "firstName": "Alexey",
          "lastName": "Sidorov",
          "specialties": [
            {
              "id": %d,
              "name": "surgery"
            },
            {
              "id": %d,
              "name": "dentistry"
            }
          ]
        }
        """.formatted(surgerySpecialtyId, dentistrySpecialtyId);

        firstVetId = requestSpecification
                .body(firstVetJson)
                .when()
                .post("/vets")
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .path("id");

        secondVetId = requestSpecification
                .body(secondVetJson)
                .when()
                .post("/vets")
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .path("id");

        thirdVetId = requestSpecification
                .body(thirdVetJson)
                .when()
                .post("/vets")
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .path("id");

        System.out.println("Sergey vetId = " + firstVetId);
        System.out.println("Ivan vetId = " + secondVetId);
        System.out.println("Alexey vetId = " + thirdVetId);
    }

    @Test
    @Order(7)
    void createVisitPositive(){
        RestAssured.given()
                .baseUri(BASE_URL)
                .basePath("/petclinic/api/")
                .accept(ContentType.JSON)
                .when()
                .get("owners/" + firstOwnerId)
                .then()
                .log().all()
                .statusCode(200);

        RestAssured.given()
                .baseUri(BASE_URL)
                .basePath("/petclinic/api/")
                .accept(ContentType.JSON)
                .when()
                .get("vets")
                .then()
                .log().all()
                .statusCode(200);

        String visitDate = "2026-09-10";
        String description = "surgery " + firstVetId;

        String visitJson = """
                {
                    "date":"%s",
                    "description":"%s"
                }
                """.formatted(visitDate, description);

        RestAssured.given()
                .baseUri(BASE_URL)
                .basePath("/petclinic/api/")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(visitJson)
                .when()
                .post("owners/" + firstOwnerId + "/pets/" + barsikId + "/visits")
                .then()
                .log().all()
                .statusCode(201);

        RestAssured.given()
                .baseUri(BASE_URL)
                .basePath("/petclinic/api/")
                .accept(ContentType.JSON)
                .when()
                .get("owners/" + firstOwnerId)
                .then()
                .log().all()
                .statusCode(200)
                .body(
                        "pets.find { it.id == " + barsikId + " }.visits.date",
                        hasItem(visitDate)
                )
                .body(
                        "pets.find { it.id == " + barsikId + " }.visits.description",
                        hasItem(description)
                );

    }

    @Test
    @Order(8)
    void getNonExistingPetNegative(){

        int nonExistingPetId = 368;

        RestAssured.given()
                .baseUri(BASE_URL)
                .basePath("/petclinic/api/")
                .accept(ContentType.JSON)
                .when()
                .get("owners/" + firstOwnerId + "/pets/" + nonExistingPetId)
                .then()
                .log().all()
                .statusCode(404);


    }
}
