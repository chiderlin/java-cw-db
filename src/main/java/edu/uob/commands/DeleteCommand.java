package edu.uob.commands;

import edu.uob.ConditionNode;
import edu.uob.Database;
import edu.uob.QueryParser;

public class DeleteCommand extends DBCommand {
  public DeleteCommand(Database db, String command) {
    super(db, command);
  }

  @Override
  public String execute() {
    if (db == null) {
      System.err.println("[ERROR] Switch database required.");
      return "[ERROR] Switch database required.";
    }

    cmd = cmd.trim().replaceAll(";$", "").trim();
    if (!cmd.matches("(?i)^DELETE\\s+FROM\\s+.+\\s+WHERE\\s+.+$")) {
      System.err.println("[ERROR] Invalid DELETE syntax.");
      return "[ERROR] Invalid DELETE syntax.";
    }

    String[] parts = cmd.split("(?i)WHERE");
    String tableName = parts[0].replaceFirst("(?i)^DELETE\\s+FROM\\s+", "").trim();
    String whereClause = parts[1].trim();

    if (tableName.isEmpty()) {
      System.err.println("[ERROR] Missing table name in DELETE FROM.");
      return "[ERROR] Missing table name in DELETE FROM.";
    }

    ConditionNode conditionTree = QueryParser.parseWhere(whereClause);
    return db.deleteData(tableName, conditionTree);
  }
}
