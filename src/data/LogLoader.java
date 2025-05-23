package data;

import structures.*;

import java.io.BufferedReader;
import java.io.File;
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
  private List<String> actorRoles = new ArrayList<String>();
  private Map<String, Phase> phases = new LinkedHashMap<>();

  public static DateTimeFormatter dateFormatters[]= {
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC),
      DateTimeFormatter.ofPattern("dd-mm-yyyy").withZone(ZoneOffset.UTC)
  };

  public static DateTimeFormatter dtFormatters[]= {
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC),
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC),
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC),
      DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").withZone(ZoneOffset.UTC),
      DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").withZone(ZoneOffset.UTC)
  };

  public static DateTimeFormatter getSuitableFormatter(String dateStr) {
    if (dateStr==null || dateStr.trim().length()<1)
      return null;
    for (DateTimeFormatter f:dtFormatters)
      try {
        LocalDateTime dt=LocalDateTime.parse(dateStr,f);
        if (dt!=null)
          return f;
      } catch (Exception ex) {}
    for (DateTimeFormatter f:dateFormatters)
      try {
        LocalDate d=LocalDate.parse(dateStr,f);
        if (d!=null)
          return f;
      } catch (Exception ex) {}
    return null;
  }

  public boolean loadPhaseTimetable(String phaseFilePath) {
    try (BufferedReader br = new BufferedReader(new FileReader(phaseFilePath))) {
      String line = br.readLine(); // skip header
      DateTimeFormatter dateFormatter=null;
      while ((line = br.readLine()) != null) {
        String[] tokens = line.split(",");
        String name = tokens[0].trim();
        if (dateFormatter==null) {
          dateFormatter = getSuitableFormatter(tokens[1].trim());
          if (dateFormatter==null) {
            System.out.println("Unrecognized date format: "+tokens[1]);
            return false;
          }
        }
        LocalDate start = LocalDate.parse(tokens[1].trim(), dateFormatter);
        LocalDate end = LocalDate.parse(tokens[2].trim(), dateFormatter);
        phases.put(name, new Phase(name, start, end));
      }
    } catch (IOException ex) {
      System.out.println("Exception reading the list of phases: \n"+ex);
      return false;
    }
    if (phases.isEmpty()) {
      System.out.println("Failed to load information about phases and their times!");
      return false;
    }
    return true;
  }

  public void loadActionPhaseMapping(String mappingFilePath) throws IOException {
    try (BufferedReader br = new BufferedReader(new FileReader(mappingFilePath))) {
      String line = br.readLine(); // skip header
      while ((line = br.readLine()) != null) {
        String[] tokens = line.split(",");
        if (tokens.length<2) continue;
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
        if (tokens.length<2) continue;
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
              if (!aType.targetRole.equalsIgnoreCase("any") &&
                  !actorRoles.contains(aType.targetRole))
                actorRoles.add(aType.targetRole);
            }
          }
        }
      }
    }
  }

  public static List<String> readRolesIfExists(String logFilePath) {
    File logFile = new File(logFilePath);
    File parentDir = logFile.getParentFile();

    if (parentDir == null) {
      System.err.println("Unable to determine parent directory of log file.");
      return null;
    }

    File rolesFile = new File(parentDir, "roles.txt");

    if (rolesFile.exists() && rolesFile.isFile()) {
      List<String> roles = new ArrayList<>();
      try (BufferedReader reader = new BufferedReader(new FileReader(rolesFile))) {
        String line;
        while ((line = reader.readLine()) != null) {
          line = line.trim();
          if (!line.isEmpty()) {
            roles.add(line);
          }
        }
      } catch (IOException e) {}
      if (!roles.isEmpty())
        return roles;
    }

    return null;
  }

  public boolean loadLog(String logFilePath) {
    int nTaskInstances=0;

    actorRoles=readRolesIfExists(logFilePath);

    try (BufferedReader br = new BufferedReader(new FileReader(logFilePath))) {
      String line; // header

      String fieldNames[]=null;
      int processIdCN=-1, dateCN=-1, actionIdCN=-1, actionTypeCN=-1, actorIdCN=-1, actorRoleCN=-1,
          targetTypeCN=-1, targetIdCN=-1, outcomeCN=-1, statusCN=-1, paramCN=-1;
      DateTimeFormatter dateTimeFormatter=null;

      while ((line = br.readLine()) != null) {
        String[] fields = line.split(",", -1);
        if (fields==null || fields.length < 3) continue;
        if (fieldNames==null) {
          fieldNames=fields;
          for (int i=0; i<fieldNames.length; i++) {
            fieldNames[i] = fieldNames[i].toLowerCase();
            String s=fieldNames[i];
            if (s.contains("process") || s.contains("submission"))
              if (processIdCN<0) processIdCN=i; else;
            else
            if (s.contains("date") || s.contains("time"))
              if (dateCN<0) dateCN=i; else;
            else
            if (s.contains("action") || s.contains("event"))
              if (!s.contains("id"))
                if (actionTypeCN<0) actionTypeCN=i; else;
              else
                if (actionIdCN<0) actionIdCN=i; else;
            else
            if (s.contains("actor") || s.contains("person") || s.contains("anonymous") ||
                s.contains("initiator") || s.contains("initiating")) {
              if (s.contains("id"))
                if (actorIdCN<0) actorIdCN=i; else;
              else
              if (s.contains("role") || s.contains("type"))
                if (actorRoleCN<0) actorRoleCN=i; else;
              else
                if (actorIdCN<0) actorIdCN=i; else;
            }
            else
            if (s.contains("target") || s.contains("subject")) {
              if (s.contains("type") || s.contains("role"))
                if (targetTypeCN<0) targetTypeCN=i; else;
              else
                if (targetIdCN<0) targetIdCN=i; else;
            }
            else
            if (s.contains("status"))
              if (statusCN<0) statusCN=i; else;
            else
            if (s.contains("outcome"))
              if (outcomeCN<0) outcomeCN=i; else;
            else
            if (s.contains("param"))
              if (paramCN<0) paramCN=i; else;
          }
          if (processIdCN<0) {
            System.out.println("No field with the process identifiers detected!");
            return false;
          }
          if (actionTypeCN<0) {
            System.out.println("No field with the action/event types detected!");
            return false;
          }
          if (dateCN<0) {
            System.out.println("No field with the dates/times detected!");
            return false;
          }
          continue;
        }

        String processId = fields[processIdCN].trim();
        if (dateTimeFormatter==null) {
          dateTimeFormatter=getSuitableFormatter(fields[dateCN].trim());
          if (dateTimeFormatter==null) {
            System.out.println("Unrecognized date format: "+fields[dateCN]);
            return false;
          }
        }

        LocalDateTime timestamp = LocalDateTime.parse(fields[dateCN].trim(), dateTimeFormatter);
        String actorId = (actorIdCN>=0)?fields[actorIdCN].trim():"none";
        String action = fields[actionTypeCN].trim();
        String param=(paramCN>=0 && paramCN<fields.length)?fields[paramCN].trim():"";
        if (param.trim().length()<1)
          param=null;
  
        ActionType aType=actionTypes.get(action);
        if (aType==null) {
          aType=new ActionType();
          aType.typeName=action;
          actionTypes.put(action,aType);
        }
        String actorRole=aType.actorRole;
        if (actorRoleCN>=0 && actorRoleCN<fields.length) {
          actorRole=fields[actorRoleCN].trim();
          if (actorRole.length()<=0)
            actorRole=null;
          else
            if (aType.actorRole==null || aType.actorRole.equalsIgnoreCase("any") ||
            aType.actorRole.equalsIgnoreCase("none"))
              aType.actorRole=actorRole;
        }
        if (actorRole==null) {
          System.out.println("NULL role of actor "+actorId+" in action "+action);
        }
        else
          if (!actorRole.equalsIgnoreCase("any") &&
              !actorRoles.contains(actorRole))
            actorRoles.add(actorRole);

        ProcessInstance process = processes.computeIfAbsent(processId, ProcessInstance::new);
        process.type="SUBMISSION";
        Actor actor = actors.get(actorId);
        if (actor==null) {
          actor=new Actor(actorId);
          actor.start=actor.end=timestamp;
          actors.put(actorId,actor);
        }
        else {
          if (timestamp.isBefore(actor.start))
            actor.start=timestamp;
          if (timestamp.isAfter(actor.end))
            actor.end=timestamp;
        }
        if (actorRole!=null)
          actor.addRole(actorRole);
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
        task.id=(actionIdCN>=0)?fields[actionIdCN].trim():String.format("task%04d",++nTaskInstances);
        task.name = action;
        task.actorsInvolved = new ArrayList<Actor>(1);
        task.actorsInvolved.add(actor);
        task.actual = new TimeInterval(timestamp, timestamp);

        String targetActorId=null, targetActorRole=null;
        if (targetTypeCN>=0 && targetIdCN>=0 && targetTypeCN<fields.length && targetIdCN<fields.length) {
          String targetType=fields[targetTypeCN].trim();
          if (targetType.length()>0)
            if (actorRoles.contains(targetType))
              targetActorRole=targetType;
            else
            if (targetType.toLowerCase().contains("actor") || targetType.toLowerCase().contains("reviewer"))
              targetActorRole=targetType;
            if (targetActorRole!=null)
              targetActorId=fields[targetIdCN].trim();
        }

        if (statusCN>=0 && statusCN<fields.length) {
          task.status = fields[statusCN].trim();
          if (task.status.length()<1)
            task.status=null;
        }

        if (outcomeCN>=0 && outcomeCN<fields.length) {
          task.outcome = fields[outcomeCN].trim();
          if (task.outcome.length()<1)
            task.outcome=null;
        }

        if (param!=null && aType.targetType!=null)
          if (aType.targetType.equalsIgnoreCase("actor"))
            targetActorId=param;
          else
          if (aType.targetType.equalsIgnoreCase("status"))
            task.status=param;
          else
          if (aType.targetType.equalsIgnoreCase("outcome"))
            task.outcome=param;

        if (targetActorId!=null)  {
          Actor targetActor = actors.get(targetActorId);
          if (targetActor==null) {
            targetActor=new Actor(targetActorId);
            targetActor.start=targetActor.end=timestamp;
            actors.put(targetActorId,targetActor);
          }
          else {
            if (timestamp.isBefore(targetActor.start))
              targetActor.start=timestamp;
            if (timestamp.isAfter(targetActor.end))
              targetActor.end=timestamp;
          }
          if (targetActorRole==null)
            targetActorRole=aType.targetRole;
          else
            aType.targetRole=targetActorRole;
          if (targetActorRole!=null) {
            targetActor.addRole(targetActorRole);
            if (!targetActorRole.equalsIgnoreCase("any") &&
                !actorRoles.contains(targetActorRole))
              actorRoles.add(targetActorRole);
          }
          task.actorsInvolved.add(targetActor);
        }
            
        state.addTask(task);
      }
    } catch (Exception ex) {
      System.out.println(ex);
      return false;
    }
    if (processes.isEmpty()) {
      System.out.println("Failed to load any process!!!");
      return false;
    }
    return true;
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

  public List<String> getActorRoles() {
    return actorRoles;
  }

  public Map<String, Actor> getActors() {
    return actors;
  }
}
