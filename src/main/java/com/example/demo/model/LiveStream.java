package com.example.demo.model;



import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "live_streams")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveStream {

    @Id
    private String id;
    private Integer streamId;
    private String name;
    private Integer categoryId;
    private String categoryName;
    private String streamUrl;
    private String streamIcon;
}
