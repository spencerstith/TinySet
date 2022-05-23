# TinySet

TinySet is a wrapper of the Java Data Base Connector that simplifies doing basic SQL operations in Java.

## Contents

- [Why Use TinySet](#why-use-tinyset)
- [Getting Started/How To Use](#getting-startedhow-to-use)
- [Advanced Features](#advanced-features)

## Why use TinySet

TinySet takes care of all the column counting and error handling when doing database operations for your project.

Error handling is an important part of any project, but for quick queries, it can be frustrating to have `try`/`catch` blocks all over the place that only `printStackTrace()`.
TinySet handles these and will still print meaningful messages when something does go wrong.

As an example using mySQL: You have a database with table `products` and columns `id`, `name`, `cost`, `quantity`.

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
import main.java.net.craigscode.tinyset.TinySet;

import java.math.BigDecimal;

class Example {
    static void printProducts() {
        // Load database settings
        TinySet.connectByResource("db.properties");
        // Get information from database
        TinySet set = new TinySet("SELECT * FROM products");
        while (set.next()) {
            int id = set.getInt();
            String name = set.getString();
            BigDecimal cost = set.getBigDec();
            int quantity = set.getInt();
            System.out.printf("ID: %d, Name: %s, Cost: %f, Quantity: %d", id, name, cost, quantity);
        }
    }
}
```

TinySet is secure and allows for usage of prepared statements:
```java
import main.java.net.craigscode.tinyset.TinySet;

class Example {
    static void printProduct(int id) {
        TinySet tinySet = new TinySet("SELECT * FROM products WHERE `id` = ?").integer(id);

        int id = tinySet.getInt();
        String name = tinySet.getString();
        BigDecimal cost = tinySet.getBigDec();
        int quantity = tinySet.getInt();
        System.out.printf("ID: %d, Name: %s, Cost: %f, Quantity: %d", id, name, cost, quantity);
    }
}
```

TinySet allows for settings `autoCommit` to false and will collect your queries for you and **automatically rollback
when an error occurs**.
```java
import main.java.net.craigscode.tinyset.TinySet;

import java.math.BigDecimal;
import java.time.LocalDate;

class Example {
    static void addProduct(int id, String name, BigDecimal cost, int quantity, LocalDate release) {
        TinySet.setAutoCommit(false);

        TinySet product = new TinySet("INSERT INTO products(`id`, `name`, `cost`, `quantity`) VALUES (?, ?, ?, ?");
        // You can chain commands!
        product.setInt(id).setString(name).setBigDec(cost).setInt(quantity);
        TinySet.collect(product);

        TinySet release = new TinySet("INSERT INTO release_dates(`id`, `release_date`) VALUES (?, ?)");
        release.setInt(id).setDate(release);
        TinySet.collect(release);

        TinySet.commitCollection();
    }
}
```

## Getting Started/How To Use
TinySet exists on the Maven Central Repository as `net.craigscode:tinyset:1.0.0`.

Maven dependency:
```xml
<dependency>
    <groupId>net.craigscode</groupId>
    <artifactId>tinyset</artifactId>
    <version>1.0.0</version>
</dependency>
```

This tutorial assumes you already have a driver library for your database in your project.

- [Create Connection](#create-connection)
- [Statements](#statements)
- [Dates](#dates)
- [Object Types](#object-types)
- [AutoCommit & Rolling Back](#autocommit-and-rolling-back)
- [Exception Handling](#exception-handling)
- [Iterable & Array Capabilities](#iterable--array-capabilities)

### Create Connection
First create a connection from either a `Properties` file or just by manually passing in credentials:
```java
// Properties file from Class's resource folder:
TinySet.connectByResources("some-resource.properties");
// Properties file form a normal path:
TinySet.connectByFile("some-file.properties");
// Credential passing:
TinySet.connect("url","user","password");
```

The properties file should contain the fields `url`, `user`, `password`.

If you need access to the connection for any reason, you can use `getConnection()`.

### Statements
Statements are easy to create and retrieve data from.
```java
TinySet tinySet = new TinySet("SQL QUERY");
while(tinySet.next()){
    int data = tinySet.getInt();
    //...
}
```

If you need to pass data into the query, that is also simple, using prepared statement syntax (`?`):
```java
TinySet tinySet = new TinySet("SQL QUERY WHERE `column` = ?").setString("parameter");
while(tinySet.next()){
    int data = tinySet.getInt();
    //...
}
```

If you only need to get a single field from a query, you can easily one line it:
```java
int quantity = new TinySet("SELECT `quantity` FROM products WHERE `name` = ?").setString(name).getInt();
```

If you are doing an `UPDATE` or `INSERT` command and `autoCommit` is `true` (default), you will need to call `commit()` at the end of a statement.
```java
new TinySet("UPDATE products SET `cost` = ? WHERE `id` = ?").setBigDec(amount).setInt(id).commit();
```

If you need access to the statement for any reason, you can use `getStatement()`.

### Dates
By default, JDBC uses `SQLDate` objects. These are old and not recommended for use. Yet, you have to use them when using
JDBC. TinySet automatically converts to/from these objects to the modern `LocalDate`, so you never have to use
an `SQLDate`.

*Note: I plan on adding more date support in the future.*

### Object Types
You can put and/or retrieve the following types with TinySet:

| Type       | Get            | Set                             |
|------------|----------------|---------------------------------|
| BigDecimal | `getBigDec()`  | `setBigDec(BigDecimal decimal)` |
| boolean    | `getBoolean()` | `setBoolean(boolean b)`         |
| byte       | `getByte()`    | `setByte(byte b)`               |
| byte[]     | `getBytes()`   | `setBytes(byte[] b)`            |
| LocalDate  | `getDate()`    | `setDate(LocalDate date)`       |
| double     | `getDouble()`  | `setDouble(double d)`           |
| float      | `getFloat()`   | `setFloat(float f)`             |
| int        | `getInt()`     | `setInt(int i)`                 |
| long       | `getLong()`    | `setLong(long l)`               |
| short      | `getShort()`   | `setShort(short s)`             |
| String     | `getString()`  | `setString(String str)`         |
| Object     | `getObject()`  | `setObject(Object object)`      |

If needed, you can also skip over a selected column in a set with `skip()`

### AutoCommit and Rolling Back
You can turn off `autoCommit` and easily collect several TinySet objects, which you can execute all at once. If any of
these return an error, all transactions will then be rolled back automatically.

```java
TinySet.setAutoCommit(false);
TinySet set1 = new TinySet("...");
TinySet.collect(set1);
//...
TinySet set2 = new TinySet("...");
TinySet.collect(set2);
//...
// One line:
TinySet.collect(new TinySet("...").setInt(42));

// Commit:
TinySet.commitCollection();
```

### Exception Handling
TinySet helps to reduce the amount of boilerplate code needed for SQL operations, including exception handling.
TinySet uses a `TinyException` to wrap the `SQLException` by extending `RuntimeException`, so you can still catch the errors, but don't always have to.
If you still wish to handle exceptions, you can use a `try/catch` block that catches a `TinyException`.
Further, you can access the underlying `SQLException` contained:

```java
try {
    TinySet set = new TinySet("...");
    int value = set.getInt();
} catch (TinyException e) {
    int statusCode = e.sqlException.getStatusCode();
    System.err.println(e.sqlException.getMessage());
}
```

### Iterable & Array Capabilities
TinySet implements `Iterable`, so you can do all sorts of fun stuff with it. Instead of this:
```java
TinySet tinySet = new TinySet("SELECT * FROM products");
while(tinySet.next()){
    int id = tinySet.getInt();
    String name = tinySet.getString();
    BigDecimal cost = tinySet.getBigDec();
    int quantity = tinySet.getInt();
    System.out.printf("ID: %d, Name: %s, Cost: %f, Quantity: %d", id, name, cost, quantity);
}
```

You can do this:
```java
new TinySet("SELECT * FROM products").forEach(t ->
    System.out.printf("ID: %d, Name: %s, Cost: %f, Quantity: %d",
        t.getInt(), t.getString(), t.getBigDec(), t.getInt()));
```

Or if you're just selecting a single item and want an array from that item:
```java
ArrayList<String> names = new ArrayList<String>;
new TinySet("SELECT `names` FROM products").forEach(t -> names.add(t.getString()));
```

TinySet can also be converted to a stream using `stream()` for any streaming needs.

## Advanced Features
If there is something you need to do that is beyond the scope of TinySet or that TinySet has yet to implement,
you can access the instance's `Connection`, `PreparedStatement`, or `ResultSet` with the methods `getConnection()`, `getStatement()`, or `getResultSet()` respectively.
