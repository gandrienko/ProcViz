package viz;

import structures.*;

import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

public class ProcessTimelinePanel extends TimelinePanel{
  public static int markRadius=4, markDiameter=markRadius*2, actorLineSpacing=8;
  public static int PROCESS_MODE=1, ACTOR_MODE=2;
  public static int SYMBOL_DOT=0, SYMBOL_CHAR=1;
  public static int SHOW_ALL=0, SHOW_SELECTED=1;
  public static Color taskSymbolColor=new Color(60,60,255,160),
      delayedTaskColor=Color.red.darker();
  public static Color threadHighlightColor=new Color(0,240,255,180),
      processHighlightColor=new Color(255,255,0,90);
  
  public GlobalProcess gProc=null;
  private SelectionManager selectionManager=null;
  public int groupingMode =PROCESS_MODE;
  public int symbolMode=SYMBOL_CHAR;
  public int filterMode=SHOW_ALL;
  
  protected Map<Rectangle, ProcessInstance> processAreas = null;
  protected Map<String,Map<Rectangle,Actor>> processActorAreas=null;
  protected Map<Rectangle,Actor> actorAreas=null;
  protected Map<String,Map<Rectangle,TaskInstance>> processTaskAreas=null;
  protected Map<String, Color> actorRoleColors=null;

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
    setPreferredSize(new Dimension(1200, 100 + gProc.processes.size() * actorLineSpacing*10));
    if (selectionManager!=null) {
      selectionManager.addTaskListener(() -> {
        repaint();
      });
      selectionManager.addProcessListener(() -> {
        repaint();
      });

      // Add mouse listener for selection
      addMouseListener(new java.awt.event.MouseAdapter() {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent e) {
          if (!handleMouseClick(e.getPoint()) && e.getClickCount()==2)
            selectionManager.clearTaskSelection();
        }
      });
    }
  }

  public SelectionManager getSelectionManager() {
    return selectionManager;
  }

  public void setGroupingMode(int groupingMode) {
    this.groupingMode = groupingMode;
    repaint();
  }

  public int getGroupingMode() {
    return groupingMode;
  }

  public int getSymbolMode() {
    return symbolMode;
  }

  public void setSymbolMode(int symbolMode) {
    this.symbolMode = symbolMode;
    repaint();
  }

  public int getFilterMode() {
    return filterMode;
  }

  public void setFilterMode(int filterMode) {
    this.filterMode = filterMode;
    repaint();
  }

  protected void paintComponent(Graphics g) {
    super.paintComponent(g); // Draw background phases
    if (groupingMode ==PROCESS_MODE)
      paintByProcesses(g);
    else
      if (groupingMode ==ACTOR_MODE)
        paintByActors(g);
  }

  private void updatePanelSize(int height) {
    height += 2 * actorLineSpacing;
    setPreferredSize(new Dimension(getPreferredSize().width, height));
    revalidate(); // Notify the scroll pane that the size has changed
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
      if (filterMode==SHOW_SELECTED && selectionManager!=null && !selectionManager.isProcessSelested(p.id))
        continue;

      TimeInterval pLife = p.getProcessLifetime();
      int xStartProcess = getXForTime(pLife.start, width);
      int xEndProcess = getXForTime(pLife.end, width);

      List<ProcessThread> sortedThreads = new ArrayList<>(p.threads.values());
      int vLineHeight = (sortedThreads.size() - 1) * actorLineSpacing;
      int maxY = y0+vLineHeight;

      if (filterMode==SHOW_ALL) {
        boolean selected = selectionManager != null && selectionManager.isProcessSelested(p.id);
        if (selected) {
          g2d.setColor(processHighlightColor);
          g2d.fillRect(xStartProcess, y0, xEndProcess - xStartProcess, vLineHeight + actorLineSpacing);
        }
      }

      if (p.hasPhaseCompletenessDates()) {
        for (Phase ph:gProc.phases.values()) {
          LocalDate d=p.getPhaseCompletenessDate(ph.name);
          if (d!=null && d.isAfter(ph.endDate)) {
            int x2=getXForTime(d.atTime(23,59,59),width),
                x1=getXForTime(ph.endDate.atTime(23,59,59),width);
            if (x2>x1) {
              Color c=phaseColors.get(ph.name);
              c=new Color(c.getRed(),c.getGreen(),c.getBlue(),128);
              g2d.setColor(c);
              g2d.fillRect(x1,y0,x2-x1,vLineHeight+actorLineSpacing);
            }
          }
        }
      }

      // Vertical initiation line
      if (!sortedThreads.isEmpty()) {
        g.setColor(Color.darkGray);
        g2d.setStroke(defaultStroke);
        g.drawLine(xStartProcess, y0 + actorLineSpacing / 2,
            xStartProcess, y0 + actorLineSpacing / 2 + vLineHeight);
      }

      for (int i = 0; i < sortedThreads.size(); i++) {
        ProcessThread thread = sortedThreads.get(i);
        TimeInterval tLife = thread.getLifetime();
        int xStartThread = getXForTime(tLife.start, width);
        int xEndThread = getXForTime(tLife.end, width);

        int y = y0 + i * actorLineSpacing + actorLineSpacing / 2;
        maxY = Math.max(maxY, y);

        // Determine if this specific thread contains selected tasks
        boolean isThreadSelected = selectionManager != null && selectionManager.hasTaskSelection() &&
            thread.hasAnyTask(selectionManager.getSelectedTasks());

        if (isThreadSelected) {
          g.setColor(threadHighlightColor);
          g.fillRect(xStartProcess, y - 2, xEndThread - xStartProcess, 5);
        }
        Color roleColor = actorRoleColors.getOrDefault(thread.role, Color.gray);
        g.setColor(roleColor);
        // Pre-activity: Thin and Dashed
        g2d.setStroke(dashedStroke);
        g.drawLine(xStartProcess, y, xStartThread, y);
        // Activity phase: Thin and Solid
        g2d.setStroke(solidStroke);
        g.drawLine(xStartThread, y, xEndThread, y);

        for (int tIdx=0; tIdx<thread.tasks.size(); tIdx++) {
          TaskInstance t=thread.tasks.get(tIdx);
          if (tIdx>0) {
            TaskInstance tPrev=thread.tasks.get(tIdx-1);
            if (tPrev.isDelayed &&
                getXForTime(t.actual.start,width)-getXForTime(tPrev.actual.start,width)<=markRadius)
              continue;
          }
          if (!t.isDelayed && tIdx+1<thread.tasks.size()) {
            TaskInstance tNext=thread.tasks.get(tIdx+1);
            if (getXForTime(tNext.actual.start,width)-getXForTime(t.actual.start,width)<=markRadius)
              continue;
          }
          drawTaskSymbol(g2d, t, width, y, p, fm);
        }

        Map<Rectangle, Actor> actorProcAreas = processActorAreas.computeIfAbsent(p.id, k -> new HashMap<>());
        actorProcAreas.put(new Rectangle(xStartProcess, y - markRadius,
            xEndThread - xStartProcess, markDiameter), thread.actor);
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

    g2d.setColor((t.isDelayed)?delayedTaskColor:taskSymbolColor);

    ActionType aType = gProc.actionTypes.get(t.actionType);
    if (symbolMode == SYMBOL_CHAR && aType != null && aType.code != null) {
      int dx = fm.stringWidth(aType.code) / 2;
      g2d.drawString(aType.code, x1 - dx, y + fm.getHeight() / 2 - fm.getDescent());
    } else {
      g2d.fillOval(x1 - markRadius, y - markRadius, markDiameter + x2 - x1, markDiameter);
    }
    if (isSelected) {
      Stroke str=g2d.getStroke();
      g2d.setStroke(new BasicStroke(2.0f));
      int radius=Math.round(0.75f*actorLineSpacing);
      g2d.drawOval(x1 - radius, y - radius, radius*2, radius*2);
      g2d.setStroke(str);
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
        if (filterMode==SHOW_SELECTED && selectionManager!=null && !selectionManager.isProcessSelested(p.id))
          continue;
        if (p.threads.containsKey(actor.id)) {
          actorThreadContexts.add(new ThreadContext(p, p.threads.get(actor.id)));
        }
      }

      if (actorThreadContexts.isEmpty()) continue;

      // 2. Order threads chronologically by the start date of the first action
      actorThreadContexts.sort(Comparator.comparing(tc -> tc.thread.tasks.get(0).actual.start));

      int currentY = y0;
      int minActorX = width, maxActorX = 0;

      // Calculate the leftmost start point to place the vertical grouping line
      for (ThreadContext tc : actorThreadContexts) {
        TimeInterval tint=tc.process.getProcessLifetime();
        int xStart = getXForTime(tint.start, width);
        if (xStart < minActorX) minActorX = xStart;
        tint=tc.thread.getLifetime();
        int xEnd=getXForTime(tint.end, width);
        if (xEnd>maxActorX) maxActorX=xEnd;
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
        ProcessThread thread = tc.thread;
        int y = y0 + i * actorLineSpacing + actorLineSpacing / 2;
        currentY = y;

        TimeInterval tLife = thread.getLifetime();
        int xStartThread = getXForTime(tLife.start, width);
        int xEndThread = getXForTime(tLife.end, width);

        // Process selection
        if (filterMode==SHOW_ALL) {
          boolean isProcessSelected = selectionManager != null && selectionManager.isProcessSelested(tc.process.id);
          if (isProcessSelected) {
            int xEndProcess = getXForTime(tc.process.getProcessLifetime().end, width);
            g.setColor(processHighlightColor);
            int hWidth = actorLineSpacing - 2;
            g.fillRect(minActorX, y - hWidth / 2, xEndProcess - minActorX, hWidth);
          }
        }

        // Thread selection
        boolean isThreadSelected = selectionManager != null && selectionManager.hasTaskSelection() &&
            thread.hasAnyTask(selectionManager.getSelectedTasks());
        if (isThreadSelected) {
          g.setColor(threadHighlightColor);
          g.fillRect(minActorX, y - 2, xEndThread - minActorX, 5);
        }
        Color roleColor = actorRoleColors.getOrDefault(thread.role, Color.gray);
        g.setColor(roleColor);
        g2d.setStroke(dashedStroke);
        g.drawLine(minActorX, y, xStartThread, y);
        g2d.setStroke(solidStroke);
        g.drawLine(xStartThread, y, xEndThread, y);

        // Draw tasks
        for (int tIdx=0; tIdx<thread.tasks.size(); tIdx++) {
          TaskInstance t=thread.tasks.get(tIdx);
          if (tIdx>0) {
            TaskInstance tPrev=thread.tasks.get(tIdx-1);
            if (tPrev.isDelayed &&
                getXForTime(t.actual.start,width)-getXForTime(tPrev.actual.start,width)<=markRadius)
              continue;
          }
          if (!t.isDelayed && tIdx+1<thread.tasks.size()) {
            TaskInstance tNext=thread.tasks.get(tIdx+1);
            if (getXForTime(tNext.actual.start,width)-getXForTime(t.actual.start,width)<=markRadius)
              continue;
          }
          drawTaskSymbol(g2d, t, width, y, tc.process, fm);
        }

        // Map areas for tooltips
        Map<Rectangle, Actor> actorProcAreas = processActorAreas.computeIfAbsent(tc.process.id, k -> new HashMap<>());
        actorProcAreas.put(new Rectangle(minActorX, y - markRadius, maxActorX - minActorX, markDiameter), actor);
      }

      // Map global actor area
      actorAreas.put(new Rectangle(minActorX - 3, y0 - 3, maxActorX - minActorX + 6, currentY - y0 + 6), actor);
      y0 = currentY + actorLineSpacing * 4;
    }
    updatePanelSize(y0);
  }

  private boolean handleMouseClick(Point pt) {
    if (selectionManager == null) return false;

    // 1. Check for Task Selection (High priority/Smallest targets)
    for (String processId : processTaskAreas.keySet()) {
      Map<Rectangle, TaskInstance> taskMap = processTaskAreas.get(processId);
      for (Map.Entry<Rectangle, TaskInstance> entry : taskMap.entrySet()) {
        if (entry.getKey().contains(pt)) {
          selectionManager.toggleTasks(Collections.singletonList(entry.getValue()));
          return true; // Selection handled
        }
      }
    }

    // 2. Check for Thread Selection (Low priority/Line targets)
    for (String processId : processActorAreas.keySet()) {
      Map<Rectangle, Actor> actorMap = processActorAreas.get(processId);
      for (Map.Entry<Rectangle, Actor> entry : actorMap.entrySet()) {
        if (entry.getKey().contains(pt)) {
          Actor actor = entry.getValue();
          ProcessInstance p = findProcessById(processId);
          if (p != null && p.threads.containsKey(actor.id)) {
            // Get all tasks associated with this specific actor's thread in this process
            List<TaskInstance> threadTasks = p.threads.get(actor.id).tasks;
            selectionManager.toggleTasks(threadTasks);
          }
          return true; // Selection handled
        }
      }
    }
    return false;
 }

  /**
   * Helper to retrieve the process instance object by its ID string.
   */
  private ProcessInstance findProcessById(String id) {
    for (ProcessInstance p : gProc.processes) {
      if (p.id.equals(id)) return p;
    }
    return null;
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
    if (groupingMode ==PROCESS_MODE) {
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
    if (groupingMode ==ACTOR_MODE) {
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
          String r=p.roleAssignments.get(a.id);
          if (r==null)
            r=a.generalRole;
          text += String.format("<br>Involved actor: <b>%s as %s</b>",a.id,r);
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
