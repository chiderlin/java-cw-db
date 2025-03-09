package edu.uob.commands;

import edu.uob.Database;

public class AlterCommand extends DBCommand{
  public AlterCommand(Database db, String command){
    super(db, command);
  }

  @Override
  public String execute(){
    if(db == null){
      System.out.println("[ERROR] Switch database required.");
      return "[ERROR] Switch database required.";
    }
    try{
        cmd = cmd.trim().replaceAll("\\s+", " ").replaceAll(";$", "");
        if (!cmd.matches("(?i)^ALTER TABLE\\s+\\w+\\s+(ADD|DROP)\\s+\\w+$")) {
            System.out.println("[ERROR] Invalid ALTER TABLE syntax.");
            return "[ERROR] Invalid ALTER TABLE syntax.";
        }
        String[] parts = cmd.split(" ");
        String tableName = parts[2];
        String action = parts[3];
        String columnName = parts[4];
        System.out.println("parts: "+parts[4]);
        return db.alterData(tableName, action, columnName);

    }catch(Exception e){
        System.out.println("[ERROR] parseAlter: " + e.getMessage());
        return "[ERROR] parseAlter: " + e.getMessage();
    }
  }
}
