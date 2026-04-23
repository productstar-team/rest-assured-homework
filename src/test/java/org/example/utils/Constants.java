package org.example.utils;

public interface Constants {
    String BASE_URL = System.getProperty("petclinic.baseUrl", "http://localhost:9966/petclinic/api");
    int SUCCESS_CODE = 200;
    int CREATED_CODE = 201;
    int NOT_FOUND_CODE = 404;
}
