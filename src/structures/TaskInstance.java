package structures;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a unit of work, possibly shared across multiple processes.
 */

public class TaskInstance {
  public String id;
  public String actionType;
  public List<Actor> actorsInvolved = new ArrayList<>();;
  public String status=null;
  public String outcome=null;
  public TimeInterval scheduled;
  public TimeInterval actual;
}
