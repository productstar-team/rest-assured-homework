package org.example.utils;

public interface Constants {
    String BASE_URL  = "http://localhost:9966";
    String BASE_PATH = "/petclinic";

    String ADMIN_USER = "admin";
    String ADMIN_PASS = "admin";

    int SUCCESS_CODE     = 200;
    int CREATED_CODE     = 201;
    int BAD_REQUEST_CODE = 400;
    int NOT_FOUND_CODE   = 404;
    int CLIENT_ERROR_CODE = 503;

    String POS_API_SCHEMA_ERROR_JSON = "";
    String POS_API_SCHEMA_EMPTY_JSON = "";
}
