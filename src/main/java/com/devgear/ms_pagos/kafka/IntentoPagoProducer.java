package com.devgear.ms_pagos.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class IntentoPagoProducer {

    private static final Logger log = LoggerFactory.getLogger(IntentoPagoProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final String topic;

    public IntentoPagoProducer(KafkaTemplate<String, String> kafkaTemplate, JsonMapper jsonMapper,
                               @Value("${devgear.kafka.topic-pagos:pagos.intentos}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
        this.topic = topic;
    }

    /**
     * Publica el intento y ESPERA la confirmación (acks=all: líder + réplicas en sincronía).
     * Así, cuando el cliente es redirigido a Webpay, el intento ya está guardado de forma durable.
     * Si Kafka no responde en 3 s se continúa igual: una caída del antifraude no debe impedir
     * vender (lección de la EV2). Devuelve true si el mensaje quedó confirmado.
     */
    public boolean publicar(String idCliente, Long monto, String idTransaccion) {
        try {
            IntentoPagoEvento evento = new IntentoPagoEvento(
                    UUID.randomUUID().toString(), idCliente, monto, idTransaccion, Instant.now().toString());
            String json = jsonMapper.writeValueAsString(evento);
            SendResult<String, String> res = kafkaTemplate.send(topic, idCliente, json).get(4, TimeUnit.SECONDS);
            log.info("Intento de pago de {} confirmado en {}-{}@{} (acks=all)", idCliente, topic,
                    res.getRecordMetadata().partition(), res.getRecordMetadata().offset());
            return true;
        } catch (Exception e) {
            log.warn("No se pudo registrar el intento de {} en {}: {}. Se continúa sin antifraude.",
                    idCliente, topic, e.getMessage());
            return false;
        }
    }
}
