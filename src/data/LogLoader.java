package data;

import structures.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LogLoader {
  private Map<String, ProcessInstance> processes = new HashMap<>();
  private Map<String, Actor> actors = new HashMap<>();
  private Set<String> actionTypes = new HashSet<>();

  private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

  public void loadLog(String filePath) throws IOException {
    List<String> lines = Files.readAllLines(Paths.get(filePath));
    lines.remove(0); // Remove header

    for (String line : lines) {
      String[] parts = line.split(",", -1);
      if (parts.length < 5) continue;

      String submissionId = parts[0].trim();
      LocalDateTime timestamp = LocalDateTime.parse(parts[1].trim(), formatter);
      String actorId = parts[2].trim();
      String action = parts[3].trim();
      String parameter = parts[4].trim();


      actionTypes.add(action);

      ProcessInstance process = processes.computeIfAbsent(submissionId, id -> new ProcessInstance(id));
      Actor actor = actors.computeIfAbsent(actorId, id -> new Actor(id));
      process.addActor(actor);

      TaskInstance task = new TaskInstance(UUID.randomUUID().toString(), action);
      task.actorsInvolved.add(actor);
      task.actual = new TimeInterval(timestamp, timestamp);
      task.status = TaskStatus.COMPLETED; // default, can be refined based on action

      if (!parameter.isEmpty()) {
        Actor target = actors.computeIfAbsent(parameter, id -> new Actor(id));
        task.actorsInvolved.add(target);
      }

      process.addTask(task);
    }
  }

  public Collection<ProcessInstance> getProcesses() {
    return processes.values();
  }

  public Set<String> getActionTypes() {
    return actionTypes;
  }

}
