package org.example.petclinic;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class PetClinicApiTest {

    private static final String VISIT_DATE = LocalDate.now().plusDays(1).toString();
    private static final String VISIT_DESCRIPTION = "dentistry vet 1";

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = "http://localhost:9966/petclinic/api";
    }

    @Test
    void positiveCreateVisitTest() {
        int ownerId = 1;
        int petId = 1;

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
    int ownerId = 1;
    int notExistingPetId = 999999;

    given()
            .when()
            .get("/owners/" + ownerId + "/pets/" + notExistingPetId + "/visits")
            .then()
            .statusCode(500);
}
}
