package viz;

import structures.Phase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimelinePanel extends JPanel {
  public final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  // Pastel colors for phases
  public static Color[] pastelColors = {
      new Color(186, 186, 255,96), // light purple
      new Color(255, 210, 160, 96), // peach
      new Color(160, 255, 190, 96), // light green
      new Color(255, 255, 160, 96), // light yellow
      new Color(255, 180, 180, 96), // light pink
      new Color(186, 225, 255, 96), // light blue
      new Color(139, 224, 164, 96), //
      new Color(255, 160, 216, 96), // rose
      new Color(201, 219, 116, 96), //
      new Color(229, 180, 255, 96), // lavender
      new Color(246, 207, 113, 96), //
      new Color(180, 255, 228, 96), // mint
      new Color(248, 156, 116, 96), //
      new Color(158, 185, 243, 96), //
      new Color(211, 180, 132, 96), //
      new Color(180, 151, 231, 96),  //
      new Color(254, 136, 177, 96) //
  };

  public List<Phase> phases=null;
  protected LocalDateTime minDate=null, maxDate=null;
  protected long totalDuration=0l;
  
  protected int fontHeight=0, yTop=0, yBottom=0, sectionHeight=0;
  
  protected Map<Rectangle, Phase> phaseAreas = null;
  protected Map<String,Color> phaseColors=null;
  protected JToolTip toolTip=null;

  public TimelinePanel(List<Phase> phases) {
    this.phases = phases;
  
    minDate = phases.get(0).startDate.atStartOfDay();
    maxDate = phases.get(phases.size() - 1).endDate.atTime(23,59,59);
    totalDuration=ChronoUnit.SECONDS.between(minDate,maxDate);
    
    phaseColors=new HashMap<String,Color>(phases.size());
    for (int i = 0; i < phases.size(); i++) {
      Phase p = phases.get(i);
      phaseColors.put(p.name,pastelColors[i % pastelColors.length]);
    }
    
    setPreferredSize(new Dimension(1000, 200));
    setBackground(Color.WHITE);

    this.toolTip = new JToolTip();
    setToolTipText(""); // Enables tooltip mechanism
    addMouseMotionListener(new MouseMotionAdapter() {
      public void mouseMoved(MouseEvent e) {
        setToolTipText(getToolTipText(e.getPoint()));
      }
    });
  }

  public Phase getPhaseForPoint(Point pt) {
    if (pt==null)
      return null;
    if (phaseAreas!=null)
      for (Map.Entry<Rectangle, Phase> entry : phaseAreas.entrySet()) {
        if (entry.getKey().contains(pt))
          return entry.getValue();
      }
    return null;
  }
  
  public String getToolTipText(Point pt) {
    Phase phase = getPhaseForPoint(pt);
    if (phase==null)
      return null;
    long duration = ChronoUnit.DAYS.between(phase.startDate, phase.endDate)+1;
    String text = String.format("<html><b>%s</b><br>Start: %s<br>End: %s<br>Duration: %d days</html>",
        phase.name,
        phase.startDate.format(formatter),
        phase.endDate.format(formatter),
        duration);
    return text;
  }

  public Color getPhaseColor(int idx) {
    if (phaseColors==null)
      return null;
    return phaseColors.get(phases.get(idx).name);
  }

  public void paintPhaseNames(Graphics g, int textHeight) {
    if (phases == null || phases.isEmpty()) return;

    Graphics2D g2d = (Graphics2D) g;
    int width = getWidth();
    int y0=(textHeight-g2d.getFontMetrics().getHeight())/2;

    for (int i = 0; i < phases.size(); i++) {
      Phase p = phases.get(i);
      LocalDateTime t1=p.startDate.atStartOfDay(),
          t2=p.endDate.atTime(23,59,59);
      long secondsFromStart = ChronoUnit.SECONDS.between(minDate,t1);
      long secondsFromStart2 = ChronoUnit.SECONDS.between(minDate,t2);

      int x1 = (int) ((secondsFromStart * width) / (double) totalDuration);
      int x2 = (int) ((secondsFromStart2 * width) / (double) totalDuration);

      g2d.setColor(Color.white);
      g2d.fillRect(x1, y0, width-x1+1, textHeight);

      g2d.setColor(phaseColors.get(p.name));
      g2d.fillRect(x1, y0, x2-x1+1, textHeight);

      g2d.setColor(Color.lightGray);
      g2d.drawRect(x1, y0, x2-x1, textHeight);
      g2d.setColor(Color.BLACK);
      g2d.drawString(p.name, x1 + 5, y0 + g2d.getFontMetrics().getAscent());
    }
  }

  public void paintPhaseDates(Graphics g, int textHeight) {
    if (phases == null || phases.isEmpty()) return;

    Graphics2D g2d = (Graphics2D) g;
    int width = getWidth();
    int y0=(textHeight-g2d.getFontMetrics().getHeight())/2;

    for (int i = 0; i < phases.size(); i++) {
      Phase p = phases.get(i);
      LocalDateTime t1=p.startDate.atStartOfDay(),
          t2=p.endDate.atTime(23,59,59);
      long secondsFromStart = ChronoUnit.SECONDS.between(minDate,t1);
      long secondsFromStart2 = ChronoUnit.SECONDS.between(minDate,t2);

      int x1 = (int) ((secondsFromStart * width) / (double) totalDuration);
      int x2 = (int) ((secondsFromStart2 * width) / (double) totalDuration);

      g2d.setColor(Color.white);
      g2d.fillRect(x1, y0, width-x1+1, textHeight);

      g2d.setColor(phaseColors.get(p.name));
      g2d.fillRect(x1, y0, x2-x1+1, textHeight);

      g2d.setColor(Color.lightGray);
      g2d.drawRect(x1, y0, x2-x1, textHeight);
      g2d.setColor(Color.BLACK);
      g2d.drawString(p.startDate.format(formatter), x1 + 2, y0 + g2d.getFontMetrics().getAscent());
    }
    //Draw end date
    String endStr=maxDate.format(formatter);
    int x=width-g2d.getFontMetrics().stringWidth(endStr)-3;
    Phase phase=getPhaseForPoint(new Point(x,y0));
    g2d.setColor((phase==null)?Color.white:phaseColors.get(phase.name));
    g2d.fillRect(x-3,y0,width-x+3,textHeight);
    g2d.setColor(Color.BLACK);
    g2d.drawString(endStr,x,y0 + g2d.getFontMetrics().getAscent());
    g2d.drawLine(width-1, y0, width-1, y0 + 5);

    // Draw timeline axis
    g2d.setColor(Color.gray);
    g2d.drawLine(0, 0, width, 0);
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (phases == null || phases.isEmpty()) return;

    Graphics2D g2d = (Graphics2D) g;
    int width = getWidth();
    int height = getHeight();

    fontHeight=g2d.getFontMetrics().getHeight();
    yTop = 0; yBottom=height; // - fontHeight-5;
    sectionHeight = yBottom-yTop;
    
    if (phaseAreas==null)
      phaseAreas=new HashMap<Rectangle,Phase>(phases.size());
    else
      phaseAreas.clear();

    for (int i = 0; i < phases.size(); i++) {
      Phase p = phases.get(i);
      LocalDateTime t1=p.startDate.atStartOfDay(),
          t2=p.endDate.atTime(23,59,59);
      long secondsFromStart = ChronoUnit.SECONDS.between(minDate,t1);
      long secondsFromStart2 = ChronoUnit.SECONDS.between(minDate,t2);

      int x1 = (int) ((secondsFromStart * width) / (double) totalDuration);
      int x2 = (int) ((secondsFromStart2 * width) / (double) totalDuration);
  
      g2d.setColor(Color.white);
      g2d.fillRect(x1, yTop, width-x1+1, sectionHeight);
      
      g2d.setColor(phaseColors.get(p.name));
      g2d.fillRect(x1, yTop, x2-x1+1, sectionHeight);

      g2d.setColor(Color.lightGray);
      g2d.drawRect(x1, yTop, x2-x1, sectionHeight);
      Rectangle r=new Rectangle(x1,yTop,x2-x1,sectionHeight);
      phaseAreas.put(r,p);

      /*
      g2d.setColor(Color.BLACK);
      g2d.drawString(p.name, x1 + 5, yTop + fontHeight);
  
      g2d.setColor(Color.white);
      g2d.fillRect(x1-5,yBottom + 1,width-x1+5,fontHeight);
      g2d.setColor(Color.BLACK);
      g2d.drawLine(x1, yBottom, x1, yBottom + 5);
      g2d.drawString(p.startDate.format(formatter), x1 + 2, yBottom+fontHeight);
      */
    }

    // Draw timeline axis
    g2d.setColor(Color.gray);
    g2d.drawLine(0, yTop + sectionHeight, width, yTop + sectionHeight);
    /*
    String endStr=maxDate.format(formatter);
    int x=width-g2d.getFontMetrics().stringWidth(endStr)-3;
    g2d.setColor(Color.white);
    g2d.fillRect(x-3,yTop + sectionHeight + 1,width-x+3,height-sectionHeight-1);
    g2d.setColor(Color.BLACK);
    g2d.drawString(endStr,x,yBottom+fontHeight);
    g2d.drawLine(width-1, yBottom, width-1, yBottom + 5);
    */
  }
}
