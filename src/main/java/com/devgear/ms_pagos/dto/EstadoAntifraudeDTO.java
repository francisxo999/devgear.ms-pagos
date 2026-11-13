package com.devgear.ms_pagos.dto;

import java.time.Instant;

public record EstadoAntifraudeDTO(boolean bloqueado, Instant bloqueadoHasta, int intentosEnVentana) {}
