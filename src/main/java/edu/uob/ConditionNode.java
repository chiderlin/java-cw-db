package edu.uob;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConditionNode {
  String condition;
  String operator; // AND or OR
  ConditionNode left, right;

  ConditionNode(String condition) {
    this.condition = condition;
    this.operator = null;
    this.left = this.right = null;
  }

  ConditionNode(String operator, ConditionNode left, ConditionNode right) {
    this.operator = operator;
    this.right = right;
    this.left = left;
  }

  // resursively evaluate
  public boolean evaluate(Map<String, String> row) {
    if (condition != null) {
      boolean result = evaluateCondition(row, condition);
      // System.out.println("[DEBUG] Evaluating condition: " + condition + " -> " +
      // result);
      // System.out.println("[INFO] ----END----");
      return result;
    }
    // System.out.println("left: " + left);
    // System.out.println("right: " + right);

    boolean leftResult = left.evaluate(row);
    boolean rightResult = right.evaluate(row);

    // System.out.println("[DEBUG] leftResult " + leftResult);
    // System.out.println("[DEBUG] rightResult " + rightResult);
    // System.out.println("[DEBUG] operator " + operator);

    if ("AND".equalsIgnoreCase(operator)) {
      boolean finalResult = leftResult && rightResult;
      // System.out.println("[DEBUG] Final Result: (AND) " + finalResult);
      // System.out.println("------------------------");
      return finalResult;
    }
    if ("OR".equalsIgnoreCase(operator)) {
      boolean finalResult = leftResult || rightResult;
      // System.out.println("[DEBUG] Final Result: (OR) " + finalResult);
      // System.out.println("------------------------");

      return finalResult;
    }

    return false;

  }

  // single condition
  private boolean evaluateCondition(Map<String, String> row, String condition) {
    // System.out.println("row: " + row);
    String[] parts = splitCondition(condition);
    if (parts.length != 3) {
      System.err.println("[ERROR] Invalid condition: " + condition);
      return false;
    }
    // for (String part : parts) {
    // System.out.println("part: " + part);
    // }

    String column = parts[0].trim();
    String extractOperator = parts[1].trim();
    String value = parts[2].trim().replaceAll("^['\"]|['\"]$", ""); // 去除 SQL 引號
    String rowValue = row.get(column.toUpperCase());

    // System.out.println("[INFO] ----START----");
    // System.out.println("[INFO] parts: " + parts[0]);
    // System.out.println("[INFO] column: " + column);
    // System.out.println("[INFO] value: " + value);
    // System.out.println("[INFO] rowValue: " + rowValue);

    if (rowValue == null)
      return false;

    // LIKE: case sensitive
    if (extractOperator.equalsIgnoreCase("LIKE")) {
      return rowValue.contains(value);
    }

    if (isNumeric(rowValue) && isNumeric(value)) {
      double num1 = Double.parseDouble(rowValue);
      double num2 = Double.parseDouble(value);
      // System.out.println("[INFO] num1: " + num1);
      // System.out.println("[INFO] num2: " + num2);
      return switch (extractOperator) {
        case "==" -> num1 == num2;
        case "!=" -> num1 != num2;
        case ">" -> num1 > num2;
        case "<" -> num1 < num2;
        case ">=" -> num1 >= num2;
        case "<=" -> num1 <= num2;
        default -> false;
      };
    }

    // System.out.println("STRING area rowValue: " + rowValue);
    // System.out.println("STRING area value: " + value);

    // if not number, do sting
    return switch (extractOperator) {
      case "==" -> rowValue.equals(value);
      case "!=" -> !rowValue.equals(value);
      default -> false;
    };
  }

  private boolean isNumeric(String str) {
    // System.out.println("[INFO] str: " + str);
    return str.matches("-?\\d+(\\.\\d+)?");
  }

  public static String[] splitCondition(String condition) {
    String operatorRegex = "(==|!=|<=|>=|<|>|LIKE)";

    Pattern pattern = Pattern.compile(operatorRegex);
    Matcher matcher = pattern.matcher(condition);

    if (matcher.find()) {
      int opIndex = matcher.start();
      String leftPart = condition.substring(0, opIndex).trim();
      String operator = matcher.group().trim();
      String rightPart = condition.substring(matcher.end()).trim();
      return new String[] { leftPart, operator, rightPart };
    }
    return new String[] { condition };
  }
}
