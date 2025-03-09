package edu.uob;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat.Style;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collector;
import java.util.stream.Collectors;


// 讀取tab檔
// 載入tab檔
// 這樣db要一次載入全部tab files? -> 先依照語法有用到的再load進來好了。看效能如何再做調整
// 已經load進來的就繼續暫存在memory裡，所以資料會越load越多 -> cache機制去控制，過期的自動刪除，避免資料太多
// 
public class Database {
  private static final char END_OF_TRANSMISSION = 4;
  private String dbName;
  private List<String> tableColumn = new ArrayList<>();
  private HashMap<String, List<List<String>>> tables = new HashMap<>(); // type 2
  private Cache tableCache;
  /**
   * tables = {
   *  users:[[1,'bob',21,'bob@bob.net'],[2,'bob',21,'bob@bob.net'],[1,'bob',21,'bob@bob.net']]
   *  sheds:[[1,'Plaza',1200,1],[2,'Plaza',1200,1]],
   * }
   */
  

  public Database(String dbName){
    this.dbName = dbName;
    this.tableCache = new Cache();
  }

  public static void main(String args[]) {
    Database db = new Database("test");
    db.loadTableData("sheds");
    db._printTable("sheds");

    // --- test insert data -------
    List<String> values = new ArrayList<>();
    values.add("test");
    values.add("1000");
    values.add("2");
    db.insertData("sheds",values);
    // --- test insert data -------

    // --- test create table ------
    List<String> cols = new ArrayList<>();
    cols.add("name");
    cols.add("mark");
    cols.add("pass");
    String pathStr = Paths.get("databases"+File.separator+"test").toAbsolutePath().toString();
    Path path = Paths.get(pathStr);
    db.createTable("test", path, cols);
    // --- test create table ------

    // --- test getData (select syntax) ------
    List<String> returncols = new ArrayList<>();
    returncols.add("id");
    returncols.add("Name");
    // List<String> 
    // db.getData("sheds",returncols,)
    // --- test getData (select syntax) ------
  }



  /**
   * read tab file in memory (data structure)
  */
  public void loadTableData(String tableName){
    try {
      // First check if the table is in cache
      // List<List<String>> cachedData = tableCache.get(tableName);
      // if (cachedData != null) {
      //   tables.put(tableName, cachedData);
      //   return;
      // }

      // If not in cache, load from file
      TableIO tableIO = new TableIO(this.dbName, tableName);
      List<List<String>> tableData = tableIO.loadFile();
      tables.put(tableName, tableData);
      
      // Add to cache
      // tableCache.add(tableName, tableData);

    } catch(Exception e){
      System.err.println("[ERROR] " + e.getMessage());
      return ;
    }
  }


  public void _printTable(String tableName){
    List<List<String>> table = this.tables.get(tableName);
    System.out.println("table :" + table);
    if(table != null && !table.isEmpty()){
      System.out.println("table.size() " + table.size());
      System.out.println(String.format("print table <%s>: %s",tableName,table));
      return;
    }
    System.out.println("[ERROR _printTable] Table Not Found.");
  }
  
  
  /*
    CREATE TABLE marks (name, mark, pass);
  */ 
  public String createTable(String tableName, Path path, List<String> values){
    
    System.out.println("path.toString() "+path.toString());
    
    String lastPart = path.getFileName().toString();
    System.out.println("lastPart "+lastPart);
    String parentPath = path.getParent().getFileName().toString();
    if(lastPart.equals("databases") || !parentPath.equals("databases")){
        System.err.println("An error occurred: Switch to your databases first.");
        return "[ERROR]";
    }


    List<String> tableSchema = new ArrayList<>();
    tableSchema.add("id");
    tableSchema.addAll(values);
    tableSchema.add("_DELETED");

    String data = String.join("\t", tableSchema);
    TableIO tableIO = new TableIO(this.dbName, tableName);
    tableIO.writeFile(data, false);
    System.out.println("[OK] Table " + tableName + " created with schema: " + tableSchema);
    return "[OK]";
  }


  private String getNewId(List<List<String>> table){
    return String.valueOf(table.size());

  }


