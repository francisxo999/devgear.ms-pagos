package com.devgear.ms_pagos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrdenCreadaDTO(Long id, String estado, BigDecimal total) {}
