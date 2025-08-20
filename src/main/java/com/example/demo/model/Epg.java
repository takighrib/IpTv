package com.example.demo.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "epg")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Epg {
    @Id
    private String id;

    private Integer streamId;
    private String title;
    private String start;
    private String end;
    private String description;
}
