package edu.uob.commands;

import edu.uob.Database;;


public abstract class DBCommand {
  protected Database db;
  protected String cmd;


  public DBCommand(Database db, String command){
    this.db = db;
    this.cmd = command;
  }

  public abstract String execute();

}
