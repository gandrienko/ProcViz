package structures;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Global Process Model
 */

public class GlobalProcess {
  public Map<String, Phase> phases; // defines overall GProc schedule
  public Map<String, ActionType> actionTypes;
  public Set<String> actorRoles;  //e.g., pc member, reviewer
  public Map<String, Actor> actors;  //key: actor id

  public Collection<ProcessInstance> processes;

  public Set<Actor> getActorsByRole(String role) {
    Set<Actor> result = new HashSet<>();
    for (Actor actor : actors.values()) {
      if (role.equals(actor.getMainRole())) {
        result.add(actor);
      }
    }
    return result;
  }

  public List<Phase> getListOfPhases() {
    if (phases==null || phases.isEmpty())
      return null;
    ArrayList<Phase> phaseList=new ArrayList<Phase>(phases.size());
    for (Phase phase:phases.values()) {
      boolean inserted=false;
      for (int i=0; i<phaseList.size() && !inserted; i++)
        if (phase.startDate.compareTo(phaseList.get(i).startDate)<0) {
          phaseList.add(i,phase);
          inserted=true;
        }
      if (!inserted)
        phaseList.add(phase);
    }
    return phaseList;
  }

  public List<ProcessInstance> getProcessesByEndTimeDescending() {
    List<ProcessInstance> sortedList = new ArrayList<>(processes);

    sortedList.sort((p1, p2) -> {
      LocalDateTime end1 = (p1.getProcessLifetime() != null) ? p1.getProcessLifetime().end : null;
      LocalDateTime end2 = (p2.getProcessLifetime() != null) ? p2.getProcessLifetime().end : null;

      if (end1 == null && end2 == null) return 0;
      if (end1 == null) return 1;  // nulls last
      if (end2 == null) return -1; // nulls last

      return end2.compareTo(end1); // descending order
    });

    return sortedList;
  }
}
