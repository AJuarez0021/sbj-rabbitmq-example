# Spring Boot RabbitMQ Example

Proyecto de ejemplo que demuestra el uso de RabbitMQ con Spring Boot 4.x, incluyendo **Topic Exchange**, **Fanout Exchange** y **Message Deduplication** para garantizar idempotencia.

## Tecnologias

- Java 17
- Spring Boot 4.0.1
- Spring AMQP 4.x
- RabbitMQ
- H2 Database (para deduplicacion)
- Lombok
- Jackson 3.x

## Estructura del Proyecto

```
src/main/java/com/work/broker/
├── config/
│   ├── RabbitMQConfig.java           # Configuracion general (MessageConverter)
│   ├── TopicExchangeConfig.java      # Topic Exchange + queues + bindings
│   └── FanoutExchangeConfig.java     # Fanout Exchange + queues + bindings
├── model/
│   └── EventMessage.java             # DTO para mensajes
├── entity/
│   └── ProcessedMessage.java         # Entidad JPA para deduplicacion
├── repository/
│   └── ProcessedMessageRepository.java
├── service/
│   └── MessageDeduplicationService.java  # Logica de idempotencia
├── producer/
│   ├── TopicExchangeProducer.java
│   └── FanoutExchangeProducer.java
├── consumer/
│   ├── TopicExchangeConsumer.java
│   └── FanoutExchangeConsumer.java
└── controller/
    ├── TopicExchangeController.java
    ├── FanoutExchangeController.java
    └── DeduplicationController.java
```

## Conceptos de RabbitMQ

### Topic Exchange

Enruta mensajes basado en patrones de **routing key**:

| Patron | Descripcion | Ejemplo |
|--------|-------------|---------|
| `*` | Coincide con exactamente UNA palabra | `order.*` coincide con `order.created` |
| `#` | Coincide con cero o mas palabras | `order.#` coincide con `order.payment.completed` |

**Configuracion en este proyecto:**

```
Topic Exchange: topic.exchange
│
├── Queue: topic.queue.orders
│   └── Binding: "order.*"
│       Recibe: order.created, order.updated, order.deleted
│
├── Queue: topic.queue.errors
│   └── Binding: "*.error"
│       Recibe: system.error, payment.error, order.error
│
└── Queue: topic.queue.all
    └── Binding: "#"
        Recibe: TODOS los mensajes
```

### Fanout Exchange

Envia mensajes a **TODAS** las colas vinculadas. El routing key es ignorado.

```
Fanout Exchange: fanout.exchange
│
├── Queue: fanout.queue.notification1 (Email Service)
├── Queue: fanout.queue.notification2 (SMS Service)
└── Queue: fanout.queue.notification3 (Push Service)

Un mensaje enviado al exchange llega a las 3 colas simultaneamente.
```

## Message Deduplication (Idempotencia)

RabbitMQ garantiza **at-least-once delivery**, lo que significa que un mensaje puede ser entregado mas de una vez. Para evitar procesamiento duplicado, implementamos deduplicacion basada en ID.

### Flujo

```
┌──────────────┐     ┌─────────────────────┐     ┌──────────────┐
│   Mensaje    │────>│ tryProcess(id,queue)│────>│   Database   │
│   Llega      │     │                     │     │     (H2)     │
└──────────────┘     └─────────────────────┘     └──────────────┘
                              │
                    ┌─────────┴─────────┐
                    │                   │
              NO EXISTE           YA EXISTE
                    │                   │
                    v                   v
              ┌──────────┐        ┌──────────┐
              │ PROCESAR │        │ IGNORAR  │
              │ + GUARDAR│        │ DUPLICADO│
              └──────────┘        └──────────┘
```

### Uso en Consumer

```java
@RabbitListener(queues = "orders.queue")
public void handle(EventMessage message) {
    // Verificar si es duplicado
    if (!deduplicationService.tryProcess(message.getId(), "orders.queue", message.getType())) {
        log.warn("DUPLICATE ignored: {}", message.getId());
        return;
    }

    try {
        // Procesar mensaje
        processOrder(message);
    } catch (Exception e) {
        // Si falla, permitir reprocesamiento
        deduplicationService.allowReprocess(message.getId());
        throw e;
    }
}
```

## API Endpoints

### Topic Exchange

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | `/api/topic/send/{routingKey}` | Enviar con routing key personalizado |
| POST | `/api/topic/order/created` | Evento order.created |
| POST | `/api/topic/order/updated` | Evento order.updated |
| POST | `/api/topic/system/error` | Evento system.error |
| POST | `/api/topic/payment/error` | Evento payment.error |
| POST | `/api/topic/user/registered` | Evento user.registered |

### Fanout Exchange

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| POST | `/api/fanout/broadcast` | Broadcast a todos los suscriptores |
| POST | `/api/fanout/alert` | Alerta del sistema |
| POST | `/api/fanout/promo` | Mensaje promocional |

### Deduplication

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| GET | `/api/deduplication/stats` | Estadisticas por cola |
| GET | `/api/deduplication/messages` | Todos los mensajes procesados |
| GET | `/api/deduplication/messages/{queue}` | Mensajes por cola |
| GET | `/api/deduplication/check/{id}` | Verificar si ID es duplicado |
| DELETE | `/api/deduplication/messages/{id}` | Permitir reprocesar mensaje |
| DELETE | `/api/deduplication/cleanup?days=7` | Limpiar registros antiguos |

## Configuracion

### application.properties

```properties
# RabbitMQ
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# H2 Database (deduplicacion)
spring.datasource.url=jdbc:h2:mem:deduplication
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

## Ejecucion

### Prerrequisitos

1. **RabbitMQ** corriendo en `localhost:5672`

   ```bash
   # Con Docker
   docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:management
   ```

2. **Java 17+**

### Ejecutar la aplicacion

```bash
mvn spring-boot:run
```

### Probar los endpoints

```bash
# Topic Exchange - Enviar orden creada
curl -X POST http://localhost:8080/api/topic/order/created \
  -H "Content-Type: text/plain" \
  -d "Nueva orden #12345"

# Topic Exchange - Enviar error del sistema
curl -X POST http://localhost:8080/api/topic/system/error \
  -H "Content-Type: text/plain" \
  -d "Error de conexion a DB"

# Fanout Exchange - Broadcast
curl -X POST http://localhost:8080/api/fanout/broadcast \
  -H "Content-Type: text/plain" \
  -d "Mantenimiento programado"

# Ver estadisticas de deduplicacion
curl http://localhost:8080/api/deduplication/stats
```

## Consolas de Administracion

- **RabbitMQ Management**: http://localhost:15672 (guest/guest)
- **H2 Console**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:deduplication`
  - User: `sa`
  - Password: (vacio)

## Comportamiento de Routing

### Topic Exchange - Ejemplos

| Routing Key | ordersQueue (order.*) | errorsQueue (*.error) | allQueue (#) |
|-------------|:---------------------:|:---------------------:|:------------:|
| order.created | SI | NO | SI |
| order.updated | SI | NO | SI |
| system.error | NO | SI | SI |
| payment.error | NO | SI | SI |
| user.registered | NO | NO | SI |
| order.payment.completed | NO | NO | SI |

### Fanout Exchange

Todos los mensajes enviados al fanout exchange llegan a las 3 colas de notificacion, sin importar el routing key.
