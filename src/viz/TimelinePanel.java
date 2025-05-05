package viz;

import structures.Phase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimelinePanel extends JPanel {
  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  // Pastel colors for phases
  public static Color[] pastelColors = {
      new Color(255, 179, 186), // light pink
      new Color(255, 223, 186), // peach
      new Color(255, 255, 186), // light yellow
      new Color(186, 255, 201), // light green
      new Color(186, 225, 255), // light blue
      new Color(218, 186, 255), // light purple
      new Color(255, 204, 229), // rose
      new Color(204, 255, 229), // mint
      new Color(229, 204, 255), // lavender
      new Color(255, 250, 205), // lemon chiffon
      new Color(240, 255, 240), // honeydew
      new Color(255, 239, 213)  // papaya whip
  };

  public List<Phase> phases=null;
  private Map<Rectangle, Phase> phaseAreas = null;
  private JToolTip toolTip=null;

  public TimelinePanel(List<Phase> phases) {
    this.phases = phases;
    setPreferredSize(new Dimension(1000, 200));
    setBackground(Color.WHITE);
    this.toolTip = new JToolTip();
    setToolTipText(""); // Enables tooltip mechanism
    addMouseMotionListener(new MouseMotionAdapter() {
      public void mouseMoved(MouseEvent e) {
        if (phaseAreas!=null)
          for (Map.Entry<Rectangle, Phase> entry : phaseAreas.entrySet()) {
            if (entry.getKey().contains(e.getPoint())) {
              Phase phase = entry.getValue();
              long duration = ChronoUnit.DAYS.between(phase.startDate, phase.endDate);
              String text = String.format("<html><b>%s</b><br>Start: %s<br>End: %s<br>Duration: %d days</html>",
                  phase.name,
                  phase.startDate.format(formatter),
                  phase.endDate.format(formatter),
                  duration);
              setToolTipText(text);
              return;
            }
          }
        setToolTipText(null);
      }
    });
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
    long totalDays = ChronoUnit.DAYS.between(minDate, maxDate);;

    int fontHeight=g2d.getFontMetrics().getHeight();
    int yTop = 5, yBottom=height - fontHeight-5;
    int sectionHeight = yBottom-yTop;
    
    if (phaseAreas==null)
      phaseAreas=new HashMap<Rectangle,Phase>(phases.size());
    else
      phaseAreas.clear();

    for (int i = 0; i < phases.size(); i++) {
      Phase p = phases.get(i);
      long daysFromStart1 = ChronoUnit.DAYS.between(minDate,p.startDate);
      long daysFromStart2 = ChronoUnit.DAYS.between(minDate,p.endDate);

      int x1 = (int) ((daysFromStart1 * width) / (double) totalDays);
      int x2 = (int) ((daysFromStart2 * width) / (double) totalDays);
  
      g2d.setColor(Color.white);
      g2d.fillRect(x1, yTop, width-x1+1, sectionHeight);
      
      g2d.setColor(pastelColors[i % pastelColors.length]);
      g2d.fillRect(x1, yTop, x2-x1+1, sectionHeight);

      g2d.setColor(Color.BLACK);
      g2d.drawRect(x1, yTop, x2-x1, sectionHeight);
      Rectangle r=new Rectangle(x1,yTop,x2-x1,sectionHeight);
      phaseAreas.put(r,p);
      g2d.drawString(p.name, x1 + 5, yTop + fontHeight);
    }

    // Draw timeline axis
    g2d.setColor(Color.BLACK);
    g2d.drawLine(0, yTop + sectionHeight, width, yTop + sectionHeight);

    for (Phase p : phases) {
      int x = (int) ((ChronoUnit.DAYS.between(minDate,p.startDate) * width) / (double) totalDays);
      g2d.setColor(Color.white);
      g2d.fillRect(x-5,yBottom + 1,width-x+5,fontHeight);
      g2d.setColor(Color.BLACK);
      g2d.drawLine(x, yBottom, x, yBottom + 5);
      g2d.drawString(p.startDate.format(formatter), x + 2, yBottom+fontHeight);
    }
    String endStr=maxDate.format(formatter);
    int x=width-g2d.getFontMetrics().stringWidth(endStr)-3;
    g2d.setColor(Color.white);
    g2d.fillRect(x-3,yTop + sectionHeight + 1,width-x+3,height-sectionHeight-1);
    g2d.setColor(Color.BLACK);
    g2d.drawString(endStr,x,yBottom+fontHeight);
    g2d.drawLine(width-1, yBottom, width-1, yBottom + 5);
  }
}
