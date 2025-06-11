package kopo.jeonnam.controller.csv;

import kopo.jeonnam.service.impl.csv.MovieCsvService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MovieImportController {

    @Autowired
    private MovieCsvService movieCsvService;

    @GetMapping("/api/import-csv")
    public String importCsv() {
        try {
            // 프로젝트 실행 디렉토리 기준 상대경로 또는 절대경로
            movieCsvService.importCsv("src/main/resources/data/mediaInfo.csv");
            return "CSV 데이터가 MongoDB에 저장되었습니다.";
        } catch (Exception e) {
            return "에러 발생: " + e.getMessage();
        }
    }
}
