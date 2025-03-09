package edu.uob.commands;

import java.util.List;
import java.util.stream.Collectors;

import edu.uob.Database;

public class JoinCommand extends DBCommand {
  public JoinCommand(Database db, String command){
    super(db, command);
  }
  @Override
  public String execute(){
    if(db == null){
        System.err.println("[ERROR] Switch database required.");
        return "[ERROR] Switch database required.";
    }

    try{
        cmd = cmd.trim().replaceAll("\\s+", " ").replaceAll(";$", ""); 

        if (!cmd.matches("(?i)^JOIN\\s+\\w+\\s+AND\\s+\\w+\\s+ON\\s+\\w+\\s+AND\\s+\\w+$")) {
            System.err.println("[ERROR] Invalid JOIN syntax.");
            return "[ERROR] Invalid JOIN syntax.";
        }

        String[] parts = cmd.split(" (?i)AND ");  // split by AND
        if (parts.length != 3) {
            System.err.println("[ERROR] Invalid JOIN syntax.");
            return "[ERROR] Invalid JOIN syntax.";
        }

        String table1Name = parts[0].split("(?i)JOIN ")[1].trim();
        String table2Name = parts[1].split("(?i)ON ")[0].trim();
        String table1JoinCol = parts[1].split("(?i)ON ")[1].trim();
        String table2JoinCol = parts[2].trim();

        List<List<String>> result = db.joinData(table1Name, table2Name, table1JoinCol, table2JoinCol);

        if (result.isEmpty()) {
            System.err.println("[ERROR] No matching rows found for JOIN.");
            return "[ERROR] No matching rows found for JOIN.";
        }

        String resultString = result.stream()
        .map(row -> String.join("\t", row))
        .collect(Collectors.joining("\n"));
        System.out.println(resultString);
        return "[OK]\n" + resultString;
    } catch(Exception e){
        System.out.println("[ERROR] parseJoin: " + e.getMessage());
        return "[ERROR] parseJoin: " + e.getMessage();
    }
  }
}
