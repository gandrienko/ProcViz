package viz;

import structures.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Visualizes aggregated phase completeness information.
 * Supports selecting process instances via clicking on bar segments.
 */
public class PhaseCompletenessOverviewPanel extends TimelinePanel {
  private GlobalProcess gProc;
  private SelectionManager selectionManager;
  private TexturePaint hatchPaint;

  public PhaseCompletenessOverviewPanel(GlobalProcess gProc, SelectionManager selectionManager) {
    super(gProc.getListOfPhases());
    this.gProc = gProc;
    this.selectionManager = selectionManager;

    // Create a 10x10 diagonal hatch pattern for selected process instances
    BufferedImage bi = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
    Graphics2D big = bi.createGraphics();
    big.setColor(new Color(0, 0, 0, 100)); // Semi-transparent black lines
    big.drawLine(0, 8, 8, 0);
    this.hatchPaint = new TexturePaint(bi, new Rectangle(0, 0, 8, 8));

    if (selectionManager != null) {
      // UPDATE: Register for process-specific selection changes
      selectionManager.addProcessListener(this::repaint);
    }

    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (!handleSelection(e.getPoint()) && e.getClickCount()==2)
          // Clear the process selection if double-clicking background
          selectionManager.clearProcessSelection();
      }
    });
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (phases == null || gProc == null) return;

    Graphics2D g2d = (Graphics2D) g;
    int width = getWidth();
    double pixelsPerProcess = (double) (sectionHeight - 5) / gProc.processes.size();

    LocalDate start = minDate.toLocalDate();
    long days = ChronoUnit.DAYS.between(start, maxDate.toLocalDate());
    int barWidth = (int) (width / (days + 1));

    for (int d = 0; d <= days; d++) {
      LocalDate currentDay = start.plusDays(d);
      List<ProcessInstance>[] stuckGroups = getStuckProcessesForDate(currentDay);

      int x = getXForDate(currentDay, width);
      int currentY = yBottom;

      for (int i = 0; i < stuckGroups.length; i++) {
        List<ProcessInstance> group = stuckGroups[i];
        if (group.isEmpty()) continue;

        int segmentHeight = (int) (group.size() * pixelsPerProcess);
        Color phaseColor = getPhaseColor(i);

        // 1. Draw solid phase segment
        g2d.setColor(new Color(phaseColor.getRed(), phaseColor.getGreen(), phaseColor.getBlue(), 255));
        g2d.fillRect(x, currentY - segmentHeight, barWidth, segmentHeight);

        // 2. Draw patterned overlay for selected instances
        if (selectionManager != null) {
          Set<String> selectedIds = selectionManager.getSelectedProcessIds();
          long selectedCount = group.stream()
              .filter(p -> selectedIds.contains(p.id))
              .count();

          if (selectedCount > 0) {
            int selectedHeight = (int) (selectedCount * pixelsPerProcess);
            Paint oldPaint = g2d.getPaint();
            g2d.setPaint(hatchPaint);
            // Draw the hatch pattern from the bottom of this specific segment upwards
            g2d.fillRect(x, currentY - selectedHeight, barWidth, selectedHeight);
            g2d.setPaint(oldPaint);
          }
        }

        // 3. Draw segment border
        g2d.setColor(new Color(0, 0, 0, 40));
        g2d.drawRect(x, currentY - segmentHeight, barWidth, segmentHeight);
        currentY -= segmentHeight;
      }
    }
  }

  private boolean handleSelection(Point pt) {
    if (selectionManager == null) return false;

    int width = getWidth();
    double ratio = (double) pt.x / width;
    LocalDate clickDate = minDate.plusSeconds((long)(ratio * totalDuration)).toLocalDate();

    List<ProcessInstance>[] groups = getStuckProcessesForDate(clickDate);
    double pixelsPerProcess = (double) (sectionHeight - 5) / gProc.processes.size();
    int currentY = yBottom;

    for (List<ProcessInstance> group : groups) {
      int segmentHeight = (int) (group.size() * pixelsPerProcess);
      if (pt.y <= currentY && pt.y >= currentY - segmentHeight) {
        List<String> ids = group.stream().map(p -> p.id).collect(Collectors.toList());
        selectionManager.toggleProcesses(ids);
        return true;
      }
      currentY -= segmentHeight;
    }
    return false;
  }

  private List<ProcessInstance>[] getStuckProcessesForDate(LocalDate date) {
    List<ProcessInstance>[] groups = new ArrayList[phases.size()];
    for (int i = 0; i < phases.size(); i++) groups[i] = new ArrayList<>();

    for (ProcessInstance pi : gProc.processes) {
      for (int i = 0; i < phases.size(); i++) {
        Phase p = phases.get(i);
        if (p.startDate.isAfter(date)) break;
        LocalDate comp = pi.getPhaseCompletenessDate(p.name);
        if (comp != null && comp.isAfter(date)) {
          groups[i].add(pi);
          break;
        }
      }
    }
    return groups;
  }

  private int getXForDate(LocalDate date, int width) {
    LocalDateTime dateTime = date.atStartOfDay();
    long secondsFromStart = ChronoUnit.SECONDS.between(minDate, dateTime);
    return (int) ((secondsFromStart * width) / (double) totalDuration);
  }

  @Override
  public String getToolTipText(Point pt) {
    if (phases == null || gProc == null) return null;

    int width = getWidth();
    double ratio = (double) pt.x / width;
    long offsetSeconds = (long) (ratio * totalDuration);
    LocalDate hoverDate = minDate.plusSeconds(offsetSeconds).toLocalDate();

    List<ProcessInstance>[] groups = getStuckProcessesForDate(hoverDate);
    double pixelsPerProcess = (double) (sectionHeight - 5) / gProc.processes.size();

    String segmentInfo = "";
    int currentY = yBottom;

    for (int i = 0; i < groups.length; i++) {
      int segmentHeight = (int) (groups[i].size() * pixelsPerProcess);
      if (pt.y <= currentY && pt.y >= currentY - segmentHeight) {
        segmentInfo = String.format("<br>Incomplete Phase: <b>%s</b>" +
                "<br>Process Count: <b>%d</b>",
            phases.get(i).name, groups[i].size());
        break;
      }
      currentY -= segmentHeight;
    }

    String baseTip = super.getToolTipText(pt);
    String dateInfo = "<br>Timeline Date: <b>" + hoverDate.format(formatter) + "</b>";

    return (baseTip == null) ? "<html>" + dateInfo + segmentInfo + "</html>" :
        baseTip.replace("</html>", dateInfo + segmentInfo + "</html>");
  }
}