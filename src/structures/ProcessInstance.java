package structures;

import java.util.*;

/**
 * Represents an IProc — one submission, PC member activity, etc.
 */

public class ProcessInstance {
  public String id;
  public ProcessType type; // e.g., SUBMISSION, PC_MEMBER, CHAIR, etc.
  public List<StateInstance> states = new ArrayList<>();
  public Map<String, Object> metadata; // optional extensibility
  public List<TaskInstance> tasks = new ArrayList<>();
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

  public void addTask(TaskInstance task) {
    tasks.add(task);
  }

  public void addActor(Actor actor) {
    actors.add(actor);
  }
}
