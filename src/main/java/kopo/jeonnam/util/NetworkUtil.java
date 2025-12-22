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

    // 재시도 및 타임아웃 설정 상수
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000; // 1초
    private static final int CONNECT_TIMEOUT_MS = 5000; // 5초
    private static final int READ_TIMEOUT_MS = 10000; // 10초

    /**
     * GET 방식으로 OpenAPI 호출하기(전송할 헤더값이 존재하지 않는 경우 사용)
     */
    public static String get(String apiUrl) {
        log.info("[NetworkUtil] GET 호출 시작. URL={}", apiUrl);
        // 최종 예외를 던지기 위해 get(apiUrl, null)을 직접 호출
        return get(apiUrl, null);
    }

    /**
     * GET 방식으로 OpenAPI 호출하기 (재시도 로직 포함)
     */
    public static String get(String apiUrl, @Nullable Map<String, String> requestHeaders) {
        log.debug("[NetworkUtil] GET 호출(헤더 포함) 시작. URL={}", apiUrl);

        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;
        Exception lastException = null;

        while (attempt < MAX_RETRIES) {
            attempt++;
            HttpURLConnection con = null;
            try {
                con = connect(apiUrl);
                con.setRequestMethod("GET");

                if (requestHeaders != null) {
                    for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
                        con.setRequestProperty(header.getKey(), header.getValue());
                    }
                }

                int responseCode = con.getResponseCode();
                log.debug("[NetworkUtil] 시도 #{}: GET 응답 코드: {}", attempt, responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    log.info("[NetworkUtil] GET 호출 성공. (시도: {}) URL={}", attempt, apiUrl);
                    return readBody(con.getInputStream());
                } else {
                    // 4xx, 5xx 에러는 재시도 대상이 아님. 서버가 요청을 처리하고 응답한 것이기 때문.
                    log.warn("[NetworkUtil] GET 에러 응답. 재시도 안함. URL={}, code={}", apiUrl, responseCode);
                    return readBody(con.getErrorStream());
                }

            } catch (IOException e) { // 네트워크 레벨 오류 (타임아웃, 연결 실패 등)
                lastException = e;
                log.warn("[NetworkUtil] 시도 #{}/{} 실패: 네트워크 오류 발생. {}ms 후 재시도... (오류: {})",
                        attempt, MAX_RETRIES, backoffMs, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("재시도 대기 중 인터럽트 발생", ie);
                    }
                    backoffMs *= 2; // Exponential backoff
                }
            } finally {
                if (con != null) {
                    con.disconnect();
                    log.debug("[NetworkUtil] GET 연결 해제. (시도: {}) URL={}", attempt, apiUrl);
                }
            }
        }

        // 모든 재시도가 실패한 경우
        log.error("[NetworkUtil] {}번의 재시도 모두 실패. 최종 오류: {}", MAX_RETRIES, lastException.getMessage(), lastException);
        throw new RuntimeException("API 요청 실패 (재시도 모두 소진): " + apiUrl, lastException);
    }


    /**
     * POST 방식으로 OpenAPI 호출하기
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
     * OpenAPI URL에 접속하기 (타임아웃 설정 추가)
     */
    private static HttpURLConnection connect(String apiUrl) {
        log.debug("[NetworkUtil] connect 호출. URL={}", apiUrl);
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(CONNECT_TIMEOUT_MS);
            con.setReadTimeout(READ_TIMEOUT_MS);
            return con;
        } catch (MalformedURLException e) {
            log.error("[NetworkUtil] 잘못된 URL. URL={}, error={}", apiUrl, e.getMessage(), e);
            throw new RuntimeException("API URL이 잘못되었습니다. : " + apiUrl, e);
        } catch (IOException e) {
            log.error("[NetworkUtil] 연결 실패. URL={}, error={}", apiUrl, e.getMessage(), e);
            // 이 예외는 이제 get 메서드에서 처리하여 재시도함.
            throw new RuntimeException("연결이 실패했습니다. : " + apiUrl, e);
        }
    }

    /**
     * OpenAPI 호출 후, 받은 결과를 문자열로 변환하기
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