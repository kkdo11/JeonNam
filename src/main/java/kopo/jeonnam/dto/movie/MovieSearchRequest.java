package kopo.jeonnam.dto.movie;

import lombok.Data;

@Data
public class MovieSearchRequest {
    private String keyword;     // 통합 검색 키워드 (제목, 장소, 주소)
    private String title;       // 제목으로만 검색
    private String location;    // 장소(location)로만 검색
    private String addr;        // 주소(Addr)로만 검색
    private String sortBy;      // 정렬 기준 (예: title, _id(최신순))
    private String sortDirection; // 정렬 방향 (asc, desc)
}