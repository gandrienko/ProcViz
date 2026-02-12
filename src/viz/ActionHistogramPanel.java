package viz;

import structures.Phase;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class ActionHistogramPanel extends TimelinePanel {
  private Map<LocalDate, Integer> dayCounts;
  private int maxCount;

  public ActionHistogramPanel(java.util.List<Phase> phases, Map<LocalDate, Integer> dayCounts, int maxCount) {
    super(phases);
    this.dayCounts = dayCounts;
    this.maxCount = maxCount;
    setPreferredSize(new Dimension(800, 80));
    setToolTipText(""); // Required to enable the Swing tooltip system
  }

  public void setMaxCount(int maxCount) {
    this.maxCount = maxCount;
    repaint();
  }

  public Map<LocalDate, Integer> getDayCounts() {
    return dayCounts;
  }

  @Override
  protected void paintComponent(Graphics g) {
    // 1. Draw phase backgrounds
    super.paintComponent(g);

    if (dayCounts == null || dayCounts.isEmpty() || maxCount <= 0) {
      drawScaleLabels(g);
      return;
    }

    Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    int width = getWidth();
    int height = getHeight();
    long totalSeconds = ChronoUnit.SECONDS.between(minDate, maxDate);

    // 2. Draw Histogram Bars
    g2d.setColor(new Color(60, 60, 60, 220));
    for (Map.Entry<LocalDate, Integer> entry : dayCounts.entrySet()) {
      LocalDateTime dayStart = entry.getKey().atStartOfDay();
      LocalDateTime dayEnd = dayStart.plusDays(1);

      long startSec = ChronoUnit.SECONDS.between(minDate, dayStart);
      long endSec = ChronoUnit.SECONDS.between(minDate, dayEnd);

      int x1 = (int) ((startSec * width) / (double) totalSeconds);
      int x2 = (int) ((endSec * width) / (double) totalSeconds);
      int barWidth = Math.max(x2 - x1, 1);

      double ratio = (double) entry.getValue() / maxCount;
      int barHeight = (int) (ratio * (height - 2));

      g2d.fillRect(x1, height - barHeight, barWidth, barHeight);
    }

    // 3. Draw Scale Labels
    drawScaleLabels(g2d);
  }

  /**
   * Draws semi-transparent scale labels in the top corners.
   */
  private void drawScaleLabels(Graphics g) {
    if (maxCount <= 0) return;

    Graphics2D g2d = (Graphics2D) g;
    String scaleStr = String.valueOf(maxCount);

    g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
    FontMetrics fm = g2d.getFontMetrics();
    int tw = fm.stringWidth(scaleStr);
    int th = fm.getAscent(), fh=fm.getHeight();

    // Background rectangle for readability (semi-transparent white)
    g2d.setColor(new Color(255, 255, 255, 140));
    g2d.fillRect(2, 1, tw + 6, fh); // Left
    g2d.fillRect(getWidth() - tw - 8, 1, tw + 6, fh); // Right

    // Text (semi-transparent black)
    g2d.setColor(new Color(0, 0, 0, 200));
    g2d.drawString(scaleStr, 5, th + 1);
    g2d.drawString(scaleStr, getWidth() - tw - 5, th + 1);
  }

  @Override
  public String getToolTipText(MouseEvent event) {
    int width = getWidth();
    int height = getHeight();
    if (width <= 0 || maxCount <= 0) return super.getToolTipText(event);

    // Determine the date under the mouse
    long totalSeconds = ChronoUnit.SECONDS.between(minDate, maxDate);
    long secondsAtMouse = (long) ((event.getX() * (double)totalSeconds) / width);
    LocalDate dateAtMouse = minDate.plusSeconds(secondsAtMouse).toLocalDate();

    // Check if we are over a bar
    Integer count = dayCounts.get(dateAtMouse);
    boolean onBar = false;
    if (count != null && count > 0) {
      double ratio = (double) count / maxCount;
      int barHeight = (int) (ratio * (height - 2));
      if (event.getY() >= (height - barHeight)) {
        onBar = true;
      }
    }

    // Get phase info from parent
    String phaseInfo = super.getToolTipText(event);
    if (phaseInfo != null) {
      // Clean HTML tags to merge info
      phaseInfo = phaseInfo.replace("<html>", "").replace("</html>", "");
    }

    if (onBar) {
      // Use the formatter from TimelinePanel for consistency
      String dateStr = dateAtMouse.format(formatter);
      return String.format("<html>Date: <b>%s</b><br>Count: <b>%d</b><hr>%s</html>",
          dateStr, count, (phaseInfo != null ? phaseInfo : ""));
    } else {
      return (phaseInfo != null) ? "<html>" + phaseInfo + "</html>" : null;
    }
  }
}