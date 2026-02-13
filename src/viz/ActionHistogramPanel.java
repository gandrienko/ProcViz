package viz;

import structures.Phase;
import structures.TaskInstance;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

public class ActionHistogramPanel extends TimelinePanel {
  private Map<LocalDate, Integer> dayCounts=null;
  private Map<LocalDate, List<TaskInstance>> tasksByDays=null;
  private SelectionManager selectionManager=null;
  private int maxCount, maxCountBarHeight=0;
  // Variables to track drag state
  private Point dragStartPoint = null;

  public ActionHistogramPanel(java.util.List<Phase> phases,
                              Map<LocalDate, Integer> dayCounts,
                              int maxCount,
                              Map<LocalDate, List<TaskInstance>> tasksByDays,
                              SelectionManager selectionManager) {
    super(phases);
    this.dayCounts=dayCounts;
    this.tasksByDays = tasksByDays;
    this.maxCount = maxCount;
    this.selectionManager=selectionManager;
    setPreferredSize(new Dimension(800, 80));
    setToolTipText(""); // Required to enable the Swing tooltip system

    MouseAdapter dragListener = new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        if (maxCountBarHeight<=0)
          return;
        dragStartPoint = e.getPoint();
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        if (dragStartPoint == null || maxCountBarHeight<=0) return;

        Point dragEndPoint = e.getPoint();

        // 2. Identify the date range
        LocalDate date1 = getDateAtX(dragStartPoint.x);
        LocalDate date2 = getDateAtX(dragEndPoint.x);

        if (date1.equals(date2)) {
          int countAtMouse=(int)Math.round((double)(maxCountBarHeight-e.getY())/maxCountBarHeight*maxCount);

          // 2. Fetch tasks for this action/date
          List<TaskInstance> tasks = tasksByDays.get(date1);
          if (tasks != null && tasks.size()>=countAtMouse) {
            selectionManager.toggleTasks(tasks);
            repaint();
          }
          else if (e.getClickCount() == 2) {
            // Clicked background
            if (selectionManager.hasSelection()) {
              selectionManager.clearSelection();
              repaint();
            }
          }
        }
        else {

          LocalDate startDate = date1.isBefore(date2) ? date1 : date2;
          LocalDate endDate = date1.isAfter(date2) ? date1 : date2;

          // 3. Collect all tasks within this date range
          List<TaskInstance> tasksToToggle = new ArrayList<TaskInstance>();

          // We iterate through our data map to find dates in the range
          for (Map.Entry<LocalDate, List<TaskInstance>> entry : tasksByDays.entrySet()) {
            LocalDate d = entry.getKey();
            if ((d.isEqual(startDate) || d.isAfter(startDate)) &&
                (d.isEqual(endDate) || d.isBefore(endDate))) {
              tasksToToggle.addAll(entry.getValue());
            }
          }

          // 4. Update the SelectionManager
          if (!tasksToToggle.isEmpty()) {
            selectionManager.toggleTasks(tasksToToggle);
            repaint();
          }
        }

        dragStartPoint = null; // Reset
      }

      public void mouseClicked(MouseEvent e) {
        // 1. Find the date under the mouse
        LocalDate dateAtMouse = getDateAtX(e.getX());

        // 2. Fetch tasks for this action/date
        List<TaskInstance> tasks = tasksByDays.get(dateAtMouse);
        if (tasks != null) {
          selectionManager.toggleTasks(tasks);
          repaint();
        }
        else if (e.getClickCount() == 2) {
          // Clicked background
          if (selectionManager.hasSelection()) {
            selectionManager.clearSelection();
            repaint();
          }
        }
      }
    };

    this.addMouseListener(dragListener);
    this.addMouseMotionListener(dragListener);

  }

  public void setMaxCount(int maxCount) {
    this.maxCount = maxCount;
    repaint();
  }

  public Map<LocalDate, Integer> getDayCounts() {
    return dayCounts;
  }

  private LocalDate getDateAtX(int x) {
    // Ensure x is within bounds
    x = Math.max(0, Math.min(x, getWidth()));

    long totalSeconds = java.time.temporal.ChronoUnit.SECONDS.between(minDate, maxDate);
    long secondsAtX = (long) ((x * (double) totalSeconds) / getWidth());
    return minDate.plusSeconds(secondsAtX).toLocalDate();
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
    maxCountBarHeight=height-2;
    long totalSeconds = ChronoUnit.SECONDS.between(minDate, maxDate);

    // 2. Draw Histogram Bars
    g2d.setColor(new Color(120, 120, 120, 220));
    for (Map.Entry<LocalDate, Integer> entry : dayCounts.entrySet()) {
      LocalDateTime dayStart = entry.getKey().atStartOfDay();
      LocalDateTime dayEnd = dayStart.plusDays(1);

      long startSec = ChronoUnit.SECONDS.between(minDate, dayStart);
      long endSec = ChronoUnit.SECONDS.between(minDate, dayEnd);

      int x1 = (int) ((startSec * width) / (double) totalSeconds);
      int x2 = (int) ((endSec * width) / (double) totalSeconds);
      int barWidth = Math.max(x2 - x1, 1);

      double ratio = (double) entry.getValue() / maxCount;
      int barHeight = (int) (ratio * maxCountBarHeight);

      g2d.fillRect(x1, height - barHeight, barWidth, barHeight);

      if (selectionManager!=null && selectionManager.hasSelection()) {
        List<TaskInstance> tasks = tasksByDays.get(dayStart.toLocalDate());
        if (tasks!=null && !tasks.isEmpty()) {
          int nSelected=0;
          for (TaskInstance task:selectionManager.getSelectedTasks())
            if (tasks.contains(task)) ++nSelected;
          if (nSelected>0) {
            double selRatio=(double)nSelected/maxCount;
            int selHeight = (int) (selRatio * maxCountBarHeight);
            Color prevColor=g2d.getColor();
            g2d.setColor(new Color(60, 60, 60, 255));
            g2d.fillRect(x1, height - selHeight, barWidth, selHeight);
            g2d.setColor(prevColor);
          }
        }
      }
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