package structures;

import java.util.*;

/**
 * Represents an IProc — one submission, PC member activity, etc.
 */

public class ProcessInstance {
  public String id;
  public String type; // e.g., submission, PC member, chair, etc.
  public List<StateInstance> states = new ArrayList<>();
  public Set<Actor> actors = new HashSet<>();

  public ProcessInstance(String id) {
    this.id = id;
  }

  public List<StateInstance> getStates() {
    return states;
  }

  public void addState(StateInstance state) {
    this.states.add(state);
  }

  public StateInstance getState(String stateName) {
    for (StateInstance s : states) {
      if (s.getName().equals(stateName)) {
        return s;
      }
    }
    return null;
  }

  public void addActor(Actor actor) {
    if (actor==null || actor.id==null)
      return;
    for (Actor a:actors)
      if (a.id.equals(actor.id))
        return; //already there
    actors.add(actor);
  }
}
