package edu.uob.commands;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import edu.uob.Database;

public class InsertCommand extends DBCommand {
  public InsertCommand(Database db, String command) {
    super(db, command);
  }

  @Override
  public String execute() {
    try {
      cmd = cmd.trim().replaceAll("\\s+", " ").replaceAll(";$", ""); // remove space & end ;
      if (!cmd.matches("(?i)^INSERT INTO\\s+\\w+\\s+VALUES\\s*\\(.*\\)$")) {
        System.err.println("[ERROR] Invalid INSERT syntax.");
        return "[ERROR] Invalid INSERT syntax.";
      }

      String[] parts = cmd.split("(?i)VALUES", 2);
      String tableName = parts[0].replaceFirst("(?i)^INSERT INTO\\s+", "").trim();

      // remove end ()
      String valuesPart = parts[1].trim();
      valuesPart = valuesPart.replaceAll("^\\(|\\)$", "");

      List<String> values = Arrays.stream(valuesPart.split("\\s*,\\s*"))
          .map(value -> value.replaceAll("^'(.*)'$", "$1").trim()) // remove ''
          .map(value -> value.equalsIgnoreCase("null") ? "" : value) // NULL to ""
          .collect(Collectors.toList());

      // execute insert
      return db.insertData(tableName, values);

    } catch (Exception e) {
      System.err.println("[ERROR] parseInsert: " + e.getMessage());
      return "[ERROR] parseInsert: " + e.getMessage();
    }
  }
}
