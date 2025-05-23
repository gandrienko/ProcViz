package viz;

import structures.*;

import java.awt.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

public class ProcessTimelinePanel extends TimelinePanel{
  public static int markRadius=4, markDiameter=markRadius*2, actorLineSpacing=8;
  public static int PROCESS_MODE=1, ACTOR_MODE=2;
  
  public GlobalProcess gProc=null;
  public int mode=ACTOR_MODE;
  
  protected Map<Rectangle, ProcessInstance> processAreas = null;
  protected Map<String,Map<Rectangle,Actor>> processActorAreas=null;
  protected Map<String,Map<Rectangle,TaskInstance>> processTaskAreas=null;
  protected Map<String, Color> actorRoleColors=null;
  
  public ProcessTimelinePanel(GlobalProcess gProc) {
    super(gProc.getListOfPhases());
    this.gProc=gProc;
    actorRoleColors=Utils.generateItemColors(gProc.actorRoles);
    setPreferredSize(new Dimension(1200, 100 + gProc.processes.size() * actorLineSpacing*10));
  }
  
  protected void paintComponent(Graphics g) {
    super.paintComponent(g); // Draw background phases
    if (mode==PROCESS_MODE)
      paintByProcesses(g);
    else
      if (mode==ACTOR_MODE)
        paintByActors(g);
  }

  public void paintByProcesses(Graphics g) {
    int width = getWidth();

    Graphics2D g2d=(Graphics2D) g;
    Stroke stroke=g2d.getStroke();
    g2d.setStroke(new BasicStroke(2));

    if (processAreas==null)
      processAreas=new HashMap<Rectangle,ProcessInstance>(gProc.processes.size());
    else
      processAreas.clear();
    if (processTaskAreas==null)
      processTaskAreas=new HashMap<String,Map<Rectangle,TaskInstance>>(gProc.processes.size());
    else
      processTaskAreas.clear();
    if (processActorAreas==null)
      processActorAreas=new HashMap<String,Map<Rectangle,Actor>>(gProc.processes.size());
    else
      processActorAreas.clear();

    int y0=yTop+2*actorLineSpacing;

    for (ProcessInstance p: gProc.processes) {
      ArrayList<Actor> sortedActors = new ArrayList<Actor>(p.actors);
      sortedActors.sort(Comparator.comparing(a -> a.id));

      Map<String, Integer> actorLineOffset = new HashMap<>();
      for (int i = 0; i < sortedActors.size(); i++) {
        actorLineOffset.put(sortedActors.get(i).id, i);
      }

      Map<Rectangle,Actor> actorAreas=processActorAreas.get(p.id);
      if (actorAreas==null) {
        actorAreas=new HashMap<Rectangle,Actor>(sortedActors.size());
        processActorAreas.put(p.id,actorAreas);
      }
      else
        actorAreas.clear();

      Map<Rectangle,TaskInstance> taskAreas=processTaskAreas.get(p.id);
      if (taskAreas==null) {
        taskAreas=new HashMap<Rectangle,TaskInstance>(100);
        processTaskAreas.put(p.id,taskAreas);
      }
      else
        taskAreas.clear();

      int x0=-1000;
      int lastX[]=new int[sortedActors.size()];
      for (int i=0; i<lastX.length; i++)
        lastX[i]=-1000;

      int maxY=y0, maxX=-1000;
      for (int i=0; i<p.states.size(); i++) {
        StateInstance s=p.states.get(i);
        Color sColor=phaseColors.get(s.name);
        if (sColor==null)
          sColor=Color.gray;
        sColor=Utils.toSaturated(sColor);

        for (int j=0; j<s.tasks.size(); j++) {
          TaskInstance t=s.tasks.get(j);
          if (t.actual!=null && t.actual.start!=null) {
            long secondsFromStart = ChronoUnit.SECONDS.between(minDate,t.actual.start);
            long secondsFromStart2 = ChronoUnit.SECONDS.between(minDate,t.actual.end);
            int x1 = (int) ((secondsFromStart * width) / (double) totalDuration);
            int x2 = (int) ((secondsFromStart2 * width) / (double) totalDuration);

            Actor primaryActor = (t.actorsInvolved==null || t.actorsInvolved.isEmpty())?null:t.actorsInvolved.get(0);
            Integer offsetIndex = (primaryActor==null)?null:actorLineOffset.get(primaryActor.id);
            int y= (offsetIndex == null)? y0:  y0 + offsetIndex * actorLineSpacing + actorLineSpacing / 2;
            if (maxY<y) maxY=y;

            if (x0<0) {
              x0=x1;
              g.setColor(Color.darkGray);
              g.drawLine(x0,y0+ actorLineSpacing / 2,x1,y0+ actorLineSpacing / 2+(sortedActors.size()-1)*actorLineSpacing);
            }
            if (offsetIndex!=null) {
              Color roleColor = (actorRoleColors == null) ? null : actorRoleColors.get(primaryActor.getMainRole());
              if (roleColor == null)
                roleColor = Color.gray;
              if (lastX[offsetIndex] < 0) {
                lastX[offsetIndex] = x0;
                g.setColor(new Color(roleColor.getRed(), roleColor.getGreen(), roleColor.getBlue(), 128));
              }
              else
                g.setColor(roleColor);
              g.drawLine(lastX[offsetIndex], y, x1, y);
            }
            g.setColor(sColor);
            g2d.drawOval(x1-markRadius,y-markRadius,markDiameter+x2-x1,markDiameter);
            g2d.fillOval(x1-markRadius,y-markRadius,markDiameter+x2-x1,markDiameter);
            if (offsetIndex!=null)
              lastX[offsetIndex]=x2;
            if (maxX<x2) maxX=x2;
            taskAreas.put(new Rectangle(x1-markRadius-3,y-markRadius-3,
                markDiameter+x2-x1+6,markDiameter+6),t);
          }
        }
      }
      Rectangle r=new Rectangle(x0-3,y0-3,maxX-x0+6,maxY-y0+6);
      processAreas.put(r,p);

      for (int i=0; i<sortedActors.size(); i++) {
        int y=  y0 + i * actorLineSpacing + actorLineSpacing / 2;
        actorAreas.put(new Rectangle(x0-markRadius-3, y-markRadius-3,
                lastX[i]-x0+markDiameter+6, markDiameter+6),
            sortedActors.get(i));
      }

      y0=maxY+actorLineSpacing*3;
    }
    g2d.setStroke(stroke);
    int height=y0+2*actorLineSpacing;
    setPreferredSize(new Dimension(getPreferredSize().width, height));
    setSize(width,height);
  }


