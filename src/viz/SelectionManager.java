package viz;

import structures.TaskInstance;
import java.util.*;

public class SelectionManager {
  private Set<TaskInstance> selectedTasks = new HashSet<>();
  private List<Runnable> listeners = new ArrayList<>();

  public void addListener(Runnable r) { listeners.add(r); }

  public void toggleTasks(List<TaskInstance> tasks) {
    if (tasks == null || tasks.isEmpty()) return;
    // If the first task in the set is already there, we assume we are unselecting the group
    if (selectedTasks.containsAll(tasks)) {
      selectedTasks.removeAll(tasks);
    } else {
      selectedTasks.addAll(tasks);
    }
    notifyListeners();
  }

  public Set<TaskInstance> getSelectedTasks() {
    return selectedTasks;
  }

  public void clearSelection() {
    selectedTasks.clear();
    notifyListeners();
  }

  public boolean isTaskSelected(TaskInstance t) {
    return selectedTasks.contains(t);
  }

  public boolean hasSelection() {
    return !selectedTasks.isEmpty();
  }

  private void notifyListeners() {
    for (Runnable r : listeners) r.run();
  }}
