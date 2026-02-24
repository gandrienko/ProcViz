package viz;

import structures.GlobalProcess;
import structures.TaskContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

public class ActionOverviewPanel extends JPanel {
  private SelectionManager selectionManager=null;
  private List<CollapsibleActionSection> sections = new ArrayList<>();
  private int globalMax = 0;
  private int mode = 1; // 1: common, 2: individual, 3: common visible
  private JPanel contentPanel; // The panel inside the scroll pane

  public ActionOverviewPanel(GlobalProcess gProc, SelectionManager selectionManager) {
    this.selectionManager=selectionManager;
    // Use BorderLayout to separate ScrollPane from ControlPanel
    setLayout(new BorderLayout());

    Map<String, Map<LocalDate, Integer>> data = gProc.getActionCountsByDay();

    // --- Logic for Sorting (remains same) ---
    List<ActionPeakInfo> sortedActions = new ArrayList<>();
    for (String type : data.keySet()) {
      int localMax = 0;
      LocalDate peakDate = null;
      for (Map.Entry<LocalDate, Integer> entry : data.get(type).entrySet()) {
        if (entry.getValue() > localMax) {
          localMax = entry.getValue();
          peakDate = entry.getKey();
        }
      }
      if (localMax > globalMax) globalMax = localMax;
      if (peakDate != null) sortedActions.add(new ActionPeakInfo(type, peakDate, localMax));
    }
    Collections.sort(sortedActions, (a, b) -> {
      int dateComp = a.peakDate.compareTo(b.peakDate);
      if (dateComp != 0) return dateComp;
      if (a.localMax!=b.localMax)
        return (a.localMax<b.localMax)?1:-1;
      return a.actionName.compareTo(b.actionName); // Sub-sort by name
    });

    // --- Content Panel Setup ---
    contentPanel = new JPanel();
    contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

    Map<String, Map<LocalDate, List<TaskContext>>> allTasksByDays=gProc.getTasksByActionAndDay();

    for (ActionPeakInfo info : sortedActions) {
      Map<LocalDate, List<TaskContext>> tasksByDays = allTasksByDays.get(info.actionName);
      if (tasksByDays==null || tasksByDays.isEmpty())
        continue;;
      ActionHistogramPanel hist = new ActionHistogramPanel(
          gProc.getListOfPhases(), globalMax,tasksByDays,selectionManager);

      // Callback now includes a call to updateLayout to remove empty space
      CollapsibleActionSection section = new CollapsibleActionSection(info.actionName, hist, () -> {
        updateScaling();
        updateLayout();
      });

      sections.add(section);
      contentPanel.add(section);
    }

    // Use a wrapper panel with BorderLayout.NORTH to prevent vertical stretching
    // of sections if the list is short
    JPanel topWrapper = new JPanel(new BorderLayout());
    topWrapper.add(contentPanel, BorderLayout.NORTH);

    JScrollPane scrollPane = new JScrollPane(topWrapper);
    scrollPane.getVerticalScrollBar().setUnitIncrement(16);

    // 1) Add ScrollPane to CENTER
    add(scrollPane, BorderLayout.CENTER);

    // 2) Add ControlPanel to SOUTH (Fixed at bottom)
    add(createControlPanel(), BorderLayout.SOUTH);

    if (selectionManager!=null) {
      selectionManager.addProcessListener(() -> {
        if (sections.get(0).getFilterMode()==ActionHistogramPanel.SHOW_SELECTED) {
          for (CollapsibleActionSection section:sections)
            section.determineLocalMax();
          determineGlobalMax();
          updateScaling();
        }
      });
    }
  }

  private void updateLayout() {
    // Force the UI to recalculate sizes and move panels up
    contentPanel.revalidate();
    contentPanel.repaint();
  }

