package org.example.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class VetFullData {
    private Integer id;
    private String firstName;
    private String lastName;
    private List<Specialty> specialties;
}
