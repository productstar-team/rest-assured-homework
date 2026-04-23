package org.example.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OwnerFullData {
    private Integer id;
    private String firstName;
    private String lastName;
    private String address;
    private String city;
    private String telephone;
}