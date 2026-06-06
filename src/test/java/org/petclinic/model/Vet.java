package org.petclinic.model;

import java.util.List;

public record Vet(
        Long id,
        String firstName,
        String lastName,
        List<Specialty> specialties
) {}
