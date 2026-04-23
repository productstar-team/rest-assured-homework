package org.example.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VisitFullData {
    private Integer id;
    private String date;
    private String description;
    private Integer petId;

}
