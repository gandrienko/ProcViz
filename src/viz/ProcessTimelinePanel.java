package viz;

import structures.*;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

public class ProcessTimelinePanel extends TimelinePanel{
  public static int markRadius=4, markDiameter=markRadius*2, actorLineSpacing=8;
  public static int PROCESS_MODE=1, ACTOR_MODE=2;
  public static int SYMBOL_DOT=0, SYMBOL_CHAR=1;
  
  public GlobalProcess gProc=null;
  private SelectionManager selectionManager=null;
  public int mode=PROCESS_MODE;
  public int symbolMode=SYMBOL_CHAR;
  
  protected Map<Rectangle, ProcessInstance> processAreas = null;
  protected Map<String,Map<Rectangle,Actor>> processActorAreas=null;
  protected Map<Rectangle,Actor> actorAreas=null;
  protected Map<String,Map<Rectangle,TaskInstance>> processTaskAreas=null;
  protected Map<String, Color> actorRoleColors=null;
  protected Map<String, Color> actionTypeColors=null;

  // Define the dashed stroke for "inactive" periods
  private final float[] dashPattern = {3.0f, 2.0f};
  private final Stroke dashedStroke = new BasicStroke(1f, BasicStroke.CAP_BUTT,
      BasicStroke.JOIN_MITER, 5.0f, dashPattern, 0.0f);
  private final Stroke solidStroke = new BasicStroke(2f); // Standard thin line
  private final Stroke highlightedStroke = new BasicStroke(3f); // Bold for selection

  public ProcessTimelinePanel(GlobalProcess gProc, SelectionManager selectionManager) {
    super(gProc.getListOfPhases());
    this.gProc=gProc;
    this.selectionManager=selectionManager;
    actorRoleColors=Utils.generateItemColors(gProc.actorRoles);
    ArrayList<String> actionTypes=new ArrayList<String>(gProc.actionTypes.keySet());
    actionTypeColors=Utils.generateItemColors(actionTypes);
    setPreferredSize(new Dimension(1200, 100 + gProc.processes.size() * actorLineSpacing*10));
    if (selectionManager!=null)
      selectionManager.addListener(() -> {
        repaint();
      });
  }

  public SelectionManager getSelectionManager() {
    return selectionManager;
  }

  public void setMode(int mode) {
    this.mode = mode;
    repaint();
  }

  public int getMode() {
    return mode;
  }

  public int getSymbolMode() {
    return symbolMode;
  }

  public void setSymbolMode(int symbolMode) {
    this.symbolMode = symbolMode;
    repaint();
  }

  protected void paintComponent(Graphics g) {
    super.paintComponent(g); // Draw background phases
    if (mode==PROCESS_MODE)
      paintByProcesses(g);
    else
      if (mode==ACTOR_MODE)
        paintByActors(g);
  }

  private void updatePanelSize(int height) {
    height += 2 * actorLineSpacing;
    setPreferredSize(new Dimension(getPreferredSize().width, height));
    revalidate(); // Notify the scroll pane that the size has changed
  }

  private int getXForTime(LocalDateTime time, int width) {
    if (time == null) return 0;
    long secondsFromStart = ChronoUnit.SECONDS.between(minDate, time);
    return (int) ((secondsFromStart * width) / (double) totalDuration);
  }

  private void clearAreaMaps() {
    if (processAreas == null) processAreas = new HashMap<>(); else processAreas.clear();
    if (processTaskAreas == null) processTaskAreas = new HashMap<>(); else processTaskAreas.clear();
    if (processActorAreas == null) processActorAreas = new HashMap<>(); else processActorAreas.clear();
    if (actorAreas == null) actorAreas = new HashMap<>(); else actorAreas.clear();
  }

