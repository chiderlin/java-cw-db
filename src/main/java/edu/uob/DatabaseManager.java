package edu.uob;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DatabaseManager {
  private Database currentDatabase;
  private String currentDbName;
  private String storageFolderPath;

  public DatabaseManager(){
        this.storageFolderPath = Paths.get("databases").toAbsolutePath().toString();
        try {
            // Create the database storage folder if it doesn't already exist !
            Files.createDirectories(Paths.get(storageFolderPath));
            
        } catch(IOException ioe) {
            System.out.println("Can't seem to create database storage folder " + storageFolderPath);
        }
  }


  public String useDatabase(String dbName){
    // switch database to /databases/<dbName>
    try{
        this.storageFolderPath = Paths.get("databases" + File.separator + dbName).toAbsolutePath().toString();
        this.currentDbName = dbName;
        this.currentDatabase = new Database(dbName);

        System.out.println("[INFO] Switch database: " + dbName + " successfully.");
        return "[OK]";
    } catch(Exception e){
        System.err.println("[ERROR] useDatabase: " + e.getMessage());
        return "[ERROR] useDatabase: " + e.getMessage();
    }
  }


  public String showDatabase(){
    try {
      Path path = Paths.get(storageFolderPath);
      String currentDb = path.getFileName().toString();
      // System.out.println("currentDb: "+currentDb);
      if(currentDb.equals("databases")){
          return "[ERROR] Haven't choose a database yet.";
      }
      return "[OK] " + currentDb;
    } catch(Exception e) {
      System.err.println("[ERROR] showDatabase: " + e.getMessage());
      return "[ERROR] showDatabase: " + e.getMessage();
    }
  }


  public String dropDatabase(String dbName){
    try {
      Path databasePath = Paths.get(storageFolderPath);
      String lastPart = databasePath.getFileName().toString();
      String parentPath = databasePath.getParent().getFileName().toString();
      if(lastPart.equals("databases")||!parentPath.equals("databases")){
          System.err.println("[ERROR] dropDatabase: Not correct database name.");
          return "[ERROR] dropDatabase: Not correct database name." ;
      }
      File dir = new File(lastPart);
      TableIO tableIO = new TableIO(dbName);
      if(tableIO.deleteDir(dir)){
          this.currentDatabase = null;
          this.currentDbName = null;
          System.out.println("Drop database "+ dir + " successfully.");
          return "[OK]";
      } else {
          System.out.println("[ERROR] dropDatabase: Drop failed.");
          return "[ERROR] dropDatabase: Drop failed." ;
      }
    } catch (Exception e){
      System.err.println("[ERROR] dropDatabase: " + e.getMessage());
      return "[ERROR] dropDatabase: " + e.getMessage();
    }

  }


  public String createDatabase(String dbName){
    System.out.println("before dbName: " + dbName);
    String normalizeDbName = dbName.replaceAll(";$", "").trim();
    System.out.println("after normalizeDbName: " + normalizeDbName);

    File createDb = new File("."+File.separator+"databases"+File.separator + normalizeDbName);
    if(!createDb.exists()){
        createDb.mkdirs();
        System.out.println("File created: " + createDb.getName());
    } else {
        System.out.println("Database already exists.");
    }
    return "[OK]";
  }
  

  public Database getCurrDatabase(){
    return this.currentDatabase;
  }

  
  public String getCurrentDbName(){
    return this.currentDbName;
  }


  public String getStorageFolderPath(){
    return this.storageFolderPath;
  }
}
