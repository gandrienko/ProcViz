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

  public Set<Actor> getActorsByRole(String role) {
    Set<Actor> result = new HashSet<>();
    for (Actor actor : actors.values()) {
      if (role.equals(actor.getMainRole())) {
        result.add(actor);
      }
    }
    return result;
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
        if (role.equals(actor.getMainRole()) && !sortedActors.contains(actor))
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
      for (StateInstance s : p.states) {
        for (TaskInstance t : s.tasks) {
          LocalDate date = t.actual.start.toLocalDate();
          counts.computeIfAbsent(t.actionType, k -> new TreeMap<>())
              .merge(date, 1, Integer::sum);
        }
      }
    }
    return counts;
  }

  public Map<String, Map<LocalDate, List<TaskInstance>>> getTasksByActionAndDay() {
    Map<String, Map<LocalDate, List<TaskInstance>>> tasksMap = new TreeMap<>();
    for (ProcessInstance p : processes) {
      for (StateInstance s : p.states) {
        for (TaskInstance t : s.tasks) {
          LocalDate date = t.actual.start.toLocalDate();
          tasksMap.computeIfAbsent(t.actionType, k -> new TreeMap<>())
              .computeIfAbsent(date, k -> new ArrayList<>())
              .add(t);
        }
      }
    }
    return tasksMap;
  }}
