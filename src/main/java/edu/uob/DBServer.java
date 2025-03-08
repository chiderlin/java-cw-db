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
import java.util.stream.Collectors;
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

        } else if(formatCommand.startsWith("UPDATE ")){
            parseUpdate(command);

        } else if(formatCommand.startsWith("ALTER ")){
            parseAlter(command);

        } else if(formatCommand.startsWith("DROP TABLE ")){
            parsedropTable(command);
        } else if(formatCommand.startsWith("DELETE FROM ")){
            parseDeleteData(command);
        }

        return "";
    }

    public void parseDeleteData(String cmd){
        if(db == null){
            System.out.println("[ERROR] Switch database required.");
            return ;
        }

        cmd = cmd.trim().replaceAll(";$","").trim();
        if (!cmd.matches("(?i)^DELETE\\s+FROM\\s+.+\\s+WHERE\\s+.+$")) {
            System.out.println("[ERROR] Invalid DELETE syntax.");
            return;
        }

        String[] parts = cmd.split("(?i)WHERE");
        String tableName = parts[0].replaceFirst("(?i)^DELETE\\s+FROM\\s+", "").trim();
        String whereClause = parts[1].trim();

        if (tableName.isEmpty()) {
            System.out.println("[ERROR] Missing table name in DELETE FROM.");
            return;
        }

        ConditionNode conditionTree = QueryParser.parseWhere(whereClause);
        db.deleteData(tableName, conditionTree);

    }

    public void parsedropTable(String cmd){
        if(db == null){
            System.out.println("[ERROR] Switch database required.");
            return ;
        }

        cmd = cmd.trim().replaceAll(";$","").trim();
        if(!cmd.matches("(?i)^DROP\\s+TABLE\\s+.+$")){
            System.out.println("[ERROR] Invalid DROP TABLE syntax.");
            return ;
        }
        String tableName = cmd.replaceFirst("(?i)^DROP\\s+TABLE\\s+", "").trim();

        if(tableName.isEmpty()){
            System.out.println("[ERROR] Missing table name in DROP TABLE.");
            return;
        }

        db.dropTable(tableName);
    }


    public void parseAlter(String cmd){

    }


    public void parseUpdate(String cmd){
        if(db == null) {
            System.out.println("[ERROR] Switch database required.");
            return;
        }

        try {
            // **🚀 1. 清理語法**
            cmd = cmd.trim().replaceAll("\\s+", " ").replaceAll(";$", ""); // 清理空格
            if (!cmd.matches("(?i)^UPDATE\\s+\\w+\\s+SET\\s+.+\\s+WHERE\\s+.+$")) {
                System.out.println("[ERROR] Invalid UPDATE syntax.");
                return;
            }

            // **🚀 2. 拆解 `UPDATE` 語句**
            String[] updateParts = cmd.split("(?i)\\s+SET\\s+", 2);
            String tableName = updateParts[0].split("\\s+")[1].trim();
            
            // **🚀 3. 拆解 `SET` 部分**
            String[] setWhereParts = updateParts[1].split("(?i)\\s+WHERE\\s+", 2);
            String setClause = setWhereParts[0].trim();
            String whereClause = setWhereParts.length > 1 ? setWhereParts[1].trim() : "";

            // **🚀 4. 解析 `SET` 欄位**
            Map<String, String> updates = new HashMap<>();
            for (String setPart : setClause.split("\\s*,\\s*")) {
                String[] keyValue = setPart.split("\\s*=\\s*");
                if (keyValue.length != 2) {
                    System.out.println("[ERROR] Invalid SET format: " + setPart);
                    return;
                }
                updates.put(keyValue[0].trim(), keyValue[1].replaceAll("'", "").trim());
            }

            // **🚀 5. 解析 `WHERE` 條件**
            ConditionNode conditionTree = null;
            if (!whereClause.isEmpty()) {
                conditionTree = QueryParser.parseWhere(whereClause);
            }

            // **🚀 6. 調用 `updateData`**
            db.updateData(tableName, updates, conditionTree);

        } catch (Exception e) {
            System.out.println("[ERROR] parseUpdate: " + e.getMessage());
        }
    }

    /**
     * CREATE TABLE marks (name, mark, pass);
     */
    public void parseCreateTable(String cmd) {
        if (db == null) {
            System.out.println("[ERROR] Switch database required.");
            return;
        }
    
        // **🚀 1. 清理 `);` 確保語法正確**
        String rawCmd = cmd.substring(13).trim().replaceAll("\\);$", "").trim(); // 確保 `);` 被移除
    
        // **🚀 2. 分割語法 -> `tableName` 和 `cols`**
        String[] parts = rawCmd.split("\\(", 2);
        if (parts.length < 2) {
            System.out.println("[ERROR] Invalid CREATE TABLE syntax.");
            return;
        }
    
        String tableName = parts[0].trim(); // `marks`
        String cols = parts[1].trim();      // `name, mark, pass`
    
        // **🚀 3. 確保 `tableName` 不為空**
        if (tableName.isEmpty()) {
            System.out.println("[ERROR] Table name missing in CREATE TABLE.");
            return;
        }
    
        // **🚀 4. 解析欄位名稱，去掉括號**
        List<String> values = new ArrayList<>(Arrays.asList(cols.split("\\s*,\\s*")));
    
        // **🚀 5. 確保 `values` 不為空**
        if (values.isEmpty() || values.get(0).isEmpty()) {
            System.out.println("[ERROR] No columns specified in CREATE TABLE.");
            return;
        }

    
        // **🚀 6. 印出解析後的欄位**
        System.out.println("Parsed columns: " + values); // ✅ 輸出 `[name, mark, pass]`
        Path path = Paths.get(storageFolderPath);
        System.out.println(db.createTable(tableName, path, values));
    }

    
    private void parseInsert(String cmd) {
        try {
            // **🚀 1. 清理語法**
            cmd = cmd.trim().replaceAll("\\s+", " ").replaceAll(";$", "");  // 清理多餘空格 & `;`
            if (!cmd.matches("(?i)^INSERT INTO\\s+\\w+\\s+VALUES\\s*\\(.*\\)$")) {
                System.out.println("[ERROR] Invalid INSERT syntax.");
                return;
            }
    
            // **🚀 2. 解析 table name**
            String[] parts = cmd.split("(?i)VALUES", 2);
            String tableName = parts[0].replaceFirst("(?i)^INSERT INTO\\s+", "").trim();
    
            // **🚀 3. 解析 values (去除括號)**
            String valuesPart = parts[1].trim();
            valuesPart = valuesPart.replaceAll("^\\(|\\)$", "");  // 移除頭尾括號
            
            List<String> values = Arrays.stream(valuesPart.split("\\s*,\\s*"))
                                        .map(value -> value.replaceAll("^'(.*)'$", "$1").trim()) // 去除單引號
                                        .map(value -> value.equalsIgnoreCase("null") ? "" : value) // NULL 轉成空字串
                                        .collect(Collectors.toList());
    
            // **🚀 4. 執行插入**
            db.insertData(tableName, values);
    
        } catch (Exception e) {
            System.out.println("[ERROR] parseInsert: " + e.getMessage());
        }
    }
    

    private void parseSelect(String cmd){
        if(db == null) {
            System.out.println("[ERROR] Switch database required.");
            return;
        }
    
        try {
            // **🚀 1. 清理語法**
            cmd = cmd.trim().replaceAll("\\s+", " ").replaceAll(";$", ""); // 清理多餘空格 & `;`
            
            if (!cmd.matches("(?i)^SELECT\\s+.+\\s+FROM\\s+.+")) {
                System.out.println("[ERROR] Invalid SELECT syntax.");
                return;
            }
        
            // **🚀 2. 拆解 SELECT 語法**
            String[] cmds = cmd.split("(?i)FROM");
            if (cmds.length < 2 || cmds[1].trim().isEmpty()) {
                System.out.println("[ERROR] Missing FROM table name.");
                return;
            }
        
            // **🚀 3. 獲取 tableName**
            String[] tableParts = cmds[1].split("(?i)WHERE", 2);
            String tableName = tableParts[0].trim();
            if (tableName.isEmpty()) {
                System.out.println("[ERROR] Table name missing after FROM.");
                return;
            }
        
            // **🚀 4. 確保 Table 存在**
            List<List<String>> table = db.getTable(tableName);
            if (table.isEmpty()) {
                System.out.println("[ERROR] Table " + tableName + " does not exist.");
                return;
            }
        
            // **🚀 5. 確保 SELECT 欄位**
            String queryCol = cmds[0].replaceFirst("(?i)^SELECT\\s*", "").trim();
            if (queryCol.isEmpty()) {
                System.out.println("[ERROR] Missing column names after SELECT.");
                return;
            }
        
            // **🚀 6. 取得 Header（轉大寫用於比對，但回傳原始大小寫）**
            List<String> header = table.get(0);
            Map<String, String> columnMap = new HashMap<>(); // 用來映射大小寫
        
            for (String col : header) {
                columnMap.put(col.toUpperCase(), col); // 轉大寫當 key，原始欄位當 value
            }
        
            List<String> selectedCols;
            if (queryCol.equals("*")) {
                selectedCols = new ArrayList<>(header);  // 選擇所有欄位
            } else {
                selectedCols = Arrays.stream(queryCol.split("\\s*,\\s*"))
                        .map(col -> columnMap.getOrDefault(col.toUpperCase(), col)) // 找到對應的原始欄位名稱
                        .collect(Collectors.toList());
            }
            selectedCols.remove("_DELETED");
            System.out.println("selectedCols: " + selectedCols);
        
            // **🚀 7. 解析 WHERE 條件**
            ConditionNode conditionTree = null;
            if (tableParts.length > 1) {
                String whereClause = tableParts[1].trim();
                if (!whereClause.matches("(?i).+\\s*(==|!=|>|<|>=|<=|LIKE)\\s*.+")) {
                    System.out.println("[ERROR] Invalid WHERE condition syntax.");
                    return;
                }
                conditionTree = QueryParser.parseWhere(whereClause);
            }
        
            // **🚀 8. 找出選擇的欄位索引**
            List<Integer> selectIdx = selectedCols.stream()
                .map(col -> header.indexOf(col)) // 找到原始欄位名稱的索引
                .filter(index -> index != -1)
                .collect(Collectors.toList());
        

            // **🚀 9. 執行條件篩選**
            int deleteIdx = header.indexOf("_DELETED");
            List<List<String>> filteredTable = new ArrayList<>();
            filteredTable.add(selectedCols); // 加入標題
    
            for (int i = 1; i < table.size(); i++) {
                List<String> row = table.get(i);
                if (deleteIdx != -1 && "TRUE".equalsIgnoreCase(row.get(deleteIdx))) {
                    continue; // ✅ **跳過 `_DELETED = TRUE` 的行**
                }
    
                Map<String, String> rowMap = Database.convertRowToMap(row, header);
                if (conditionTree == null || conditionTree.evaluate(rowMap)) {
                    List<String> selectedRow = selectIdx.stream()
                            .map(row::get)
                            .collect(Collectors.toList());
                    filteredTable.add(selectedRow);
                }
            }


            // **🚀 11. 輸出結果**
            String tableResult = filteredTable.stream()
                .map(row -> String.join("\t", row))
                .collect(Collectors.joining("\n"));
        
            System.out.println(tableResult);
        
        } catch (Exception e) {
            System.out.println("[ERROR] parseSelect: " + e.getMessage());
        }
        
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
