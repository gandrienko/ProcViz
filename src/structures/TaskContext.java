package structures;

public class TaskContext {
  public TaskInstance task;
  public String processId;

  public TaskContext(TaskInstance task, String processId) {
    this.task=task; this.processId=processId;
  }
}
