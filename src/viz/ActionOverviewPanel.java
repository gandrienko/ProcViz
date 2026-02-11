package viz;

import structures.GlobalProcess;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.Map;

public class ActionOverviewPanel extends JPanel {
  public ActionOverviewPanel(GlobalProcess gProc) {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    // Use your new method in GlobalProcess
    Map<String, Map<LocalDate, Integer>> data = gProc.getActionCountsByDay();

    // Find global max across all action types for consistent bar scale
    int globalMax = 0;
    for (Map<LocalDate, Integer> counts : data.values()) {
      for (int val : counts.values()) {
        if (val > globalMax) globalMax = val;
      }
    }

    // Add a section for each action type
    for (String actionType : data.keySet()) {
      ActionHistogramPanel hist = new ActionHistogramPanel(
          gProc.getListOfPhases(),
          data.get(actionType),
          globalMax
      );
      add(new CollapsibleActionSection(actionType, hist));
    }

    // Keeps everything at the top if there aren't many actions
    add(Box.createVerticalGlue());
  }
}