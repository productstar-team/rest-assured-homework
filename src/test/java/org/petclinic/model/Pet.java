package org.petclinic.model;

public record Pet(
        Long id,
        String name,
        String birthDate,
        PetType type,
        Owner owner
) {}