  public void paintByProcesses(Graphics g) {
    int width = getWidth();
    Graphics2D g2d = (Graphics2D) g;
    Stroke defaultStroke = g2d.getStroke();
    FontMetrics fm = g2d.getFontMetrics();

    clearAreaMaps();
    int y0 = yTop + 2 * actorLineSpacing;

    for (ProcessInstance p : gProc.processes) {
      TimeInterval pLife = p.getProcessLifetime();
      int xStartProcess = getXForTime(pLife.start, width);
      int xEndProcess = getXForTime(pLife.end, width);

      List<ProcessThread> sortedThreads = new ArrayList<>(p.threads.values());
      int maxY = y0;

      // Vertical initiation line
      if (!sortedThreads.isEmpty()) {
        g.setColor(Color.darkGray);
        g2d.setStroke(defaultStroke);
        int vLineHeight = (sortedThreads.size() - 1) * actorLineSpacing;
        g.drawLine(xStartProcess, y0 + actorLineSpacing / 2,
            xStartProcess, y0 + actorLineSpacing / 2 + vLineHeight);
      }

      for (int i = 0; i < sortedThreads.size(); i++) {
        ProcessThread thread = sortedThreads.get(i);
        int y = y0 + i * actorLineSpacing + actorLineSpacing / 2;
        maxY = Math.max(maxY, y);

        // Determine if this specific thread contains selected tasks
        boolean isThreadSelected = false;
        if (selectionManager != null && selectionManager.hasSelection()) {
          for (TaskInstance t : thread.tasks) {
            if (selectionManager.isTaskSelected(t)) {
              isThreadSelected = true;
              break;
            }
          }
        }

        TaskInstance firstTask = thread.tasks.get(0);
        int xFirstAction = getXForTime(firstTask.actual.start, width);

        // Coloring Logic
        if (isThreadSelected) {
          g.setColor(Color.black);
          // Pre-activity: Thin and Dashed
          g2d.setStroke(dashedStroke);
          g.drawLine(xStartProcess, y, xFirstAction, y);
          // Activity phase: Bold and Solid
          g2d.setStroke(highlightedStroke);
          g.drawLine(xFirstAction, y, xEndProcess, y);
        } else {
          Color roleColor = actorRoleColors.getOrDefault(thread.role, Color.gray);
          g.setColor(roleColor);
          // Pre-activity: Thin and Dashed
          g2d.setStroke(dashedStroke);
          g.drawLine(xStartProcess, y, xFirstAction, y);
          // Activity phase: Thin and Solid
          g2d.setStroke(solidStroke);
          g.drawLine(xFirstAction, y, xEndProcess, y);
        }

        for (TaskInstance t : thread.tasks) {
          drawTaskSymbol(g2d, t, width, y, p, fm);
        }

        Map<Rectangle, Actor> actorProcAreas = processActorAreas.computeIfAbsent(p.id, k -> new HashMap<>());
        actorProcAreas.put(new Rectangle(xStartProcess, y - markRadius, xEndProcess - xStartProcess, markDiameter), thread.actor);
      }

      processAreas.put(new Rectangle(xStartProcess - 3, y0 - 3, xEndProcess - xStartProcess + 6, maxY - y0 + 6), p);
      y0 = maxY + actorLineSpacing * 4;
    }
    updatePanelSize(y0);
  }

  private void drawTaskSymbol(Graphics2D g2d, TaskInstance t, int width, int y, ProcessInstance p, FontMetrics fm) {
    if (t.actual == null || t.actual.start == null) return;
    int x1 = getXForTime(t.actual.start, width);
    int x2 = getXForTime(t.actual.end, width);

    boolean isSelected = selectionManager != null && selectionManager.isTaskSelected(t);

    if (isSelected) {
      g2d.setColor(Color.black);
    } else {
      Color symColor = actionTypeColors.getOrDefault(t.actionType, Color.gray);
      g2d.setColor(Utils.toSaturated(symColor));
    }

    ActionType aType = gProc.actionTypes.get(t.actionType);
    if (symbolMode == SYMBOL_CHAR && aType != null && aType.code != null) {
      int dx = fm.stringWidth(aType.code) / 2;
      g2d.drawString(aType.code, x1 - dx, y + fm.getHeight() / 2 - fm.getDescent());
    } else {
      g2d.fillOval(x1 - markRadius, y - markRadius, markDiameter + x2 - x1, markDiameter);
      if (isSelected) {
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawOval(x1 - markRadius, y - markRadius, markDiameter + x2 - x1, markDiameter);
      }
    }

    Map<Rectangle, TaskInstance> taskAreas = processTaskAreas.computeIfAbsent(p.id, k -> new HashMap<>());
    taskAreas.put(new Rectangle(x1 - markRadius - 3, y - markRadius - 3,
        markDiameter + x2 - x1 + 6, markDiameter + 6), t);
  }

