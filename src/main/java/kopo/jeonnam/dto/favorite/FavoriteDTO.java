package kopo.jeonnam.dto.favorite;

public record FavoriteDTO(
        String userId,
        String type,
        String name,
        String location,
        String addr,
        String posterUrl,
        double x,
        double y,
        String planPhone,
        String planHomepage,
        String planParking,
        String planContents
) {
}
