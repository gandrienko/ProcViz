import data.LogLoader;
import data.StatusChecker;
import structures.*;
import viz.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.*;
import java.util.*;

import static data.LogLoader.transformCsv;

public class Main {

    public static void main(String[] args) {

      String configFile = (args.length > 0) ? args[0] : "paths.txt";
      String phasesFilePath = null;
      String actionsMappingFilePath = null;
      String actionsEncodingFilePath = null;
      String actorsMappingFilePath = null;
      String logFilePath = null;

      try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
        String line;
        while ((line = reader.readLine()) != null) {
          line = line.trim();
          if (line.isEmpty() || line.startsWith("#")) continue; // skip empty or comment lines

          String[] parts = line.split("=", 2);
          if (parts.length < 2) continue;

          String key = parts[0].trim();
          String value = parts[1].trim().replaceAll("^\"|\"$", ""); // remove surrounding quotes

          switch (key) {
            case "phasesFilePath":
              phasesFilePath = value;
              break;
            case "actionsMappingFilePath":
              actionsMappingFilePath = value;
              break;
            case "actionsEncodingFilePath":
              actionsEncodingFilePath = value;
              break;
            case "actorsMappingFilePath":
              actorsMappingFilePath = value;
              break;
            case "logFilePath":
              logFilePath = value;
              break;
            default:
              System.out.println("Unknown key: " + key);
          }
        }
      } catch (IOException e) {
        System.err.println("Error reading configuration file: " + e.getMessage());
        return;
      }

      // Print to verify values (or use them as needed)
      System.out.println("phasesFilePath = " + phasesFilePath);
      System.out.println("actionsMappingFilePath = " + actionsMappingFilePath);
      System.out.println("actionsEncodingFilePath = " + actionsEncodingFilePath);
      System.out.println("actorsMappingFilePath = " + actorsMappingFilePath);
      System.out.println("logFilePath = " + logFilePath);

      LogLoader loader = new LogLoader();
      GlobalProcess gProc=new GlobalProcess();