  public void insertData(String tableName, List<String>values){
    loadTableData(tableName);
    _printTable(tableName);
    List<List<String>> table = this.tables.get(tableName);
    if(tables == null || !tables.containsKey(tableName)){
      System.out.println("[ERROR] Table not found: " + tableName);
      return ;
    }
    List<String> newData = new ArrayList<String>();
    String id = this.getNewId(table);
    newData.add(id);
    newData.addAll(values); //[name, height, purchaserID]
    newData.add("FALSE");

    // add into table
    table.add(newData);
    this.exportDataToTabFile(tableName);
    
    // Update cache
    // tableCache.invalidate(tableName);
    // tableCache.add(tableName, table);
  }


  public void exportDataToTabFile(String tableName){
    if(tables == null || !tables.containsKey(tableName)){
      System.out.println("[ERROR] Table not found: " + tableName);
      return ;
    }
    try {
      List<List<String>> table = tables.get(tableName);
      StringBuilder fileContent = new StringBuilder();
      for(List<String> rowData: table){
        String rowDataStr = String.join("\t", rowData);
        // System.out.println(rowDataStr);
        fileContent.append(rowDataStr).append("\n");
      }
  
      TableIO tableIO = new TableIO(this.dbName, tableName);
      tableIO.writeFile(fileContent.toString(), false);
    } catch(Exception e){
      System.err.println("[ERROR] exportDataToTabFile: " + e.getMessage());
    }
  }





  //
  /**
   *   UPDATE marks SET age = 35 WHERE name == 'Simon';
   *   UPDATE test SET age = 35, City= 'Frankfurt' WHERE age < 18;
   *    [OK]
   */
  public void updateData(String tableName, Map<String, String> updates, ConditionNode conditionTree){
    loadTableData(tableName);
    _printTable(tableName);

    if (!tables.containsKey(tableName)) {
        System.out.println("[ERROR] Table " + tableName + " does not exist.");
        return;
    }

    List<List<String>> table = tables.get(tableName);
    List<String> header = table.get(0);

    // **🚀 1. 轉換 `header` 欄位名稱為大寫**
    List<String> headerUpper = header.stream()
            .map(String::toUpperCase)
            .collect(Collectors.toList());

    // **🚀 2. 轉換 `updates` 的 key 為大寫比對**
    Map<String, String> updatesUpper = new HashMap<>();
    for (Map.Entry<String, String> entry : updates.entrySet()) {
        updatesUpper.put(entry.getKey().toUpperCase(), entry.getValue());
    }

    // **🚀 3. 取得 `SET` 欄位的 index**
    List<Integer> updateIndices = new ArrayList<>();
    for (String column : updatesUpper.keySet()) {
        int colIdx = headerUpper.indexOf(column);
        if (colIdx == -1) {
            System.out.println("[ERROR] Column " + column + " not found in table " + tableName);
            return;
        }
        updateIndices.add(colIdx);
    }

    // **🚀 4. 遍歷資料，更新符合條件的行**
    for (int i = 1; i < table.size(); i++) {
        Map<String, String> row = new HashMap<>();
        for (int j = 0; j < header.size(); j++) {
            row.put(headerUpper.get(j), table.get(i).get(j)); // 轉成大寫比對
        }

        if (conditionTree == null || conditionTree.evaluate(row)) {
            for (Map.Entry<String, String> entry : updatesUpper.entrySet()) {
                int colIdx = headerUpper.indexOf(entry.getKey());
                table.get(i).set(colIdx, entry.getValue());
            }
            System.out.println("[OK] Updated row: " + table.get(i));
        }
    }

    // **🚀 5. 存回 `.tab` 檔案**
    exportDataToTabFile(tableName);
    System.out.println("[OK] Table " + tableName + " updated successfully.");
}



  public List<List<String>> whereQuery(ConditionNode conditionTree, List<List<String>> table, List<String> header, List<Integer> selectedIdx){
    if(table.isEmpty()) return Collections.emptyList();

    List<List<String>> result = new ArrayList<>();

    // **🚀 1. 先加入篩選後的標題**
    List<String> selectedHeader = selectedIdx.stream()
            .map(header::get)  // 選擇原始標題
            .collect(Collectors.toList());
    result.add(selectedHeader);

    int deleteIdx = header.indexOf("_DELETED");

    for(int i=1; i<table.size(); i++){
      if (deleteIdx != -1 && "TRUE".equalsIgnoreCase(table.get(i).get(deleteIdx))) {
        continue; // skip deleted data.
      }

      List<String> currentRow = table.get(i);
      Map<String, String> row = new HashMap<>();
      for(int j=0; j< header.size(); j++){
        row.put(header.get(j).toUpperCase(), currentRow.get(j));
      }
      System.out.println("row: "+row);

      if(conditionTree.evaluate(row)){
        List<String> selectedRow = selectedIdx.stream()
                .map(idx -> currentRow.get(idx))
                .collect(Collectors.toList());
        result.add(selectedRow);
      }
      System.out.println("result: " + result);
    }
    return result;
  }


