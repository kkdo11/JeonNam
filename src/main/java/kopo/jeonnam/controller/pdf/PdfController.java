package kopo.jeonnam.controller.pdf;

import jakarta.servlet.http.HttpServletResponse;
import kopo.jeonnam.service.IPdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/gpt/schedule")  // 🔧 프런트 요청 경로에 맞춤
public class PdfController {

    private final IPdfService pdfService;

    /**
     * 여행 일정 데이터를 받아 PDF를 생성하고 클라이언트에 응답합니다.
     *
     * @param schedule JSON으로 들어오는 일정 정보 (날짜별 리스트)
     * @param lang     번역할 언어 코드 (예: "ko", "en", "zh-CN", "ja")
     * @param response HTTP 응답 객체
     */
    @PostMapping("/pdf")
    public void generateSchedulePdf(
            @RequestBody Map<String, List<Map<String, String>>> schedule,
            @RequestParam(name = "lang", defaultValue = "ko") String lang,
            HttpServletResponse response
    ) {
        try {
            log.info("📥 [PDF 생성 요청] lang = {}, schedule = {}", lang, schedule);
            byte[] pdfBytes = pdfService.createPdf(schedule, lang);

            String fileName = URLEncoder.encode("jeonnam_schedule.pdf", "UTF-8").replaceAll("\\+", "%20");
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

            OutputStream os = response.getOutputStream();
            os.write(pdfBytes);
            os.flush();
            os.close();

        } catch (Exception e) {
            log.error("❌ PDF 생성 중 오류 발생", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
