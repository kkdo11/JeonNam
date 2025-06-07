package kopo.jeonnam.util;

import kopo.jeonnam.dto.csv.ProductDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
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
}

//파싱 X
//package kopo.jeonnam.util;
//
//import kopo.jeonnam.dto.csv.ProductDTO;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.csv.CSVFormat;
//import org.apache.commons.csv.CSVRecord;
//
//import java.io.BufferedReader;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.nio.charset.Charset;
//import java.util.ArrayList;
//import java.util.List;
//
//@Slf4j
//public class CsvParserUtil {
//
//    public static List<ProductDTO> parseProducts(InputStream csvInputStream) {
//        List<ProductDTO> productList = new ArrayList<>();
//        try (BufferedReader reader = new BufferedReader(
//                new InputStreamReader(csvInputStream, Charset.forName("MS949")))) {
//
//            Iterable<CSVRecord> records = CSVFormat.DEFAULT
//                    .withFirstRecordAsHeader()
//                    .parse(reader);
//
//            for (CSVRecord record : records) {
//                ProductDTO dto = ProductDTO.builder()
//                        .proId(null)
//                        .proRegNo(record.get("등록번호").trim())
//                        .proName(record.get("등록명칭").trim())
//                        .proRegDate(record.get("등록일자").trim())
//                        .proArea(record.get("대상지역").trim())
//                        .proPlanQty(record.get("생산계획량(톤)").trim())
//                        .proCompany(record.get("업체명").trim())
//                        .proBaseDate(record.get("데이터기준일자").trim())
//                        .build();
//
//                productList.add(dto);
//            }
//
//            log.info("✅ CSV 파싱 완료, 총 {}개 아이템 읽음", productList.size());
//
//        } catch (Exception e) {
//            log.error("❌ CSV 파싱 중 오류 발생", e);
//        }
//
//        return productList;
//    }
//}