      try {
        loader.loadPhaseTimetable(phasesFilePath);
        loader.loadActionPhaseMapping(actionsMappingFilePath);
        loader.loadActionToRolesMapping(actorsMappingFilePath);
        loader.loadActionEncodings(actionsEncodingFilePath);
        loader.loadLog(logFilePath);

        gProc.actionTypes=loader.getActionTypes();
        gProc.phases=loader.getPhases();
        gProc.actorRoles=loader.getActorRoles();
        gProc.actors=loader.getActors();

        System.out.println("Loaded action types:");
        for (String action : gProc.actionTypes.keySet()) {
            System.out.println(" - " + action);
        }

        System.out.println("Loaded phases:");
        for (String phase : gProc.phases.keySet()) {
          System.out.println(" - " + phase);
        }

        System.out.println("Action Type to Phase Mapping:");
        for (Map.Entry<String, ActionType> entry : gProc.actionTypes.entrySet()) {
          System.out.println(entry.getKey() + " => " + entry.getValue().phaseName);
        }

        System.out.println("Action Type to Actor Roles Mapping:");
        for (Map.Entry<String, ActionType> entry : gProc.actionTypes.entrySet()) {
          System.out.println(entry.getKey() + " => " + entry.getValue().actorRole);
        }
        /*
        if (loader.getActorRoles()!=null && !loader.getActorRoles().isEmpty()) {
          System.out.println("Loaded roles of actors:");
          for (String role : gProc.actorRoles) {
            System.out.println("ROLE \"" + role+"\":");
            Set<Actor> actors=gProc.getActorsByRole(role);
            for (Actor actor:actors)
              System.out.println(actor.id);
          }
        }
        */

        gProc.processes =loader.getProcesses();

        System.out.println("Total number of processes loaded: " + gProc.processes.size());

      } catch (IOException e) {
        System.err.println("Error reading the log file: " + e.getMessage());
      }
      if (gProc.phases!=null && !gProc.phases.isEmpty()) {
        StatusChecker checker=new StatusChecker(gProc);
        checker.determinePhaseCompletenessDates();

        ProcessTimelinePanel processMainPanel = new ProcessTimelinePanel(gProc,new SelectionManager());

        int origPrefWidth=processMainPanel.getPreferredSize().width;

        JFrame frame = new JFrame("Process Timeline");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(processMainPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        JScrollBar hScrollBar = scrollPane.getHorizontalScrollBar();
        JScrollBar vScrollBar = scrollPane.getVerticalScrollBar();

        JPanel topPanel=new TimelineTextsPanel(processMainPanel,TimelineTextsPanel.SHOW_TITLES);
        JPanel bottomPanel=new TimelineTextsPanel(processMainPanel,TimelineTextsPanel.SHOW_TIMES);

        JViewport topViewport = new JViewport(), bottomViewport = new JViewport();
        topViewport.setView(topPanel);
        bottomViewport.setView(bottomPanel);

        processMainPanel.addComponentListener(new ComponentAdapter() {
          @Override
          public void componentResized(ComponentEvent e) {
            super.componentResized(e);
            int width=processMainPanel.getPreferredSize().width;
            if (width>origPrefWidth && hScrollBar.isVisible()) {
              processMainPanel.setPreferredSize(new Dimension(origPrefWidth,processMainPanel.getHeight()));
              processMainPanel.setSize(origPrefWidth,processMainPanel.getHeight());
              scrollPane.revalidate();
            }
            if (width!=topPanel.getPreferredSize().width) {
              topPanel.setPreferredSize(new Dimension(width,topPanel.getHeight()));
              bottomPanel.setPreferredSize(new Dimension(width,bottomPanel.getHeight()));
            }
            width=processMainPanel.getWidth();
            topPanel.setSize(width,topPanel.getHeight());
            bottomPanel.setSize(width,bottomPanel.getHeight());
            topViewport.revalidate();
            bottomViewport.revalidate();
            int scrollX=(hScrollBar.isVisible())?hScrollBar.getValue():0;
            topViewport.setViewPosition(new Point(scrollX, 0));
            bottomViewport.setViewPosition(new Point(scrollX, 0));
          }
        });

        hScrollBar.addAdjustmentListener(e -> {
          int scrollX = e.getValue();
          topViewport.setViewPosition(new Point(scrollX, 0));
          bottomViewport.setViewPosition(new Point(scrollX, 0));
        });
        /*
        scrollPane.getViewport().addChangeListener(new ChangeListener() {
          @Override
          public void stateChanged(ChangeEvent e) {
            //topPanel.setSize(processMainPanel.getWidth(),topPanel.getHeight());
            //bottomPanel.setSize(processMainPanel.getWidth(),bottomPanel.getHeight());
            int scrollX=(hScrollBar.isVisible())?hScrollBar.getValue():0;
            topViewport.setViewPosition(new Point(scrollX, 0));
            bottomViewport.setViewPosition(new Point(scrollX, 0));
          }
        });
        */

        //int scrollbarWidth = UIManager.getInt("ScrollBar.width");
        int scrollbarWidth = vScrollBar.getPreferredSize().width+3;
        JPanel topContainer=new JPanel();
        topContainer.setLayout(new BorderLayout());
        topContainer.add(topViewport,BorderLayout.CENTER);
        topContainer.add(Box.createRigidArea(new Dimension(scrollbarWidth, 1)), BorderLayout.EAST);
        JPanel bottomContainer=new JPanel();
        bottomContainer.setLayout(new BorderLayout());
        bottomContainer.add(bottomViewport,BorderLayout.CENTER);
        bottomContainer.add(Box.createRigidArea(new Dimension(scrollbarWidth, 1)), BorderLayout.EAST);

        JPanel processPanel=new JPanel();
        processPanel.setLayout(new BorderLayout());
        processPanel.add(topContainer,BorderLayout.NORTH);
        processPanel.add(scrollPane, BorderLayout.CENTER);
        processPanel.add(bottomContainer,BorderLayout.SOUTH);

        processPanel.addComponentListener(new ComponentAdapter() {
          @Override
          public void componentResized(ComponentEvent e) {
            super.componentResized(e);
            int width=processMainPanel.getWidth();
            topPanel.setSize(width,topPanel.getHeight());
            bottomPanel.setSize(width,bottomPanel.getHeight());
            width-=scrollbarWidth;
            topViewport.setSize(width,topViewport.getHeight());
            bottomViewport.setSize(width,bottomViewport.getHeight());
            int scrollX=(hScrollBar.isVisible())?hScrollBar.getValue():0;
            topViewport.setViewPosition(new Point(scrollX, 0));
            bottomViewport.setViewPosition(new Point(scrollX, 0));
          }
        });

        frame.add(processPanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 30,0));
        frame.add(controlPanel, BorderLayout.SOUTH);

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

        frame.pack();
        frame.setSize(frame.getWidth(), 850);
        frame.setVisible(true);

        //temporarily off
        /*
        ActionOverviewPanel overviewPanel = new ActionOverviewPanel(gProc, processMainPanel.getSelectionManager());
        JFrame overviewFrame = new JFrame("Actions Overview");
        overviewFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        overviewFrame.add(overviewPanel);
        overviewFrame.setSize(1000, 600);
        overviewFrame.setVisible(true);
        */

        // 1. Initialize the panels
        PhaseCompletenessOverviewPanel completenessPanel = new PhaseCompletenessOverviewPanel(gProc);
        ActionOverviewPanel overviewPanel = new ActionOverviewPanel(gProc, processMainPanel.getSelectionManager());

        // 2. Create the Split Pane
        // VERTICAL_SPLIT places the first component on top and the second on the bottom
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, completenessPanel, overviewPanel);

        // 3. Configure Split Pane behavior
        splitPane.setDividerLocation(250); // Set initial height of the upper panel in pixels
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);

        // 4. Setup the Frame
        JFrame overviewFrame = new JFrame("Process Overview & Phase Completeness");
        overviewFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        overviewFrame.add(splitPane);
        overviewFrame.setSize(1000, 850); // Increased height slightly to accommodate both
        overviewFrame.setVisible(true);
      }
    }

  public static void transformAssignmentActionText() {
    transformAssignmentActionText(
        "c:\\CommonGISprojects\\events\\ProcessMining-Conf\\c25f_submission_logs.csv",
        "c:\\CommonGISprojects\\events\\ProcessMining-Conf\\c25f_log_transformed.csv");
  }

  public static void transformAssignmentActionText(String inputPath,String outputPath) {
    transformCsv(inputPath, outputPath);
    System.out.println("CSV transformation complete.");
  }
}
