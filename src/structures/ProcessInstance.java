package structures;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents an IProc — one submission, PC member activity, etc.
 */

public class ProcessInstance {
  String id;
  ProcessType type; // e.g., SUBMISSION, PC_MEMBER, CHAIR, etc.
  List<StateInstance> states = new ArrayList<>();
  Map<String, Object> metadata; // optional extensibility
}
