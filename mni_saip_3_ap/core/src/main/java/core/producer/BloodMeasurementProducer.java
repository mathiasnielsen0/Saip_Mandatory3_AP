package core.producer;

public class BloodMeasurementProducer implements IClientMeasurementProducer {
  public static final String SENSOR217_IDENTIFIER = "bloodmeasurement_producer";

  private double currentTemperature = 104.75;
  @Override
  public double createMeasurement() {
    double delta = Math.random() * 2.0 - 1.0;
    currentTemperature += delta;
    return currentTemperature;
  }
}
