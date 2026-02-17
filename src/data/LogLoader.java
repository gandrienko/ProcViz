package data;

import structures.*;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

public class LogLoader {
  private Map<String, ProcessInstance> processes = new HashMap<>();

  private Map<String, Actor> actors = new LinkedHashMap<>();
  private Map<String, ActionType> actionTypes = new LinkedHashMap<>();
  private List<String> actorRoles = null;
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

  // Regex pattern: matches "assigns <id> as <role>,"
  private static final Pattern ASSIGN_PATTERN = Pattern.compile(
      "\\bassigns\\s+([a-fA-F0-9]+)\\s+as\\s+(external|secondary|primary),"
  );


  public static void transformCsv(String inputFilePath, String outputFilePath) {
    try (
        BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))
    ) {
      String line;
      while ((line = reader.readLine()) != null) {
        // Replace all matching patterns
        String updatedLine = ASSIGN_PATTERN.matcher(line).replaceAll(
            "assigns as $2,$1"
        );
        writer.write(updatedLine);
        writer.newLine();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
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
    actorRoles=readRolesIfExists(mappingFilePath);
    if (actorRoles==null)
      actorRoles = new ArrayList<String>();
    else
      Actor.rolePriorities=actorRoles;
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
    if (logFilePath==null)
      return null;
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

  public void loadActionEncodings(String encodingFilePath) {
    if (encodingFilePath==null)
      return;
    try (BufferedReader reader = new BufferedReader(new FileReader(encodingFilePath))) {
      String line = reader.readLine(); // skip header

      while ((line = reader.readLine()) != null) {
        String[] parts = line.split(",", 2);
        if (parts.length < 2) continue;

        String typeName = parts[0].trim();
        String code = parts[1].trim();

        if (typeName.isEmpty() || code.isEmpty()) continue;

        // Get or create ActionType
        ActionType action = actionTypes.getOrDefault(typeName, new ActionType());
        action.typeName = typeName;
        action.code = code;

        actionTypes.put(typeName, action); // store or update
      }

    } catch (IOException e) {
      System.err.println("Error reading action encoding file: " + e.getMessage());
    }
  }

  public boolean loadLog(String logFilePath) {
    int nTaskInstances=0;

    String line=null;

    try (BufferedReader br = new BufferedReader(new FileReader(logFilePath))) {
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
            if (s.contains("param"))
              if (paramCN<0) paramCN=i; else;
            else
            if (s.contains("action") || s.contains("event")) {
              if (!s.contains("id"))
                if (actionTypeCN<0) actionTypeCN=i;
                else ;
              else
                if (actionIdCN<0) actionIdCN=i;
                else ;
            }
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

        String timeStr=fields[dateCN].trim();
        if (timeStr.contains("T"))
          timeStr=timeStr.replace('T',' ');
        LocalDateTime timestamp = null;
        try {
          timestamp=LocalDateTime.parse(timeStr, dateTimeFormatter);
        } catch (Exception ex) {}
        if (timestamp==null)
          continue;
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
        Actor performer = actors.get(actorId);
        if (performer==null) {
          performer=new Actor(actorId);
          performer.start=performer.end=timestamp;
          actors.put(actorId,performer);
        }
        else {
          if (timestamp.isBefore(performer.start))
            performer.start=timestamp;
          if (timestamp.isAfter(performer.end))
            performer.end=timestamp;
        }
        if (actorRole!=null && !actorRole.equals(performer.generalRole))
          if (performer.generalRole==null) {
            //if (actorRole.equals("Paper Chair"))
              //System.out.println(line);
            performer.generalRole = actorRole;
          }
          else
            if (Actor.rolePriorities!=null && Actor.rolePriorities.contains(actorRole) &&
                (!Actor.rolePriorities.contains(performer.generalRole) ||
                    Actor.rolePriorities.indexOf(performer.generalRole)>Actor.rolePriorities.indexOf(actorRole))) {
              //if (actorRole.equals("Paper Chair"))
                //System.out.println(line);
              performer.generalRole = actorRole;
            }
        process.addActor(performer);
        if (process.roleAssignments.get(performer.id)==null)
          process.roleAssignments.put(performer.id,(actorRole!=null)?actorRole:performer.generalRole);

        ProcessThread thread=process.getOrCreateThread(performer,null);

        // Add this action as a TaskInstance (minimal form)
        TaskInstance task = new TaskInstance();
        task.id=(actionIdCN>=0)?fields[actionIdCN].trim():String.format("task%04d",++nTaskInstances);
        task.actionType = action;
        task.actorsInvolved = new ArrayList<Actor>(1);
        task.actorsInvolved.add(performer);
        task.actual = new TimeInterval(timestamp, timestamp);

        thread.addTask(task);

        String targetActorId=null, targetActorRole=null, tId2=null;
        boolean toAddNewThread=false;

        if (action.toLowerCase().contains("assign")){
          String roleInProcess=null;
          if (param!=null && !param.isEmpty()) {
            if (action.toLowerCase().contains(" as ")) {
              int idx=action.toLowerCase().indexOf(" as ");
              roleInProcess=action.substring(idx+4).trim();
              targetActorId=param;
              process.roleAssignments.put(targetActorId,roleInProcess);
            }
            else
              if (param.contains("(") && param.contains(")")) {
                int p1 = param.indexOf('(');
                int p2 = param.indexOf(')');
                if (p1 > 0 && p2 > p1) {
                  targetActorId = param.substring(0, p1).trim();
                  roleInProcess = param.substring(p1 + 1, p2).trim();
                  process.roleAssignments.put(targetActorId,roleInProcess);
                }
                if (p2+1<param.length()) {
                  String sub=param.substring(p2+1).trim();
                  if (sub.startsWith(";"))
                    sub=sub.substring(1).trim();
                  while ((p1 = sub.indexOf('('))>=0 && (p2 = sub.indexOf(')')) > p1) {
                    String aId=sub.substring(0,p1).trim(), aRole=sub.substring(p1+1,p2).trim();
                    process.roleAssignments.put(aId,aRole);
                    tId2=aId;
                    if (p2+1>=sub.length())
                      break;
                    sub=sub.substring(p2+1).trim();
                    if (sub.startsWith(";"))
                      sub=sub.substring(1).trim();
                  }
                }
              }
            toAddNewThread= targetActorId!=null && roleInProcess!=null;
          }
          else
            if (action.toLowerCase().contains("clear all")) {
              process.roleAssignments.clear();
            }
        }
        else
        if (action.toLowerCase().contains("changes role")) {
          if (param!=null && !param.isEmpty() && param.contains(" to ") && !process.roleAssignments.isEmpty()) {
            int p=param.indexOf(" to ");
            String role1=param.substring(0,p).trim(), role2=param.substring(p+4).trim();
            for (Map.Entry<String,String> e:process.roleAssignments.entrySet())
              if (e.getValue().equals(role1)) {
                e.setValue(role2);
                break;
              }
          }
        }
        else
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

        if (targetActorId==null && param!=null && aType.targetType!=null) {
          if (aType.targetType.equalsIgnoreCase("actor")) {
            if (targetActorId == null) targetActorId = param;
            else ;
          } else if (aType.targetType.equalsIgnoreCase("status"))
            task.status = param;
          else if (aType.targetType.equalsIgnoreCase("outcome"))
            task.outcome = param;
        }

        if (targetActorId!=null)
          for (int t=0; t<2; t++) {
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
            if (targetActorRole==null) {
              String role=process.roleAssignments.get(targetActorId);
              if (role!=null && (role.equalsIgnoreCase("primary") ||
                  role.equalsIgnoreCase("secondary")))
                targetActorRole=targetActor.generalRole="PC Member";
              if (targetActorRole==null)
                targetActorRole = aType.targetRole;
            }
            if (targetActorRole!=null && !targetActorRole.equals(targetActor.generalRole))
              if (!actorRoles.contains(targetActorRole))
                actorRoles.add(targetActorRole);
              if (targetActor.generalRole==null) {
                //if (targetActorRole.equals("Paper Chair"))
                  //System.out.println(line);
                targetActor.generalRole = targetActorRole;
              }
              else
              if (Actor.rolePriorities!=null && Actor.rolePriorities.contains(targetActorRole) &&
                  (!Actor.rolePriorities.contains(targetActor.generalRole) ||
                      Actor.rolePriorities.indexOf(targetActor.generalRole)>Actor.rolePriorities.indexOf(targetActorRole))) {
                //if (targetActorRole.equals("Paper Chair"))
                  //System.out.println(line);
                targetActor.generalRole = targetActorRole;
              }
            task.actorsInvolved.add(targetActor);
            if (toAddNewThread)
              process.getOrCreateThread(targetActor,null);
            if (tId2==null)
              break;
            if (t==0) {
              targetActorId=tId2;
              targetActorRole=null;
            }
          }
            
      }
    } catch (IOException ex) {
      System.out.println(ex);
      System.out.println(line);
      return false;
    }
    if (processes.isEmpty()) {
      System.out.println("Failed to load any process!!!");
      return false;
    }

    // Finalize all processes
    for (ProcessInstance pi : processes.values()) {
      pi.cleanAndSortThreads();
      if (!pi.roleAssignments.isEmpty())
        for (Map.Entry<String,String> e:pi.roleAssignments.entrySet()) {
          ProcessThread th=pi.threads.get(e.getKey());
          if (th!=null) {
            th.role = e.getValue();
            if (th.role!=null && !actorRoles.contains(th.role))
              actorRoles.add(th.role);
            Actor a=actors.get(th.actor.id);
            if (th.role==null)
              th.role=a.generalRole;
            if (a!=null)
              a.processRoleAssignments.put(pi.id,th.role);
          }
        }
    }
    for (Map.Entry<String, Actor> e:actors.entrySet()) {
      Actor a=e.getValue();
      if ((a.generalRole==null ||
          (!a.generalRole.equalsIgnoreCase("Paper Chair") &&
          !a.generalRole.equalsIgnoreCase("PC Member"))) &&
          (a.hasProcessRole("primary") || a.hasProcessRole("secondary")))
        a.generalRole="PC Member";
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
