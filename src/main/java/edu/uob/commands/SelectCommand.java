package edu.uob.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.uob.ConditionNode;
import edu.uob.Database;
import edu.uob.QueryParser;

public class SelectCommand extends DBCommand {
  public SelectCommand(Database db, String command){
    super(db, command);
  }


  @Override
  public String execute(){
    if(db == null) {
        System.err.println("[ERROR] Switch database required.");
        return "[ERROR] Switch database required.";
    }

    try {
        // **🚀 1. 清理語法**
        cmd = cmd.trim().replaceAll("\\s+", " ").replaceAll(";$", ""); // 清理多餘空格 & `;`
        
        if (!cmd.matches("(?i)^SELECT\\s+.+\\s+FROM\\s+.+")) {
            System.err.println("[ERROR] Invalid SELECT syntax.");
            return "[ERROR] Invalid SELECT syntax.";
        }
    
        // **🚀 2. 拆解 SELECT 語法**
        String[] cmds = this.cmd.split("(?i)FROM");
        if (cmds.length < 2 || cmds[1].trim().isEmpty()) {
            System.err.println("[ERROR] Missing FROM table name.");
            return "[ERROR] Missing FROM table name.";
        }
    
        // **🚀 3. 獲取 tableName**
        String[] tableParts = cmds[1].split("(?i)WHERE", 2);
        String tableName = tableParts[0].trim();
        if (tableName.isEmpty()) {
            System.err.println("[ERROR] Table name missing after FROM.");
            return "[ERROR] Table name missing after FROM.";
        }
    
        // **🚀 4. 確保 Table 存在**
        List<List<String>> table = db.getTable(tableName);
        if (table.isEmpty()) {
            System.err.println("[ERROR] Table " + tableName + " does not exist.");
            return "[ERROR] Table " + tableName + " does not exist.";
        }
    
        // **🚀 5. 確保 SELECT 欄位**
        String queryCol = cmds[0].replaceFirst("(?i)^SELECT\\s*", "").trim();
        if (queryCol.isEmpty()) {
            System.err.println("[ERROR] Missing column names after SELECT.");
            return "[ERROR] Missing column names after SELECT.";
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
                System.err.println("[ERROR] Invalid WHERE condition syntax.");
                return "[ERROR] Invalid WHERE condition syntax.";
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
        return "[OK]\n" + tableResult;
    
    } catch (Exception e) {
        System.err.println("[ERROR] parseSelect: " + e.getMessage());
        return "[ERROR] parseSelect: " + e.getMessage();
    }
        
  }

}