  public List<List<String>> getTable(String tableName){
    loadTableData(tableName);
    _printTable(tableName);
    if(tables == null || !tables.containsKey(tableName)){
      System.out.println("[ERROR] Table not found: " + tableName);
      return Collections.emptyList();
    }
    return this.tables.get(tableName);
  }




  /* 
   * 
   * JOIN coursework AND marks ON submission AND id;
   * 
   * Way of using it:
   * List<List<String>> result = joinData("coursework", "marks", "marksId", "id");
   */
  public List<List<String>> joinData(String table1Name, String table2Name, String table1JoinCol, String table2JoinCol){
    System.out.println("table1Name: " + table1Name);
    System.out.println("table2Name: " + table2Name);
    System.out.println("table1JoinCol: " + table1JoinCol);
    System.out.println("table2JoinCol: " + table2JoinCol);
    loadTableData(table1Name);
    loadTableData(table2Name);
    if(!tables.containsKey(table1Name) || !tables.containsKey(table2Name)){
        System.out.println("One or both tables do not exist.");
        return Collections.emptyList();
    }

    List<List<String>> table1 = tables.get(table1Name);
    List<List<String>> table2 = tables.get(table2Name);

    // Get headers from both tables
    List<String> header1 = table1.get(0);
    List<String> header2 = table2.get(0);

    // 找到 JOIN 欄位的索引
    int joinIdx1 = header1.indexOf(table1JoinCol);
    int joinIdx2 = header2.indexOf(table2JoinCol);

    if(joinIdx1 == -1 || joinIdx2 == -1) {
        System.out.println("[ERROR] Join column not found in one or both tables.");
        return Collections.emptyList();
    }

    // **🚀 過濾掉 _DELETED 為 TRUE 的行**
    List<List<String>> filteredTable1 = table1.stream()
            .filter(row -> row == header1 || !row.get(header1.indexOf("_DELETED")).equalsIgnoreCase("TRUE"))
            .collect(Collectors.toList());

    List<List<String>> filteredTable2 = table2.stream()
            .filter(row -> row == header2 || !row.get(header2.indexOf("_DELETED")).equalsIgnoreCase("TRUE"))
            .collect(Collectors.toList());

    // **🚀 準備 JOIN 的標題，去掉 _DELETED**
    List<String> joinedHeader = new ArrayList<>();
    joinedHeader.addAll(header1);
    joinedHeader.addAll(header2);
    joinedHeader.remove("_DELETED");
    joinedHeader.remove("_DELETED");

    List<List<String>> joinedTable = new ArrayList<>();
    joinedTable.add(joinedHeader); // 加入過濾後的標題

    // **🚀 執行 JOIN**
    for(int i = 1; i < filteredTable1.size(); i++){ // 跳過標題行
        List<String> row1 = filteredTable1.get(i);
        for(int j = 1; j < filteredTable2.size(); j++){
            List<String> row2 = filteredTable2.get(j);
            if(row1.get(joinIdx1).equals(row2.get(joinIdx2))){ // 找到匹配行
                List<String> joinedRow = new ArrayList<>();
                joinedRow.addAll(row1);
                joinedRow.addAll(row2);
                
                // **🚀 去除 _DELETED 欄位**
                joinedRow.remove(header1.indexOf("_DELETED"));
                joinedRow.remove(header2.indexOf("_DELETED"));

                joinedTable.add(joinedRow);
            }
        }
    }

    return joinedTable;
}



