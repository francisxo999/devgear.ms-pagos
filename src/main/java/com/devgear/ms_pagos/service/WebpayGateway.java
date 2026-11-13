package com.devgear.ms_pagos.service;

import cl.transbank.common.IntegrationApiKeys;
import cl.transbank.webpay.webpayplus.WebpayPlus;
import com.devgear.ms_pagos.exception.WebpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Único punto de contacto con el SDK de Transbank (ambiente de integración). */
@Component
public class WebpayGateway {

    public record TransaccionCreada(String token, String url) {}

    public record TransaccionConfirmada(String status, String buyOrder, long monto, String sessionId) {}

    private final String commerceCode;

    public WebpayGateway(@Value("${devgear.webpay.commerce-code:597055555532}") String commerceCode) {
        this.commerceCode = commerceCode;
    }

    private WebpayPlus.Transaction transaccion() {
        return WebpayPlus.Transaction.buildForIntegration(commerceCode, IntegrationApiKeys.WEBPAY);
    }

    public TransaccionCreada crear(String buyOrder, String sessionId, long monto, String returnUrl) {
        try {
            var respuesta = transaccion().create(buyOrder, sessionId, (double) monto, returnUrl);
            return new TransaccionCreada(respuesta.getToken(), respuesta.getUrl());
        } catch (Exception e) {
            throw new WebpayException("No se pudo iniciar la transacción en Webpay", e);
        }
    }

    public TransaccionConfirmada confirmar(String token) {
        try {
            var r = transaccion().commit(token);
            // getAmount() puede ser float/double según la versión del SDK; se normaliza a long.
            long monto = new BigDecimal(String.valueOf(r.getAmount())).longValue();
            return new TransaccionConfirmada(r.getStatus(), r.getBuyOrder(), monto, r.getSessionId());
        } catch (Exception e) {
            throw new WebpayException("No se pudo confirmar la transacción en Webpay", e);
        }
    }
}
