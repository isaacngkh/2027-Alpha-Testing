// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot;

import org.wpilib.driverstation.DriverStationErrors;
import org.wpilib.networktables.BooleanArrayPublisher;
import org.wpilib.networktables.NetworkTable;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.networktables.StringPublisher;
import org.wpilib.networktables.StringSubscriber;

public class GamePieceScoredTracker {
  public enum ReefLevel {
    L4(0),
    L3(1),
    L2(2);

    public final int index;

    private ReefLevel(int index) {
      this.index = index;
    }
  }

  public enum Branch {
    A(0),
    B(1),
    C(2),
    D(3),
    E(4),
    F(5),
    G(6),
    H(7),
    I(8),
    J(9),
    K(10),
    L(11);

    public final int index;

    private Branch(int index) {
      this.index = index;
    }
  }

  /*
   * Network Table Entries
   */
  private static final NetworkTable nt =
      NetworkTableInstance.getDefault().getTable("GamePieceScoredTracker");
  private static final BooleanArrayPublisher ntOccupiedPublisher =
      nt.getBooleanArrayTopic("Occupied").publish();
  private static final StringSubscriber ntOperatorSelectionSubscriber =
      nt.getStringTopic("Operator Selection").subscribe("");
  private static final StringPublisher ntOperatorSelectionPublisher =
      nt.getStringTopic("Operator Selection").publish();

  private static boolean[] occupied = new boolean[36];

  static {
    ntOccupiedPublisher.set(occupied);
    ntOperatorSelectionPublisher.setDefault("");
    nt.getEntry(".type").setString("GamePieceScoredTracker");
  }

  private static int getIndex(ReefLevel level, Branch branch) {
    return level.index * 12 + branch.index;
  }

  private static boolean applySelection(String str) {
    // Expected format: "L4-A"
    if (str.length() != 4 || str.charAt(2) != '-') {
      return false;
    }
    try {
      ReefLevel level = ReefLevel.valueOf(str.substring(0, 2));
      Branch branch = Branch.valueOf(str.substring(3));
      if (isScored(level, branch)) {
        markAsEmpty(level, branch);
      } else {
        markAsScored(level, branch);
      }
      return true;
    } catch (IllegalArgumentException e) {
      return false; // unknown level or branch
    }
  }

  public static void markAsScored(ReefLevel level, Branch branch) {
    occupied[getIndex(level, branch)] = true;
    ntOccupiedPublisher.accept(occupied);
  }

  public static void markAsEmpty(ReefLevel level, Branch branch) {
    occupied[getIndex(level, branch)] = false;
    ntOccupiedPublisher.accept(occupied);
  }

  public static boolean isScored(ReefLevel level, Branch branch) {
    return occupied[getIndex(level, branch)];
  }

  public static int reefLevelCount(ReefLevel level) {
    int count = 0;
    int startIndex = level.index * 12;

    for (int i = 0; i < 12; i++) {
      if (occupied[startIndex + i]) {
        count++;
      }
    }

    return count;
  }

  public static void periodic() {
    String selection = ntOperatorSelectionSubscriber.get();
    if (!selection.isEmpty()) {
      if (!applySelection(selection)) {
        DriverStationErrors.reportWarning("Bad Operator Selection: " + selection, false);
      }
      ntOperatorSelectionPublisher.accept("");
    }
  }
}
