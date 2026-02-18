package structures;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents an IProc — one submission, PC member activity, etc.
 */

public class ProcessInstance {
  public String id;
  public String type; // e.g., submission, PC member, chair, etc.

  // Assignments of actor IDs to roles in this process instance
  public Map<String,String> roleAssignments=new LinkedHashMap<>();

  // Map of Actor ID to their specific ProcessThread
  public Map<String, ProcessThread> threads = new LinkedHashMap<>();

  public ProcessInstance(String id) {
    this.id = id;
  }

  /**
   * Finds an existing thread or creates a new one.
   * If role is provided, it updates the thread's role.
   */
  public ProcessThread getOrCreateThread(Actor actor, String role) {
    ProcessThread t = threads.get(actor.id);
    if (t == null) {
      t = new ProcessThread(id, actor, role);
      threads.put(actor.id, t);
    } else if (role != null) {
      t.role = role; // Update role if a specific assignment is found later
    }
    return t;
  }

  /**
   * Removes threads with no tasks and orders the remaining threads
   * by the start date of their first action.
   */
  public void cleanAndSortThreads() {
    // 1. Remove threads that have no tasks
    threads.entrySet().removeIf(entry -> entry.getValue().tasks.isEmpty());

    // 2. Convert map values to a list for sorting
    List<ProcessThread> sortedThreads = new ArrayList<>(threads.values());

    // 3. Sort by the start time of the first task in each thread
    sortedThreads.sort((t1, t2) -> {
      // Each thread's tasks should already be sorted chronologically
      // because they were added in log order, but we can be safe:
      java.time.LocalDateTime start1 = t1.tasks.get(0).actual.start;
      java.time.LocalDateTime start2 = t2.tasks.get(0).actual.start;
      return start1.compareTo(start2);
    });

    // 4. Re-populate the map with sorted entries
    threads.clear();
    for (ProcessThread pt : sortedThreads) {
      threads.put(pt.actor.id, pt);
    }
  }

  public TimeInterval getProcessLifetime() {
    LocalDateTime earliestStart = null;
    LocalDateTime latestEnd = null;

    for (Map.Entry<String, ProcessThread> e:threads.entrySet()) {
      TimeInterval tint=e.getValue().getLifetime();
      if (tint==null)
        continue;
      if (earliestStart == null || tint.start.isBefore(earliestStart)) {
        earliestStart = tint.start;
      }
      if (latestEnd == null || tint.end.isAfter(latestEnd)) {
        latestEnd = tint.end;
      }
    }

    if (earliestStart != null && latestEnd != null)
      return new TimeInterval(earliestStart,latestEnd);

    return null;
  }
}
