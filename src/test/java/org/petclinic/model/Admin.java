package org.petclinic.model;

public record Admin(String username, String password, String[] roles) {}
