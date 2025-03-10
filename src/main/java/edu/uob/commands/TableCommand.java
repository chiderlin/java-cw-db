package edu.uob.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.uob.Database;

public class TableCommand extends DBCommand {
  public TableCommand(Database db, String command) {
    super(db, command);
  }

  @Override
  public String execute() {
    String formatCommand = this.cmd.trim().toUpperCase();
    if (formatCommand.startsWith("CREATE TABLE ")) {
      return parseCreateTable(this.cmd);

    } else if (formatCommand.startsWith("DROP TABLE ")) {
      return parsedropTable(this.cmd);
    }
    return "";
  }

  public String parseCreateTable(String cmd) {
    if (db == null) {
      System.err.println("[ERROR] Switch database required.");
      return "[ERROR] Switch database required.";
    }

    String rawCmd = cmd.substring(13).trim().replaceAll("\\);$", "").trim();
    String[] parts = rawCmd.split("\\(", 2);
    if (parts.length < 2) {
      System.err.println("[ERROR] Invalid CREATE TABLE syntax.");
      return "[ERROR] Invalid CREATE TABLE syntax.";
    }

    String tableName = parts[0].trim(); // `marks`
    String cols = parts[1].trim(); // `name, mark, pass`

    if (tableName.isEmpty()) {
      System.err.println("[ERROR] Table name missing in CREATE TABLE.");
      return "[ERROR] Table name missing in CREATE TABLE.";
    }

    // remove ()
    List<String> values = new ArrayList<>(Arrays.asList(cols.split("\\s*,\\s*")));

    if (values.isEmpty() || values.get(0).isEmpty()) {
      System.err.println("[ERROR] No columns specified in CREATE TABLE.");
      return "[ERROR] No columns specified in CREATE TABLE.";
    }

    System.out.println("Parsed columns: " + values);
    return db.createTable(tableName, values);
  }

  public String parsedropTable(String cmd) {
    if (db == null) {
      System.out.println("[ERROR] Switch database required.");
      return "[ERROR] Switch database required.";
    }

    cmd = cmd.trim().replaceAll(";$", "").trim();
    if (!cmd.matches("(?i)^DROP\\s+TABLE\\s+.+$")) {
      System.out.println("[ERROR] Invalid DROP TABLE syntax.");
      return "[ERROR] Invalid DROP TABLE syntax.";
    }
    String tableName = cmd.replaceFirst("(?i)^DROP\\s+TABLE\\s+", "").trim();

    if (tableName.isEmpty()) {
      System.out.println("[ERROR] Missing table name in DROP TABLE.");
      return "[ERROR] Missing table name in DROP TABLE.";
    }

    return db.dropTable(tableName);
  }

}