  public void paintByActors(Graphics g) {
    int width = getWidth();
    Graphics2D g2d = (Graphics2D) g;
    Stroke defaultStroke = g2d.getStroke();
    FontMetrics fm = g2d.getFontMetrics();
    clearAreaMaps();

    // Use the global list of actors, sorted by their overall involvement
    List<Actor> sortedGlobalActors = gProc.getActorsSorted(gProc.actors.values());
    int y0 = yTop + 2 * actorLineSpacing;

    for (Actor actor : sortedGlobalActors) {
      // 1. Group all threads for this actor across all processes
      List<ThreadContext> actorThreadContexts = new ArrayList<>();
      for (ProcessInstance p : gProc.processes) {
        if (p.threads.containsKey(actor.id)) {
          actorThreadContexts.add(new ThreadContext(p, p.threads.get(actor.id)));
        }
      }

      if (actorThreadContexts.isEmpty()) continue;
      /*
      if (actorThreadContexts.size()>1)
        System.out.println("Actor "+actor.id+": N threads = "+actorThreadContexts.size());
      */

      // 2. Order threads chronologically by the start date of the first action
      actorThreadContexts.sort(Comparator.comparing(tc -> tc.thread.tasks.get(0).actual.start));

      int currentY = y0;
      int minActorX = width, maxActorX = 0;

      // Calculate the leftmost start point to place the vertical grouping line
      for (ThreadContext tc : actorThreadContexts) {
        int xPStart = getXForTime(tc.process.getProcessLifetime().start, width);
        if (xPStart < minActorX) minActorX = xPStart;
      }

      // 3. Draw vertical grouping line (similar to paintByProcesses)
      g.setColor(Color.darkGray);
      g2d.setStroke(defaultStroke);
      int vLineHeight = (actorThreadContexts.size() - 1) * actorLineSpacing;
      g.drawLine(minActorX, y0 + actorLineSpacing / 2,
          minActorX, y0 + actorLineSpacing / 2 + vLineHeight);

      // 4. Draw each thread line
      for (int i = 0; i < actorThreadContexts.size(); i++) {
        ThreadContext tc = actorThreadContexts.get(i);
        ProcessInstance p = tc.process;
        ProcessThread thread = tc.thread;
        int y = y0 + i * actorLineSpacing + actorLineSpacing / 2;
        currentY = y;

        TimeInterval pLife = p.getProcessLifetime();
        int xPStart = getXForTime(pLife.start, width);
        int xPEnd = getXForTime(pLife.end, width);
        int xFirstAction = getXForTime(thread.tasks.get(0).actual.start, width);

        if (xPEnd > maxActorX) maxActorX = xPEnd;

        // Determine Selection
        boolean isThreadSelected = false;
        if (selectionManager != null && selectionManager.hasSelection()) {
          for (TaskInstance t : thread.tasks) {
            if (selectionManager.isTaskSelected(t)) {
              isThreadSelected = true;
              break;
            }
          }
        }

        // Appearance logic (Identical to paintByProcesses)
        if (isThreadSelected) {
          g.setColor(Color.black);
          g2d.setStroke(dashedStroke);
          g.drawLine(xPStart, y, xFirstAction, y);
          g2d.setStroke(highlightedStroke);
          g.drawLine(xFirstAction, y, xPEnd, y);
        } else {
          Color roleColor = actorRoleColors.getOrDefault(thread.role, Color.gray);
          g.setColor(roleColor);
          g2d.setStroke(dashedStroke);
          g.drawLine(xPStart, y, xFirstAction, y);
          g2d.setStroke(solidStroke);
          g.drawLine(xFirstAction, y, xPEnd, y);
        }

        // Draw tasks
        for (TaskInstance t : thread.tasks) {
          drawTaskSymbol(g2d, t, width, y, p, fm);
        }

        // Map areas for tooltips
        Map<Rectangle, Actor> actorProcAreas = processActorAreas.computeIfAbsent(p.id, k -> new HashMap<>());
        actorProcAreas.put(new Rectangle(xPStart, y - markRadius, xPEnd - xPStart, markDiameter), actor);
      }

      // Map global actor area
      actorAreas.put(new Rectangle(minActorX - 3, y0 - 3, maxActorX - minActorX + 6, currentY - y0 + 6), actor);
      y0 = currentY + actorLineSpacing * 4;
    }
    updatePanelSize(y0);
  }

  /**
   * Helper class to keep a thread associated with its process context
   */
  private static class ThreadContext {
    ProcessInstance process;
    ProcessThread thread;
    ThreadContext(ProcessInstance p, ProcessThread t) {
      this.process = p;
      this.thread = t;
    }
  }

