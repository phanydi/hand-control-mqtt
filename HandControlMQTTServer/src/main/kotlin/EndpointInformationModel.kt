import io.vertx.mqtt.MqttEndpoint
import io.vertx.mqtt.MqttTopicSubscription

class EndpointInformationModel(mqttEndpoint : MqttEndpoint) {
    val endpoint : MqttEndpoint = mqttEndpoint
    val topics : MutableList<MqttTopicSubscription> = ArrayList()
}