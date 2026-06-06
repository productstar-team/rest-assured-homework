package org.petclinic;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;

@Slf4j
public class BaseTest {

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = "http://localhost:9966/petclinic/api";
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        log.info("[Config] RestAssured configured with baseURI: {}", RestAssured.baseURI);
    }
}
