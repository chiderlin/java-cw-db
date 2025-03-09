package edu.uob.commands;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import edu.uob.Database;

public class InsertCommand extends DBCommand {
  public InsertCommand(Database db, String command){
    super(db, command);
  }

  @Override
  public String execute(){
    try {
      // **🚀 1. 清理語法**
      cmd = cmd.trim().replaceAll("\\s+", " ").replaceAll(";$", "");  // 清理多餘空格 & `;`
      if (!cmd.matches("(?i)^INSERT INTO\\s+\\w+\\s+VALUES\\s*\\(.*\\)$")) {
          System.err.println("[ERROR] Invalid INSERT syntax.");
          return "[ERROR] Invalid INSERT syntax.";
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
      return db.insertData(tableName, values);

    } catch (Exception e) {
        System.err.println("[ERROR] parseInsert: " + e.getMessage());
        return "[ERROR] parseInsert: " + e.getMessage();
    }
  }
}
