package org.petclinic;

import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.petclinic.model.Owner;
import org.petclinic.model.Pet;
import org.petclinic.setup.DataInitializer;

import java.time.LocalDate;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@Slf4j
public class NegativeVisitTest extends BaseTest {
    private static Owner owner;
    private static Pet existingPet;
    private static final Long NON_EXISTENT_VISIT_ID = 99999L;
    private static final Long NON_EXISTENT_PET_ID = 99999L;

    @BeforeAll
    public static void init() {
        DataInitializer.initialize();
        owner = DataInitializer.getFirstOwner()
                .orElseThrow(() -> new IllegalStateException("No owner"));
        existingPet = DataInitializer.getFirstPet()
                .orElseThrow(() -> new IllegalStateException("No pet"));

        log.info("[TestData] Init using owner={}, existingPet={}", owner, existingPet);
    }

    @Test
    void getVisitByNonExistentId_shouldReturn404() {
        given()
                .get("/visits/{visitId}", NON_EXISTENT_VISIT_ID)
                .then()
                .statusCode(404)
                .body(containsString("not found"));
    }

    @Test
    void createVisitForNonExistentPet_shouldReturn400() {
        var visit = Map.of(
                "date", LocalDate.now().plusDays(2).toString(),
                "description", "Некорректный визит"
        );
        given()
                .contentType(ContentType.JSON)
                .body(visit)
                .post("/owners/{ownerId}/pets/{petId}/visits", owner.id(), NON_EXISTENT_PET_ID)
                .then()
                .statusCode(400);
    }

    @Test
    void createVisitWithInvalidDateFormat_shouldReturn400() {
        var visit = Map.of(
                "date", "2023-13-45",
                "description", "Неправильная дата"
        );
        given()
                .contentType(ContentType.JSON)
                .body(visit)
                .post("/owners/{ownerId}/pets/{petId}/visits", owner.id(), existingPet.id())
                .then()
                .statusCode(400);
    }

    @Test
    void createVisitWithoutRequiredField_shouldReturn400() {
        var visit = Map.of("description", "Только описание");
        given()
                .contentType(ContentType.JSON)
                .body(visit)
                .post("/owners/{ownerId}/pets/{petId}/visits", owner.id(), existingPet.id())
                .then()
                .statusCode(400);
    }
}
