package dev.felipeg48.rmq

fun main(args: Array<String>) {
    val admin =
        rmqAdmin {
            connection {
                host = "localhost"
                port = 5672
            }
        }

    val myMessage = "Hello, World!"

    val producer =
        rmqProducer {
            message {
                body = myMessage
                routingKey = "queue"
            }
        }

    producer(admin).send()

    var count = 1
    val consumer =
        rmqConsumer {
            queueName = "queue"
            deliveryHandler = { message ->
                println("${count++} $message")
            }
        }

    consumer(admin).consume()
}
