package kopo.jeonnam.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

@Slf4j
public class NetworkUtil {

    /**
     * GET 방식으로 OpenAPI 호출하기(전송할 헤더값이 존재하지 않는 경우 사용)
     * 네트워크 통신의 시작과 종료, 예외 발생 시 로그를 남깁니다.
     *
     * @param apiUrl 호출할 OpenAPI URL 주소
     * @return API 응답 결과 문자열
     */
    public static String get(String apiUrl) {
        log.info("[NetworkUtil] GET 호출 시작. URL={}", apiUrl);
        try {
            String result = get(apiUrl, null);
            log.info("[NetworkUtil] GET 호출 성공. URL={}", apiUrl);
            return result;
        } catch (Exception e) {
            log.error("[NetworkUtil] GET 호출 실패. URL={}, error={}", apiUrl, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * GET 방식으로 OpenAPI 호출하기
     * 네트워크 통신의 시작과 종료, 예외 발생 시 로그를 남깁니다.
     *
     * @param apiUrl         호출할 OpenAPI URL 주소
     * @param requestHeaders 전송하고 싶은 해더 정보
     * @return API 응답 결과 문자열
     */
    public static String get(String apiUrl, @Nullable Map<String, String> requestHeaders) {
        log.debug("[NetworkUtil] GET 호출(헤더 포함) 시작. URL={}", apiUrl);
        HttpURLConnection con = connect(apiUrl);
        try {
            con.setRequestMethod("GET");
            if (requestHeaders != null) {
                for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
                    con.setRequestProperty(header.getKey(), header.getValue());
                }
            }
            int responseCode = con.getResponseCode();
            log.debug("[NetworkUtil] GET 응답 코드: {}", responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return readBody(con.getInputStream());
            } else {
                log.warn("[NetworkUtil] GET 에러 응답. URL={}, code={}", apiUrl, responseCode);
                return readBody(con.getErrorStream());
            }
        } catch (IOException e) {
            log.error("[NetworkUtil] GET 요청/응답 실패. URL={}, error={}", apiUrl, e.getMessage(), e);
            throw new RuntimeException("API 요청과 응답 실패", e);
        } finally {
            con.disconnect();
            log.debug("[NetworkUtil] GET 연결 해제. URL={}", apiUrl);
        }
    }

    /**
     * POST 방식으로 OpenAPI 호출하기
     * 네트워크 통신의 시작과 종료, 예외 발생 시 로그를 남깁니다.
     *
     * @param apiUrl         호출할 OpenAPI URL 주소
     * @param postParams     전송할 파라미터
     * @param requestHeaders 전송하고 싶은 해더 정보
     * @return API 응답 결과 문자열
     */
    public static String post(String apiUrl, @Nullable Map<String, String> requestHeaders, String postParams) {
        log.info("[NetworkUtil] POST 호출 시작. URL={}", apiUrl);
        HttpURLConnection con = connect(apiUrl);
        try {
            con.setRequestMethod("POST");
            for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }
            con.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                wr.write(postParams.getBytes());
                wr.flush();
            }
            int responseCode = con.getResponseCode();
            log.debug("[NetworkUtil] POST 응답 코드: {}", responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return readBody(con.getInputStream());
            } else {
                log.warn("[NetworkUtil] POST 에러 응답. URL={}, code={}", apiUrl, responseCode);
                return readBody(con.getErrorStream());
            }
        } catch (IOException e) {
            log.error("[NetworkUtil] POST 요청/응답 실패. URL={}, error={}", apiUrl, e.getMessage(), e);
            throw new RuntimeException("API 요청과 응답 실패", e);
        } finally {
            con.disconnect();
            log.debug("[NetworkUtil] POST 연결 해제. URL={}", apiUrl);
        }
    }

    /**
     * OpenAPI URL에 접속하기
     * 내부에서만 사용. URL 연결 시도와 예외 발생 시 로그를 남깁니다.
     *
     * @param apiUrl 호출할 OpenAPI URL 주소
     * @return HttpURLConnection 객체
     */
    private static HttpURLConnection connect(String apiUrl) {
        log.debug("[NetworkUtil] connect 호출. URL={}", apiUrl);
        try {
            URL url = new URL(apiUrl);
            return (HttpURLConnection) url.openConnection();
        } catch (MalformedURLException e) {
            log.error("[NetworkUtil] 잘못된 URL. URL={}, error={}", apiUrl, e.getMessage(), e);
            throw new RuntimeException("API URL이 잘못되었습니다. : " + apiUrl, e);
        } catch (IOException e) {
            log.error("[NetworkUtil] 연결 실패. URL={}, error={}", apiUrl, e.getMessage(), e);
            throw new RuntimeException("연결이 실패했습니다. : " + apiUrl, e);
        }
    }

    /**
     * OpenAPI 호출 후, 받은 결과를 문자열로 변환하기
     * 내부에서만 사용. 변환 시작/종료, 예외 발생 시 로그를 남깁니다.
     *
     * @param body 읽은 결과값
     * @return 변환된 문자열
     */
    private static String readBody(InputStream body) {
        log.debug("[NetworkUtil] readBody 호출");
        InputStreamReader streamReader = new InputStreamReader(body);
        try (BufferedReader lineReader = new BufferedReader(streamReader)) {
            StringBuilder responseBody = new StringBuilder();
            String line;
            while ((line = lineReader.readLine()) != null) {
                responseBody.append(line);
            }
            log.debug("[NetworkUtil] readBody 변환 완료");
            return responseBody.toString();
        } catch (IOException e) {
            log.error("[NetworkUtil] readBody 실패: {}", e.getMessage(), e);
            throw new RuntimeException("API 응답을 읽는데 실패했습니다.", e);
        }
    }


}