package edu.uob;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExampleDBTests {

    private DBServer server;

    // Create a new server _before_ every @Test
    @BeforeEach
    public void setup() {
        server = new DBServer();
    }

    // Random name generator - useful for testing "bare earth" queries (i.e. where
    // tables don't previously exist)
    private String generateRandomName() {
        String randomName = "";
        for (int i = 0; i < 10; i++)
            randomName += (char) (97 + (Math.random() * 25.0));
        return randomName;
    }

    private String sendCommandToServer(String command) {
        // Try to send a command to the server - this call will timeout if it takes too
        // long (in case the server enters an infinite loop)
        return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> {
            return server.handleCommand(command);
        },
                "Server took too long to respond (probably stuck in an infinite loop)");
    }

    // A basic test that creates a database, creates a table, inserts some test
    // data, then queries it.
    // It then checks the response to see that a couple of the entries in the table
    // are returned as expected
    @Test
    public void testBasicCreateAndQuery() {
        String randomName = generateRandomName();
        System.out.println("randomName: " + randomName);
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Sion', 55, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Rob', 35, FALSE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Chris', 20, FALSE);");
        String response = sendCommandToServer("SELECT * FROM marks;");
        assertTrue(response.contains("[OK]"), "A valid query was made, however an [OK] tag was not returned");
        assertFalse(response.contains("[ERROR]"), "A valid query was made, however an [ERROR] tag was returned");
        assertTrue(response.contains("Simon"),
                "An attempt was made to add Simon to the table, but they were not returned by SELECT *");
        assertTrue(response.contains("Chris"),
                "An attempt was made to add Chris to the table, but they were not returned by SELECT *");
    }

    // A test to make sure that querying returns a valid ID (this test also
    // implicitly checks the "==" condition)
    // (these IDs are used to create relations between tables, so it is essential
    // that suitable IDs are being generated and returned !)
    @Test
    public void testQueryID() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        String response = sendCommandToServer("SELECT id FROM marks WHERE name == 'Simon';");
        // Convert multi-lined responses into just a single line
        String singleLine = response.replace("\n", " ").trim();
        // Split the line on the space character
        String[] tokens = singleLine.split(" ");
        // Check that the very last token is a number (which should be the ID of the
        // entry)
        String lastToken = tokens[tokens.length - 1];

        try {
            Integer.valueOf(lastToken);
        } catch (NumberFormatException nfe) {
            fail("The last token returned by `SELECT id FROM marks WHERE name == 'Simon';` should have been an integer ID, but was "
                    + lastToken);
        }
    }

    // A test to make sure that databases can be reopened after server restart
    @Test
    public void testTablePersistsAfterRestart() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        // Create a new server object
        server = new DBServer();
        sendCommandToServer("USE " + randomName + ";");
        String response = sendCommandToServer("SELECT * FROM marks;");
        assertTrue(response.contains("Simon"),
                "Simon was added to a table and the server restarted - but Simon was not returned by SELECT *");
    }

    // Test to make sure that the [ERROR] tag is returned in the case of an error
    // (and NOT the [OK] tag)
    @Test
    public void testForErrorTag() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        String response = sendCommandToServer("SELECT * FROM libraryfines;");
        assertTrue(response.contains("[ERROR]"),
                "An attempt was made to access a non-existent table, however an [ERROR] tag was not returned");
        assertFalse(response.contains("[OK]"),
                "An attempt was made to access a non-existent table, however an [OK] tag was returned");
    }

    @Test
    public void testAlterTable() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE students (name, age);");

        sendCommandToServer("ALTER TABLE students ADD grade;");
        String response = sendCommandToServer("SELECT * FROM students;");
        assertTrue(response.contains("grade"), "The column 'grade' was added, but not found in the table schema.");

        sendCommandToServer("ALTER TABLE students DROP age;");
        response = sendCommandToServer("SELECT * FROM students;");
        assertFalse(response.contains("age"), "The column 'age' was dropped, but still found in the table schema.");
    }

    @Test
    public void testDeleteData() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE employees (name, salary);");
        sendCommandToServer("INSERT INTO employees VALUES ('Alice', 4000);");
        sendCommandToServer("INSERT INTO employees VALUES ('Bob', 4500);");
        sendCommandToServer("INSERT INTO employees VALUES ('Alex', 6000);");
        sendCommandToServer("INSERT INTO employees VALUES ('Peter', 6600);");

        sendCommandToServer("DELETE FROM employees WHERE name == 'Bob';");

        String response = sendCommandToServer("SELECT * FROM employees;");
        assertFalse(response.contains("Bob"), "Bob was deleted but still appears in SELECT results.");
        assertTrue(response.contains("Alice"), "Alice should not be deleted but is missing from SELECT results.");

        sendCommandToServer("DELETE FROM employees WHERE salary <= 5000;");
        String response2 = sendCommandToServer("SELECT * FROM employees;");
        assertFalse(response2.contains("Bob"), "Bob was deleted but still appears in SELECT results.");
        assertFalse(response2.contains("Alice"), "Alice was deleted but still appears in SELECT results.");
        assertTrue(response2.contains("Alex"), "Alex should not be deleted but is missing from SELECT results.");

    }

    @Test
    public void testUpdateData() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE products (name, price);");
        sendCommandToServer("INSERT INTO products VALUES ('Laptop', 1000);");

        sendCommandToServer("UPDATE products SET price = 1200 WHERE name == 'Laptop';");

        String response = sendCommandToServer("SELECT * FROM products;");
        assertTrue(response.contains("1200"), "Laptop's price was updated to 1200, but not found in SELECT results.");
        assertFalse(response.contains("1000"), "Old price 1000 should have been updated, but still exists.");
    }

    @Test
    public void testJoinTables() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE orders (order_id, customer_id);");
        sendCommandToServer("CREATE TABLE customers (customer_id, name);");
        sendCommandToServer("INSERT INTO orders VALUES (1, 101);");
        sendCommandToServer("INSERT INTO orders VALUES (2, 101);");

        sendCommandToServer("INSERT INTO customers VALUES (101, 'John Doe');");
        sendCommandToServer("INSERT INTO customers VALUES (102, 'Tom');");

        String response = sendCommandToServer("JOIN orders AND customers ON customer_id AND customer_id;");
        assertTrue(response.contains("John Doe"),
                "Customer 'John Doe' should appear in the join result but is missing.");
        assertTrue(response.contains("1"), "Order ID '1' should appear in the join result but is missing.");
    }

    @Test
    public void testInvalidSQL() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");

        String response = sendCommandToServer("CREAT TABLE students (name, age);");
        assertTrue(response.contains("[ERROR]"), "An invalid SQL command was used, but no [ERROR] tag was returned.");

        response = sendCommandToServer("INSERT INTO students ('Alice', 20);");
        assertTrue(response.contains("[ERROR]"),
                "An invalid INSERT statement was used, but no [ERROR] tag was returned.");
    }

    // NULL -> store "" in db
    @Test
    public void testNullValues() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE employees (name, salary);");

        sendCommandToServer("INSERT INTO employees VALUES ('Alice', NULL);");
        sendCommandToServer("INSERT INTO employees VALUES (NULL, 5000);");

        String response = sendCommandToServer("SELECT * FROM employees;");
        assertTrue(response.contains(""), "Inserted NULL values, but they were not returned in SELECT.");
    }

    @Test
    public void testCaseInsensitiveSQL() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE students (name, age);");
        sendCommandToServer("INSERT INTO students VALUES ('Alice', 20);");

        String response1 = sendCommandToServer("select * from students;");
        String response2 = sendCommandToServer("SeLeCt * FROM students;");
        assertTrue(response1.equals(response2), "SQL should be case insensitive, but results differ.");
    }

    @Test
    public void testAdvanceWhereQuery() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE employees (name, salary);");
        sendCommandToServer("INSERT INTO employees VALUES ('Alice', 4000);");
        sendCommandToServer("INSERT INTO employees VALUES ('Bob', 4500);");
        sendCommandToServer("INSERT INTO employees VALUES ('Alex', 6000);");
        sendCommandToServer("INSERT INTO employees VALUES ('Peter', 6600);");
        sendCommandToServer("INSERT INTO employees VALUES ('Amy', 5500);");

        String response = sendCommandToServer("SELECT * FROM employees WHERE name LIKE 'b';");
        assertTrue(response.contains("Bob"), "Bob should appears in SELECT results.");
        assertFalse(response.contains("Alice"), "Alice should not appears in SELECT results.");

        //
        String response2 = sendCommandToServer(
                "SELECT * FROM employees WHERE name LIKE 'A' AND (salary > 5000 OR name == 'Bob');");
        assertTrue(response2.contains("Amy"), "Amy should appears in SELECT results.");
        assertTrue(response2.contains("Alex"), "Alex should appears in SELECT results.");
        assertFalse(response2.contains("Bob"), "Bob should not appears in SELECT results.");

        String response3 = sendCommandToServer(
                "SELECT * FROM employees WHERE name LIKE 'A' AND salary > 5000 OR name == 'Bob';");
        assertTrue(response3.contains("Amy"), "Amy should appears in SELECT results.");
        assertTrue(response3.contains("Alex"), "Alex should appears in SELECT results.");
        assertTrue(response3.contains("Bob"), "Bob should appears in SELECT results.");

    }
}
