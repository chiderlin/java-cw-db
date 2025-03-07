package edu.uob;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import javax.xml.crypto.Data;

import java.nio.file.Files;
import java.nio.file.Path;


/** This class implements the DB server. */
public class DBServer {

    private static final char END_OF_TRANSMISSION = 4;
    private String storageFolderPath;
    Database db = null;

    public static void main(String args[]) throws IOException {
        DBServer server = new DBServer();
        server.blockingListenOn(8888);
    }

    /**
    * KEEP this signature otherwise we won't be able to mark your submission correctly.
    */
    public DBServer() {
        storageFolderPath = Paths.get("databases").toAbsolutePath().toString();
        try {
            // Create the database storage folder if it doesn't already exist !
            Files.createDirectories(Paths.get(storageFolderPath));
            
        } catch(IOException ioe) {
            System.out.println("Can't seem to create database storage folder " + storageFolderPath);
        }
    }

    /**
    * KEEP this signature (i.e. {@code edu.uob.DBServer.handleCommand(String)}) otherwise we won't be
    * able to mark your submission correctly.
    *
    * <p>This method handles all incoming DB commands and carries out the required actions.
    */
    public String handleCommand(String command) {
        // implement your server logic here
        String formatCommand = command.trim().toUpperCase();
        if(formatCommand.startsWith("USE ")){
            String dbName = command.substring(4).trim();
            System.out.println(useDatabase(dbName));

        } else if(formatCommand.startsWith("CREATE DATABASE ")){
            String dbName = command.substring(16).trim();
            System.out.println(createDatabase(dbName));

        } else if(formatCommand.startsWith("DROP DATABASE ")){
            String dbName = command.substring(14).trim();
            dropDatabase(dbName);

        } else if(formatCommand.startsWith("CREATE TABLE ")){
            parseCreateTable(command);

        } else if(formatCommand.startsWith("SHOW DATABASES")){
            System.out.println(showDatabase());

        } else if(formatCommand.startsWith("SELECT ")){
            parseSelect(command);

        } else if(formatCommand.startsWith("INSERT ")){
            parseInsert(command);

        } 
        // else if(formatCommand.contains("WHERE")){
        //     /**
        //      * 有WHERE的處理順序為：
        //      * 1. 取得是哪個tableName並where回來資料
        //      * 2. 再判斷是要做什麼操作：UPDATE? SELECT? DELETE? 
        //      * */ 
        //     ParseQuery rs = parseWhere(command);
        //     System.out.println("rs: "+rs);
        // }
        return "";

    }

    /**
     * @param cmd
     * CREATE TABLE marks (name, mark, pass);
     * 
     */
    public void parseCreateTable(String cmd){
        if(db == null) {
            System.out.println("Switch database required.");
            return ;
        }
        String rawCmd = cmd.substring(13).trim(); //marks (name, mark, pass);
        rawCmd.replace("\\);","").trim();
        String[] parts = rawCmd.split("\\(",2);
        String tableName = parts[0].trim();
        String cols = parts[1].trim();
        List<String> values = new ArrayList<>(Arrays.asList(cols.split(", ")));
        System.out.println("values: "+ values);
        Path path = Paths.get(storageFolderPath);

        System.out.println(db.createTable(tableName, path, values));
    }

    /*
    sql優先會先執行and 再執行or (如果沒有特別括號的話)
     * SELECT ID from people where name == "bob";
     * SELECT ID from people where name == "bob" or age >  18 and age < 30;
     * SELECT ID from people where name == "bob" and age >  18 or age < 30;
     * SELECT ID from people where name == "bob" or age > 18;
     * SELECT id,Name from sheds where name == "test";
     */
    public ParseQuery parseWhere(String cmd){
        String[] queryParts = cmd.split("(?i)\\s+where\\s+", 2); //1.marks  2.name = \"bob\" or age < 18;";
        if(queryParts.length < 2){
            return new ParseQuery("ERROR",null ,Collections.emptyList(),Collections.emptyList());
        }

        String tableName = queryParts[0].split("(?i)\\s+from\\s+")[1].trim();
        String whereClause = queryParts[1].replace(";","").trim(); // where name = \"bob\" or age < 18;";

        List<String> andConditions = new ArrayList<>();
        List<String> orConditions = new ArrayList<>();
        List<String> condition = new ArrayList<>();



        String[] conditions = whereClause.split("(?i)\\s+or\\s+"); //1.where name = \"bob\" 2.age < 18
        System.out.println("conditions.length: "+conditions.length);
        System.out.println("conditions: "+conditions[0]);
        if(conditions.length == 1) {
            // only one condition (no and/or)
            condition.addAll(Arrays.asList(conditions));
            return new ParseQuery(tableName, condition, Collections.emptyList(), Collections.emptyList());
        } 


        for(String orBlock:conditions){
            String[] andParts = orBlock.split("(?i)\\s+and\\s+");

            if(andParts.length >1) {
                andConditions.addAll(Arrays.asList(andParts));
            } else {
                orConditions.add(andParts[0].trim());
            }
        }

        // type: SELECT/UPDATE/DELETE
        // 可能有多個filer, 用List<>: condition:(WHERE) (columnName) (symbol) (filter) 
        return new ParseQuery(tableName, Collections.emptyList(), andConditions, orConditions);
    }


