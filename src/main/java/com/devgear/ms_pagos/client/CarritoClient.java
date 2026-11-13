package com.devgear.ms_pagos.client;

import com.devgear.ms_pagos.dto.ItemCarritoDTO;
import com.devgear.ms_pagos.exception.ServicioExternoException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class CarritoClient {

    private final RestClient restClient;

    public CarritoClient(RestClient carritoRestClient) {
        this.restClient = carritoRestClient;
    }

    /** Total a cobrar = suma de subtotales del carrito, calculado en el servidor (no se confía en el cliente). */
    public long totalCarrito(String bearerToken) {
        List<ItemCarritoDTO> items;
        try {
            items = restClient.get()
                    .uri("/api/carrito")
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ItemCarritoDTO>>() {});
        } catch (RestClientException ex) {
            throw new ServicioExternoException("No se pudo leer el carrito: " + ex.getMessage());
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Tu carrito está vacío");
        }
        BigDecimal total = items.stream()
                .map(ItemCarritoDTO::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.setScale(0, RoundingMode.HALF_UP).longValue();
    }
}
