package viz;

import java.awt.*;

public class Utils {
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
