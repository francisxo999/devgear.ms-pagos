package com.devgear.ms_pagos.dto;

import java.time.Instant;

public record BloqueoDTO(String idCliente, Instant bloqueadoHasta, int intentosEnVentana) {}
