* v1.0.0: Inicialización del microservicio de pagos: integración Webpay Plus (integración Transbank), monto calculado desde el carrito, orden creada solo tras pago aprobado, antifraude con Kafka (tópico pagos.intentos, grupo payment-velocity-group: 3 intentos en 10 s bloquean 30 min).
* v1.1.0: Al aprobar el pago se lee de Webpay el tipo de tarjeta y sus últimos 4 dígitos, se guarda como método de pago y se informa a ms-ordenes al crear la orden.

* v1.2.0: Se agregó la telemetría en tiempo real: cada intento de pago y cada bloqueo antifraude se envía a DynamoDB para el dashboard por WebSocket.
