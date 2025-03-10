package edu.uob.commands;

import edu.uob.DatabaseManager;

public class DatabaseCommand extends DBCommand{
  private DatabaseManager dbManager;
  
  public DatabaseCommand(DatabaseManager dbManager, String command){
    super(null, command);
    this.dbManager = dbManager;
  }

  @Override
  public String execute(){
    String formatCommand = this.cmd.trim().toUpperCase();

    if (formatCommand.matches("(?i)^USE\\s+\\w+;$")) {
        String normalizeDbName = cmd.substring(4).replaceAll(";$", "").trim();
        return dbManager.useDatabase(normalizeDbName);

    } else if (formatCommand.matches("(?i)^DROP\\s+DATABASE\\s+\\w+;$")) {
        String normalizeDbName = cmd.substring(14).replaceAll(";$", "").trim();
        return dbManager.dropDatabase(normalizeDbName);

    } else if (formatCommand.matches("(?i)^SHOW\\s+DATABASES;$")) {
        return dbManager.showDatabase();

    } else if(formatCommand.matches("(?i)^CREATE\\s+DATABASE\\s+\\w+;$")){
        String normalizeDbName = cmd.substring(16).replaceAll(";$", "").trim();
        return dbManager.createDatabase(normalizeDbName);

    }

    return "[ERROR] Invalid database command.";
  }

}
