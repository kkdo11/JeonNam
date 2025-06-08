package kopo.jeonnam.controller.theme;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MapTestController {

    @Value("${naver.map.javascript.key}")
    private String naverMapClientKey;

    @GetMapping("/map/map-test")
    public String showMapTestPage(Model model) {
        model.addAttribute("naverMapClientKey", naverMapClientKey);
        return "map/map-test";
    }
}
