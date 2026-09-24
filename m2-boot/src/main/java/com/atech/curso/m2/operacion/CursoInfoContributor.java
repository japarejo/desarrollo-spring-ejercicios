package com.atech.curso.m2.operacion;

import java.util.Map;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/** EJ 2.4 - Información adicional en /actuator/info. */
@Component
public class CursoInfoContributor implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("curso", Map.of("nombre", "Desarrollo de aplicaciones con Spring", "modulo", 2));
    }
}
