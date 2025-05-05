package data;

import structures.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LogLoader {
  private Map<String, ProcessInstance> processes = new HashMap<>();

  private Map<String, Actor> actors = new HashMap<>();
  private Set<String> actionTypes = new HashSet<>();
  private Map<String, String> actionToPhaseMap = new HashMap<>();
  private Map<String, String> actionToActorMap = new HashMap<>(), actionToTargetMap = new HashMap<>();
  private Set<String> actorRoles = new HashSet<>();
  private Map<String, Phase> phases = new LinkedHashMap<>();

  private DateTimeFormatter dateFormatter= DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC),
      dateTimeFormatter= DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

  public void loadPhaseTimetable(String phaseFilePath) {
    try (BufferedReader br = new BufferedReader(new FileReader(phaseFilePath))) {
      String line = br.readLine(); // skip header
      while ((line = br.readLine()) != null) {
        String[] tokens = line.split(",");
        String name = tokens[0].trim();
        LocalDate start = LocalDate.parse(tokens[1].trim(), dateFormatter);
        LocalDate end = LocalDate.parse(tokens[2].trim(), dateFormatter);
        phases.put(name, new Phase(name, start, end));
      }
    } catch (IOException ex) {
      System.out.println("Exception reading the list of phases: \n"+ex);
    }
  }

  public void loadActionPhaseMapping(String mappingFilePath) throws IOException {
    try (BufferedReader br = new BufferedReader(new FileReader(mappingFilePath))) {
      String line = br.readLine(); // skip header
      while ((line = br.readLine()) != null) {
        String[] tokens = line.split(",");
        String action = tokens[0].trim();
        String phase = tokens[1].trim();
        actionToPhaseMap.put(action, phase);
      }
    }
  }

  public void loadActionToRolesMapping(String mappingFilePath) throws IOException {
    try (BufferedReader br = new BufferedReader(new FileReader(mappingFilePath))) {
      String line = br.readLine(); // skip header
      while ((line = br.readLine()) != null) {
        String[] tokens = line.split(",");
        String action = tokens[0].trim();
        String role = tokens[1].trim();
        actionToActorMap.put(action, role);
        if (tokens.length>2 && tokens[2]!=null && tokens[2].trim().length()>0) {
          role = tokens[1].trim();
          actionToTargetMap.put(action, role);
        }
      }
    }
  }

  public void loadLog(String logFilePath) throws IOException {
    int nTaskInstances=0;

    try (BufferedReader br = new BufferedReader(new FileReader(logFilePath))) {
      String line = br.readLine(); // header
      while ((line = br.readLine()) != null) {
        String[] tokens = line.split(",", -1);
        if (tokens.length < 4) continue;

        String submissionId = tokens[0].trim();
        LocalDateTime timestamp = LocalDateTime.parse(tokens[1].trim(), dateTimeFormatter);
        String actorId = tokens[2].trim();
        String action = tokens[3].trim();

        actionTypes.add(action);

        ProcessInstance process = processes.computeIfAbsent(submissionId, ProcessInstance::new);
        process.type="SUBMISSION";
        Actor actor = actors.get(actorId);
        if (actor==null) {
          actor=new Actor(actorId);
          actors.put(actorId,actor);
          actor.role=actionToActorMap.get(action);
          if (actor.role==null) {
            System.out.println("NULL role of actor "+actorId+" in action "+action);
          }
          if (actor.role!=null && !actorRoles.contains(actor.role))
            actorRoles.add(actor.role);
        }
        process.addActor(actor);

        // Determine which phase this action belongs to
        String phaseName = actionToPhaseMap.get(action);
        if (phaseName == null) continue;

        Phase phase = phases.get(phaseName);
        if (phase == null) continue;

        // Get or create StateInstance in process
        StateInstance state = process.getState(phaseName);
        if (state == null) {
          state = new StateInstance(phaseName);
          state.setScheduled(new TimeInterval(phase.startDate.atStartOfDay(),
              phase.endDate.atTime(23, 59,59)));
          process.addState(state);
        }
        state.addActor(actor);

        // Add this action as a TaskInstance (minimal form)
        TaskInstance task = new TaskInstance();
        //task.id = UUID.randomUUID().toString();
        task.id=String.format("task%04d",++nTaskInstances);
        task.name = action;
        task.actorsInvolved = Collections.singletonList(actor);
        task.actual = new TimeInterval(timestamp, timestamp);
        state.addTask(task);
      }
    }
  }


  public Collection<ProcessInstance> getProcesses() {
    return processes.values();
  }

  public Set<String> getActionTypes() {
    return actionTypes;
  }


  public Map<String, Phase> getPhases() {
    return phases;
  }

  public Map<String, String> getActionToPhaseMap() {
    return actionToPhaseMap;
  }

  public Map<String, String> getActionToActorMap() {
    return actionToActorMap;
  }

  public Set<String> getActorRoles() {
    return actorRoles;
  }

  public Map<String, Actor> getActors() {
    return actors;
  }
}
