package emulator.models

class MqttDataModel(topicMqtt: String, dataMqtt: ByteArray) {
    val topic : String = topicMqtt
    val data : ByteArray = dataMqtt

}