package com.devgear.ms_pagos.client;

import com.devgear.ms_pagos.dto.OrdenCreadaDTO;
import com.devgear.ms_pagos.exception.ServicioExternoException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OrdenClient {

    private final RestClient restClient;

    public OrdenClient(RestClient ordenesRestClient) {
        this.restClient = ordenesRestClient;
    }

    /** Crea la orden a partir del carrito del usuario (mismo endpoint que ya usaba el frontend). */
    public OrdenCreadaDTO crearOrden(String bearerToken, String metodoPago) {
        try {
            return restClient.post()
                    .uri("/api/ordenes")
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(java.util.Map.of("metodoPago", metodoPago == null ? "" : metodoPago))
                    .retrieve()
                    .body(OrdenCreadaDTO.class);
        } catch (RestClientException ex) {
            throw new ServicioExternoException("No se pudo crear la orden: " + ex.getMessage());
        }
    }
}
