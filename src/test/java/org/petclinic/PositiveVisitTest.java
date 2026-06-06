package org.petclinic;

import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.petclinic.model.Owner;
import org.petclinic.model.Pet;
import org.petclinic.model.Vet;
import org.petclinic.setup.DataInitializer;

import java.time.LocalDate;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Slf4j
public class PositiveVisitTest extends BaseTest {

    private static Owner owner;
    private static Pet pet;
    private static Vet vet;
    private static String visitDate;
    private static String visitDescription;

    @BeforeAll
    public static void init() {
        DataInitializer.initialize();
        owner = DataInitializer.getFirstOwner()
                .orElseThrow(() -> new IllegalStateException("No owners found"));
        pet = DataInitializer.getFirstPet()
                .orElseThrow(() -> new IllegalStateException("No pets found"));
        vet = DataInitializer.getFirstVet()
                .orElseThrow(() -> new IllegalStateException("No vets found"));

        // Получаем специализацию ветеринара (из его списка specialties)
        var specialtyName = vet.specialties().isEmpty() ? "Unknown" : vet.specialties().get(0).name();
        visitDate = LocalDate.now().plusDays(1).toString();
        visitDescription = specialtyName + " + " + vet.id();

        log.info("[TestData] Init using owner={}, pet={}, vet={}", owner, pet, vet);
    }

    @Test
    void getOwnerById_shouldReturnOwner() {
        var actual = given()
                .get("/owners/{ownerId}", owner.id())
                .then()
                .statusCode(200)
                .extract().as(Owner.class);

        var softly = new SoftAssertions();

        softly.assertThat(actual.id()).isNotNull();
        softly.assertThat(owner.id()).isNotNull();

        softly.assertThat(actual.id()).isEqualTo(owner.id());
        softly.assertThat(actual.firstName()).isEqualTo(owner.firstName());
        softly.assertThat(actual.lastName()).isEqualTo(owner.lastName());
        softly.assertThat(actual.address()).isEqualTo(owner.address());
        softly.assertThat(actual.city()).isEqualTo(owner.city());
        softly.assertThat(actual.telephone()).isEqualTo(owner.telephone());

        var expectedPets = DataInitializer.getPetsByOwnerId(owner.id());
        softly.assertThat(actual.pets())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(expectedPets);

        softly.assertAll();
    }

    @Test
    void getAllVets_shouldReturnList() {
        var actualVets = given()
                .get("/vets")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", Vet.class);

        var expectedVets = DataInitializer.getAllVets();

        var softly = new SoftAssertions();
        softly.assertThat(actualVets).hasSizeGreaterThan(expectedVets.size());
        softly.assertThat(actualVets)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(expectedVets);
        softly.assertAll();
    }

    @Test
    void createVisit_shouldSucceedAndBeRetrievable() {
        var visitBody = Map.of(
                "date", visitDate,
                "description", visitDescription
        );

        var createdVisitId = given()
                .contentType(ContentType.JSON)
                .body(visitBody)
                .post("/owners/{ownerId}/pets/{petId}/visits", owner.id(), pet.id())
                .then()
                .statusCode(201)
                .body("date", equalTo(visitDate))
                .body("description", equalTo(visitDescription))
                .extract().jsonPath().getLong("id");

        given()
                .get("/visits/{visitId}", createdVisitId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) createdVisitId))
                .body("date", equalTo(visitDate))
                .body("description", equalTo(visitDescription))
                .body("pet.id", equalTo(pet.id().intValue()));
    }
}
