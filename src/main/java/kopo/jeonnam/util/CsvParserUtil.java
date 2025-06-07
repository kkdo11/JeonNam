package kopo.jeonnam.util;

import kopo.jeonnam.dto.csv.MediaSpotDTO;
import kopo.jeonnam.dto.csv.ProductDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CsvParserUtil {

    public static List<ProductDTO> parseProducts(InputStream csvInputStream) {
        List<ProductDTO> productList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(csvInputStream, Charset.forName("MS949")))) {

            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .parse(reader);

            for (CSVRecord record : records) {
                String rawProName = record.get("등록명칭").trim();
                String rawProArea = record.get("대상지역").trim();

                String proName = extractProductName(rawProName);
                String proArea = extractAreaName(rawProArea);

                ProductDTO dto = ProductDTO.builder()
                        .proId(null)
                        .proRegNo(record.get("등록번호").trim())
                        .proName(proName)
                        .proRegDate(record.get("등록일자").trim())
                        .proArea(proArea)
                        .proPlanQty(record.get("생산계획량(톤)").trim())
                        .proCompany(record.get("업체명").trim())
                        .proBaseDate(record.get("데이터기준일자").trim())
                        .build();

                productList.add(dto);
            }

            log.info("✅ CSV 파싱 완료, 총 {}개 아이템 읽음", productList.size());

        } catch (Exception e) {
            log.error("❌ CSV 파싱 중 오류 발생", e);
        }

        return productList;
    }

    private static String extractProductName(String rawName) {
        // "보성 녹차" -> "녹차"
        String[] words = rawName.split(" ");
        return words.length > 0 ? words[words.length - 1] : rawName;
    }

    private static String extractAreaName(String rawArea) {
        // "행정구역상 전라남도 보성군 일원" 또는 "전라남도 순천시 일원" → "보성군", "순천시"
        String cleaned = rawArea.replace("행정구역상", "").trim();

        // 군 또는 시를 포함하는 패턴
        Pattern pattern = Pattern.compile("전라남도\\s+(.+?[군시])\\s+일원");
        Matcher matcher = pattern.matcher(cleaned);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return rawArea;
    }

    public static List<MediaSpotDTO> parseMediaSpots(InputStream csvInputStream) {
        List<MediaSpotDTO> list = new ArrayList<>();
        Set<String> spotNameSet = new HashSet<>();  // 중복 방지용 Set

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new BOMInputStream(csvInputStream), StandardCharsets.UTF_8))) {

            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .parse(reader);

            for (CSVRecord record : records) {
                String ctprvnNm = record.get("CTPRVN_NM").trim();
                if (ctprvnNm == null || !"전라남도".equals(ctprvnNm)) continue;

                String rawTitle = record.get("POI_NM").trim();
                String cleanedTitle = cleanTitle(rawTitle);

                if (cleanedTitle.isEmpty()) continue;
                if (spotNameSet.contains(cleanedTitle)) continue;  // 중복 방지

                MediaSpotDTO dto = MediaSpotDTO.builder()
                        .spotId(record.get("ID").trim())
                        .spotNm(cleanedTitle)
                        .spotArea(record.get("SIGNGU_NM").trim())
                        .spotLegalDong(record.get("LEGALDONG_NM").trim())
                        .spotRi(record.get("LI_NM").trim())
                        .spotBunji(record.get("LNBR_NO").trim())
                        .spotRoadAddr(record.get("RDNMADR_NM").trim())
                        .spotLon(record.get("LC_LO").trim())
                        .spotLat(record.get("LC_LA").trim())
                        .build();

                list.add(dto);
                spotNameSet.add(cleanedTitle);  // 등록
            }

            log.info("✅ MediaSpot CSV 파싱 완료, 전라남도 중복 제거 후 총 {}개", list.size());

        } catch (Exception e) {
            log.error("❌ MediaSpot CSV 파싱 중 오류 발생", e);
        }

        return list;
    }

    /**
     * 촬영지 제목 클린업
     * - 앞에 "영화" 제거
     * - 뒤에 "촬영지", "촬영장", "영화", "세트장" 제거
     * - "나주영상테마파크" 제거
     */
    private static String cleanTitle(String title) {
        String result = title;

        // 앞에 "영화" 제거
        if (result.startsWith("영화")) {
            result = result.substring(2).trim();
        }

        // 뒤에 특정 단어 제거
        String[] suffixes = {"촬영지", "촬영장", "영화", "세트장"};
        for (String suffix : suffixes) {
            if (result.endsWith(suffix)) {
                result = result.substring(0, result.length() - suffix.length()).trim();
            }
        }

        // "나주영상테마파크" 제거 (앞이나 중간에 있으면)
        result = result.replace("나주영상테마파크", "").trim();

        return result;
    }
}