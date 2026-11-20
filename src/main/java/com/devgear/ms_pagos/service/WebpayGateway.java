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

    public record TransaccionConfirmada(String status, String buyOrder, long monto, String sessionId,
                                        String tipoPago, String ultimosDigitos) {}

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

    /** Traduce el código de Transbank a texto para el cliente (VD débito, VN crédito, VC cuotas, etc.). */
    public static String describirMedioDePago(String tipoPago, String ultimosDigitos) {
        String tipo = switch (tipoPago == null ? "" : tipoPago) {
            case "VD" -> "Tarjeta de débito";
            case "VN" -> "Tarjeta de crédito";
            case "VC", "SI", "S2", "NC" -> "Tarjeta de crédito en cuotas";
            case "VP" -> "Tarjeta prepago";
            default -> "Tarjeta";
        };
        String texto = "Webpay Plus · " + tipo;
        if (ultimosDigitos != null && !ultimosDigitos.isBlank()) {
            texto += " terminada en " + ultimosDigitos;
        }
        return texto;
    }

    public TransaccionConfirmada confirmar(String token) {
        try {
            var r = transaccion().commit(token);
            // getAmount() puede ser float/double según la versión del SDK; se normaliza a long.
            long monto = new BigDecimal(String.valueOf(r.getAmount())).longValue();
            String tipoPago = r.getPaymentTypeCode();
            String tarjeta = r.getCardDetail() == null ? null : r.getCardDetail().getCardNumber();
            String ultimos = tarjeta == null ? null : tarjeta.substring(Math.max(0, tarjeta.length() - 4));
            return new TransaccionConfirmada(r.getStatus(), r.getBuyOrder(), monto, r.getSessionId(), tipoPago, ultimos);
        } catch (Exception e) {
            throw new WebpayException("No se pudo confirmar la transacción en Webpay", e);
        }
    }
}
