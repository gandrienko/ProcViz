package viz;

import structures.Phase;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TimelinePanel extends JPanel {
  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  // Pastel colors for phases
  public static Color[] pastelColors = {
      new Color(179, 205, 227),
      new Color(251, 180, 174),
      new Color(204, 235, 197),
      new Color(222, 203, 228),
      new Color(254, 217, 166),
      new Color(255, 255, 204),
      new Color(229, 216, 189)
  };

  public List<Phase> phases=null;


  public TimelinePanel(List<Phase> phases) {
    this.phases = phases;
    setPreferredSize(new Dimension(1000, 200));
    setBackground(Color.WHITE);
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (phases == null || phases.isEmpty()) return;

    Graphics2D g2d = (Graphics2D) g;
    int width = getWidth();
    int height = getHeight();

    LocalDate minDate = phases.get(0).startDate;
    LocalDate maxDate = phases.get(phases.size() - 1).endDate;
    long totalDays = minDate.until(maxDate).getDays();

    int labelY = 30;
    int sectionHeight = height - 60;


    for (int i = 0; i < phases.size(); i++) {
      Phase p = phases.get(i);
      long daysFromStart = minDate.until(p.startDate).getDays();
      long phaseDuration = p.startDate.until(p.endDate).getDays();

      int x = (int) ((daysFromStart * width) / (double) totalDays);
      int w = (int) ((phaseDuration * width) / (double) totalDays);

      g2d.setColor(pastelColors[i % pastelColors.length]);
      g2d.fillRect(x, labelY, w, sectionHeight);

      g2d.setColor(Color.BLACK);
      g2d.drawRect(x, labelY, w, sectionHeight);
      g2d.drawString(p.name, x + 5, labelY + 15);
    }

    // Draw timeline axis
    g2d.setColor(Color.BLACK);
    g2d.drawLine(0, labelY + sectionHeight, width, labelY + sectionHeight);

    for (Phase p : phases) {
      int x = (int) ((minDate.until(p.startDate).getDays() * width) / (double) totalDays);
      g2d.drawLine(x, labelY + sectionHeight, x, labelY + sectionHeight + 5);
      g2d.drawString(p.startDate.format(formatter), x + 2, labelY + sectionHeight + 20);
    }
  }
}
