package viz;

import structures.TaskInstance;
import java.util.*;

public class SelectionManager {
  private Set<TaskInstance> selectedTasks = new HashSet<>();
  private Set<String> selectedProcessIds = new HashSet<>(); // New: Process selection
  private List<Runnable> taskListeners = new ArrayList<>(),
      processListeners = new ArrayList<>();

  public void addTaskListener(Runnable r) { taskListeners.add(r); }

  public void addProcessListener(Runnable r) { processListeners.add(r); }

  public void toggleTasks(List<TaskInstance> tasks) {
    if (tasks == null || tasks.isEmpty()) return;
    // If the first task in the set is already there, we assume we are unselecting the group
    if (selectedTasks.containsAll(tasks)) {
      selectedTasks.removeAll(tasks);
    } else {
      selectedTasks.addAll(tasks);
    }
    notifyListeners(true,false);
  }

  // New: Toggle process instances
  public void toggleProcesses(Collection<String> ids) {
    if (ids == null || ids.isEmpty()) return;
    if (selectedProcessIds.containsAll(ids)) {
      selectedProcessIds.removeAll(ids);
    } else {
      selectedProcessIds.addAll(ids);
    }
    notifyListeners(false,true);
  }

  public Set<TaskInstance> getSelectedTasks() {
    return selectedTasks;
  }

  public Set<String> getSelectedProcessIds() {
    return selectedProcessIds;
  }

  public void clearTaskSelection() {
    selectedTasks.clear();
    notifyListeners(true, false);
  }

  public void clearProcessSelection() {
    selectedProcessIds.clear();
    notifyListeners(false,true);
  }

  public boolean isTaskSelected(TaskInstance t) {
    return selectedTasks.contains(t);
  }

  public boolean hasTaskSelection() {
    return !selectedTasks.isEmpty();
  }

  public boolean isProcessSelested(String id) {
    return selectedProcessIds.contains(id);
  }

  private void notifyListeners(boolean aboutTasks, boolean aboutProcesses) {
    if (aboutTasks)
      for (Runnable r : taskListeners) r.run();
    if (aboutProcesses)
      for (Runnable r : processListeners)
        if (!aboutTasks || !taskListeners.contains(r))
          r.run();
  }
}
