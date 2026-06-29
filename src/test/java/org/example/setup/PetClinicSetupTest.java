package org.example.setup;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.*;

import static io.restassured.RestAssured.*;
import static org.example.utils.Constants.*;

/**
 * Предварительная настройка системы pet-clinic через REST API.
 * Запускается первым (Surefire step1-setup) и создаёт тестовые данные:
 *   - 1 пользователь с ролью admin
 *   - 2 владельца питомцев
 *   - 3 вида питомцев
 *   - 5 питомцев
 *   - 2 специализации ветеринаров
 *   - 3 ветеринара
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PetClinicSetupTest {

    static final int[] ownerIds     = new int[2];
    static final int[] petTypeIds   = new int[3];
    static final int[] specialtyIds = new int[2];

    @BeforeAll
    static void configure() {
        RestAssured.baseURI  = BASE_URL;
        RestAssured.basePath = BASE_PATH;
        RestAssured.authentication = preemptive().basic(ADMIN_USER, ADMIN_PASS);
    }

    // -----------------------------------------------------------------------
    // 1 пользователь с ролью admin
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("Создание пользователя с ролью admin")
    void createAdminUser() {
        Map<String, Object> user = new HashMap<>();
        user.put("username", "petclinic_admin");
        user.put("password", "Admin@12345");
        user.put("enabled", true);
        user.put("roles", List.of(Map.of("name", "ROLE_OWNER_ADMIN")));

        given()
            .contentType(ContentType.JSON)
            .body(user)
        .when()
            .post("/api/users")
        .then()
            .statusCode(CREATED_CODE);
    }

    // -----------------------------------------------------------------------
    // 2 владельца питомцев
    // -----------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("Создание 2 владельцев питомцев")
    void createOwners() {
        ownerIds[0] = createOwner("John",  "Smith", "123 Elm Street", "Springfield", "6085551212");
        ownerIds[1] = createOwner("Maria", "Garcia","456 Oak Avenue", "Shelbyville",  "6085559876");
    }

    // -----------------------------------------------------------------------
    // 3 вида питомцев
    // -----------------------------------------------------------------------

    @Test
    @Order(3)
    @DisplayName("Создание 3 видов питомцев")
    void createPetTypes() {
        petTypeIds[0] = createPetType("Rabbit");
        petTypeIds[1] = createPetType("Turtle");
        petTypeIds[2] = createPetType("Parrot");
    }

    // -----------------------------------------------------------------------
    // 5 питомцев (зависит от шагов 2 и 3)
    // -----------------------------------------------------------------------

    @Test
    @Order(4)
    @DisplayName("Создание 5 питомцев")
    void createPets() {
        createPet("Fluffy",   "2021-03-10", petTypeIds[0], ownerIds[0]);
        createPet("Speedy",   "2020-07-22", petTypeIds[1], ownerIds[0]);
        createPet("Rio",      "2022-01-15", petTypeIds[2], ownerIds[0]);
        createPet("Snowball", "2019-11-05", petTypeIds[0], ownerIds[1]);
        createPet("Mango",    "2023-06-30", petTypeIds[2], ownerIds[1]);
    }

    // -----------------------------------------------------------------------
    // 2 специализации ветеринаров
    // -----------------------------------------------------------------------

    @Test
    @Order(5)
    @DisplayName("Создание 2 специализаций ветеринаров")
    void createSpecialties() {
        specialtyIds[0] = createSpecialty("Neurology");
        specialtyIds[1] = createSpecialty("Oncology");
    }

    // -----------------------------------------------------------------------
    // 3 ветеринара (зависит от шага 5)
    // -----------------------------------------------------------------------

    @Test
    @Order(6)
    @DisplayName("Создание 3 ветеринаров")
    void createVets() {
        createVet("Alice", "Johnson", List.of(specialtyIds[0]));
        createVet("Bob",   "Williams", List.of(specialtyIds[1]));
        createVet("Carol", "Brown",    List.of(specialtyIds[0], specialtyIds[1]));
    }

    // -----------------------------------------------------------------------
    // Вспомогательные методы
    // -----------------------------------------------------------------------

    private int createOwner(String firstName, String lastName,
                            String address, String city, String telephone) {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", firstName);
        body.put("lastName",  lastName);
        body.put("address",   address);
        body.put("city",      city);
        body.put("telephone", telephone);

        return given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/owners")
        .then()
            .statusCode(CREATED_CODE)
            .extract().path("id");
    }

    private int createPetType(String name) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name))
        .when()
            .post("/api/pettypes")
        .then()
            .statusCode(CREATED_CODE)
            .extract().path("id");
    }

    private void createPet(String name, String birthDate, int typeId, int ownerId) {
        Map<String, Object> body = new HashMap<>();
        body.put("name",      name);
        body.put("birthDate", birthDate);
        body.put("type",      Map.of("id", typeId));

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/owners/" + ownerId + "/pets")
        .then()
            .statusCode(CREATED_CODE);
    }

    private int createSpecialty(String name) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name))
        .when()
            .post("/api/specialties")
        .then()
            .statusCode(CREATED_CODE)
            .extract().path("id");
    }

    private void createVet(String firstName, String lastName, List<Integer> ids) {
        List<Map<String, Object>> specialties = new ArrayList<>();
        for (int id : ids) {
            specialties.add(Map.of("id", id));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("firstName",   firstName);
        body.put("lastName",    lastName);
        body.put("specialties", specialties);

        given()
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/vets")
        .then()
            .statusCode(CREATED_CODE);
    }
}
