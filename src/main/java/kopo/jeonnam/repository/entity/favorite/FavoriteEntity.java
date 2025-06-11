package kopo.jeonnam.repository.entity.favorite;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "favorite")
@CompoundIndexes({
        @CompoundIndex(name = "user_name_type_idx", def = "{'userId': 1, 'name': 1, 'type': 1}", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteEntity {
    @Id
    private String id;

    private String userId;
    private String type;
    private String name;
    private String location;
    private String x;
    private String y;

    private String planPhone;
    private String planHomepage;
    private String planParking;
    private String planContents;
}
