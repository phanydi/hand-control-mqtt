package emulator.models

import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.index.Indexed

@RedisHash("MqttData")
class MqttDataModel(topicMqtt: String, dataMqtt: ByteArray) {
    @Indexed
    val topic : String = topicMqtt
    val data : ByteArray = dataMqtt
}