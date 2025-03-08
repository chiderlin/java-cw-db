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
  boolean evaluate(Map<String, String> row){
    if(condition != null){
      return evaluateCondition(row, condition);
    }
    boolean leftResult = left.evaluate(row);
    System.out.println("leftResult " + leftResult);
    boolean rightResult = right.evaluate(row);
    System.out.println("rightResult " + rightResult);

    return operator.equalsIgnoreCase("AND") ? (leftResult && rightResult) : (leftResult || rightResult);
  }

  // single condition
  private boolean evaluateCondition(Map<String,String> row, String condition){
    String[] parts = condition.split("\\s*(==|!=|>|<|>=|<=|LIKE)\\s*");
    System.out.println("parts: " + parts[0]);
    if(parts.length != 2) return false;
    String column = parts[0].trim().toUpperCase();
    System.out.println("column: " + column);

    String value = parts[1].replaceAll("['\"]", "").replaceAll(";$","").trim();
    System.out.println("value: " + value);
    String rowValue = row.get(column);
    System.out.println("rowValue: " + rowValue);

    if(rowValue == null) return false;

    String operator = condition.replaceAll(".*?(==|!=|>|<|>=|<=|LIKE).*", "$1");

    if(operator.equals("LIKE")){
      return rowValue.toUpperCase().contains(value.toUpperCase());
    }

    if(isNumeric(rowValue) && isNumeric(value)){
      double num1 = Double.parseDouble(rowValue);
      double num2 = Double.parseDouble(value);
      return switch(operator){
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
    return str.matches("-?\\d+(\\.\\d+)?");
  }
}
