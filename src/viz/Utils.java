package viz;

import java.awt.*;
import java.awt.List;
import java.util.*;

public class Utils {

  // Set1 palette from ColorBrewer (up to 9 distinct colors)
  public static final Color[] SET1 = new Color[]{
      new Color(228, 26, 28),   // Red
      new Color(55, 126, 184),  // Blue
      new Color(77, 175, 74),   // Green
      new Color(152, 78, 163),  // Purple
      new Color(255, 127, 0),   // Orange
      new Color(255, 255, 51),  // Yellow
      new Color(166, 86, 40),   // Brown
      new Color(247, 129, 191) // Pink
  };

  public static Map<String, Color> generateItemColors(Set<String> items) {
    if (items==null || items.isEmpty())
      return null;
    Map<String, Color> itemColors = new HashMap<>();
    Iterator<String> itemIterator = items.iterator();
    int i = 0;

    while (itemIterator.hasNext()) {
      String role = itemIterator.next();
      Color color = SET1[i % SET1.length]; // wrap around if too many roles
      itemColors.put(role, color);
      i++;
    }

    return itemColors;
  }

  public static Color toSaturated(Color pastel) {
    // Convert pastel RGB to HSB
    float[] hsb = Color.RGBtoHSB(pastel.getRed(), pastel.getGreen(), pastel.getBlue(), null);
    
    // Set full saturation and optionally adjust brightness
    float hue = hsb[0];
    float brightness = Math.min(hsb[2], 0.85f); // prevent too bright
    float saturation = 1.0f;
    
    // Convert back to Color
    return Color.getHSBColor(hue, saturation, brightness);
  }
}
