package structures;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

  public TimeInterval getLifetime() {
    LocalDateTime earliestStart = null;
    LocalDateTime latestEnd = null;

    for (TaskInstance task : tasks) {
      if (task.actual != null) {
        LocalDateTime start = task.actual.start;
        LocalDateTime end = task.actual.end;

        if (start != null) {
          if (earliestStart == null || start.isBefore(earliestStart)) {
            earliestStart = start;
          }
        }

        if (end != null) {
          if (latestEnd == null || end.isAfter(latestEnd)) {
            latestEnd = end;
          }
        }
      }
    }

    if (earliestStart != null && latestEnd != null)
      return new TimeInterval(earliestStart,latestEnd);

    return null;
  }
}