# TinySet

TinySet is a wrapper of the Java Data Base Connector that simplifies doing basic SQL operations in Java.

When using the JDBC normally, there is a lot you have to do to get or put information in a database.

TinySet takes care of all the column counting and error handling when doing database operations for your project.

For example: You have a database with table `products` and columns `id`, `name`, `cost`, `quantity`.

With just JDBC, there is so much you have to do to get information:

```java
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

class Example {
    static void printProducts() {
        // Load database settings
        Connection connection;
        try {
            InputStream secrets = Example.class.getResourceAsStream("db.properties");
            Properties properties = new Properties();
            properties.load(secrets);
            connection = DriverManager.getConnection(
                    properties.getProperty("url"),
                    properties.getProperty("user"),
                    properties.getProperty("password"));
        } catch (SQLException e) {
            System.out.println("Error loading database settings");
            e.printStackTrace();
        }

        // Get information from database
        try {
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM products");
            while (resultSet.next()) {
                int id = resultSet.getInt(1);
                String name = resultSet.getString(2);
                BigDecimal cost = resultSet.getBigDecimal(3);
                int quantity = resultSet.getInt(4);
                // If you rearrange or add columns to the database, you have to recode column numbers.
                System.out.printf("ID: %d, Name: %s, Cost: %f, Quantity: %d", id, name, cost, quantity);
            }
        } catch (SQLException e) {
            System.out.println("Could not get database information");
            e.printStackTrace();
        }
    }
}
```

With TinySet:

```java
import net.craigscode.tinyset.TinySet;

import java.math.BigDecimal;

class Example {
    static void printProducts() {
        // Load database settings
        TinySet.connect("db.properties");
        // Get information from database
        TinySet tinySet = new TinySet("SELECT * FROM products");
        while (tinySet.next()) {
            int id = tinySet.integer();
            String name = tinySet.string();
            BigDecimal cost = tinySet.bigDec();
            int quantity = tinySet.integer();
            System.out.printf("ID: %d, Name: %s, Cost: %f, Quantity: %d", id, name, cost, quantity);
        }
    }
}
```

TinySet is secure and allows for usage of prepared statements:

```java
import net.craigscode.tinyset.TinySet;

class Example {
    static void printProduct(int id) {
        TinySet tinySet = new TinySet("SELECT * FROM products WHERE `id` = ?").integer(id);

        int id = tinySet.integer();
        String name = tinySet.string();
        BigDecimal cost = tinySet.bigDec();
        int quantity = tinySet.integer();
        System.out.printf("ID: %d, Name: %s, Cost: %f, Quantity: %d", id, name, cost, quantity);
    }
}
```

TinySet allows for settings `autoCommit` to false and will collect your queries for you and **automatically rollback
when an error occurs**.

```java
import net.craigscode.tinyset.TinySet;

import java.math.BigDecimal;
import java.time.LocalDate;

class Example {
    static void pushProduct(int id, String name, BigDecimal cost, int quantity, LocalDate release) {
        TinySet.setAutoCommit(false);

        TinySet product = new TinySet("INSERT INTO products(`id`, `name`, `cost`, `quantity`) VALUES (?, ?, ?, ?");
        // You can chain commands!
        product.integer(id).string(name).bigDec(cost).integer(quantity);
        TinySet.collect(product);

        TinySet release = new TinySet("INSERT INTO release_dates(`id`, `release_date`) VALUES (?, ?)");
        release.integer(id).date(release);
        TinySet.collect(release);
        
        TinySet.commitCollection();
    }
}
```