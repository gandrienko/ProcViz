package viz;

import structures.Phase;
import java.awt.*;
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
    setPreferredSize(new Dimension(800, 100)); // Default height for unfolded state
  }

  @Override
  protected void paintComponent(Graphics g) {
    // 1. Draw the background phases using TimelinePanel logic
    super.paintComponent(g);

    if (dayCounts == null || dayCounts.isEmpty() || maxCount == 0) return;

    Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    int width = getWidth();
    int height = getHeight();
    long totalSeconds = ChronoUnit.SECONDS.between(minDate, maxDate);

    g2d.setColor(new Color(50, 50, 50, 200)); // Dark gray for bars

    for (Map.Entry<LocalDate, Integer> entry : dayCounts.entrySet()) {
      LocalDateTime dayStart = entry.getKey().atStartOfDay();
      LocalDateTime dayEnd = dayStart.plusDays(1);

      long startSec = ChronoUnit.SECONDS.between(minDate, dayStart);
      long endSec = ChronoUnit.SECONDS.between(minDate, dayEnd);

      int x1 = (int) ((startSec * width) / (double) totalSeconds);
      int x2 = (int) ((endSec * width) / (double) totalSeconds);
      int barWidth = Math.max(x2 - x1, 2);

      // Calculate height based on maxCount
      double ratio = entry.getValue() / (double) maxCount;
      int barHeight = (int) (ratio * (height - 10));

      g2d.fillRect(x1, height - barHeight, barWidth, barHeight);
    }
  }
}