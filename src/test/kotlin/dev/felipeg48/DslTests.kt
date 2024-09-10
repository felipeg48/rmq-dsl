package dev.felipeg48

import dev.felipeg48.rmq.MessageConverter
import dev.felipeg48.rmq.rmqAdmin
import dev.felipeg48.rmq.rmqConsumer
import dev.felipeg48.rmq.rmqProducer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlin.test.Test

class DslTests {
    val logger = LoggerFactory.getLogger(DslTests::class.java)

    @Test
    fun `should pass`() {
        val admin =
            rmqAdmin {
                connection {
                    host = "localhost"
                    port = 5672
                }
//                exchange {
//                    name = "exchange"
//                    type = "direct"
//                }
//                queue {
//                    name = "queue"
//                    durable = true
//                }
//                binding {
//                    exchange = "exchange"
//                    queue = "queue"
//                    routingKey = "key"
//                }
            }

        // admin.declare()

        val producer =
            rmqProducer {
                message {
                    // /body = Message("Hello, World!")
                    body = "Hello, World!"
                    routingKey = "queue"
                    // messageConverter = JsonMessageConverter()
                    messageProperties {
                        contentType("application/json")
                        headers(mapOf("header1" to "value1"))
                    }
                    // exchange = "exchange"
                    // routingKey = "key"
                }
            }

        producer(admin).send()

        val consumer =
            rmqConsumer {
                queueName = "queue"
                // messageConverter = JsonMessageConverter()
                deliveryHandler = { message ->
                    logger.info(message.toString())
                }
            }

        consumer(admin).consume()

        assert(true)
    }
}

@Serializable
data class Message(
    val text: String,
)

class JsonMessageConverter : MessageConverter {
    override fun toMessage(data: Any?): ByteArray? = Json.encodeToString(data as Message).toByteArray(Charsets.UTF_8)

    override fun fromMessage(message: ByteArray?): Any? =
        Json.decodeFromString(Message.serializer(), message?.toString(Charsets.UTF_8) ?: "")
}
