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
    // super.paintComponent draws the translucent phase background areas
    super.paintComponent(g);

    if (phases == null || phases.isEmpty() || gProc == null || gProc.processes.isEmpty()) {
      return;
    }

    Graphics2D g2d = (Graphics2D) g;
    int width = getWidth();

    // Calculate scaling: how many pixels per process instance
    int totalProcesses = gProc.processes.size();
    double pixelsPerProcess = (double) (sectionHeight - 5) / totalProcesses;

    // Iterate through every day in the timeline
    LocalDate start = minDate.toLocalDate();
    LocalDate end = maxDate.toLocalDate();
    long days = ChronoUnit.DAYS.between(start, end);
    int barWidth=(int)(width/days);

    for (int d = 0; d <= days; d++) {
      LocalDate currentDay = start.plusDays(d);

      // Tally how many processes are "stuck" in each phase
      // Stuck means this is the earliest phase not completed by currentDay
      int[] stuckCounts = new int[phases.size()];
      for (int i=0; i<stuckCounts.length; i++)
        stuckCounts[i]=0;

      for (ProcessInstance pi : gProc.processes) {
        for (int i = 0; i < phases.size(); i++) {
          Phase p = phases.get(i);
          if (p.startDate.isAfter(currentDay))
            break;
          LocalDate completionDate = pi.getPhaseCompletenessDate(p.name);

          // If process hasn't finished this phase yet, or finished it after today
          if (completionDate != null && completionDate.isAfter(currentDay)) {
            stuckCounts[i]++;
            break; // Stop at the first incomplete phase
          }
        }
      }

      // Calculate X coordinate for this day
      int x = getXForDate(currentDay, width);

      // Draw the stacked bar from bottom to top
      int currentY = yBottom;
      for (int i = 0; i < stuckCounts.length; i++) {
        if (stuckCounts[i] == 0) continue;

        int segmentHeight = (int) (stuckCounts[i] * pixelsPerProcess);

        // Get the opaque version of the phase color
        Color phaseColor = getPhaseColor(i);
        Color opaqueColor = new Color(phaseColor.getRed(), phaseColor.getGreen(), phaseColor.getBlue(), 255);

        g2d.setColor(opaqueColor);
        g2d.fillRect(x - barWidth / 2, currentY - segmentHeight, barWidth, segmentHeight);

        // Optional: thin border for segment separation
        g2d.setColor(new Color(0, 0, 0, 40));
        g2d.drawRect(x - barWidth / 2, currentY - segmentHeight, barWidth, segmentHeight);

        currentY -= segmentHeight;
      }
    }
  }

  /**
   * Maps a LocalDate to an X coordinate based on the panel width.
   */
  private int getXForDate(LocalDate date, int width) {
    LocalDateTime dateTime = date.atStartOfDay();
    long secondsFromStart = ChronoUnit.SECONDS.between(minDate, dateTime);
    return (int) ((secondsFromStart * width) / (double) totalDuration);
  }

  @Override
  public String getToolTipText(Point pt) {
    // Reuse phase tooltip logic from parent
    String baseTip = super.getToolTipText(pt);

    // Add specific data info if hovering over a specific day
    int width = getWidth();
    double ratio = (double) pt.x / width;
    long offsetSeconds = (long) (ratio * totalDuration);
    LocalDate hoverDate = minDate.plusSeconds(offsetSeconds).toLocalDate();

    String dateInfo = "<br>Date: <b>" + hoverDate.format(formatter) + "</b>";

    return baseTip == null ? "<html>" + dateInfo + "</html>" : baseTip.replace("</html>", dateInfo + "</html>");
  }
}