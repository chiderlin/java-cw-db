package edu.uob.commands;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.uob.Database;

public class TableCommand extends DBCommand {
  public TableCommand(Database db, String command){
    super(db, command);
  }

  @Override
  public String execute(){
    String formatCommand = this.cmd.trim().toUpperCase();
    if (formatCommand.startsWith("CREATE TABLE ")) {
      return parseCreateTable(this.cmd);

    } else if (formatCommand.startsWith("DROP TABLE ")) {
        return parsedropTable(this.cmd);
    }
    return "";
  }

  public String parseCreateTable(String cmd) {
    if (db == null) {
        System.err.println("[ERROR] Switch database required.");
        return "[ERROR] Switch database required.";
    }

    // **🚀 1. 清理 `);` 確保語法正確**
    String rawCmd = cmd.substring(13).trim().replaceAll("\\);$", "").trim(); // 確保 `);` 被移除

    // **🚀 2. 分割語法 -> `tableName` 和 `cols`**
    String[] parts = rawCmd.split("\\(", 2);
    if (parts.length < 2) {
        System.err.println("[ERROR] Invalid CREATE TABLE syntax.");
        return "[ERROR] Invalid CREATE TABLE syntax.";
    }

    String tableName = parts[0].trim(); // `marks`
    String cols = parts[1].trim();      // `name, mark, pass`

    // **🚀 3. 確保 `tableName` 不為空**
    if (tableName.isEmpty()) {
        System.err.println("[ERROR] Table name missing in CREATE TABLE.");
        return "[ERROR] Table name missing in CREATE TABLE.";
    }

    // **🚀 4. 解析欄位名稱，去掉括號**
    List<String> values = new ArrayList<>(Arrays.asList(cols.split("\\s*,\\s*")));

    // **🚀 5. 確保 `values` 不為空**
    if (values.isEmpty() || values.get(0).isEmpty()) {
        System.err.println("[ERROR] No columns specified in CREATE TABLE.");
        return "[ERROR] No columns specified in CREATE TABLE.";
    }


    // **🚀 6. 印出解析後的欄位**
    System.out.println("Parsed columns: " + values); // ✅ 輸出 `[name, mark, pass]`
    return db.createTable(tableName, values);
}


  public String parsedropTable(String cmd){
    if(db == null){
        System.out.println("[ERROR] Switch database required.");
        return "[ERROR] Switch database required.";
    }

    cmd = cmd.trim().replaceAll(";$","").trim();
    if(!cmd.matches("(?i)^DROP\\s+TABLE\\s+.+$")){
        System.out.println("[ERROR] Invalid DROP TABLE syntax.");
        return "[ERROR] Invalid DROP TABLE syntax.";
    }
    String tableName = cmd.replaceFirst("(?i)^DROP\\s+TABLE\\s+", "").trim();

    if(tableName.isEmpty()){
        System.out.println("[ERROR] Missing table name in DROP TABLE.");
        return "[ERROR] Missing table name in DROP TABLE.";
    }

    return db.dropTable(tableName);
  }

}