  public void paintByActors(Graphics g) {
    int width = getWidth();

    Graphics2D g2d=(Graphics2D) g;
    Stroke stroke=g2d.getStroke();
    g2d.setStroke(new BasicStroke(2));

    if (processAreas!=null)
      processAreas.clear();
    ArrayList<Actor> sortedActors = new ArrayList<Actor>(gProc.actors.values());
    sortedActors.sort(Comparator.comparing(a -> a.id));

    if (processAreas==null)
      processAreas=new HashMap<Rectangle,ProcessInstance>(gProc.processes.size());
    else
      processAreas.clear();
    if (processTaskAreas==null)
      processTaskAreas=new HashMap<String,Map<Rectangle,TaskInstance>>(gProc.processes.size());
    else
      processTaskAreas.clear();
    if (processActorAreas==null)
      processActorAreas=new HashMap<String,Map<Rectangle,Actor>>(gProc.processes.size());
    else
      processActorAreas.clear();

    List<ProcessInstance> sortedProcesses=gProc.getProcessesByEndTimeDescending();

    int y0=yTop+2*actorLineSpacing;

    for (int aIdx=0; aIdx<sortedActors.size(); aIdx++) {
      Actor actor=sortedActors.get(aIdx);
      List<ProcessInstance> aProc=new ArrayList<ProcessInstance>(10);
      for (int pIdx=0; pIdx<sortedProcesses.size(); pIdx++)
        if (sortedProcesses.get(pIdx).actors.contains(actor))
          aProc.add(sortedProcesses.get(pIdx));
      if (aProc.isEmpty())
        continue;

      for (int pIdx=0; pIdx<aProc.size(); pIdx++) {
        ProcessInstance p=aProc.get(pIdx);
        TimeInterval pTime=p.getProcessLifetime();

        long secondsFromStart = ChronoUnit.SECONDS.between(minDate,pTime.start);
        long secondsFromStart2 = ChronoUnit.SECONDS.between(minDate,pTime.end);
        int xStart = (int) ((secondsFromStart * width) / (double) totalDuration);
        int xEnd = (int) ((secondsFromStart2 * width) / (double) totalDuration);

        Color actorColor = (actorRoleColors == null) ? null : actorRoleColors.get(actor.getMainRole());
        if (actorColor == null)
          actorColor = Color.gray;
        g.setColor(actorColor);
        g.drawLine(xStart,y0,xEnd,y0);

        Map<Rectangle,Actor> actorAreas=processActorAreas.get(p.id);
        if (actorAreas==null) {
          actorAreas=new HashMap<Rectangle,Actor>(10);
          processActorAreas.put(p.id,actorAreas);
        }
        else
          actorAreas.clear();

        Map<Rectangle,TaskInstance> taskAreas=processTaskAreas.get(p.id);
        if (taskAreas==null) {
          taskAreas=new HashMap<Rectangle,TaskInstance>(100);
          processTaskAreas.put(p.id,taskAreas);
        }
        else
          taskAreas.clear();

        int maxY=y0, maxX=-1000;
        for (int sIdx=0; sIdx<p.states.size(); sIdx++) {
          StateInstance s=p.states.get(sIdx);
          Color sColor=phaseColors.get(s.name);
          if (sColor==null)
            sColor=Color.gray;
          sColor=Utils.toSaturated(sColor);

          for (int tIdx=0; tIdx<s.tasks.size(); tIdx++) {
            TaskInstance t=s.tasks.get(tIdx);
            if (!t.actorsInvolved.contains(actor) || t.actual==null || t.actual.start==null)
              continue;

            secondsFromStart = ChronoUnit.SECONDS.between(minDate,t.actual.start);
            secondsFromStart2 = ChronoUnit.SECONDS.between(minDate,t.actual.end);
            int x1 = (int) ((secondsFromStart * width) / (double) totalDuration);
            int x2 = (int) ((secondsFromStart2 * width) / (double) totalDuration);

            g.setColor(sColor);
            g2d.drawOval(x1-markRadius,y0-markRadius,markDiameter+x2-x1,markDiameter);
            g2d.fillOval(x1-markRadius,y0-markRadius,markDiameter+x2-x1,markDiameter);
            taskAreas.put(new Rectangle(x1-markRadius-3,y0-markRadius-3,
                markDiameter+x2-x1+6,markDiameter+6),t);
          }
        }
        actorAreas.put(new Rectangle(xStart-markRadius-3, y0-markRadius-3,
                xEnd-xStart+markDiameter+6, markDiameter+6),actor);

        y0+=actorLineSpacing;
      }
      y0+=actorLineSpacing*2;
    }
    g2d.setStroke(stroke);
    int height=y0+2*actorLineSpacing;
    setPreferredSize(new Dimension(getPreferredSize().width, height));
    setSize(width,height);
  }

  
  public String getToolTipText(Point pt) {
    if (pt==null)
      return null;
    if (processAreas!=null)
      for (Map.Entry<Rectangle, ProcessInstance> entry : processAreas.entrySet()) {
        if (entry.getKey().contains(pt)) {
          ProcessInstance p = entry.getValue();
          //check if the mouse points at a specific task
          Map<Rectangle,TaskInstance> taskAreas=processTaskAreas.get(p.id);
          if (taskAreas!=null)
            for (Map.Entry<Rectangle,TaskInstance> taskEntry:taskAreas.entrySet())
              if (taskEntry.getKey().contains(pt)) {
                TaskInstance t=taskEntry.getValue();
                String text = String.format("<html>Process ID: <b>%s</b><br>Process type: <b>%s</b>" +
                        "<br>Task ID: <b>%s</b><br>Name: <b>%s</b><br>"+
                    "Actual time: <b>%s -- %s</b>",p.id,p.type,
                    t.id,t.name,t.actual.start.format(formatter),t.actual.end.format(formatter));
                if (t.scheduled!=null)
                  text+=String.format("<br>Scheduled time: <b>%s -- %s</b>",
                      t.scheduled.start.format(formatter),t.scheduled.end.format(formatter));
                Actor primaryActor = (t.actorsInvolved==null || t.actorsInvolved.isEmpty())?null:t.actorsInvolved.get(0);
                if (primaryActor!=null) {
                  text += String.format("<br>Primary actor: <b>%s</b> in role <b>%s</b>",
                      primaryActor.id, primaryActor.getMainRole());
                  if (t.actorsInvolved.size()>1)
                    for (int i=1; i<t.actorsInvolved.size(); i++) {
                      Actor a=t.actorsInvolved.get(i);
                      text+=String.format("<br>Involved actor: <b>%s</b> in role <b>%s</b>",
                          a.id, a.getMainRole());
                    }
                }
                if (t.status!=null)
                  text+="<br>Status: <b>"+t.status+"</b>";
                if (t.outcome!=null)
                  text+="<br>Outcome: <b>"+t.outcome+"</b>";
                text+="</html>";
                return text;
              }
          Map<Rectangle,Actor> actorAreas=processActorAreas.get(p.id);
          if (actorAreas!=null)
            for (Map.Entry<Rectangle,Actor> actorEntry:actorAreas.entrySet())
              if (actorEntry.getKey().contains(pt)) {
                Actor a=actorEntry.getValue();
                String text=String.format("<html>Process ID: <b>%s</b><br>Process type: <b>%s</b>" +
                    "<br>Involved actor: <b>%s</b> in role <b>%s</b></html>",p.id,p.type,a.id,a.getMainRole());
                return text;
              }

          String text = String.format("<html>Process ID: <b>%s</b><br>Process type: <b>%s</b></html>",
              p.id,p.type);
          return text;
        }
      }
    return super.getToolTipText(pt);
  }
}
