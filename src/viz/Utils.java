package viz;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Utils {

  // Set1 palette from ColorBrewer (up to 9 distinct colors)
  public static final Color[] SET1 = new Color[]{
      new Color(228, 26, 28),   // Red
      new Color(55, 126, 184),  // Blue
      new Color(152, 78, 163),  // Purple
      new Color(255, 127, 0),   // Orange
      new Color(77, 175, 74),   // Green
      new Color(255, 255, 51),  // Yellow
      new Color(166, 86, 40),   // Brown
      new Color(247, 129, 191) // Pink
  };

  public static Map<String, Color> generateItemColors(List<String> items) {
    if (items==null || items.isEmpty())
      return null;
    Map<String, Color> itemColors = new HashMap<>();
    Iterator<String> itemIterator = items.iterator();
    int i = 0;

    while (itemIterator.hasNext()) {
      String role = itemIterator.next();
      Color color = null;
      if (role.toLowerCase().contains("external"))
        color=itemColors.get("External Reviewer");
      else
      if (role.toLowerCase().contains("coordinator") || role.toLowerCase().contains("primary") ||
          role.toLowerCase().contains("secondary"))
        color=itemColors.get("PC Member");
      if (color==null)
        color = SET1[i % SET1.length]; // wrap around if too many roles
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
