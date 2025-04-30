package structures;

import java.util.ArrayList;
import java.util.List;

/**
 * Captures the state/phases a process goes through.
 */

public class StateInstance {
  String name; // e.g., "Reviewing"
  TimeInterval scheduled;
  TimeInterval actual;
  List<TaskInstance> tasks = new ArrayList<>();
  boolean isDelayed() {
    return actual.getEnd().isAfter(scheduled.getEnd());
  }
}
