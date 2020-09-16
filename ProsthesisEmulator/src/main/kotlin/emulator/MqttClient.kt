package emulator

import emulator.models.MqttDataModel
import io.netty.handler.codec.mqtt.MqttQoS
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.mqtt.MqttClient
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.lang.RuntimeException

class MqttClient : io.vertx.core.AbstractVerticle() {
    private companion object {
        const val BROKER_HOST = "localhost"
        const val BROKER_PORT = 1883
    }

    private val client: MqttClient = MqttClient.create(Vertx.vertx())
    private val logger: Logger = LogManager.getLogger(MqttClient::class.java.name)

    /**
     * Rx PublishSubject для подписки.
     */
    private val dataSubject: PublishSubject<MqttDataModel> = PublishSubject.create()

    fun getDataObservable() : Observable<MqttDataModel> {
        return dataSubject.share()
    }

    /**
     * Выполнить подключение к серверу и подписаться на события Mqtt.
     */
    override fun start() {
        // handler will be called when we have a message in topic we subscribing for
        client.publishHandler { publish ->

            logger.info("Received message on [${publish.topicName()}], payload [${publish.payload().bytes.size} bytes], QoS [${publish.qosLevel()}]")

            val mqttData = MqttDataModel(publish.topicName(), publish.payload().bytes)
            dataSubject.onNext(mqttData)
        }

        // handle response on subscribe request
        client.subscribeCompletionHandler { h ->
            logger.info("Receive SUBACK from server with granted QoS : ${h.grantedQoSLevels()}")
        }

        // handle response on unsubscribe request
        client.unsubscribeCompletionHandler {
            logger.info("Receive UNSUBACK from server")
        }

        // connect to a server
        client.connect(BROKER_PORT, BROKER_HOST) { ch ->
            if (ch.succeeded()) {
                logger.info("Connected to a server")
                client.subscribe("testtopic/kotlin", 2)
            } else {
                logger.error("Failed to connect to a server")
                logger.error(ch.cause())
            }
        }
    }

    /**
     * Отправить бинарный поток на topic
     */
    fun sendData(topic: String, data: ByteArray) {
        if (!client.isConnected) {
            throw RuntimeException("Client not connected")
        }

        val vertxData = Buffer.buffer(data)
        client.publish(topic, vertxData, MqttQoS.EXACTLY_ONCE, false, false) { s ->
            logger.info("Publish sent to a server")
        }
    }

    /**
     * Отправить текстовое сообщение на топик
     */
    fun sendMessage(topic: String, message: String) {
        if (!client.isConnected) {
            throw RuntimeException("Client not connected")
        }

        val vertxData = Buffer.buffer(message)

        client.publish(topic, vertxData, MqttQoS.EXACTLY_ONCE, false, false) { s ->
            println("Publish sent to a server")
        }
    }
}