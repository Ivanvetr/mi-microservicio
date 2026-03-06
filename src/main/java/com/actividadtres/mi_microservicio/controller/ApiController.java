package com.actividadtres.mi_microservicio.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.Map;


@RestController
public class ApiController {
    // Lee la variable de entorno ENTORNO (definida en Helm/K8S)
    @Value("${app.entorno:desarrollo}")
    private String entorno;

    // GET / — respuesta principal
    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
            "mensaje", "Microservicio Java funcionando en Kubernetes! Al fin :)",
            "version", "1.2000",
            "timestamp", LocalDateTime.now().toString(),
            "entorno", entorno
        );
    }

    // GET /info — información del servicio
    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
            "servicio", "mi-microservicio",
            "lenguaje", "Java 25 + Spring Boot 4",
            "endpoints", new String[]{"/", "/info", "/actuator/health"}
        );
    }

}
