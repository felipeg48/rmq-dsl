@file:Suppress("ktlint:standard:no-wildcard-imports")

package dev.felipeg48.rmq

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope

class RmqAdmin {
    var connection: Connection? = null
    var channel: Channel? = null
    var exchangeName: String? = null
    var exchangeType: String? = null
    var exchangeDurable: Boolean = true
    var exchangeDeclareIt: Boolean = false
    var queueName: String? = null
    var queueDurable: Boolean = false
    var queueExclusive: Boolean = false
    var queueAutoDelete: Boolean = false
    var queueArguments: Map<String, Any> = emptyMap()
    var queueDeclareIt: Boolean = false
    var bindingExchange: String? = null
    var bindingQueue: String? = null
    var bindingRoutingKey: String? = null

    fun connection(block: ConnectionConfig.() -> Unit) {
        val config = ConnectionConfig()
        config.block()
        val factory = ConnectionFactory()
        factory.host = config.host
        factory.port = config.port
        factory.username = config.username
        factory.password = config.password // Assuming you'll add a 'password' property to ConnectionConfig
        connection = factory.newConnection()
        channel = connection?.createChannel()
    }

    fun exchange(block: ExchangeConfig.() -> Unit) {
        val config = ExchangeConfig()
        config.block()
        exchangeName = config.name
        exchangeType = config.type
        exchangeDurable = config.durable
        exchangeDeclareIt = config.declareIt

        if (exchangeDeclareIt) {
            channel?.exchangeDeclare(exchangeName, exchangeType, exchangeDurable)
        }
    }

    fun queue(block: QueueConfig.() -> Unit) {
        val config = QueueConfig()
        config.block()
        queueName = config.name
        queueDurable = config.durable
        queueExclusive = config.exclusive
        queueAutoDelete = config.autoDelete
        queueArguments = config.arguments
        queueDeclareIt = config.declareIt

        if (queueDeclareIt) {
            channel?.queueDeclare(queueName, queueDurable, queueExclusive, queueAutoDelete, queueArguments)
        }
    }

    fun binding(block: BindingConfig.() -> Unit) {
        val config = BindingConfig()
        config.block()
        bindingExchange = config.exchange
        bindingQueue = config.queue
        bindingRoutingKey = config.routingKey
        channel?.queueBind(bindingQueue, bindingExchange, bindingRoutingKey)
    }

    fun declare() {
        channel?.apply {
            exchangeDeclare(exchangeName, exchangeType, true) // Declare the exchange
            queueDeclare(queueName, queueDurable, false, false, null) // Declare the queue
            queueBind(queueName, exchangeName, bindingRoutingKey) // Bind the queue to the exchange
        }
    }

    fun close() {
        channel?.close()
        connection?.close()
    }
}

// Configuration classes for DSL blocks
class ConnectionConfig {
    var host: String = "localhost"
    var port: Int = 5672
    var username: String = "guest"
    var password: String = "guest"
    // Add 'password' property here if needed
}

class ExchangeConfig {
    var name: String = ""
    var type: String = ""
    var durable: Boolean = false
    var declareIt: Boolean = false
}

class QueueConfig {
    var name: String = ""
    var durable: Boolean = false
    var exclusive: Boolean = false
    var autoDelete: Boolean = false
    var arguments: Map<String, Any> = emptyMap()
    var declareIt: Boolean = false
}

class BindingConfig {
    var exchange: String = ""
    var queue: String = ""
    var routingKey: String = ""
}

class RmqProducer {
    var exchange: String? = null
    var routingKey: String? = null
    var body: Any? = null
    var messageConverter: MessageConverter = object : DefaultMessageConverter() {}
    var messageProperties: AMQP.BasicProperties = AMQP.BasicProperties()

    fun message(block: MessageConfig.() -> Unit) {
        val config = MessageConfig()
        config.block()
        exchange = config.exchange
        routingKey = config.routingKey
        body = config.body
        messageProperties = config.messageProperties
    }

