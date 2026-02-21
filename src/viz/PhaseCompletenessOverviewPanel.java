package viz;

import structures.GlobalProcess;
import structures.Phase;
import structures.ProcessInstance;

import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Visualizes aggregated phase completeness information.
 * Vertical bars represent processes that haven't completed phases by a specific day.
 */
public class PhaseCompletenessOverviewPanel extends TimelinePanel {
  private GlobalProcess gProc;

  public PhaseCompletenessOverviewPanel(GlobalProcess gProc) {
    super(gProc.getListOfPhases());
    this.gProc = gProc;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    if (phases == null || phases.isEmpty() || gProc == null || gProc.processes.isEmpty()) {
      return;
    }

    Graphics2D g2d = (Graphics2D) g;
    int width = getWidth();

    int totalProcesses = gProc.processes.size();
    double pixelsPerProcess = (double) (sectionHeight - 5) / totalProcesses;

    LocalDate start = minDate.toLocalDate();
    LocalDate end = maxDate.toLocalDate();
    long days = ChronoUnit.DAYS.between(start, end);
    int barWidth = (int) (width / (days + 1));

    for (int d = 0; d <= days; d++) {
      LocalDate currentDay = start.plusDays(d);
      int[] stuckCounts = getStuckCountsForDate(currentDay);

      int x = getXForDate(currentDay, width);
      int currentY = yBottom;

      for (int i = 0; i < stuckCounts.length; i++) {
        if (stuckCounts[i] == 0) continue;

        int segmentHeight = (int) (stuckCounts[i] * pixelsPerProcess);
        Color phaseColor = getPhaseColor(i);
        Color opaqueColor = new Color(phaseColor.getRed(), phaseColor.getGreen(), phaseColor.getBlue(), 255);

        g2d.setColor(opaqueColor);
        g2d.fillRect(x, currentY - segmentHeight, barWidth, segmentHeight);

        g2d.setColor(new Color(0, 0, 0, 40));
        g2d.drawRect(x, currentY - segmentHeight, barWidth, segmentHeight);

        currentY -= segmentHeight;
      }
    }
  }

  /**
   * Helper to calculate how many processes are stuck in each phase for a given date.
   */
  private int[] getStuckCountsForDate(LocalDate date) {
    int[] stuckCounts = new int[phases.size()];
    for (ProcessInstance pi : gProc.processes) {
      for (int i = 0; i < phases.size(); i++) {
        Phase p = phases.get(i);
        // If the phase hasn't even started by this date, it's not a bottleneck yet
        if (p.startDate.isAfter(date)) break;

        LocalDate completionDate = pi.getPhaseCompletenessDate(p.name);
        // If not completed or completed after this date, this is the current bottleneck
        if (completionDate != null && completionDate.isAfter(date)) {
          stuckCounts[i]++;
          break;
        }
      }
    }
    return stuckCounts;
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

    // Re-calculate counts for the specific hovered date
    int[] stuckCounts = getStuckCountsForDate(hoverDate);
    int totalProcesses = gProc.processes.size();
    double pixelsPerProcess = (double) (sectionHeight - 5) / totalProcesses;

    String segmentInfo = "";
    int currentY = yBottom;

    // Determine which segment the mouse is pointing at
    for (int i = 0; i < stuckCounts.length; i++) {
      if (stuckCounts[i] == 0) continue;

      int segmentHeight = (int) (stuckCounts[i] * pixelsPerProcess);
      int segmentTop = currentY - segmentHeight;

      if (pt.y <= currentY && pt.y >= segmentTop) {
        segmentInfo = String.format("<br>Incomplete Phase: <b>%s</b>" +
                "<br>Process Count: <b>%d</b>",
            phases.get(i).name, stuckCounts[i]);
        break;
      }
      currentY -= segmentHeight;
    }

    // Get the background phase name (underlying schedule)
    String baseTip = super.getToolTipText(pt);
    String dateInfo = "<br>Timeline Date: <b>" + hoverDate.format(formatter) + "</b>";

    if (baseTip == null) {
      return "<html>" + dateInfo + segmentInfo + "</html>";
    } else {
      // baseTip usually contains the phase name of the background timeline
      return baseTip.replace("</html>", dateInfo + segmentInfo + "</html>");
    }
  }
}