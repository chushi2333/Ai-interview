package com.chushi.template.controller;

import com.chushi.template.commons.vo.Response;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HealthController extends BaseController {
    @GetMapping("/api/health")
    public Response<Map<String, Object>> health() {
        return wrap(Map.of(
                "status", "ok",
                "time", LocalDateTime.now()
        ));
    }
}
