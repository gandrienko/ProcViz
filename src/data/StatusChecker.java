package data;

import structures.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Specific for conference: checks how a submission process fulfils
 * the requirements of the phases
 */

public class StatusChecker {
  private GlobalProcess gProc=null;

  public StatusChecker(GlobalProcess gProc) {
    this.gProc=gProc;
  }

  /**
   * Goes through all process instances and records for each process instance when it completed different phases.
   * Returns true if successfully done.
   */
  public boolean determinePhaseCompletenessDates(){
    if (gProc==null || gProc.phases==null || gProc.phases.isEmpty() ||
        gProc.processes==null || gProc.processes.isEmpty() ||
        gProc.actors==null || gProc.actors.isEmpty())
      return false;
    int nDatesGot=0;
    for (ProcessInstance pi:gProc.processes) {
      if (pi.threads==null || pi.threads.isEmpty())
        continue;
      for (Phase phase:gProc.phases.values()) {
        LocalDate d=getPhaseCompletenessDate(phase,pi);
        if (d==null) continue;
        if (pi.phaseDone==null)
          pi.phaseDone=new LinkedHashMap<>(Math.round(gProc.phases.size()*1.5f));
        pi.phaseDone.put(phase.name,d);
      }
    }
    return nDatesGot>0;
  }

  /**
   * Checks the phase completeness status of the given process instance by days.
   * Returns a boolean array by days.
   */
  public LocalDate getPhaseCompletenessDate(Phase phase, ProcessInstance process) {
    if (gProc==null)
      return null;
    if (phase==null  || process==null)
      return null;
    if (phase.name.equalsIgnoreCase("Assignment to PC Reviewers")) {
      //checks if the process has 2 assignments of PC members made by a paper chair
      LocalDate d1=null, d2=null;
      for (Map.Entry<String, ProcessThread> e:process.threads.entrySet()) {
        Actor actor=gProc.actors.get(e.getKey());
        if (actor.generalRole!=null &&
            actor.generalRole.equalsIgnoreCase("Paper Chair")) {
          ProcessThread chairThread=e.getValue();
          //find assignment tasks
          for (TaskInstance task: chairThread.tasks)
            if (task.actionType.toLowerCase().contains("assign") && task.actorsInvolved.size()>1) {
              for (int aIdx=1; aIdx<task.actorsInvolved.size(); aIdx++) {
                Actor aAss=task.actorsInvolved.get(aIdx);
                if (process.roleAssignments.containsKey(aAss.id) &&
                    isPCMemberRole(process.roleAssignments.get(aAss.id))) {
                  //check if there is a thread of this actor
                  ProcessThread pcMemberThread=process.threads.get(aAss.id);
                  if (pcMemberThread!=null) {
                    LocalDate d=task.actual.getStart().toLocalDate();
                    task.isDelayed=d.isAfter(phase.endDate);
                    if (d1 == null)
                      d1 = d;
                    else
                    if (d.isBefore(d1)) {
                      d2=d1; d1=d;
                    }
                    else
                    if (d2==null || d.isBefore(d2))
                      d2=d;
                  }
                }
              }
            }
        }
      }
      return d2;
    }
    else
    if (phase.name.equalsIgnoreCase("Assignment to External Reviewers")) {
      //checks if the process has 2 assigned external reviewers who accepted the invitations
      LocalDate d1=null, d2=null;
      for (ProcessThread th:process.threads.values())
        if (isExternalRole(th.role)) {
          // find acceptance of the invitation
          for (TaskInstance rTask:th.tasks)
            if (rTask.actionType.toLowerCase().contains("accept")) {
              LocalDate d = rTask.actual.getStart().toLocalDate();
              rTask.isDelayed=d.isAfter(phase.endDate);
              if (d1 == null)
                d1 = d;
              else
              if (d.isBefore(d1)) {
                d2=d1; d1=d;
              }
              else
              if (d2==null || d.isBefore(d2))
                d2=d;
              break;
            }
        }
      for (Map.Entry<String, ProcessThread> e:process.threads.entrySet()) {
        Actor actor=gProc.actors.get(e.getKey());
        if ((actor.generalRole!=null && actor.generalRole.equalsIgnoreCase("Paper Chair")) ||
            isPCMemberRole(e.getValue().role)) {
          ProcessThread pcMemberThread=e.getValue();
          //find assignment tasks
          ArrayList<ProcessThread> revThreads=new ArrayList<ProcessThread>(10);
          for (TaskInstance task: pcMemberThread.tasks)
            if ((task.actionType.toLowerCase().contains("assign") ||
                task.actionType.toLowerCase().contains("emails invitation")) &&
                task.actorsInvolved.size()>1) {
              task.isDelayed=task.actual.start.toLocalDate().isAfter(phase.endDate);
            }
        }
      }
      return d2;
    }
    return null;
  }

  private boolean isPCMemberRole(String role) {
    if (role==null)
      return false;
    role=role.toLowerCase();
    return role.equals("primary") || role.equals("secondary") || role.equals("coordinator") ||
        role.equals("committee member") || role.equals("pc member");
  }

  private boolean isExternalRole(String role) {
    if (role==null)
      return false;
    role=role.toLowerCase();
    return role.contains("external") || role.contains("reviewer");
  }
}
