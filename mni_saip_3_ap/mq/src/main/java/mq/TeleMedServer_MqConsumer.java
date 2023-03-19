package mq;

import com.rabbitmq.client.*;
import core.consumer.TeleMedServer;
import core.producer.IClientMeasurementProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class TeleMedServer_MqConsumer {
  private static final String QUEUE_NAME = "telemed-queue";
  private String bindingKey = "*.measurements"; // request all temperature readings from any source

  public static void main(String[] args) throws InterruptedException, IOException, TimeoutException {
    System.out.println("=== Starting TeleMed Server ===");
    new TeleMedServer_MqConsumer();
  }

  private final Logger logger;

  public TeleMedServer_MqConsumer() throws InterruptedException, IOException, TimeoutException {
    logger = LoggerFactory.getLogger(TeleMedServer_MqConsumer.class);
    GetMeasurement();
  }

  private void GetMeasurement() throws InterruptedException, IOException, TimeoutException {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    channel.exchangeDeclare(TeleMedClient_MqProducer.EXCHANGE_NAME, "topic", true);
    channel.queueDeclare(QUEUE_NAME, true, false, false, null);
    channel.queueBind(QUEUE_NAME, TeleMedClient_MqProducer.EXCHANGE_NAME, bindingKey);

    logger.info("Starting reading measurements...");

    while (true){
      GetResponse response = channel.basicGet(QUEUE_NAME, true);
      String tempAsString = new String(response.getBody(), "UTF-8");
      logger.info("Read measurement from TeleMed Client: {}", tempAsString);
      Thread.currentThread().sleep(TeleMedServer.READ_DELAY_MILLISECOND);
    }
  }
}
