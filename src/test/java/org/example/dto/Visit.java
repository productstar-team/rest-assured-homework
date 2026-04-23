package org.example.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Visit {
    private Integer id;
    private String date;
}