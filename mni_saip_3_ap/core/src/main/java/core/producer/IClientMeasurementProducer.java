package core.producer;

public interface IClientMeasurementProducer {
  long EMIT_DELAY_MILLISECOND = 24;
  double createMeasurement();
}
