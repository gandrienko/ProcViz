package structures;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Captures the state/phases a process goes through.
 */

public class StateInstance {
  public String name; // e.g., "Reviewing"
  public TimeInterval scheduled;
  public TimeInterval actual;
  public List<TaskInstance> tasks = new ArrayList<>();
  public Set<Actor> actors = new HashSet<>();

  public boolean isDelayed() {
    return actual.getEnd().isAfter(scheduled.getEnd());
  }

}
