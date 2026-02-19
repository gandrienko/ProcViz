package structures;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Phase {
  public String name;
  public LocalDate startDate;
  public LocalDate endDate;

  public Phase(String name, LocalDate startDate, LocalDate endDate) {
    this.name = name;
    this.startDate = startDate;
    this.endDate = endDate;
  }

  public boolean contains(LocalDateTime timestamp) {
    return !timestamp.toLocalDate().isBefore(startDate) && !timestamp.toLocalDate().isAfter(endDate);
  }

  public boolean contains(LocalDate date) {
    return !date.isBefore(startDate) && !date.isAfter(endDate);
  }
}
