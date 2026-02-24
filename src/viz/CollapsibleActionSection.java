package viz;

import javax.swing.*;
import java.awt.*;

public class CollapsibleActionSection extends JPanel {
  private ActionHistogramPanel histogram;
  private int localMax;
  private Runnable onToggle;
  private JButton toggleButton;

  public CollapsibleActionSection(String name, ActionHistogramPanel hist, Runnable onToggle) {
    this.histogram = hist;
    this.onToggle = onToggle;
    determineLocalMax();

    setLayout(new BorderLayout());

    toggleButton = new JButton("▼ " + name);
    toggleButton.setHorizontalAlignment(SwingConstants.LEFT);
    toggleButton.setBackground(new Color(240, 240, 240));
    toggleButton.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));

    toggleButton.addActionListener(e -> {
      boolean visible = !histogram.isVisible();
      histogram.setVisible(visible);
      toggleButton.setText((visible ? "▼ " : "► ") + name);

      // KEY FIX: When invisible, set preferred size to 0 to collapse space
      if (!visible) {
        histogram.setPreferredSize(new Dimension(histogram.getPreferredSize().width, 0));
      } else {
        // Return to original height (e.g., 80)
        histogram.setPreferredSize(new Dimension(histogram.getPreferredSize().width, 80));
      }

      if (onToggle != null) onToggle.run();
    });

    add(toggleButton, BorderLayout.NORTH);
    add(histogram, BorderLayout.CENTER);
  }

  // Getters for the parent to use
  public boolean isExpanded() { return histogram.isVisible(); }
  public int getLocalMax() { return localMax; }
  public ActionHistogramPanel getHistogramPanel() { return histogram; }


  public int getFilterMode() {
    return histogram.getFilterMode();
  }

  public void setFilterMode(int filterMode) {
    if (histogram.getFilterMode()==filterMode)
      return;
    histogram.setFilterMode(filterMode);
    determineLocalMax();
  }

  public int determineLocalMax() {
    localMax = 0;
    if (histogram.getDayCounts()!=null)
      for (int val : histogram.getDayCounts().values()) localMax = Math.max(localMax, val);
    return localMax;
  }

}