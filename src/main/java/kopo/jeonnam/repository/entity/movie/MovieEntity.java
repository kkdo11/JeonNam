package kopo.jeonnam.repository.entity.movie;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
@Data
@Document(collection = "movies")
public class MovieEntity {
    @Id
    private String id;

    private String title;
    private String location;   // planArea
    private String posterUrl;  // posterURL
    private String Addr;  //
    private double x;          // planLon
    private double y;          // planLat
}