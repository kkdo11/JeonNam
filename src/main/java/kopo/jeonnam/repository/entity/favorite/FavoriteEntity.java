package kopo.jeonnam.repository.entity.favorite;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "favorite")
@Getter
@NoArgsConstructor
public class FavoriteEntity {

    @Id
    private String id;

    private String userId;
    private String type; // "media", "place", ...
    private String name;
    private String location;
    private String addr;
    private double x;
    private double y;
    private String posterUrl;
    private String planPhone;
    private String planHomepage;
    private String planParking;
    private String planContents;

    @Builder
    public FavoriteEntity(String id, String userId, String type, String name, String location,
                          String addr, double x, double y, String posterUrl,
                          String planPhone, String planHomepage,
                          String planParking, String planContents) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.name = name;
        this.location = location;
        this.addr = addr; // ✅ assign to field
        this.x = x;
        this.y = y;
        this.posterUrl = posterUrl;
        this.planPhone = planPhone;
        this.planHomepage = planHomepage;
        this.planParking = planParking;
        this.planContents = planContents;
    }
}
