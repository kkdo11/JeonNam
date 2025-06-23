package kopo.jeonnam.service;

import java.util.List;
import java.util.Map;

public interface IPdfService {
    byte[] createPdf(Map<String, List<Map<String, String>>> schedule, String lang) throws Exception;
}
