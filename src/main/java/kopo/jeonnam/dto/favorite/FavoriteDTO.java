package kopo.jeonnam.dto.favorite;

public record FavoriteDTO(
        String userId,
        String type,
        String name,
        String location,
        String x,
        String y,
        String planPhone,
        String planHomepage,
        String planParking,
        String planContents
) {}
