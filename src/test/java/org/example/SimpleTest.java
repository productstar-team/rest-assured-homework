package org.example;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.example.dto.*;
import org.example.utils.Constants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.example.utils.Constants.BASE_URL;
import static org.example.utils.Constants.SUCCESS_CODE;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;

public class SimpleTest {
    private static RequestSpecification requestSpecification;

    @BeforeAll
    public static void setUp() {
        requestSpecification = new RequestSpecBuilder()
                .setBaseUri(Constants.BASE_URL)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
    }

    private static RequestSpecification newRequest() {
        return RestAssured.given().spec(requestSpecification);
    }

    @Test
    public void shouldCreateVisitForSelectedOwnerPetAndVet() {
        User user = User.builder().username("Markus Kolg").password("WwqzF223").enabled(true).roles(new HashSet<>(List.of(Role.builder().name("admin").build()))).build();

        Owner owner = Owner.builder()
                .firstName("Julia")
                .lastName("Mold")
                .address("123 Broadway")
                .city("New York")
                .telephone("1314316792")
                .build();
        Integer ownerId = createOwner(owner);

        Integer petTypeId = createPetType("cat");
        Integer petId = createPet(
                Pet.builder()
                        .name("Simba")
                        .birthDate("2023-10-12")
                        .type(PetTypeFullData.builder().id(petTypeId).name("cat").build())
                        .ownerId(ownerId)
                        .build(),
                ownerId
        );

        Integer specialtyId = createSpecialty(Specialty.builder().name("radiology").build());
        Integer vetId = createVet(
                Vet.builder()
                        .firstName("Max")
                        .lastName("Down")
                        .specialties(new ArrayList<>(List.of(
                                SpecialtyFullData.builder().id(specialtyId).name("radiology").build()
                        )))
                        .build()
        );

        newRequest()
                .pathParam("ownerId", ownerId)
                .get("/owners/{ownerId}")
                .then()
                .statusCode(Constants.SUCCESS_CODE)
                .body("id", equalTo(ownerId))
                .body("pets.id", hasItem(petId));

        List<Map<String, Object>> vets = newRequest()
                .get("/vets")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("");

        Map<String, Object> selectedVet = vets.stream()
                .filter(vet -> Integer.valueOf(vet.get("id").toString()).equals(vetId))
                .findFirst()
                .orElseThrow();

        List<Map<String, Object>> specialties = (List<Map<String, Object>>) selectedVet.get("specialties");
        String specializationName = specialties.get(0).get("name").toString();

        String visitDate = "2026-04-24";
        String visitDescription = specializationName + " " + vetId;

        createVisit(ownerId, petId, VisitFullData.builder()
                .date(visitDate)
                .description(visitDescription)
                .build());

        newRequest()
                .pathParam("ownerId", ownerId)
                .get("/owners/{ownerId}")
                .then()
                .statusCode(Constants.SUCCESS_CODE)
                .body(String.format("pets.find { it.id == %d }.visits.date", petId), hasItem(visitDate))
                .body(String.format("pets.find { it.id == %d }.visits.description", petId), hasItem(visitDescription));
    }

    @Test
    public void shouldReturnNotFoundWhenGettingVisitsForNonExistingPet() {
        Integer ownerId = createOwner(Owner.builder()
                .firstName("John")
                .lastName("Kriq")
                .address("789 Michigan Avenue")
                .city("Chicago")
                .telephone("3125550456")
                .build());
        Integer petTypeId = createPetType("dog");
        Integer petId = createPet(
                Pet.builder()
                        .name("Charlie")
                        .birthDate("2018-05-01")
                        .type(PetTypeFullData.builder().id(petTypeId).name("dog").build())
                        .ownerId(ownerId)
                        .build(),
                ownerId
        );

        Integer visitId = createVisit(ownerId, petId, VisitFullData.builder()
                .date("2026-04-25")
                .description("regular-checkup " + petId)
                .build());

        newRequest()
                .pathParam("visitId", visitId + 999_999)
                .get("/visits/{visitId}")
                .then()
                .statusCode(Constants.NOT_FOUND_CODE);
    }

    private Integer createOwner(Owner owner) {
        return newRequest()
                .body(owner)
                .post("/owners")
                .then()
                .statusCode(Constants.CREATED_CODE)
                .extract()
                .jsonPath()
                .getInt("id");
    }

    private Integer createPetType(String name) {
        return newRequest()
                .body(PetType.builder().name(name).build())
                .post("/pettypes")
                .then()
                .statusCode(Constants.CREATED_CODE)
                .extract()
                .jsonPath()
                .getInt("id");
    }

    private Integer createPet(Pet pet, Integer ownerId) {
        return newRequest()
                .pathParam("ownerId", ownerId)
                .body(pet)
                .post("/owners/{ownerId}/pets")
                .then()
                .statusCode(Constants.CREATED_CODE)
                .extract()
                .jsonPath()
                .getInt("id");
    }

    private Integer createSpecialty(Specialty specialty) {
        return newRequest()
                .with().body(specialty)
                .contentType(ContentType.JSON)
                .when()
                .post("/specialties")
                .then()
                .statusCode(Constants.CREATED_CODE)
                .extract()
                .jsonPath()
                .get("id");
    }

    private Integer createVet(Vet vet) {
        return newRequest()
                .with().body(vet)
                .contentType(ContentType.JSON)
                .when()
                .post("/vets")
                .then()
                .statusCode(Constants.CREATED_CODE)
                .extract()
                .jsonPath()
                .get("id");
    }

    private Integer createVisit(Integer ownerId, Integer petId, VisitFullData visit) {
        return newRequest()
                .pathParam("ownerId", ownerId)
                .pathParam("petId", petId)
                .body(visit)
                .post("/owners/{ownerId}/pets/{petId}/visits")
                .then()
                .statusCode(Constants.CREATED_CODE)
                .body("id", notNullValue())
                .body("date", equalTo(visit.getDate()))
                .body("description", equalTo(visit.getDescription()))
                .extract()
                .jsonPath()
                .getInt("id");
    }
}
