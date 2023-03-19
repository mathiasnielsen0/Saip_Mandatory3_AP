package mq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import core.producer.IClientMeasurementProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import core.consumer.TeleMedServer;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/** The server responsible for monitoring a temperature sensor,
 * and publishing its read values to a specific exchange with
 * a specific topic (routing key in RabbitMQ terminology).
 */
public class TeleMedClient_MqProducer extends core.TeleMedClient {

  public static final String EXCHANGE_NAME = "measurements";
  public static final int NUMBER_OF_CLIENTS = 40;

  private ConnectionFactory factory;
  public static final String MEASUREMENT_ROUTING_KEY = "telemed.measurements";

  public static void main(String[] args) throws InterruptedException {
    System.out.println("Starting TeleMed Client...");
    System.out.println("Spinning up " + NUMBER_OF_CLIENTS + " clients");

    for (int i = 0; i < NUMBER_OF_CLIENTS; i++){
      new TeleMedClient_MqProducer();
    }
  }

  public TeleMedClient_MqProducer() throws InterruptedException {
    super();
  }

  @Override
  protected Logger createLogger() {
    return LoggerFactory.getLogger(TeleMedClient_MqProducer.class);
  }

  @Override
  protected void initializeConnector() {
    factory = new ConnectionFactory();
    factory.setHost("localhost");
  }

  @Override
  protected void runTemperatureSampling(IClientMeasurementProducer ts) throws InterruptedException {
    int count = 0;
    try (Connection connection = factory.newConnection();
         Channel channel = connection.createChannel()) {

      // Declare a durable (=survive mq restarts) topic exchange (
      channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
      // Set the message property to 'deliveryMode = 2' which means
      // messages are persisted
      AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder().deliveryMode(2).build();

      while (true) {
        String tempAsString = "" + ts.createMeasurement();
        logger.info("Sending measurement to TeleMed server - Value: {}", tempAsString);
        channel.basicPublish(EXCHANGE_NAME, MEASUREMENT_ROUTING_KEY, properties, tempAsString.getBytes("UTF-8"));
        Thread.currentThread().sleep(IClientMeasurementProducer.EMIT_DELAY_MILLISECOND);
      }
    } catch (TimeoutException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
