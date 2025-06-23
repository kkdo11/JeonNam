package kopo.jeonnam.service.impl.pdf;

// OpenPDF 관련 임포트
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import jakarta.annotation.PostConstruct;
import kopo.jeonnam.service.IPapagoTextTranslateService;
import kopo.jeonnam.service.IPdfService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.*;
import java.util.*;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfService implements IPdfService {

    private final IPapagoTextTranslateService papagoTextTranslateService;

    private static final String FONT_PATH = "src/main/resources/static/fonts/NanumGothic.ttf";
    private static final String LOGO_PATH = "src/main/resources/static/images/JN_logo_nobg.png";
    private Font koreanFont;
    private Font koreanBoldFont;

    @PostConstruct
    public void initFonts() {
        try {
            BaseFont baseFont = BaseFont.createFont(FONT_PATH, BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            koreanFont = new Font(baseFont, 10, Font.NORMAL, Color.BLACK);
            koreanBoldFont = new Font(baseFont, 18, Font.BOLD, Color.BLACK);
        } catch (IOException | com.lowagie.text.DocumentException e) {
            log.error("❌ 한글 폰트 로드 실패: {}", FONT_PATH, e);
            koreanFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
            koreanBoldFont = new Font(Font.HELVETICA, 18, Font.BOLD, Color.BLACK);
        }
    }

    @Override
    public byte[] createPdf(Map<String, List<Map<String, String>>> schedule, String lang) throws Exception {
        log.info("✅ [PDF 생성 시작] 언어: {}", lang);
        log.info("✅ [받은 schedule 데이터] = {}", schedule);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);

        document.open();

        try {
            Image logo = Image.getInstance(LOGO_PATH);
            logo.scaleAbsolute(100, 72);
            logo.setAlignment(Element.ALIGN_CENTER);
            document.add(logo);
            document.add(new Paragraph(" ", new Font(Font.HELVETICA, 10)));
        } catch (IOException | com.lowagie.text.DocumentException e) {
            log.error("❌ 로고 이미지 로드 또는 추가 실패: {}", LOGO_PATH, e);
        }

        String normalizedLang = normalizeLang(lang);  // 여기에 lang 정제
        String titleText = switch (normalizedLang) {
            case "en" -> "Jeonnam Travel Schedule";
            case "zh-cn" -> "全罗南道旅行日程";
            case "ja" -> "全羅南道 旅行スケジュール";
            default -> "전라남도 여행 일정표";
        };


        Paragraph titleParagraph = new Paragraph(titleText, koreanBoldFont);
        titleParagraph.setAlignment(Paragraph.ALIGN_CENTER);
        titleParagraph.setSpacingAfter(20);
        document.add(titleParagraph);

        for (Map.Entry<String, List<Map<String, String>>> entry : schedule.entrySet()) {
            String date = entry.getKey();
            List<Map<String, String>> daySchedule = entry.getValue();

            log.info("📅 [{}] 날짜에 대한 일정 데이터", date);
            Paragraph dateParagraph = new Paragraph("📅 " + date, new Font(koreanFont.getBaseFont(), 13, Font.BOLD, Color.BLACK));
            dateParagraph.setSpacingAfter(10);
            document.add(dateParagraph);

            PdfPTable table = new PdfPTable(new float[]{1.5f, 3.5f, 4.0f, 5.0f});
            table.setWidthPercentage(100);

            String[] headers = switch (lang.toLowerCase(Locale.ROOT)) {
                case "en" -> new String[]{"Time", "Place", "Activity", "Address"};
                case "zh-cn" -> new String[]{"时间", "地点", "活动", "地址"};
                case "ja" -> new String[]{"時間", "場所", "活動", "住所"};
                default -> new String[]{"시간", "장소", "활동", "주소"};
            };

            for (String header : headers) {
                PdfPCell headerCell = new PdfPCell(new Phrase(header, new Font(koreanFont.getBaseFont(), 10, Font.BOLD, Color.BLACK)));
                headerCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                headerCell.setPadding(5);
                table.addCell(headerCell);
            }

            daySchedule.sort(Comparator.comparing(item -> item.getOrDefault("time", "00:00")));

            for (Map<String, String> item : daySchedule) {
                log.info("📌 [{}] 일정 항목: {}", date, item);

                String time = cleanText(item.getOrDefault("time", "-"));
                String place = cleanText(item.getOrDefault("place", "-"));
                String activity = cleanText(item.getOrDefault("activity", "-"));
                String addr = cleanText(item.getOrDefault("addr", "-"));

                // ✅ 번역 적용 (ko가 아닌 경우)
                if (!"ko".equalsIgnoreCase(lang)) {
                    String targetLang = switch (normalizedLang) {
                        case "en" -> "en";
                        case "zh-cn" -> "zh-CN";
                        case "ja" -> "ja";
                        default -> "ko";
                    };

                    log.info("🌐 번역 대상 언어: {}", targetLang);
                    log.info("🔤 원문: place={}, activity={}, addr={}", place, activity, addr);

                    place = papagoTextTranslateService.translateText("ko", targetLang, place);
                    activity = papagoTextTranslateService.translateText("ko", targetLang, activity);
                    addr = papagoTextTranslateService.translateText("ko", targetLang, addr);
                }

                PdfPCell timeCell = new PdfPCell(new Phrase(time, koreanFont));
                timeCell.setPadding(5);
                table.addCell(timeCell);

                PdfPCell placeCell = new PdfPCell(new Phrase(place, koreanFont));
                placeCell.setPadding(5);
                table.addCell(placeCell);

                PdfPCell activityCell = new PdfPCell(new Phrase(activity, koreanFont));
                activityCell.setPadding(5);
                table.addCell(activityCell);

                PdfPCell addrCell = new PdfPCell(new Phrase(addr, koreanFont));
                addrCell.setPadding(5);
                table.addCell(addrCell);
            }

            document.add(table);
            document.add(new Paragraph(" ", new Font(Font.HELVETICA, 10)));
        }

        document.close();
        return baos.toByteArray();
    }

    private String cleanText(String value) {
        if (value == null || value.trim().isEmpty()) return "-";
        String trimmed = value.trim();
        if (trimmed.equals("-") || trimmed.equals("()") || trimmed.equals("( )") || trimmed.equalsIgnoreCase("undefined")) {
            return "-";
        }
        return trimmed;
    }
    private String normalizeLang(String lang) {
        if (lang == null) return "ko";
        lang = lang.trim().toLowerCase();

        if (lang.startsWith("en")) return "en";
        if (lang.startsWith("zh") || lang.equals("cn")) return "zh-cn";
        if (lang.startsWith("ja") || lang.equals("jp")) return "ja";

        return "ko";
    }

}
