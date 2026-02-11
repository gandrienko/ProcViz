package viz;

import javax.swing.*;
import java.awt.*;

public class CollapsibleActionSection extends JPanel {
  private String actionName;
  private ActionHistogramPanel histogram;
  private JButton toggleButton;

  public CollapsibleActionSection(String actionName, ActionHistogramPanel histogram) {
    this.actionName = actionName;
    this.histogram = histogram;
    setLayout(new BorderLayout());

    toggleButton = new JButton("▼ " + actionName);
    toggleButton.setHorizontalAlignment(SwingConstants.LEFT);
    toggleButton.setFocusPainted(false);

    toggleButton.addActionListener(e -> {
      boolean isVisible = histogram.isVisible();
      histogram.setVisible(!isVisible);
      toggleButton.setText((isVisible ? "► " : "▼ ") + actionName);
      revalidate();
    });

    add(toggleButton, BorderLayout.NORTH);
    add(histogram, BorderLayout.CENTER);
    setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
  }
}