  private JPanel createControlPanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));

    JRadioButton commonBtn = new JRadioButton("Common for all", mode == 1);
    JRadioButton individualBtn = new JRadioButton("Individual", mode == 2);
    JRadioButton visibleBtn = new JRadioButton("Common for visible", mode == 3);

    ButtonGroup group = new ButtonGroup();
    group.add(commonBtn); group.add(individualBtn); group.add(visibleBtn);

    commonBtn.addActionListener(e -> { mode = 1; updateScaling(); });
    individualBtn.addActionListener(e -> { mode = 2; updateScaling(); });
    visibleBtn.addActionListener(e -> { mode = 3; updateScaling(); });

    panel.add(new JLabel("Scale Mode: "));
    panel.add(commonBtn); panel.add(individualBtn); panel.add(visibleBtn);

    // --- Filter Mode Controls ---
    JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JRadioButton allButton = new JRadioButton("all");
    JRadioButton selectedButton = new JRadioButton("selected only");
    ButtonGroup filterGroup = new ButtonGroup();
    filterGroup.add(allButton);
    filterGroup.add(selectedButton);

    if (sections.get(0).getFilterMode() == ProcessTimelinePanel.SHOW_ALL)
      allButton.setSelected(true);
    else
      selectedButton.setSelected(true);

    filterPanel.add(new JLabel("View:"));
    filterPanel.add(allButton);
    filterPanel.add(selectedButton);
    panel.add(filterPanel); // Adding to the central control panel

    allButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        for (CollapsibleActionSection section:sections)
          section.setFilterMode(ActionHistogramPanel.SHOW_ALL);
        determineGlobalMax();
        updateScaling();
      }
    });
    selectedButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        for (CollapsibleActionSection section:sections) {
          section.setFilterMode(ActionHistogramPanel.SHOW_SELECTED);
          section.determineLocalMax();
        }
        determineGlobalMax();
        updateScaling();
      }
    });

    panel.add(new JSeparator(JSeparator.VERTICAL));
    JButton propagateBtn = new JButton("Select Processes of Tasks");
    propagateBtn.setToolTipText("Select process instances containing currently selected tasks");

    propagateBtn.addActionListener(e -> {
      if (selectionManager == null) return;

      Set<String> processIdsToSelect = new HashSet<>();
      for (CollapsibleActionSection section : sections) {
        // Collect IDs from each histogram panel
        processIdsToSelect.addAll(section.getHistogramPanel().getProcessIdsForSelectedTasks());
      }

      if (!processIdsToSelect.isEmpty()) {
        // Toggle the collected IDs in the SelectionManager
        selectionManager.toggleProcesses(processIdsToSelect);
      }
    });

    panel.add(propagateBtn);
    return panel;
  }

  public void determineGlobalMax () {
    globalMax=0;
    for (CollapsibleActionSection sec:sections)
      globalMax=Math.max(globalMax,sec.getLocalMax());
    for (CollapsibleActionSection sec:sections)
      sec.getHistogramPanel().setMaxCount(globalMax);
  }

  public void updateScaling() {
    int currentVisibleMax = 0;
    if (mode == 3) {
      for (CollapsibleActionSection sec : sections) {
        if (sec.isExpanded()) currentVisibleMax = Math.max(currentVisibleMax, sec.getLocalMax());
      }
    }

    for (CollapsibleActionSection sec : sections) {
      int targetMax = (mode == 1) ? globalMax : (mode == 2 ? sec.getLocalMax() : currentVisibleMax);
      sec.getHistogramPanel().setMaxCount(targetMax);
    }
  }

  private static class ActionPeakInfo {
    String actionName; LocalDate peakDate; int localMax;
    ActionPeakInfo(String n, LocalDate d, int m) { actionName = n; peakDate = d; localMax = m; }
  }

  public int getScrollbarWidth() {
    for (Component c:getComponents())
      if (c instanceof JScrollPane)
        return ((JScrollPane)c).getVerticalScrollBar().getPreferredSize().width+3;
    return 0;
  }
}