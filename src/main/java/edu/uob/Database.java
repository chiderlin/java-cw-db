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
  /**
   * 1.
   * tables = {
   *  users: [
   *   {
   *    id: int,
   *    name: string,
   *    age: int,
   *    email: string
   *   },
   *   {
   *    id: int,
   *    name: string,
   *    age: int,
   *    email: string
   *   }
   *  ],
   * >
   *  sheds: [
   *   {
   *    id: int,
   *    name: string,
   *    height: int,
   *    purchaserId: int
   *   },
   *   {
   *    id: int,
   *    name: string,
   *    height: int,
   *    purchaserId: int
   *   },
   *  ]
   * 
   * 2.>
   * tables = {
   *  users:[[1,'bob',21,'bob@bob.net'],[2,'bob',21,'bob@bob.net'],[1,'bob',21,'bob@bob.net']]
   *  sheds:[[1,'Plaza',1200,1],[2,'Plaza',1200,1]],
   * }
   * 
   * }
   */
  


  public Database(String dbName){
    this.dbName = dbName;
    // this.tableName = 
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
   * 先把tab資料全部視為string, 之後用query再分load進來的類型？？
  */
  public void loadTableData(String tableName){
    try {
        String fileName = String.format("databases/%s/%s.tab", dbName, tableName);
        File fileToOpen = new File(fileName);
        if(!fileToOpen.exists()){
          System.out.println("File not found: "+ fileName);
          return ;
        }

        FileReader reader = new FileReader(fileToOpen);
        BufferedReader buffReader = new BufferedReader(reader);
        String lineData = buffReader.readLine();

        if(lineData == null){
          System.out.println("File is empty " + fileName);
          buffReader.close();
          return ;
        }

        List<String> tableColumn = new ArrayList<>(Arrays.asList(lineData.split("\t")));
        List<List<String>> rowdataArr = new ArrayList<>();
        rowdataArr.add(tableColumn);

        while((lineData = buffReader.readLine()) != null){
          rowdataArr.add(Arrays.asList(lineData.split("\t")));
        }
        
        tables.put(tableName, rowdataArr);
        buffReader.close();
        System.out.println("loadTableData successful");
        // System.out.println(tables); //rs: {sheds=[[id, Name, Height, PurchaserID], [1, Dorchester, 1800, 3], [2, Plaza, 1200, 1], [3, Excelsior, 1000, 2]]}

    } catch(IOException e){
        System.err.println(e.getMessage());
    }
  }


  public void _printTable(String tableName){
    List<List<String>> table = this.tables.get(tableName);
    if(table != null && !table.isEmpty()){
      System.out.println("table.size() " + table.size());
      System.out.println(String.format("print table <%s>: %s",tableName,table));
      return;
    }
    System.out.println("[ERROR _printTable] Table Not Found.");
  }
  
  
  /*
  skip datatype just now.. seems not necessary 
  CREATE TABLE User (
      id int -> generate automatically
      Name varchar(50),
      Age int,
      Email varchar(50)
  );
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

    String data = String.join("\t",values);
    writeFile(tableName, data);

    return "[OK]";
  }


  private String getNewId(List<List<String>> table){
    return String.valueOf(table.size());

  }


  public void insertData(String tableName, List<String>values){
    List<List<String>> table = this.tables.get(tableName);
    List<String> newData = new ArrayList<String>();
    String id = this.getNewId(table);
    newData.add(id);
    newData.addAll(values); //[name, height, purchaserID]
    // add into table
    table.add(newData);
    this.exportDataToTabFile(tableName);
  }


  public void exportDataToTabFile(String tableName){
    List<List<String>> table = tables.get(tableName);
    if(table == null){
      System.out.println("Table not found: " + tableName);
      return ;
    }

    StringBuilder fileContent = new StringBuilder();
    for(List<String> rowData: table){
      String rowDataStr = rowData.stream().map(String::toString).collect(Collectors.joining("\t"));
      System.out.println(rowDataStr);
      fileContent.append(rowDataStr).append("\n");
    }

    this.writeFile(tableName, fileContent.toString());
  }


  public void writeFile(String tableName, String data){
    if(this.dbName == null) return;
    String fileName = String.format("databases/%s/%s.tab", this.dbName, tableName); //TODO: 有更好寫法？
    File fileToOpen = new File(fileName);
    try {
      if(!fileToOpen.exists()){
        fileToOpen.createNewFile();
      }

      // try-with-resources -> close pipe automatically when finishing
      try(BufferedWriter buffWriter = new BufferedWriter(new FileWriter(fileToOpen))){
        buffWriter.write(data);
      }

      System.out.println("write file successfully.");

    } catch(IOException e){
      System.err.println(e.getMessage());
    }
  }


  //TODO:updateData
  /**
   *   UPDATE marks SET age = 35 WHERE name == 'Simon';
   *    [OK]
   */
  public void updateData(){

  }

  public boolean compareValue(String operator, String value1, String value2){
    if(value1 == null || value2 == null) return false;
    try {
      double num1 = Double.parseDouble(value1);
      double num2 = Double.parseDouble(value2);
      return switch(operator){
        case "==" -> num1 == num2;
        case "!=" -> num1 != num2;
        case ">" -> num1 > num2;
        case "<" -> num1 < num2;
        case ">=" -> num1 >= num2;
        case "<=" -> num1 <= num2;
        default -> false;
      };

    } catch(NumberFormatException e) {
      return switch(operator){
        case "==" -> value1.equals(value2);
        case "!=" -> !value1.equals(value2);
        case ">" -> value1.compareTo(value2) > 0;
        case "<" -> value1.compareTo(value2) < 0;
        case ">=" -> value1.compareTo(value2) >= 0;
        case "<=" -> value1.compareTo(value2) <= 0;
        default -> false;
      };
    }
  }

  public List<List<String>> whereQuery(List<String> con, List<List<String>> table, List<String> header, List<Integer> selectedIdx, boolean isOrCondtion){

      // 分==|!=|>=|<=|>|<
      List<QueryCondition> parsedConditions = con.stream()
                .map(cond -> this.parseCondition(cond, header))
                .filter(cond -> cond != null)
                .collect(Collectors.toList());

      System.out.println("parsedConditions" + parsedConditions);


      List<List<String>> result = table.stream()
                .skip(1)
                .filter(row -> {
                  System.out.println("row:"+row);
                  
                  return isOrCondtion ? parsedConditions.stream().anyMatch(cond -> compareValue(cond.operator, row.get(cond.columnIndex), cond.targetValue)) : parsedConditions.stream().allMatch(cond -> {
                    System.out.println("cond.columnIndex: "+cond.columnIndex);
                    System.out.println("compareValue(cond.operator, row.get(cond.columnIndex), cond.targetValue):"+compareValue(cond.operator, row.get(cond.columnIndex), cond.targetValue));
                    return compareValue(cond.operator, row.get(cond.columnIndex), cond.targetValue);
                  });
                })
                // .filter(row -> row.get(columnIndex).toString().equals(targetValue))
                // .filter(row -> compareValue(operator,row.get(columnIndex), targetValue))
                .map(row -> {
                  return selectedIdx.stream().map(index -> row.get(index)).collect(Collectors.toList());
                })
                .collect(Collectors.toList());
      System.out.println("result" + result);
      

      List<String> selectedHeader = selectedIdx.stream().map(index -> header.get(index)).collect(Collectors.toList());
      result.add(0, selectedHeader);
      return result;
    // return Collections.emptyList();
  }

  //TODO:getData
  /**
   *   SELECT * FROM marks;
   *    [OK]
   * 
   * col: ["name","age"] or ["*"]
   * condition: WHERE name == "bob" or WHERE age > 18 or WHERE name like "bob";
   */
  public List<List<String>> getData(String tableName, List<String>selectedCols, Map<String, List<String>> condition){

    System.out.println("condition: "+condition);
    loadTableData(tableName);
    _printTable(tableName);
    List<List<String>> table = this.tables.get(tableName);
    if(table == null || table.isEmpty()) return Collections.emptyList();

    // uppercase on column
    List<String> header = table.get(0).stream().map(column -> column.toString().toUpperCase()).collect(Collectors.toList()); 
    System.out.println("header " + header);
    List<Integer> selectedIdx = selectedCols.stream()
                      .map(col -> header.indexOf(col.toUpperCase()))
                      .filter(index -> index != -1)
                      .collect(Collectors.toList());
    System.out.println("selectedIdx" + selectedIdx);
    if(selectedIdx.isEmpty()){
      System.out.println("Selected columns not found.");
      return Collections.emptyList();
    }

    if(condition.containsKey("condition") && !condition.get("condition").isEmpty()) {
      List<String> con = condition.get("condition");
      List<List<String>> result = this.whereQuery(con, table, header, selectedIdx,false);
      String tabResult = result.stream()
                .map(row -> String.join("\t", row))
                .collect(Collectors.joining("\n"));
      System.out.println(tabResult);
    }

    // FIXME: not correct
    if(condition.containsKey("add") && !condition.get("add").isEmpty()){
        List<String> con = condition.get("add");
        List<List<String>> result = this.whereQuery(con, table, header, selectedIdx,false);
        String tabResult = result.stream()
                .map(row -> String.join("\t", row))
                .collect(Collectors.joining("\n"));
        System.out.println(tabResult);
    }

    // FIXME: not correct
    if(condition.containsKey("or") && !condition.get("or").isEmpty()){
        List<String> con = condition.get("or");
        List<List<String>> result = this.whereQuery(con, table, header, selectedIdx,true);
        String tabResult = result.stream()
                .map(row -> String.join("\t", row))
                .collect(Collectors.joining("\n"));
        System.out.println(tabResult);
    }

    return Collections.emptyList();
  }

  private QueryCondition parseCondition(String condition, List<String> header){
    Pattern pattern = Pattern.compile("\\s*(==|!=|>=|<=|>|<)\\s*");
    Matcher matcher = pattern.matcher(condition);
    if(!matcher.find()){
      System.out.println("Invalid condition format: " + condition);
      return null;
    }
    String columnName = condition.substring(0, matcher.start()).trim();
    System.out.println("columnName " + columnName);
    String operator = matcher.group(1);
    System.out.println("operator " + operator);
    String targetValue = condition.substring(matcher.end()).trim();
    System.out.println("targetValue " + targetValue);

    int columnIndex = header.indexOf(columnName);
    if(columnIndex == -1){
      System.out.println("Column not found: " + columnName);
      return null;
    }

    QueryCondition queryCondition = new QueryCondition(columnName, operator, targetValue);
    queryCondition.setColumnIndex(columnIndex);
    return queryCondition;
  }

  
  class QueryCondition {
    String columnName, operator, targetValue;
    int columnIndex;

    QueryCondition(String columnName, String operator, String targetValue){
      this.columnName = columnName;
      this.operator = operator;
      this.targetValue = targetValue;
    }

    void setColumnIndex(int index){
      this.columnIndex = index;
    }
  }


  //TODO:joinData
  /**
   * JOIN coursework AND marks ON submission AND id;
   */
  public List<List<String>> joinData(String table1Name, String table2Name, String table1JoinCol, String table2JoinCol){
    if(!tables.containsKey(table1Name) || !tables.containsKey(table2Name)){
      System.out.println("One or both tables do not exist.")
      return Collections.emptyList();
    }

    List<List<String>> table1 = tables.get(table1Name);
    List<List<String>> table2 = tables.get(table2Name);

    // Get header from both tables;
    List<String> header1 = table1.get(0);
    List<String> header2 = table2.get(0);

    
    // find the index of the join col in both tables.
    int joinIdx1 = header1.indexOf(table1JoinCol);
    int joinIdx2 = header2.indexOf(table2JoinCol);

    if(joinIdx1 == -1 || joinIdx2 == -1) {
      System.out.println("Join column not found in one or both tables.");
      return Collections.emptyList();
    }

    //create the joined header
    List<String> joinedHeader = new ArrayList<>(header1)
    joinedHeader.addAll(header2);

    // create a list to store the joined rows
    List<List<String>> joinedTable = new ArrayList<>()
    joinedTable.add(joinedHeader);

    // perform the join
    for(int i=1; i< table1.size(); i++){ // skip header row
      List<String> row1 = table1.get(i);
      for(int j=1; j< table2.size(); j++){
        List<String> row2 = table2.get(j);
        if(row1.get(joinIdx1).equals(row2.get(joinIdex2))){ // watching rows
          List<String> joinedRow = new ArrayList<>(row1);
          joinedRow.addAll(row2);
          joinedTable.add(joinedRow);
        }
      }
    }
    return joinedTable;
  }

}
