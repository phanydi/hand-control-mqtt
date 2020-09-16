import io.netty.handler.codec.mqtt.MqttConnectReturnCode
import io.netty.handler.codec.mqtt.MqttQoS
import io.vertx.core.Vertx
import io.vertx.mqtt.MqttServer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.streams.toList

class MQTTServer : io.vertx.core.AbstractVerticle() {
    private val logger: Logger = LogManager.getLogger(MQTTServer::class.java.name)
    private var controllerLogin = "controller"
    private var controllerPassword = "controller"
    private val regexControllers = Regex("HC-.*")

    private val endpointsHashMap: HashMap<String, EndpointInformationModel> = HashMap()

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

            val isController = regexControllers.matches(endpoint.clientIdentifier())
            if (isController) {
                if (endpoint.auth().username != controllerLogin || endpoint.auth().password != controllerPassword) {
                    logger.info("MQTT client [${endpoint.clientIdentifier()}] is controller but username or password is incorrect.")
                    endpoint.reject(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD)
                    return@endpointHandler
                }
            }

            if (endpointsHashMap.containsKey(endpoint.clientIdentifier())) {
                endpoint.reject(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED)
                return@endpointHandler
            }

            endpointsHashMap[endpoint.clientIdentifier()] = EndpointInformationModel(endpoint)
            logger.info("Add [${endpoint.clientIdentifier()}] to HashMap [${endpointsHashMap.size}]")

            // accept connection from the remote client
            endpoint.accept(true)

            // handling requests for subscriptions
            endpoint.subscribeHandler { subscribe ->
                val grantedQosLevels = mutableListOf<MqttQoS>()
                for (s in subscribe.topicSubscriptions()) {
                    logger.info("Subscription for ${s.topicName()} with QoS ${s.qualityOfService()}")
                    grantedQosLevels.add(s.qualityOfService())
                    endpointsHashMap[endpoint.clientIdentifier()]!!.topics.add(s)
                    logger.info("Add new topic [${s.topicName()}] to [${endpoint.clientIdentifier()}]")
                }

                // ack the subscriptions request
                endpoint.subscribeAcknowledge(subscribe.messageId(), grantedQosLevels)

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
                    val removeTopic = endpointsHashMap[endpoint.clientIdentifier()]?.topics?.stream()?.filter { topic ->
                        topic.topicName() == t
                    }

                    endpointsHashMap[endpoint.clientIdentifier()]!!.topics.remove(removeTopic)
                    logger.info("Remove topic [${t}] from [${endpoint.clientIdentifier()}]")
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
                endpointsHashMap.remove(endpoint.clientIdentifier())
                logger.info("Remove [${endpoint.clientIdentifier()}] from HashMap [${endpointsHashMap.size}]")
            }

            // handling incoming published messages
            endpoint.publishHandler { message ->
                logger.info("Just received message on [${message.topicName()}] payload [${message.payload()}] with QoS [${message.qosLevel()}]")

                val endpointsWithSameTopics = endpointsHashMap.values.stream().filter { endpointFilter ->
                    endpointFilter.topics.stream().anyMatch { topic ->
                        topic.topicName() == message.topicName()
                    }
                }.toList()
                logger.info("Find ${endpointsWithSameTopics.count()} endpoints with same topic: $endpointsWithSameTopics")

                endpointsWithSameTopics.forEach { endpointSend ->
                    endpointSend.endpoint.publish(
                        message.topicName(),
                        message.payload(),
                        message.qosLevel(),
                        false,
                        false
                    )

                    if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
                        endpointSend.endpoint.publishAcknowledge(message.messageId())
                    } else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
                        endpointSend.endpoint.publishReceived(message.messageId())
                    }
                }
            }.publishReleaseHandler { messageId ->
                endpoint.publishComplete(messageId)
            }

        }.listen(1883, "0.0.0.0") { ar ->
            if (ar.succeeded()) {
                logger.info("MQTT server is listening on port ${mqttServer.actualPort()}")
            } else {
                logger.error("Error on starting the server ${ar.cause().printStackTrace()}")
            }
        }
    }
}