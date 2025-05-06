package viz;

import structures.*;

import java.awt.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ProcessTimelinePanel extends TimelinePanel{
  public static int markRadius=4, markDiameter=markRadius*2, actorLineSpacing=8;
  
  public GlobalProcess gProc=null;
  
  protected Map<Rectangle, ProcessInstance> processAreas = null;
  protected Map<String,Map<Rectangle,Actor>> processActorAreas=null;
  protected Map<String,Map<Rectangle,TaskInstance>> processTaskAreas=null;
  
  public ProcessTimelinePanel(GlobalProcess gProc) {
    super(gProc.getListOfPhases());
    this.gProc=gProc;
    setPreferredSize(new Dimension(1200, 100 + gProc.processes.size() * actorLineSpacing*10));
  }
  
  protected void paintComponent(Graphics g) {
    super.paintComponent(g); // Draw background phases
    Graphics2D g2d=(Graphics2D) g;
    int width = getWidth();
    
    Stroke stroke=g2d.getStroke();
    g2d.setStroke(new BasicStroke(2));
  
    if (processAreas==null)
      processAreas=new HashMap<Rectangle,ProcessInstance>(gProc.processes.size());
    else
      processAreas.clear();
    
    int y0=yTop+actorLineSpacing+fontHeight;
    for (ProcessInstance p: gProc.processes) {
      ArrayList<Actor> sortedActors = new ArrayList<Actor>(p.actors);
      sortedActors.sort(Comparator.comparing(a -> a.id));
  
      Map<String, Integer> actorLineOffset = new HashMap<>();
      for (int i = 0; i < sortedActors.size(); i++) {
        actorLineOffset.put(sortedActors.get(i).id, i);
      }

      if (processActorAreas==null)
        processActorAreas=new HashMap<String,Map<Rectangle,Actor>>(gProc.processes.size());
      Map<Rectangle,Actor> actorAreas=processActorAreas.get(p.id);
      if (actorAreas==null) {
        actorAreas=new HashMap<Rectangle,Actor>(sortedActors.size());
        processActorAreas.put(p.id,actorAreas);
      }
      else
        actorAreas.clear();

      if (processTaskAreas==null)
        processTaskAreas=new HashMap<String,Map<Rectangle,TaskInstance>>(gProc.processes.size());
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
            if (offsetIndex!=null && lastX[offsetIndex]<0) {
              lastX[offsetIndex]=x0;
              g.setColor(Color.lightGray);
            }
            else
              g.setColor(Color.gray);
            if (offsetIndex!=null)
              g.drawLine(lastX[offsetIndex],y,x1,y);
            g.setColor(sColor);
            g2d.drawOval(x1-markRadius,y-markRadius,markDiameter+x2-x1,markDiameter);
            g2d.fillOval(x1-markRadius,y-markRadius,markDiameter+x2-x1,markDiameter);
            if (offsetIndex!=null)
              lastX[offsetIndex]=x2;
            if (maxX<x2) maxX=x2;
            taskAreas.put(new Rectangle(x1-markRadius,y-markRadius,
                markDiameter+x2-x1,markDiameter),t);
          }
        }
      }
      Rectangle r=new Rectangle(x0,y0,maxX-x0,maxY-y0);
      processAreas.put(r,p);

      for (int i=0; i<sortedActors.size(); i++) {
        int y=  y0 + i * actorLineSpacing + actorLineSpacing / 2;
        actorAreas.put(new Rectangle(x0-markRadius, y-markRadius, lastX[i]-x0+markDiameter, markDiameter),
            sortedActors.get(i));
      }

      y0=maxY+actorLineSpacing*3;
    }
    g2d.setStroke(stroke);
    setPreferredSize(new Dimension(width, y0+2*actorLineSpacing));
    setSize(getPreferredSize());
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
                String text = String.format("<html>Process ID: <b>%s</b><br>Type: <b>%s</b>" +
                        "<br>Task ID: <b>%s</b><br>Name: <b>%s</b><br>"+
                    "Actual time: <b>%s -- %s</b>",p.id,p.type,
                    t.id,t.name,t.actual.start.format(formatter),t.actual.end.format(formatter));
                if (t.scheduled!=null)
                  text+=String.format("<br>Scheduled time: <b>%s -- %s</b>",
                      t.scheduled.start.format(formatter),t.scheduled.end.format(formatter));
                Actor primaryActor = (t.actorsInvolved==null || t.actorsInvolved.isEmpty())?null:t.actorsInvolved.get(0);
                if (primaryActor!=null) {
                  text += String.format("<br>Primary actor: <b>%s</b> in role <b>%s</b>",
                      primaryActor.id, primaryActor.role);
                  if (t.actorsInvolved.size()>1)
                    for (int i=1; i<t.actorsInvolved.size(); i++) {
                      Actor a=t.actorsInvolved.get(i);
                      text+=String.format("<br>Involved actor: <b>%s</b> in role <b>%s</b>",
                          a.id, a.role);
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
                String text=String.format("<html>Process ID: <b>%s</b><br>Type: <b>%s</b>" +
                    "<br>Involved actor: <b>%s</b> in role <b>%s</b></html>",p.id,p.type,a.id,a.role);
                return text;
              }

          String text = String.format("<html>Process ID: <b>%s</b><br>Type: <b>%s</b></html>",
              p.id,p.type);
          return text;
        }
      }
    return super.getToolTipText(pt);
  }
}
