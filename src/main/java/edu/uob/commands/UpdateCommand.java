package edu.uob.commands;

import java.util.HashMap;
import java.util.Map;

import edu.uob.ConditionNode;
import edu.uob.Database;
import edu.uob.QueryParser;

public class UpdateCommand extends DBCommand {
    public UpdateCommand(Database db, String command){
      super(db, command);
    }

    @Override
    public String execute(){
      if(db == null) {
          System.err.println("[ERROR] Switch database required.");
          return "[ERROR] Switch database required.";
      }

      try {
          cmd = cmd.trim().replaceAll("\\s+", " ").replaceAll(";$", ""); // 清理空格
          if (!cmd.matches("(?i)^UPDATE\\s+\\w+\\s+SET\\s+.+\\s+WHERE\\s+.+$")) {
              System.err.println("[ERROR] Invalid UPDATE syntax.");
              return "[ERROR] Invalid SELECT syntax.";
          }

          // UPDATE syntax
          String[] updateParts = cmd.split("(?i)\\s+SET\\s+", 2);
          String tableName = updateParts[0].split("\\s+")[1].trim();
          
          // SET syntax
          String[] setWhereParts = updateParts[1].split("(?i)\\s+WHERE\\s+", 2);
          String setClause = setWhereParts[0].trim();
          String whereClause = setWhereParts.length > 1 ? setWhereParts[1].trim() : "";

          // SET value
          Map<String, String> updates = new HashMap<>();
          for (String setPart : setClause.split("\\s*,\\s*")) {
              String[] keyValue = setPart.split("\\s*=\\s*");
              if (keyValue.length != 2) {
                  System.err.println("[ERROR] Invalid SET format: " + setPart);
                  return "[ERROR] Invalid SET format: " + setPart;
              }
              updates.put(keyValue[0].trim(), keyValue[1].replaceAll("'", "").trim());
          }

          // handle WHERE condition
          ConditionNode conditionTree = null;
          if (!whereClause.isEmpty()) {
              conditionTree = QueryParser.parseWhere(whereClause);
          }

          return db.updateData(tableName, updates, conditionTree);

      } catch (Exception e) {
          System.out.println("[ERROR] parseUpdate: " + e.getMessage());
          return "[ERROR] parseSelect: " + e.getMessage();
      }
    }
}
