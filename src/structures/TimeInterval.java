package structures;

import java.time.*;

/**
 * Encapsulates a start and end timestamp.
 */

public class TimeInterval {
  public LocalDateTime start;
  public LocalDateTime end;

  public TimeInterval(LocalDateTime start, LocalDateTime end) {
    this.start = start;
    this.end = end;
  }

  public Duration getDuration() {
    return Duration.between(start, end);
  }

  public LocalDateTime getStart() {
    return start;
  }

  public void setStart(LocalDateTime start) {
    this.start = start;
  }

  public LocalDateTime getEnd() {
    return end;
  }

  public void setEnd(LocalDateTime end) {
    this.end = end;
  }
}