    //TODO:parseInsert
    private void parseInsert(String cmd){

    }

    /*
     * (1)SELECT * from marks;
     * (2)SELECT name, age from marks;
     * (3)SELECT ID from people where name == "bob";
     * (4)SELECT ID from people where name == "bob" or age >  18 and age < 30;
     *    SELECT ID from people where name == "bob" and age >  18 or age < 30;
     *    SELECT ID from people where name == "bob" or age >  18;
     * SELECT ID from people where name == "Bob" or age < 18;
     * SELECT id,name from sheds where name == "Dorchester";
     */
    private void parseSelect(String cmd){
        if(db == null) {
            System.out.println("Switch database required.");
            return ;
        }
        String[] cmds = cmd.split("(?i)FROM"); // SELECT *, marks where name == "bob";
        ParseQuery result = parseWhere(cmd);
        System.out.println("tablename: "+ result.tableName);
        System.out.println("and: "+ result.and);
        System.out.println("or: "+ result.or);
        System.out.println("condition: "+ result.condition);


        // String tableName = cmds[1].trim();
        String tableName = result.tableName;
        Map<String, List<String>> conditions = new HashMap<>();
        conditions.put("and", result.and);
        conditions.put("or", result.or);
        conditions.put("condition", result.condition);


        // cmds[0] // "SELECT * "
        String queryCol = cmds[0].replaceFirst("^(?i)SELECT\\s*", "").trim(); //* or "name, age"
        String[] array = queryCol.split("\\s*,\\s*"); // separate ,
        List<String> cols = Arrays.asList(array);
        System.out.println("cols: "+ cols);
        if(conditions.size() == 0){
            db.getData(tableName, cols, null);
            return ;
        }
        db.getData(tableName, cols, conditions);

    }

    
    public String createDatabase(String dbName){
        if(db == null) {
            System.out.println("Switch database required.");
            return "";
        }
        File createDb = new File("."+File.separator+"databases"+File.separator + dbName);
        if(!createDb.exists()){
            createDb.mkdirs();
            System.out.println("File created: " + createDb.getName());
        } else {
            System.out.println("Database already exists.");
        }
        return "[OK]";
      }


    public void dropDatabase(String dbName){
        Path databasePath = Paths.get(storageFolderPath);
        String lastPart = databasePath.getFileName().toString();
        String parentPath = databasePath.getParent().getFileName().toString();
        if(lastPart.equals("databases")||!parentPath.equals("databases")){
            System.err.println("Not correct database name.");
            return;
        }
        File dir = new File(lastPart);
        if(this.deleteDir(dir)){
            System.out.println("Drop database "+ dir + " successfully.");
        } else {
            System.out.println("error occur: Drop failed.");
        }
    }


    private boolean deleteDir(File dir){
        if(!dir.exists()) return false;
        File[] files = dir.listFiles();
        if(files != null) {
            for(File file: files){
                if(file.isDirectory()){
                    deleteDir(file); // recursly 
                } else {
                    file.delete(); // delete file
                }
            }
        }
        return dir.delete(); // delete empty folder;
    }


    private String useDatabase(String dbName){
        // switch database to /databases/<dbName>
        try{
            storageFolderPath = Paths.get("databases" + File.separator + dbName).toAbsolutePath().toString();
            db = new Database(dbName);
            System.out.println("Switch database: " + dbName + " successfully.");
            return "[OK]";
        } catch(Exception e){
            return "[ERROR] " + e.getMessage();
        }
    }


    private String showDatabase(){
        Path path = Paths.get(storageFolderPath);
        String currentDb = path.getFileName().toString();
        // System.out.println("currentDb: "+currentDb);
        if(currentDb.equals("databases")){
            return "[ERROR] Haven't choose a database yet.";
        }
        return "[OK] " + currentDb;
    }


    //  === Methods below handle networking aspects of the project - you will not need to change these ! ===

    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            System.out.println("Server listening on port " + portNumber);
            while (!Thread.interrupted()) {
                try {
                    blockingHandleConnection(s);
                } catch (IOException e) {
                    System.err.println("Server encountered a non-fatal IO error:");
                    e.printStackTrace();
                    System.err.println("Continuing...");
                }
            }
        }
    }

    private void blockingHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket s = serverSocket.accept();
        BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {

            System.out.println("Connection established: " + serverSocket.getInetAddress());
            while (!Thread.interrupted()) {
                String incomingCommand = reader.readLine();
                System.out.println("Received message: " + incomingCommand);
                String result = handleCommand(incomingCommand);
                writer.write(result);
                writer.write("\n" + END_OF_TRANSMISSION + "\n");
                writer.flush();
            }
        }
    }
}


class ParseQuery {
    String tableName;
    List<String> condition;
    List<String> and;
    List<String> or;

    ParseQuery(String tableName, List<String> condition ,List<String> andConditions, List<String>orConditions){
        this.tableName = tableName;
        this.condition = condition;
        this.and = andConditions;
        this.or = orConditions;
    }
}