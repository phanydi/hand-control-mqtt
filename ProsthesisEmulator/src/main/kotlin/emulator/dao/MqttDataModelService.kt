package emulator.dao

import emulator.models.MqttDataModel

interface MqttDataModelService {
    fun getMqttDataModel(id: String): MqttDataModel
    
    fun getAllMqttDataModels(): List<MqttDataModel>

    fun createMqttDataModel(mqttDataModel: MqttDataModel): MqttDataModel

    fun deleteMqttDataModel(id: String)
}