package org.example;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

public class NegativeVisitTest {

    private static final String BASE_URL =
            "http://localhost:9966/petclinic/api";

    @Test
    void shouldNotCreateVisitForNonExistingPet() {

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "date": "2026-11-05",
                            "description": "radiology, vet id: 2"
                        }
                        """)
                .when()
                .post(BASE_URL + "/owners/1/pets/9999/visits")
                .then()
                .statusCode(404);
    }
}