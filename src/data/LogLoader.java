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
  private Map<String, ActionType> actionTypes = new HashMap<>();
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
        ActionType aType=actionTypes.get(action);
        if (aType==null) {
          aType=new ActionType();
          aType.typeName=action;
          actionTypes.put(action,aType);
        }
        aType.phaseName=phase;
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
        ActionType aType=actionTypes.get(action);
        if (aType==null) {
          aType=new ActionType();
          aType.typeName=action;
          actionTypes.put(action,aType);
        }
        aType.actorRole=role;
        if (tokens.length>2 && tokens[2]!=null && tokens[2].trim().length()>0) {
          aType.targetType = tokens[2].trim();
          if (aType.targetType.equalsIgnoreCase("actor")) {
            if (tokens.length>3 && tokens[3]!=null && tokens[3].trim().length()>0) {
              aType.targetRole=tokens[3].trim();
              if (!actorRoles.contains(aType.targetRole))
                actorRoles.add(aType.targetRole);
            }
          }
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
        String param=(tokens.length>4)?tokens[4].trim():"";
        if (param.trim().length()<1)
          param=null;
  
        ActionType aType=actionTypes.get(action);
        if (aType==null) {
          aType=new ActionType();
          aType.typeName=action;
          actionTypes.put(action,aType);
        }

        ProcessInstance process = processes.computeIfAbsent(submissionId, ProcessInstance::new);
        process.type="SUBMISSION";
        Actor actor = actors.get(actorId);
        if (actor==null) {
          actor=new Actor(actorId);
          actors.put(actorId,actor);
          actor.role=aType.actorRole;
          if (actor.role==null) {
            System.out.println("NULL role of actor "+actorId+" in action "+action);
          }
          if (actor.role!=null && !actorRoles.contains(actor.role))
            actorRoles.add(actor.role);
        }
        process.addActor(actor);

        // Determine which phase this action belongs to
        if (aType.phaseName == null) continue;

        Phase phase = phases.get(aType.phaseName);
        if (phase == null) continue;

        // Get or create StateInstance in process
        StateInstance state = process.getState(aType.phaseName);
        if (state == null) {
          state = new StateInstance(aType.phaseName);
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
        task.actorsInvolved = new ArrayList<Actor>(1);
        task.actorsInvolved.add(actor);
        task.actual = new TimeInterval(timestamp, timestamp);
        if (param!=null && aType.targetType!=null)
          if (aType.targetType.equalsIgnoreCase("actor")) {
            Actor targetActor = actors.get(param);
            if (targetActor==null) {
              targetActor=new Actor(param);
              actors.put(param,targetActor);
              targetActor.role=aType.targetRole;
              if (targetActor.role!=null && !actorRoles.contains(targetActor.role))
                actorRoles.add(targetActor.role);
            }
            task.actorsInvolved.add(targetActor);
          }
          else
          if (aType.targetType.equalsIgnoreCase("status"))
            task.status=param;
          else
          if (aType.targetType.equalsIgnoreCase("outcome"))
            task.outcome=param;
            
        state.addTask(task);
      }
    }
  }


  public Collection<ProcessInstance> getProcesses() {
    return processes.values();
  }

  public Map<String, ActionType> getActionTypes() {
    return actionTypes;
  }


  public Map<String, Phase> getPhases() {
    return phases;
  }

  public Set<String> getActorRoles() {
    return actorRoles;
  }

  public Map<String, Actor> getActors() {
    return actors;
  }
}
