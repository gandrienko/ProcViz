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

    int y0=yTop+actorLineSpacing+fontHeight;
    for (ProcessInstance p: gProc.processes) {
      ArrayList<Actor> sortedActors = new ArrayList<Actor>(p.actors);
      sortedActors.sort(Comparator.comparing(a -> a.id));
  
      Map<String, Integer> actorLineOffset = new HashMap<>();
      for (int i = 0; i < sortedActors.size(); i++) {
        actorLineOffset.put(sortedActors.get(i).id, i);
      }

      int x0=-1000;
      int lastX[]=new int[sortedActors.size()];
      for (int i=0; i<lastX.length; i++)
        lastX[i]=-1000;
      
      int maxY=y0;
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
            if (lastX[offsetIndex]<0) {
              lastX[offsetIndex]=x0;
              g.setColor(Color.lightGray);
            }
            else
              g.setColor(Color.gray);
            g.drawLine(lastX[offsetIndex],y,x1,y);
            g.setColor(sColor);
            g2d.drawOval(x1-markRadius,y-markRadius,markDiameter+x2-x1,markDiameter);
            lastX[offsetIndex]=x2;
          }
        }
      }
      y0=maxY+actorLineSpacing*3;
    }
    g2d.setStroke(stroke);
    setPreferredSize(new Dimension(width, y0+2*actorLineSpacing));
    setSize(getPreferredSize());
  }

}
