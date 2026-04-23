package org.example.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class Vet {
    private String firstName;
    private String lastName;
    private List<SpecialtyFullData> specialties;
}

