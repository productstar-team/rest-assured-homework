package org.example.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PetTypeFullData {
    private int id;
    private String name;
}
