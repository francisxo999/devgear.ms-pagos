package com.devgear.ms_pagos.dto;

import java.util.Map;

public record MetricasPagosDTO(
        long intentosTotales,
        long intentosUltimoMinuto,
        long clientesBloqueados,
        Map<String, Long> pagosPorEstado
) {}
