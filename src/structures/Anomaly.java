package structures;

import java.time.Duration;

/**
 * Encodes deviations or disruptions.
 */

public class Anomaly {
  AnomalyType type;
  String description;
  Duration severity; // Optional
}
