import io.netty.handler.codec.mqtt.MqttQoS
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.mqtt.MqttServer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class MQTTServer : io.vertx.core.AbstractVerticle()  {
    private val logger: Logger = LogManager.getLogger(MQTTServer::class.java.name)

    override fun start() {

        val mqttServer = MqttServer.create(Vertx.vertx())

        mqttServer.endpointHandler { endpoint ->

            // shows main connect info
            logger.info("MQTT client [${endpoint.clientIdentifier()}] request to connect, clean session = ${endpoint.isCleanSession}")

            if (endpoint.auth() != null) {
                logger.info("[username = ${endpoint.auth().userName()}, password = ${endpoint.auth().password()}]")
            }
            if (endpoint.will() != null) {
                logger.info("Connected client without auth")
            }

            logger.info("[keep alive timeout = ${endpoint.keepAliveTimeSeconds()}]")

            // accept connection from the remote client
            endpoint.accept(false)

            // handling requests for subscriptions
            endpoint.subscribeHandler { subscribe ->
                val grantedQosLevels = mutableListOf<MqttQoS>()
                for (s in subscribe.topicSubscriptions()) {
                    logger.info("Subscription for ${s.topicName()} with QoS ${s.qualityOfService()}")
                    grantedQosLevels.add(s.qualityOfService())
                }
                // ack the subscriptions request
                endpoint.subscribeAcknowledge(subscribe.messageId(), grantedQosLevels)

                // just as example, publish a message on the first topic with requested QoS
                endpoint.publish(
                        subscribe.topicSubscriptions()[0].topicName(),
                        Buffer.buffer("Hello from the Vert.x MQTT server"),
                        subscribe.topicSubscriptions()[0].qualityOfService(),
                        false,
                        false
                )

                // specifying handlers for handling QoS 1 and 2
                endpoint.publishAcknowledgeHandler { messageId ->
                    logger.info("Received ack for message = $messageId")
                }.publishReceivedHandler { messageId ->
                    endpoint.publishRelease(messageId)
                }.publishCompletionHandler { messageId ->
                    logger.info("Received ack for message = $messageId")
                }
            }

            // handling requests for unsubscribes
            endpoint.unsubscribeHandler { unsubscribe ->
                for (t in unsubscribe.topics()) {
                    logger.info("Unsubscription for $t")
                }

                // ack the subscriptions request
                endpoint.unsubscribeAcknowledge(unsubscribe.messageId())
            }

            // handling ping from client
            endpoint.pingHandler {
                logger.info("Ping received from client ${endpoint.clientIdentifier()}")
            }

            // handling disconnect message
            endpoint.disconnectHandler {
                logger.info("Received disconnect from client ${endpoint.clientIdentifier()}")
            }

            // handling closing connection
            endpoint.closeHandler {
                logger.info("Connection closed ${endpoint.clientIdentifier()}")
            }

            // handling incoming published messages
            endpoint.publishHandler { message ->
                logger.info("Just received message on [${message.topicName()}] payload [${message.payload()}] with QoS [${message.qosLevel()}]")

                endpoint.publish(
                        message.topicName(),
                        Buffer.buffer(message.payload().toString() + " Server"),
                        message.qosLevel(),
                        false,
                        false
                )

                if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
                    endpoint.publishAcknowledge(message.messageId())
                } else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
                    endpoint.publishReceived(message.messageId())
                }
            }.publishReleaseHandler { messageId ->
                endpoint.publishComplete(messageId)
            }

        }.listen(1883, "0.0.0.0") { ar ->
            if (ar.succeeded()) {
                logger.info("MQTT server is listening on port ${mqttServer.actualPort()}")
            } else {
                System.err.println("Error on starting the server${ar.cause().printStackTrace()}")
            }
        }
    }
}