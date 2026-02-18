package structures;

import java.util.ArrayList;
import java.util.List;

public class ProcessThread {
  public String processID; // ID of the process that includes this thread
  public Actor actor;
  public String role; // Local role: e.g., "primary", "secondary", "external"
  public List<TaskInstance> tasks = new ArrayList<>();

  public ProcessThread(String processId, Actor actor, String role) {
    this.processID=processId;
    this.actor = actor;
    this.role = role;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public void addTask(TaskInstance task) {
    if (!tasks.contains(task)) {
      tasks.add(task);
    }
  }
}