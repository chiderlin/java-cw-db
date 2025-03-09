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
        cmd = cmd.trim().replaceAll("\\s+", " ").replaceAll(";$", "");
        if (!cmd.matches("(?i)^SELECT\\s+.+\\s+FROM\\s+.+")) {
            System.err.println("[ERROR] Invalid SELECT syntax.");
            return "[ERROR] Invalid SELECT syntax.";
        }
    
        String[] cmds = this.cmd.split("(?i)FROM");
        if (cmds.length < 2 || cmds[1].trim().isEmpty()) {
            System.err.println("[ERROR] Missing FROM table name.");
            return "[ERROR] Missing FROM table name.";
        }
    
        String[] tableParts = cmds[1].split("(?i)WHERE", 2);
        String tableName = tableParts[0].trim();
        if (tableName.isEmpty()) {
            System.err.println("[ERROR] Table name missing after FROM.");
            return "[ERROR] Table name missing after FROM.";
        }
    
        List<List<String>> table = db.getTable(tableName);
        if (table.isEmpty()) {
            System.err.println("[ERROR] Table " + tableName + " does not exist.");
            return "[ERROR] Table " + tableName + " does not exist.";
        }
    
        String queryCol = cmds[0].replaceFirst("(?i)^SELECT\\s*", "").trim();
        if (queryCol.isEmpty()) {
            System.err.println("[ERROR] Missing column names after SELECT.");
            return "[ERROR] Missing column names after SELECT.";
        }
    
        List<String> header = table.get(0);
        Map<String, String> columnMap = new HashMap<>();
    
        for (String col : header) {
            columnMap.put(col.toUpperCase(), col); // uppercase be key，original be value
        }
    
        List<String> selectedCols;
        if (queryCol.equals("*")) {
            selectedCols = new ArrayList<>(header); // all cols
        } else {
            selectedCols = Arrays.stream(queryCol.split("\\s*,\\s*"))
                    .map(col -> columnMap.getOrDefault(col.toUpperCase(), col)) // relevant cols
                    .collect(Collectors.toList());
        }
        selectedCols.remove("_DELETED");
        System.out.println("selectedCols: " + selectedCols);
    
        // handle WHERE condition
        ConditionNode conditionTree = null;
        if (tableParts.length > 1) {
            String whereClause = tableParts[1].trim();
            if (!whereClause.matches("(?i).+\\s*(==|!=|>|<|>=|<=|LIKE)\\s*.+")) {
                System.err.println("[ERROR] Invalid WHERE condition syntax.");
                return "[ERROR] Invalid WHERE condition syntax.";
            }
            conditionTree = QueryParser.parseWhere(whereClause);
        }
    
        List<Integer> selectIdx = selectedCols.stream()
            .map(col -> header.indexOf(col)) // original col index
            .filter(index -> index != -1)
            .collect(Collectors.toList());
    

        int deleteIdx = header.indexOf("_DELETED");
        List<List<String>> filteredTable = new ArrayList<>();
        filteredTable.add(selectedCols); // add header

        for (int i = 1; i < table.size(); i++) {
            List<String> row = table.get(i);
            if (deleteIdx != -1 && "TRUE".equalsIgnoreCase(row.get(deleteIdx))) {
                continue; // skip `_DELETED = TRUE` 
            }

            Map<String, String> rowMap = Database.convertRowToMap(row, header);
            if (conditionTree == null || conditionTree.evaluate(rowMap)) {
                List<String> selectedRow = selectIdx.stream()
                        .map(row::get)
                        .collect(Collectors.toList());
                filteredTable.add(selectedRow);
            }
        }

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
