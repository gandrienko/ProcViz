package viz;

import structures.Phase;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class TimelineTextsPanel extends JPanel {
  public static final int SHOW_TITLES=1, SHOW_TIMES=2;

  protected TimelinePanel timeline=null;
  protected int mode=SHOW_TITLES;
  public static int fontHeight=0;

  public TimelineTextsPanel(TimelinePanel timeline, int mode) {
    this.timeline=timeline; this.mode=mode;
    getFontHeight();
    setPreferredSize(new Dimension(timeline.getPreferredSize().width,fontHeight+6));
  }

  public static void getFontHeight() {
    if (fontHeight==0) {
      BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g2d = img.createGraphics();
      FontMetrics metrics = g2d.getFontMetrics();
      fontHeight = metrics.getHeight();
      g2d.dispose();
    }
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (timeline==null)
      return;
    if (mode==SHOW_TITLES)
      timeline.paintPhaseNames(g,getHeight());
    else
      if (mode==SHOW_TIMES)
        timeline.paintPhaseDates(g,getHeight());
  }
}
