package kopo.jeonnam.dto.movie;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieDTO {
    private String id;
    private String title;
    private String location;
    private String posterUrl;
    private String addr;
    private double x;
    private double y;

}

