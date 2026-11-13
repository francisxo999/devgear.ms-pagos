package com.devgear.ms_pagos.kafka;

import com.devgear.ms_pagos.service.AntifraudeService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Consumer group payment-velocity-group: detecta ráfagas de intentos de pago por cliente. */
@Component
public class IntentoPagoConsumer {

    private static final Logger log = LoggerFactory.getLogger(IntentoPagoConsumer.class);

    private final AntifraudeService antifraude;
    private final JsonMapper jsonMapper;

    public IntentoPagoConsumer(AntifraudeService antifraude, JsonMapper jsonMapper) {
        this.antifraude = antifraude;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(
            topics = "${devgear.kafka.topic-pagos:pagos.intentos}",
            concurrency = "${devgear.kafka.concurrency:3}")
    public void consumir(ConsumerRecord<String, String> registro) {
        IntentoPagoEvento evento;
        try {
            evento = jsonMapper.readValue(registro.value(), IntentoPagoEvento.class);
        } catch (Exception e) {
            log.warn("Mensaje inválido en {}-{}@{}: {}", registro.topic(), registro.partition(),
                    registro.offset(), e.getMessage());
            return;
        }
        if (evento.eventId() == null || evento.idCliente() == null || evento.timestamp() == null) {
            log.warn("Intento incompleto en {}-{}@{}, se omite", registro.topic(), registro.partition(), registro.offset());
            return;
        }
        antifraude.procesarIntento(evento, registro.partition(), registro.offset());
    }
}
