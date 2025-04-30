package structures;

import java.time.Duration;

/**
 * Encodes deviations or disruptions.
 */

public class Anomaly { // test
  public AnomalyType type;
  public String description;
  public Duration severity; // Optional
}
