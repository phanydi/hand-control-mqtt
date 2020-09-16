package emulator

import emulator.models.MqttDataModel
import io.netty.handler.codec.mqtt.MqttQoS
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.mqtt.MqttClient
import io.vertx.mqtt.MqttClientOptions
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.*

class MqttClient : io.vertx.core.AbstractVerticle() {
    private companion object {
        const val BROKER_HOST = "localhost"
        const val BROKER_PORT = 1883
    }

    private val client: MqttClient = MqttClient.create(
        Vertx.vertx(),
        MqttClientOptions()
            .setUsername("controller")
            .setPassword("controller")
            .setClientId("HC-"+ UUID.randomUUID())
    )

    private val logger: Logger = LogManager.getLogger(MqttClient::class.java.name)

    /**
     * Rx PublishSubject для подписки.
     */
    private val dataSubject: PublishSubject<MqttDataModel> = PublishSubject.create()

    /**
     * Rx PublishSubject для состояния подключения.
     */
    private val isConnectedSubject: PublishSubject<Boolean> = PublishSubject.create()

    fun getDataObservable() : Observable<MqttDataModel> {
        return dataSubject.share()
    }

    fun getIsConnectedObservable() : Observable<Boolean> {
        return isConnectedSubject.share()
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
                client.subscribe("controllers", 2)
                isConnectedSubject.onNext(true)
            } else {
                logger.error("Failed to connect to a server")
                logger.error(ch.cause())
                isConnectedSubject.onError(ch.cause())
            }
        }
    }

    /**
     * Отправить бинарный поток на topic
     */
    fun sendData(topic: String, data: ByteArray) {
        val vertxData = Buffer.buffer(data)
        sendData(topic, vertxData)
    }

    /**
     * Отправить текстовое сообщение на топик
     */
    fun sendData(topic: String, message: String) {
        val vertxData = Buffer.buffer(message)
        sendData(topic, vertxData)
    }

    private fun sendData(topic: String, vertxData: Buffer) {
        if (!client.isConnected) {
            throw RuntimeException("Client not connected")
        }

        client.publish(topic, vertxData, MqttQoS.EXACTLY_ONCE, true, true) { s ->
            if (s.failed()) {
                logger.error("Publish sent error to a server on $topic. Exception ${s.cause()}")
            } else {
                logger.info("Publish sent ${vertxData.bytes.size} bytes to a server on $topic")
            }
        }
    }
}