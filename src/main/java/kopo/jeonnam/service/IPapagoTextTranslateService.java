package kopo.jeonnam.service;

public interface IPapagoTextTranslateService {

    /**
     * 단일 텍스트를 번역합니다.
     *
     * @param sourceLang 원본 언어 (예: "ko")
     * @param targetLang 번역할 언어 (예: "en", "zh-CN", "ja")
     * @param text 번역할 텍스트
     * @return 번역된 결과 문자열
     * @throws Exception 번역 중 오류 발생 시
     */
    String translateText(String sourceLang, String targetLang, String text) throws Exception;
}
