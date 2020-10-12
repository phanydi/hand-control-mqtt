package emulator.dao

import emulator.models.MqttDataModel
import org.springframework.stereotype.Service

@Service
class MqttDataModelServiceImpl(private val repository: MqttDataModelRepository) : MqttDataModelService {
    override fun getMqttDataModel(id: String): MqttDataModel = repository.findById(id).get()

    override fun getAllMqttDataModels(): List<MqttDataModel> = repository.findAll().toList()

    override fun createMqttDataModel(mqttDataModel: MqttDataModel): MqttDataModel =  repository.save(mqttDataModel)

    override fun deleteMqttDataModel(id: String) = repository.deleteById(id)

}
