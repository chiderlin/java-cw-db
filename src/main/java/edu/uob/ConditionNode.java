package edu.uob;
import java.util.Map;



public class ConditionNode {
  String condition; 
  String operator; // AND or OR
  ConditionNode left, right; 

  ConditionNode(String condition){
    this.condition = condition;
    this.operator = null;
    this.left = this.right = null;
  }

  ConditionNode(String operator, ConditionNode left, ConditionNode right){
    this.operator = operator;
    this.right = right;
    this.left = left;
  }

  // resursively evaluate 
  public boolean evaluate(Map<String, String> row){
    if(condition != null){
      boolean result = evaluateCondition(row, condition);
      System.out.println("[DEBUG] Evaluating condition: " + condition + " -> " + result);
      return result;
      // return evaluateCondition(row, condition);
    }
    boolean leftResult = left.evaluate(row);
    boolean rightResult = right.evaluate(row);
    
    System.out.println("[DEBUG] leftResult " + leftResult);
    System.out.println("[DEBUG] rightResult " + rightResult);
    System.out.println("[DEBUG] operator " + operator);

    boolean finalResult = operator.equalsIgnoreCase("AND") ? (leftResult && rightResult) : (leftResult || rightResult);
    System.out.println("[DEBUG] Final Result: " + finalResult);
    return finalResult;
    // return operator.equalsIgnoreCase("AND") ? (leftResult && rightResult) : (leftResult || rightResult);
  }

  // single condition
  private boolean evaluateCondition(Map<String,String> row, String condition){
    String[] parts = condition.split("(?<=<=|>=|==|!=|>|<|LIKE)\\s+");
    if(parts.length != 2) return false;
    String column = parts[0].replaceAll("\\s*(<=|>=|==|!=|>|<|LIKE)\\s*$", "").trim().toUpperCase();
    String value = parts[1].replaceAll("['\"]", "").replaceAll(";$", "").trim();
    String rowValue = row.get(column);
    
    System.out.println("[INFO] parts: " + parts[0]);
    System.out.println("[INFO] column: " + column);
    System.out.println("[INFO] value: " + value);
    System.out.println("[INFO] rowValue: " + rowValue);
    
    if(rowValue == null) return false;
    String operator = condition.replaceAll("^.*?\\s*(<=|>=|==|!=|>|<|LIKE)\\s*.*$", "$1");
    
    // LIKE: case sensitive
    if(operator.equals("LIKE")){ 
      return rowValue.contains(value);
    }

    if(isNumeric(rowValue) && isNumeric(value)){
      double num1 = Double.parseDouble(rowValue);
      double num2 = Double.parseDouble(value);
      System.out.println("[INFO] num1: "+ num1);
      System.out.println("[INFO] num2: "+ num2);
      return switch (operator) {
          case "==" -> num1 == num2;
          case "!=" -> num1 != num2;
          case ">" -> num1 > num2;
          case "<" -> num1 < num2;
          case ">=" -> num1 >= num2;
          case "<=" -> num1 <= num2;
          default -> false;
      };
    }
    
    // if not number, do sting 
    return switch(operator){
      case "==" -> rowValue.equals(value);
      case "!=" -> !rowValue.equals(value);
      default -> false;
    };
  }

  
  private boolean isNumeric(String str){
    System.out.println("[INFO] str: "+ str);
    return str.matches("-?\\d+(\\.\\d+)?");
  }
}
