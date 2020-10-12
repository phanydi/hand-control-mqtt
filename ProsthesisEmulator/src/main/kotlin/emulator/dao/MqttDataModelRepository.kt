package emulator.dao

import emulator.models.MqttDataModel
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface MqttDataModelRepository: CrudRepository <MqttDataModel, String>