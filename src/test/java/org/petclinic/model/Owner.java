package org.petclinic.model;

import java.util.List;

public record Owner(
        Long id,
        String firstName,
        String lastName,
        String address,
        String city,
        String telephone,
        List<Pet> pets
) {}
