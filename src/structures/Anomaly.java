package structures;

import java.time.Duration;

/**
 * Encodes deviations or disruptions.
 */

public class Anomaly { // test
  AnomalyType type;
  String description;
  Duration severity; // Optional
}
