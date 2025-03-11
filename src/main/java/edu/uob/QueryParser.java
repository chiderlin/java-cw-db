package edu.uob;

import edu.uob.commands.AlterCommand;
import edu.uob.commands.DBCommand;
import edu.uob.commands.DatabaseCommand;
import edu.uob.commands.DeleteCommand;
import edu.uob.commands.InsertCommand;
import edu.uob.commands.JoinCommand;
import edu.uob.commands.SelectCommand;
import edu.uob.commands.TableCommand;
import edu.uob.commands.UpdateCommand;

public class QueryParser {

  public static DBCommand parse(String rawCommand, DatabaseManager dbManager) {
    String formatCommand = rawCommand.trim().toUpperCase();
    if (formatCommand.startsWith("USE ") ||
        formatCommand.startsWith("DROP DATABASE ") ||
        formatCommand.startsWith("SHOW DATABASES") ||
        formatCommand.startsWith("CREATE DATABASE ")) {
      return new DatabaseCommand(dbManager, rawCommand);
    }

    Database db = dbManager.getCurrDatabase();
    if (db == null) {
      return new DBCommand(null, rawCommand) {
        @Override
        public String execute() {
          return "[ERROR] No database selected.";
        }
      };
    }

    String commandType = formatCommand.split(" ")[0];

    return switch (commandType) {
      case "CREATE", "DROP" -> new TableCommand(db, rawCommand);
      case "INSERT" -> new InsertCommand(db, rawCommand);
      case "SELECT" -> new SelectCommand(db, rawCommand);
      case "UPDATE" -> new UpdateCommand(db, rawCommand);
      case "DELETE" -> new DeleteCommand(db, rawCommand);
      case "ALTER" -> new AlterCommand(db, rawCommand);
      case "JOIN" -> new JoinCommand(db, rawCommand);
      default -> null;
    };
  }

  public static ConditionNode parseWhere(String whereClause) {
    whereClause = whereClause.trim();

    if (whereClause.startsWith("(") && whereClause.endsWith(")")) {
      int lastIndex = findMatchingParenthesis(whereClause);
      // System.out.println("[INFO] Detected Brackets: " + whereClause);
      // return parseWhere(whereClause.substring(1, whereClause.length() - 1));
      if (lastIndex == whereClause.length() - 1) {
        return parseWhere(whereClause.substring(1, lastIndex));
      }
    }

    // single condition -> where name == 'Bob';
    if (!whereClause.matches("(?i).*\\s(AND|OR)\\s.*")) {
      // System.out.println("Single condition detected: " + whereClause);
      return new ConditionNode(whereClause);
    }
    // find outer AND/OR operator
    int mainOpIndex = findMainOperator(whereClause);
    if (mainOpIndex == -1)
      return new ConditionNode(whereClause);

    String leftPart = whereClause.substring(0, mainOpIndex).trim();
    String operator = whereClause.substring(mainOpIndex, mainOpIndex + 3).trim();
    String rightPart = whereClause.substring(mainOpIndex + 3).trim();

    // System.out.println("[INFO] Splitting condition:");
    // System.out.println("[INFO] Left: " + leftPart);
    // System.out.println("[INFO] Operator: " + operator);
    // System.out.println("[INFO] Right: " + rightPart);

    // recurse left/right side.
    return new ConditionNode(operator, parseWhere(leftPart), parseWhere(rightPart));
  }

  public static int findMainOperator(String whereClause) {
    int level = 0;
    int lastAndIndex = -1;
    int lastOrIndex = -1;
    for (int i = 0; i < whereClause.length() - 2; i++) {
      if (whereClause.charAt(i) == '(')
        level++;
      if (whereClause.charAt(i) == ')')
        level--;

      if (level == 0) {
        if (whereClause.substring(i).toUpperCase().startsWith("AND")) {
          lastAndIndex = i; // store `AND` location
        }
        if (whereClause.substring(i).toUpperCase().startsWith("OR")) {
          lastOrIndex = i; // store `OR` location
        }
      }

    }
    return lastOrIndex != -1 ? lastOrIndex : lastAndIndex;
  }

  private static int findMatchingParenthesis(String whereClause) {
    int level = 0;
    for (int i = 0; i < whereClause.length(); i++) {
      if (whereClause.charAt(i) == '(')
        level++;
      if (whereClause.charAt(i) == ')') {
        level--;
        if (level == 0)
          return i;
      }
    }
    return -1;
  }

}
