package kopo.jeonnam.controller.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/media-spots")
public class MediaSpotViewController {

    @Value("${naver.map.javascript.key}")
    private String naverMapClientKey;

    @GetMapping("/view")
    public String mediaSpotsView() {
        // resources/templates/api/mediaSpots.html 을 렌더링
        return "api/mediaSpots";
    }

    @GetMapping("/map")
    public String mediaSpotsMapView(Model model) {
        model.addAttribute("naverMapClientKey", naverMapClientKey);
        return "map/test-media-spot"; // → resources/templates/map/test-media-spot.html
    }
}
