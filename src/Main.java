import data.LogLoader;
import structures.ActionType;
import structures.Actor;
import structures.GlobalProcess;
import viz.ProcessTimelinePanel;
import viz.TimelinePanel;
import viz.TimelineTextsPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
      /*
      transformAssignmentActionText(
          "c:\\CommonGISprojects\\events\\ProcessMining-Conf\\c25f_submission_logs.csv",
          "c:\\CommonGISprojects\\events\\ProcessMining-Conf\\c25f_log_transformed.csv");
      */

      LogLoader loader = new LogLoader();
      /**/
      String phasesFilePath="c:\\CommonGISprojects\\events\\ProcessMining-Conf\\Timeline25.csv";
      String actionsMappingFilePath="c:\\CommonGISprojects\\events\\ProcessMining-Conf\\actions2phases.csv";
      String actorsMappingFilePath="c:\\CommonGISprojects\\events\\ProcessMining-Conf\\actions2roles.csv";
      String logFilePath = "c:\\CommonGISprojects\\events\\ProcessMining-Conf\\new\\c25f_log_transformed.csv";
      //String logFilePath = "c:\\CommonGISprojects\\events\\ProcessMining-Conf\\conf25log.csv";
      /**/
      /*
      String phasesFilePath="c:\\CommonGISprojects\\events\\ProcessMining-Conf\\synthetic\\Timeline25.csv";
      String actionsMappingFilePath="c:\\CommonGISprojects\\events\\ProcessMining-Conf\\synthetic\\actions2phases.csv";
      String actorsMappingFilePath="c:\\CommonGISprojects\\events\\ProcessMining-Conf\\synthetic\\actions2roles.csv";
      String logFilePath = "c:\\CommonGISprojects\\events\\ProcessMining-Conf\\synthetic\\object_centric_log.csv";
      /**/

      GlobalProcess gProc=new GlobalProcess();

      try {
        loader.loadPhaseTimetable(phasesFilePath);
        loader.loadActionPhaseMapping(actionsMappingFilePath);
        loader.loadActionToRolesMapping(actorsMappingFilePath);
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
        ProcessTimelinePanel processMainPanel = new ProcessTimelinePanel(gProc);
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

        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel label = new JLabel("Grouping mode:");

        JRadioButton processesButton = new JRadioButton("processes");
        JRadioButton actorsButton = new JRadioButton("actors");

        // Group them
        ButtonGroup group = new ButtonGroup();
        group.add(processesButton);
        group.add(actorsButton);

        // Set default
        processesButton.setSelected(true);

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

        modePanel.add(label);
        modePanel.add(processesButton);
        modePanel.add(actorsButton);

        JPanel controlPanel = new JPanel();
        controlPanel.add(modePanel);
        frame.add(controlPanel, BorderLayout.SOUTH);

        frame.pack();
        frame.setSize(frame.getWidth(), 850);
        frame.setVisible(true);
      }
    }

  public static void transformAssignmentActionText(String inputPath,String outputPath) {
    transformCsv(inputPath, outputPath);
    System.out.println("CSV transformation complete.");
  }
}