  public String getToolTipText(Point pt) {
    if (pt==null)
      return null;
    if (mode==PROCESS_MODE) {
      if (processAreas == null)
        return super.getToolTipText(pt);
      for (Map.Entry<Rectangle, ProcessInstance> entry : processAreas.entrySet()) {
        if (entry.getKey().contains(pt)) {
          ProcessInstance p = entry.getValue();
          //check if the mouse points at a specific task
          Map<Rectangle, TaskInstance> taskAreas = processTaskAreas.get(p.id);
          if (taskAreas != null)
            for (Map.Entry<Rectangle, TaskInstance> taskEntry : taskAreas.entrySet())
              if (taskEntry.getKey().contains(pt))
                return getTextForTask(p,taskEntry.getValue());
          Map<Rectangle, Actor> actorProcAreas = processActorAreas.get(p.id);
          if (actorProcAreas != null)
            for (Map.Entry<Rectangle, Actor> actorEntry : actorProcAreas.entrySet())
              if (actorEntry.getKey().contains(pt))
                return getTextForActor(p,actorEntry.getValue());

          String text = String.format("<html>Process ID: <b>%s</b><br>Process type: <b>%s</b></html>",
              p.id, p.type);
          return text;
        }
      }
    }
    if (mode==ACTOR_MODE) {
      if (actorAreas!=null)
        if (actorAreas != null)
          for (Map.Entry<Rectangle, Actor> actorEntry : actorAreas.entrySet())
            if (actorEntry.getKey().contains(pt)) {
              Actor actor=actorEntry.getValue();
              if (processActorAreas!=null)
                for (ProcessInstance p: gProc.processes) {
                  Map<Rectangle, Actor> actorProcAreas = processActorAreas.get(p.id);
                  if (actorProcAreas != null && actorProcAreas.containsValue(actor)) {
                    //check if the mouse points at a specific task
                    Map<Rectangle, TaskInstance> taskAreas = processTaskAreas.get(p.id);
                    if (taskAreas != null)
                      for (Map.Entry<Rectangle, TaskInstance> taskEntry : taskAreas.entrySet())
                        if (taskEntry.getKey().contains(pt))
                          return getTextForTask(p,taskEntry.getValue());
                    for (Map.Entry<Rectangle, Actor> aE : actorProcAreas.entrySet())
                      if (aE.getValue().equals(actor) && aE.getKey().contains(pt))
                        return getTextForActor(p, actor);
                  }
                }
              return getTextForActor(null, actorEntry.getValue());
            }
    }
    return super.getToolTipText(pt);
  }

  protected String getTextForTask(ProcessInstance p, TaskInstance t) {
    if (t==null)
      return null;
    String text = String.format("<html>Process ID: <b>%s</b><br>Process type: <b>%s</b>" +
            "<br>Task ID: <b>%s</b><br>Name: <b>%s</b><br>" +
            "Actual time: <b>%s -- %s</b>", p.id, p.type,
        t.id, t.actionType, t.actual.start.format(formatter), t.actual.end.format(formatter));
    if (t.scheduled != null)
      text += String.format("<br>Scheduled time: <b>%s -- %s</b>",
          t.scheduled.start.format(formatter), t.scheduled.end.format(formatter));
    Actor primaryActor = (t.actorsInvolved == null || t.actorsInvolved.isEmpty()) ? null : t.actorsInvolved.get(0);
    if (primaryActor != null) {
      String role=p.threads.get(primaryActor.id).role;
      text += String.format("<br>Primary actor: <b>%s</b> in role <b>%s</b>",
          primaryActor.id, role);
      if (t.actorsInvolved.size() > 1)
        for (int i = 1; i < t.actorsInvolved.size(); i++) {
          Actor a = t.actorsInvolved.get(i);
          text += String.format("<br>Involved actor: <b>%s</b>",a.id);
        }
    }
    if (t.status != null)
      text += "<br>Status: <b>" + t.status + "</b>";
    if (t.outcome != null)
      text += "<br>Outcome: <b>" + t.outcome + "</b>";
    text += "</html>";
    return text;
  }

  protected String getTextForActor(ProcessInstance p, Actor a) {
    if (a==null)
      return null;
    if (p==null)
      return String.format("<html>Actor ID: <b>%s</b><br>Main role: <b>%s</b></html>",
          a.id, a.generalRole);
    String role=p.threads.get(a.id).role;
    return String.format("<html>Process ID: <b>%s</b><br>Process type: <b>%s</b>" +
        "<br>Involved actor: <b>%s</b> in role <b>%s</b></html>", p.id, p.type, a.id, role);
  }
}