  /* 
   * add col or drop col
   * ALTER TABLE marks ADD age;
   * ALTER TABLE marks DROP pass;
  */
  public void alterData(String tableName, String action, String columnName){
    loadTableData(tableName);
    _printTable(tableName);

    if(!tables.containsKey(tableName)){
      System.out.println("Table " + tableName + "does not exist.");
      return ;
    }
    List<List<String>> table = tables.get(tableName);

    List<String> header = new ArrayList<>(table.get(0));
    if("ADD".equalsIgnoreCase(action)){
      if(header.contains(columnName)){
        System.out.println("Column " + columnName + " already exists in table " + tableName);
        return ;
      }
      // Add column to header 
      header.add(columnName);
      System.out.println("Column " + columnName + " added successfully.");
      System.out.println("header: " + header);
      // Add default empty values to existing rows;
      for(int i=1; i<table.size(); i++){
        if(table.get(i) == null){
          continue;
        }
        try {
          table.set(i, new ArrayList<>(table.get(i))); // Immutable List turn it to ArrayList, so we can add value
          table.get(i).add(" "); // add an empty string as the default value;
          System.out.println(" table.get(i) " +   table.get(i));
          System.out.println(" table.get(i) " +   table.get(i).size());

        } catch(Exception e){
          System.err.println("[ERROR] Failed to add column to row " + i + ": " + e.getMessage());
          continue;
        }
      }

    } else if ("DROP".equalsIgnoreCase(action)){
      int colIdx = header.indexOf(columnName);
      if(colIdx == -1){
        System.out.println("Column " + columnName + " does not exist in table " + tableName);
        return ;
      }

      if (columnName.equalsIgnoreCase("ID") || columnName.equalsIgnoreCase("_DELETED")) {
        System.out.println("[ERROR] Cannot drop primary key 'ID' or system column '_DELETED'.");
        return;
      }

      //Remove col from header
      header.remove(colIdx);
      System.out.println("Column " + columnName + " dropped successfully.");

      // Remove col from all rows
      for(int i=1; i<table.size(); i++){
        table.get(i).remove(colIdx);
      }

    } else {
      System.out.println("Invalid action: " + action + ". Use ADD or DROP.");
      return ;
    }

    // save updated table
    table.set(0, header);
    tables.put(tableName, table);
    exportDataToTabFile(tableName);
  }

  /*

   * DELETE FROM marks WHERE name == 'Sion';
  */
  public void deleteData(String tableName, ConditionNode conditionTree){
    // ensure table exists;
    loadTableData(tableName);
    _printTable(tableName);
    if(!tables.containsKey(tableName)){
      System.out.println("Table " + tableName + "does not exists.");
      return ;
    }

    List<List<String>> table = tables.get(tableName);
    List<String> header = table.get(0);

    int deleteIdx = header.indexOf("_DELETED");
    if (deleteIdx == -1) {
      System.out.println("[ERROR] Table " + tableName + " does not support deletion tracking.");
      return;
    }

    boolean updated = false;
    for (int i = 1; i < table.size(); i++) {
        Map<String, String> row = convertRowToMap(table.get(i), header);
        if (conditionTree.evaluate(row)) {
            table.get(i).set(deleteIdx, "TRUE"); // ✅ 標記刪除
            updated = true;
        }
    }

    if (updated) {
      exportDataToTabFile(tableName);
      System.out.println("[OK] Deleted matching rows from table " + tableName);
    } else {
        System.out.println("[INFO] No matching records found to delete.");
    }

  }

  public static Map<String, String> convertRowToMap(List<String> row, List<String> header){
    Map<String, String> rowMap = new HashMap<>();
    for(int i = 0; i < header.size(); i++){
      rowMap.put(header.get(i).toUpperCase(), row.get(i));
    }
    return rowMap;
  }

  /* 
   * DROP TABLE marks;
   */
  public void dropTable(String tableName){
    // Remove from cache first
    // tableCache.invalidate(tableName);
    
    // Remove from tables object
    if(tables.containsKey(tableName)) {
      tables.remove(tableName);
    }
    
    // Delete .tab file
    String fileName = String.format("databases/%s/%s.tab", dbName, tableName);
    File file = new File(fileName);
    if(!file.exists()) {
      System.out.println("File not found: " + fileName);
      return;
    }
    if(file.delete()) {
      System.out.println("Table '" + tableName + "' removed successfully.");
    } else {
      System.out.println("Failed to delete file: " + tableName);
    }
  }
}
