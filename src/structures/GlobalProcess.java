package structures;

import java.util.*;

/**
 * Global Process Model
 */

public class GlobalProcess {
  public Map<String, Phase> phases; // defines overall GProc schedule
  public Set<String> actionTypes;
  public Set<String> actorRoles;  //e.g., pc member, reviewer
  public Map<String, String> actionToPhaseMap;  //key: action type; value: phase name
  public Map<String, String> actionToActorMap; //key: action type; value: actor role
  public Map<String, Actor> actors;  //key: actor id

  public Collection<ProcessInstance> processes;

  public Set<Actor> getActorsByRole(String role) {
    Set<Actor> result = new HashSet<>();
    for (Actor actor : actors.values()) {
      if (role.equals(actor.getRole())) {
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
}
