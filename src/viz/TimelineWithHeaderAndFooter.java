package viz;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class TimelineWithHeaderAndFooter extends JPanel {
  public TimelinePanel contentPanel=null;
  
  public TimelineWithHeaderAndFooter(TimelinePanel content, boolean isScrolledVertically) {
    contentPanel=content;
    
    JPanel topPanel=new TimelineTextsPanel(contentPanel,TimelineTextsPanel.SHOW_TITLES);
    JPanel bottomPanel=new TimelineTextsPanel(contentPanel,TimelineTextsPanel.SHOW_TIMES);

    JViewport topViewport = new JViewport(), bottomViewport = new JViewport();
    topViewport.setView(topPanel);
    bottomViewport.setView(bottomPanel);

    JScrollPane scrollPane = (isScrolledVertically)?new JScrollPane(contentPanel):null;
    if (isScrolledVertically) {
      scrollPane.getVerticalScrollBar().setUnitIncrement(8);
      scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
      scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

      scrollPane.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
          super.componentResized(e);
          int width=scrollPane.getViewport().getWidth();
          contentPanel.setSize(width,contentPanel.getHeight());
          contentPanel.setPreferredSize(new Dimension(width,contentPanel.getHeight()));
        }
      });
    }

    contentPanel.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        super.componentResized(e);
        int width=contentPanel.getPreferredSize().width;
        if (width!=topPanel.getPreferredSize().width) {
          topPanel.setPreferredSize(new Dimension(width,topPanel.getHeight()));
          bottomPanel.setPreferredSize(new Dimension(width,bottomPanel.getHeight()));
        }
        width=contentPanel.getWidth();
        topPanel.setSize(width,topPanel.getHeight());
        bottomPanel.setSize(width,bottomPanel.getHeight());
        topViewport.revalidate();
        bottomViewport.revalidate();
      }
    });

    setLayout(new BorderLayout());
    add((scrollPane!=null)?scrollPane:contentPanel,BorderLayout.CENTER);
    int scrollbarWidth=0;
    if (scrollPane!=null) {
      JScrollBar vScrollBar = scrollPane.getVerticalScrollBar();
      scrollbarWidth = vScrollBar.getPreferredSize().width+3;
      JPanel topContainer=new JPanel();
      topContainer.setLayout(new BorderLayout());
      topContainer.add(topViewport,BorderLayout.CENTER);
      topContainer.add(Box.createRigidArea(new Dimension(scrollbarWidth, 1)), BorderLayout.EAST);
      JPanel bottomContainer=new JPanel();
      bottomContainer.setLayout(new BorderLayout());
      bottomContainer.add(bottomViewport,BorderLayout.CENTER);
      bottomContainer.add(Box.createRigidArea(new Dimension(scrollbarWidth, 1)), BorderLayout.EAST);
      add(topContainer,BorderLayout.NORTH);
      add(bottomContainer,BorderLayout.SOUTH);
    }
    else {
      add(topViewport,BorderLayout.NORTH);
      add(bottomViewport,BorderLayout.SOUTH);
    }

    int sbWidth=scrollbarWidth;

    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
      super.componentResized(e);
      int width=contentPanel.getWidth();
      topPanel.setSize(width,topPanel.getHeight());
      bottomPanel.setSize(width,bottomPanel.getHeight());
      width-=sbWidth;
      topViewport.setSize(width,topViewport.getHeight());
      bottomViewport.setSize(width,bottomViewport.getHeight());
      }
    });
  }
}
