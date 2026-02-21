package viz;

import structures.GlobalProcess;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ProcessTimelineWithControls extends JPanel {
  private ProcessTimelinePanel processMainPanel;

  public ProcessTimelineWithControls(GlobalProcess gProc, SelectionManager selectionManager) {
    processMainPanel = new ProcessTimelinePanel(gProc,selectionManager);
    JPanel processPanel=new TimelineWithHeaderAndFooter(processMainPanel,true);

    setLayout(new BorderLayout());
    add(processPanel,BorderLayout.CENTER);
    JPanel controlPanel = new JPanel();
    controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 30,0));
    add(controlPanel, BorderLayout.SOUTH);

    JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JRadioButton processesButton = new JRadioButton("processes");
    JRadioButton actorsButton = new JRadioButton("actors");
    // Group them
    ButtonGroup group = new ButtonGroup();
    group.add(processesButton);
    group.add(actorsButton);
    // Set default
    if (processMainPanel.getMode()==ProcessTimelinePanel.PROCESS_MODE)
      processesButton.setSelected(true);
    else
      actorsButton.setSelected(true);

    modePanel.add(new JLabel("Grouping mode:"));
    modePanel.add(processesButton);
    modePanel.add(actorsButton);
    controlPanel.add(modePanel);

    // Add action listeners
    processesButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (processesButton.isSelected())
          processMainPanel.setMode(ProcessTimelinePanel.PROCESS_MODE);
      }
    });
    actorsButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (actorsButton.isSelected())
          processMainPanel.setMode(ProcessTimelinePanel.ACTOR_MODE);
      }
    });

    modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JRadioButton dotsButton = new JRadioButton("dots");
    JRadioButton charsButton = new JRadioButton("characters");
    // Group them
    group = new ButtonGroup();
    group.add(dotsButton);
    group.add(charsButton);
    // Set default
    if (processMainPanel.getSymbolMode()==ProcessTimelinePanel.SYMBOL_DOT)
      dotsButton.setSelected(true);
    else
      charsButton.setSelected(true);

    modePanel.add(new JLabel("Symbols for actions:"));
    modePanel.add(dotsButton);
    modePanel.add(charsButton);
    controlPanel.add(modePanel);

    // Add action listeners
    dotsButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (dotsButton.isSelected())
          processMainPanel.setSymbolMode(ProcessTimelinePanel.SYMBOL_DOT);
      }
    });
    charsButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (charsButton.isSelected())
          processMainPanel.setSymbolMode(ProcessTimelinePanel.SYMBOL_CHAR);
      }
    });
  }
}
