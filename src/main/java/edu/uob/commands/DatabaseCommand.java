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

    if (formatCommand.startsWith("USE ")) {
      return dbManager.useDatabase(formatCommand.substring(4).trim());

    } else if (formatCommand.startsWith("DROP DATABASE ")) {
        return dbManager.dropDatabase(formatCommand.substring(14).trim());

    } else if (formatCommand.startsWith("SHOW DATABASES")) {
        return dbManager.showDatabase();
    }

    return "[ERROR] Invalid database command.";
  }

}
