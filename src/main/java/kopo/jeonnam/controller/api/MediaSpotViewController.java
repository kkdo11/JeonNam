package kopo.jeonnam.controller.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/media-spots")
public class MediaSpotViewController {

    @GetMapping("/view")
    public String mediaSpotsView() {
        // resources/templates/api/mediaSpots.html 을 렌더링
        return "api/mediaSpots";
    }
}
