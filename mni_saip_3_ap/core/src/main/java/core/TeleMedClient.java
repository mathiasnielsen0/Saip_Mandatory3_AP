package core;

import core.producer.IClientMeasurementProducer;
import core.producer.BloodMeasurementProducer;
import org.slf4j.Logger;

public abstract class TeleMedClient {
  public Logger logger;

  protected abstract Logger createLogger();

  protected abstract void initializeConnector();

  protected abstract void runTemperatureSampling(IClientMeasurementProducer ts) throws InterruptedException;

  public TeleMedClient() throws InterruptedException {
    initializeConnector();
    logger = createLogger();
    IClientMeasurementProducer ts = new BloodMeasurementProducer();
    runTemperatureSampling(ts);
  }


}