    fun messageConverter(block: () -> DefaultMessageConverter) {
        messageConverter = block()
    }

    operator fun invoke(admin: RmqAdmin) = Producer(admin, exchange, routingKey, body, messageConverter, messageProperties)
}

class Producer(
    private val admin: RmqAdmin,
    private val exchange: String?,
    private val routingKey: String?,
    private val body: Any?,
    private val messageConverter: MessageConverter = object : DefaultMessageConverter() {},
    private val messageProperties: AMQP.BasicProperties = AMQP.BasicProperties(),
) {
    fun send(times: IntRange = 1..1) {
        for (i in times) {
            admin.channel?.basicPublish(exchange, routingKey, messageProperties, messageConverter?.toMessage(body))
        }
    }
}

class MessageConfig {
    var exchange: String = ""
    var routingKey: String = ""
    var body: Any? = ""
    var messageProperties: AMQP.BasicProperties = AMQP.BasicProperties()

    fun messageProperties(block: AMQP.BasicProperties.Builder.() -> Unit) {
        val builder = AMQP.BasicProperties.Builder()
        builder.block()
        messageProperties = builder.build()
    }
}

class RmqConsumer {
    var queueName: String? = null
    var deliveryHandler: ((Any) -> Unit)? = null
    var messageConverter: MessageConverter = object : DefaultMessageConverter() {}

    fun queue(block: QueueConfig.() -> Unit) {
        val config = QueueConfig()
        config.block()
        queueName = config.name
    }

    fun messageConverter(block: () -> DefaultMessageConverter) {
        messageConverter = block()
    }

    fun delivery(handler: (Any) -> Unit) {
        deliveryHandler = handler
    }

    operator fun invoke(admin: RmqAdmin) = Consumer(admin, queueName, deliveryHandler, messageConverter)
}

class Consumer(
    private val admin: RmqAdmin,
    private val queueName: String?,
    private val deliveryHandler: ((Any) -> Unit)?,
    private val messageConverter: MessageConverter = object : DefaultMessageConverter() {},
) {
    fun consume() {
        val channel = admin.channel ?: throw IllegalStateException("Channel not initialized")

        channel?.basicConsume(
            queueName,
            true,
            object : DefaultConsumer(channel) {
                override inline fun handleDelivery(
                    consumerTag: String?,
                    envelope: Envelope?,
                    properties: AMQP.BasicProperties?,
                    body: ByteArray?,
                ) {
                    val message = messageConverter.fromMessage(body)
                    deliveryHandler?.invoke(message ?: "")
                }
            },
        )

//        // This is a blocking call that waits for the consumer to be cancelled
//        // admin.channel?.waitForConfirmsOrDie()
//        while (channel.isOpen) {
//            Thread.sleep(1000)
//        }
    }
}

// DSL entry points
fun rmqAdmin(block: RmqAdmin.() -> Unit): RmqAdmin {
    val admin = RmqAdmin()
    admin.block()
    return admin
}

fun rmqProducer(block: RmqProducer.() -> Unit): RmqProducer {
    val producer = RmqProducer()
    producer.block()
    return producer
}

fun rmqConsumer(block: RmqConsumer.() -> Unit): RmqConsumer {
    val consumer = RmqConsumer()
    consumer.block()
    return consumer
}

// Utilities
interface MessageConverter {
    @Throws(RuntimeException::class)
    fun toMessage(data: Any?): ByteArray?

    @Throws(RuntimeException::class)
    fun fromMessage(message: ByteArray?): Any?
}

abstract class DefaultMessageConverter : MessageConverter {
    override fun toMessage(data: Any?): ByteArray? = data.toString().toByteArray(Charsets.UTF_8)

    override fun fromMessage(message: ByteArray?): String = message?.toString(Charsets.UTF_8) ?: ""
}
