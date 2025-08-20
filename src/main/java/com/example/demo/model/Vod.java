package com.example.demo.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "vods")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Vod {
    @Id
    private String id;

    private Integer vodId;
    private String name;
    private Integer categoryId;
    private String categoryName;
    private String streamUrl;
    private String streamIcon;
}
