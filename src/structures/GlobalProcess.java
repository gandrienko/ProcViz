package structures;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Global Process Model
 */

public class GlobalProcess {
  public Map<String, Phase> phases; // defines overall GProc schedule
  public Map<String, ActionType> actionTypes;
  public List<String> actorRoles;  //e.g., pc member, reviewer
  public Map<String, Actor> actors;  //key: actor id

  public Collection<ProcessInstance> processes;

  public Collection<ProcessInstance> getProcesses() {
    return processes;
  }

  public List<Actor> getActorsSorted(Collection<Actor> actors) {
    if (actors==null || actors.isEmpty())
      return null;
    if (actorRoles==null || actorRoles.size()<2) {
      ArrayList<Actor> sortedActors = new ArrayList<Actor>(actors);
      Collections.sort(sortedActors);
      return sortedActors;
    }
    ArrayList<Actor> sortedActors = new ArrayList<Actor>(actors.size());
    ArrayList<Actor> roleActors=new ArrayList<Actor>(actors.size());
    for (int rIdx=0; rIdx<actorRoles.size(); rIdx++) {
      roleActors.clear();
      String role=actorRoles.get(rIdx);
      for (Actor actor : actors) {
        if (role.equals(actor.generalRole) && !sortedActors.contains(actor))
          roleActors.add(actor);
      }
      Collections.sort(roleActors);
      sortedActors.addAll(roleActors);
    }
    return sortedActors;
  }

  public List<Phase> getListOfPhases() {
    if (phases==null || phases.isEmpty())
      return null;
    ArrayList<Phase> phaseList=new ArrayList<Phase>(phases.size());
    for (Phase phase:phases.values()) {
      boolean inserted=false;
      for (int i=0; i<phaseList.size() && !inserted; i++)
        if (phase.startDate.compareTo(phaseList.get(i).startDate)<0) {
          phaseList.add(i,phase);
          inserted=true;
        }
      if (!inserted)
        phaseList.add(phase);
    }
    return phaseList;
  }

  public Phase getPhaseForDate(LocalDate d) {
    if (phases==null || d==null)
      return null;
    for (Phase phase:phases.values())
      if (phase.contains(d))
        return phase;
    return null;
  }

  public LocalDate[] getPhaseDatesRange() {
    if (phases==null || phases.isEmpty())
      return null;
    LocalDate d1=null, d2=null;
    for (Phase phase:phases.values()) {
      if (d1==null || d1.isAfter(phase.startDate))
        d1=phase.startDate;
      if (d2==null || d2.isBefore(phase.endDate))
        d2=phase.endDate;
    }
    LocalDate range[]={d1,d2};
    return range;
  }

  public List<ProcessInstance> getProcessesByEndTimeDescending() {
    List<ProcessInstance> sortedList = new ArrayList<>(processes);

    sortedList.sort((p1, p2) -> {
      LocalDateTime end1 = (p1.getProcessLifetime() != null) ? p1.getProcessLifetime().end : null;
      LocalDateTime end2 = (p2.getProcessLifetime() != null) ? p2.getProcessLifetime().end : null;

      if (end1 == null && end2 == null) return 0;
      if (end1 == null) return 1;  // nulls last
      if (end2 == null) return -1; // nulls last

      return end2.compareTo(end1); // descending order
    });

    return sortedList;
  }

  public Map<String, Map<LocalDate, Integer>> getActionCountsByDay() {
    Map<String, Map<LocalDate, Integer>> counts = new TreeMap<>();
    for (ProcessInstance p : getProcesses()) {
      for (Map.Entry<String, ProcessThread> e:p.threads.entrySet()) {
        ProcessThread th=e.getValue();
        for (TaskInstance t : th.tasks) {
          LocalDate date = t.actual.start.toLocalDate();
          counts.computeIfAbsent(t.actionType, k -> new TreeMap<>())
              .merge(date, 1, Integer::sum);
        }
      }
    }
    return counts;
  }

  public Map<String, Map<LocalDate, List<TaskContext>>> getTasksByActionAndDay() {
    Map<String, Map<LocalDate, List<TaskContext>>> tasksMap = new TreeMap<>();
    for (ProcessInstance p : processes) {
      for (Map.Entry<String, ProcessThread> e:p.threads.entrySet()) {
        ProcessThread th=e.getValue();
        for (TaskInstance t : th.tasks) {
          LocalDate date = t.actual.start.toLocalDate();
          tasksMap.computeIfAbsent(t.actionType, k -> new TreeMap<>())
              .computeIfAbsent(date, k -> new ArrayList<>())
              .add(new TaskContext(t,p.id));
        }
      }
    }
    return tasksMap;
  }

  public static boolean isPaperChairRole(String role) {
    if (role==null)
      return false;
    return role.equalsIgnoreCase("paper chair");
  }

  public static boolean isPrimaryRole(String role) {
    if (role==null)
      return false;
    role=role.toLowerCase();
    return role.equals("primary") || role.equals("coordinator");
  }

  public static boolean isSecondaryRole(String role) {
    if (role==null)
      return false;
    role=role.toLowerCase();
    return role.equals("secondary") ||
        role.equals("committee member") || role.equals("pc member");
  }

  public static boolean isPCMemberRole(String role) {
    if (role==null)
      return false;
    role=role.toLowerCase();
    return role.equals("primary") || role.equals("secondary") || role.equals("coordinator") ||
        role.equals("committee member") || role.equals("pc member");
  }

  public static boolean isExternalRole(String role) {
    if (role==null)
      return false;
    role=role.toLowerCase();
    return role.contains("external") || role.contains("reviewer");
  }

}
