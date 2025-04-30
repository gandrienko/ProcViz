package structures;

import java.util.List;

/**
 * Represents a unit of work, possibly shared across multiple processes.
 */

public class TaskInstance {
  String id;
  String name;
  List<Actor> actorsInvolved;
  TaskStatus status;
  TimeInterval scheduled;
  TimeInterval actual;
  boolean isParallel;
  boolean isFork;
  boolean isConfluence;
  List<Anomaly> anomalies;
}
