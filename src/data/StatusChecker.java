package data;

import structures.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    LocalDate datesRange[]=gProc.getPhaseDatesRange();
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
        if (GlobalProcess.isPaperChairRole(actor.generalRole)) {
          ProcessThread chairThread=e.getValue();
          //find assignment tasks
          for (TaskInstance task: chairThread.tasks)
            if (task.actionType.toLowerCase().contains("assign") && task.actorsInvolved.size()>1) {
              for (int aIdx=1; aIdx<task.actorsInvolved.size(); aIdx++) {
                Actor aAss=task.actorsInvolved.get(aIdx);
                if (process.roleAssignments.containsKey(aAss.id) &&
                    GlobalProcess.isPCMemberRole(process.roleAssignments.get(aAss.id))) {
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
      return (d2!=null)?d2:process.getProcessLifetime().end.toLocalDate();
    }
    else
    if (phase.name.equalsIgnoreCase("Assignment to External Reviewers")) {
      //checks if the process has 2 assigned external reviewers who accepted the invitations
      LocalDate d1=null, d2=null;
      for (ProcessThread th:process.threads.values())
        if (GlobalProcess.isExternalRole(th.role) &&
            (th.tasks.size()>1 || !th.tasks.get(0).actionType.toLowerCase().contains("declines"))) {
          boolean hasAccepted=false;
          // find acceptance of the invitation
          for (TaskInstance rTask:th.tasks)
            if (rTask.actionType.toLowerCase().contains("accept")) {
              hasAccepted=true;
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
          if (!hasAccepted) {  //acceptance record may be missing in the log
            for (TaskInstance rTask:th.tasks)
              if (rTask.actionType.toLowerCase().contains("review")) {
                LocalDate d = rTask.actual.getStart().toLocalDate();
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
        }
      if (d1!=null && d2==null) { //some processes may have two secondaries instead of two externals
        ArrayList<ProcessThread> sThreads=new ArrayList<>(5);
        for (ProcessThread th:process.threads.values())
          if (GlobalProcess.isSecondaryRole(th.role) &&
              (th.tasks.size()>1 || !th.tasks.get(0).actionType.toLowerCase().contains("declines"))) {
            if (sThreads.isEmpty())
              sThreads.add(th);
            else {
              LocalDateTime dt=th.tasks.get(0).actual.start;
              int idx=0;
              for (int i=sThreads.size()-1; i>=0; i--)
                if (sThreads.get(i).tasks.get(0).actual.start.isBefore(dt))
                  idx=i+1;
              sThreads.add(idx,th);
            }
          }
        if (sThreads.size()>1) {
          ProcessThread th=sThreads.get(sThreads.size()-1);
          boolean hasAccepted=false;
          // find acceptance of the invitation
          for (TaskInstance rTask:th.tasks)
            if (rTask.actionType.toLowerCase().contains("accept")) {
              hasAccepted=true;
              LocalDate d = rTask.actual.getStart().toLocalDate();
              rTask.isDelayed=d.isAfter(phase.endDate);
              if (d2==null || d.isBefore(d2))
                d2=d;
              break;
            }
          if (!hasAccepted) {  //acceptance record may be missing in the log
            for (TaskInstance rTask:th.tasks)
              if (rTask.actionType.toLowerCase().contains("review")) {
                LocalDate d = rTask.actual.getStart().toLocalDate();
                if (d2==null || d.isBefore(d2))
                  d2=d;
                break;
              }
          }
          if (d2!=null && d2.isBefore(d1))
            d2=d1;
        }
      }
      for (Map.Entry<String, ProcessThread> e:process.threads.entrySet()) {
        Actor actor=gProc.actors.get(e.getKey());
        if (GlobalProcess.isPaperChairRole(actor.generalRole) ||
            GlobalProcess.isPCMemberRole(e.getValue().role)) {
          ProcessThread pcMemberThread=e.getValue();
          //find assignment tasks
          for (TaskInstance task: pcMemberThread.tasks)
            if ((task.actionType.toLowerCase().contains("assign") ||
                task.actionType.toLowerCase().contains("emails invitation")) &&
                task.actorsInvolved.size()>1) {
              task.isDelayed=task.actual.start.toLocalDate().isAfter(phase.endDate);
            }
        }
      }
      return (d2!=null)?d2:process.getProcessLifetime().end.toLocalDate();
    }
    else
    if (phase.name.equalsIgnoreCase("Reviewing")) {
      // find when at least 4 actors, including 2 PC members and 2 externals, provided their complete reviews
      LocalDate dLast=null;
      int nReviewsDone=0;
      for (ProcessThread th:process.threads.values())
        if (!GlobalProcess.isPaperChairRole(th.role)) {
          boolean completed=false;
          for (TaskInstance task: th.tasks)
            if (task.actionType.toLowerCase().endsWith("review")) {
              completed=task.status!=null && task.status.equalsIgnoreCase("complete");
              LocalDate d=task.actual.start.toLocalDate();
              task.isDelayed=d.isAfter(phase.endDate);
              if (completed) {
                if (dLast==null || dLast.isBefore(d))
                  dLast=d;
                break;
              }
            }
          if (completed) ++nReviewsDone;
        }
      // check all reviewing tasks for being delayed
      Phase decision1Phase=gProc.phases.get("Round 1 Decision"),
          revPhase=gProc.phases.get("Revision Submission"),
          finalPhase=gProc.phases.get("Final Decision");
      for (ProcessThread th:process.threads.values())
        if (!GlobalProcess.isPaperChairRole(th.role))
          for (TaskInstance task: th.tasks)
            if (task.actionType.toLowerCase().endsWith("review")) {
              LocalDate d=task.actual.start.toLocalDate();
              task.isDelayed=!d.isBefore(decision1Phase.startDate) &&
                  (d.isBefore(revPhase.startDate) || !d.isBefore(finalPhase.startDate));
            }

      return (nReviewsDone>=4)?dLast:process.getProcessLifetime().end.toLocalDate();
    }
    else
    if (phase.name.equalsIgnoreCase("Discussion")) {
      // find when at least 3 actors, including 2 PC members and at least 1 external,
      // added their comments to discussion
      LocalDate dLast=null, dSummary=null, dDecision=null;
      int nActorsInvolved=0;
      for (ProcessThread th:process.threads.values())
        if (!GlobalProcess.isPaperChairRole(th.role)) {
          boolean commented=false;
          for (TaskInstance task: th.tasks)
            if (task.actionType.toLowerCase().contains("comment")) {
              LocalDate d=task.actual.start.toLocalDate();
              task.isDelayed=d.isAfter(phase.endDate);
              if (!commented) {
                commented=true;
                if (dLast==null || dLast.isBefore(d))
                  dLast=d;
              }
            }
            else
              if (dSummary==null && commented && GlobalProcess.isPCMemberRole(th.role) &&
                  task.actionType.toLowerCase().endsWith("review") &&
                  task.actual.start.toLocalDate().isAfter(phase.endDate) &&
                  task.status!=null && task.status.equalsIgnoreCase("complete")) {
                dSummary = task.actual.start.toLocalDate();
                break;
              }
          if (commented)
            ++nActorsInvolved;
        }
        if (nActorsInvolved<3 && dSummary==null) {
          Phase decisionPhase=gProc.phases.get("Round 1 Decision");
          if (decisionPhase==null)
            decisionPhase=gProc.phases.get("Revision Submission");
          if (decisionPhase!=null) {
            for (ProcessThread th : process.threads.values())
              if (GlobalProcess.isPaperChairRole(th.role)) {
                for (TaskInstance task : th.tasks)
                  if (task.actionType.toLowerCase().contains("decision") &&
                      task.actual.start.toLocalDate().isBefore(decisionPhase.endDate)) {
                    if (dDecision==null || dDecision.isAfter(task.actual.start.toLocalDate()))
                      dDecision=task.actual.start.toLocalDate();
                    break;
                  }
              }
          }
        }
      return (nActorsInvolved>=3)?dLast:(dSummary!=null)?dSummary:
          (dDecision!=null)?dDecision:process.getProcessLifetime().end.toLocalDate();
    }
    else
    if (phase.name.equalsIgnoreCase("Review Summarization")) {
      LocalDate dSum=null;
      Phase decisionPhase=gProc.phases.get("Round 1 Decision");
      for (ProcessThread th:process.threads.values())
        if (GlobalProcess.isPCMemberRole(th.role)) {
          boolean discussed=false;
          for (TaskInstance task : th.tasks) {
            if (discussed && task.actionType.toLowerCase().endsWith("review") &&
                task.status != null && task.status.equalsIgnoreCase("complete") &&
                task.actual.start.toLocalDate().isBefore(decisionPhase.endDate)) {
              if (dSum == null || dSum.isBefore(task.actual.start.toLocalDate()))
                dSum = task.actual.start.toLocalDate();
              task.isDelayed = task.actual.start.toLocalDate().isAfter(phase.endDate);
            } else
              discussed = discussed || task.actionType.toLowerCase().contains("comment");
          }
        }
      if (dSum!=null)
        return dSum;
      for (ProcessThread th : process.threads.values())
        if (GlobalProcess.isPaperChairRole(th.role)) {
          for (TaskInstance task : th.tasks)
            if (task.actionType.toLowerCase().contains("decision") &&
                task.actual.start.toLocalDate().isBefore(decisionPhase.endDate)) {
              return task.actual.start.toLocalDate();
            }
        }
      return process.getProcessLifetime().end.toLocalDate();
    }
    else
    if (phase.name.equalsIgnoreCase("Round 1 Decision")) {
      Phase finalPhase=gProc.phases.get("Final Decision");
      LocalDate d=null;
      for (ProcessThread th:process.threads.values())
        if (GlobalProcess.isPaperChairRole(th.role)) {
          for (TaskInstance task : th.tasks)
            if (task.actionType.toLowerCase().contains("decision") &&
                task.actual.start.toLocalDate().isBefore(finalPhase.startDate)) {
              task.isDelayed=task.actual.start.toLocalDate().isAfter(phase.endDate);
              if (d==null || d.isAfter(task.actual.start.toLocalDate()) )
                d=task.actual.start.toLocalDate();
            }
        }
      if (d!=null)
        return d;
      return process.getProcessLifetime().end.toLocalDate();
    }
    else
    if (phase.name.equalsIgnoreCase("Recommendation Update")) {
      Phase revPhase=gProc.phases.get("Revision Submission");
      LocalDate d=null;
      for (ProcessThread th:process.threads.values())
        if (GlobalProcess.isPCMemberRole(th.role)) {
          for (TaskInstance task : th.tasks) {
            if (task.actionType.toLowerCase().endsWith("review") &&
                task.status != null && task.status.equalsIgnoreCase("complete") &&
                task.actual.start.toLocalDate().isAfter(revPhase.endDate)) {
              task.isDelayed = task.actual.start.toLocalDate().isAfter(phase.endDate);
              if (d==null || d.isAfter(task.actual.start.toLocalDate()))
                d=task.actual.start.toLocalDate();
            }
          }
        }
      if (d!=null)
        return d;
      return process.getProcessLifetime().end.toLocalDate();
    }
    else
    if (phase.name.equalsIgnoreCase("Final Decision")) {
      LocalDate d=null;
      for (ProcessThread th:process.threads.values())
        if (GlobalProcess.isPaperChairRole(th.role)) {
          for (TaskInstance task : th.tasks)
            if (task.actionType.toLowerCase().contains("decision") &&
                !task.actual.start.toLocalDate().isBefore(phase.startDate)) {
              task.isDelayed=task.actual.start.toLocalDate().isAfter(phase.endDate);
              if (d==null) d=task.actual.start.toLocalDate();
            }
        }
      if (d!=null)
        return d;
      return process.getProcessLifetime().end.toLocalDate();
    }
    return null;
  }
}
