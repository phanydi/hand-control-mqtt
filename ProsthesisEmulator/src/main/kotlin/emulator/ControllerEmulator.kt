package emulator

import emulator.models.MqttDataModel
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.concurrent.timer

/**
 * Эмулятор бионического протеза руки. Поддерживает протокол протеза и выполняет отправку данных, эмулирующих протез.
 */
class ControllerEmulator {
    private val client: MqttClient = MqttClient();
    private val logger: Logger = LogManager.getLogger(ControllerEmulator::class.java.name)

    private var isConnected: Boolean = false

    /**
     * Запуск эмулятора. Выполняет подключение к Mqtt брокеру и начинает прием сообщений.
     */
    fun start() {
        // Подписка на прием данных
        client.getDataObservable().subscribeBy(onNext = {
            this.receiveDataHandler(it)
        }, onError = {
            logger.error(it.message)
        }, onComplete = {
            logger.info("DataObservable complete")
        })

        // Подписка на состояние подключения
        client.getIsConnectedObservable().subscribeBy(onNext = {
            isConnected = true
        }, onError = {
            logger.error(it.message)
        }, onComplete = {
            logger.info("IsConnectedObservable complete")
        })

        client.start()
    }

    /**
     * Эмуляция обработки входных данных на контроллере.
     */
    private fun receiveDataHandler(data: MqttDataModel) {
        logger.info("receiveDataHandler start on topic [${data.topic}] and data [${data.data.size} bytes]")
        logger.info("Data content string - ${data.data.toString(java.nio.charset.Charset.defaultCharset())}")
    }
}