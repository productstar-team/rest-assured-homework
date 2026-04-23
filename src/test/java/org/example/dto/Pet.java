package org.example.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Pet {
    private String name;
    private String birthDate;
    private PetTypeFullData type;
    private Integer ownerId;
}
