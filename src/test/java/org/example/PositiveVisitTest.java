package org.example;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class PositiveVisitTest {

    private static final String BASE_URL =
            "http://localhost:9966/petclinic/api";

    @Test
    void shouldCreateVisit() {

        // 1. Выбираем владельца
        given()
                .get(BASE_URL + "/owners/1")
                .then()
                .statusCode(200);

        // 2. Получаем список ветеринаров
        given()
                .get(BASE_URL + "/vets")
                .then()
                .statusCode(200);

        // 3. Создаём запись на приём
        String date = "2026-11-05";
        String description = "radiology, vet id: 2";

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "date": "%s",
                            "description": "%s"
                        }
                        """.formatted(date, description))
                .when()
                .post(BASE_URL + "/owners/1/pets/15/visits")
                .then()
                .statusCode(201)
                .body("date", equalTo(date))
                .body("description", equalTo(description));

        // 4. Проверяем, что запись сохранилась
        given()
                .get(BASE_URL + "/owners/1")
                .then()
                .statusCode(200)
                .body("pets.find { it.id == 15 }.visits[0].date",
                        equalTo(date))
                .body("pets.find { it.id == 15 }.visits[0].description",
                        equalTo(description));

    }



}