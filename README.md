# TinySet

TinySet is a wrapper of the Java Data Base Connector that simplifies doing basic SQL operations in Java.

## Contents

- [Why Use TinySet](#why-use-tinyset)
- [Getting Started/How To Use](#getting-startedhow-to-use)

## Why use TinySet

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
        TinySet.connectByResource("db.properties");
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

## Getting Started/How To Use

Unfortunately, TinySet currently only exists as a jar file, which you must download and add to your project. Get the jar
file from the releases or packages page. I'm working on getting it on Grade/Maven.

This tutorial assumes you already have a driver library for your database in your project.

- [Create Connection](#create-connection)
- [Statements](#statements)
- [Dates](#date)
- [Object Types](#object-types)
- [AutoCommit & Rolling Back](#autocommit-and-rolling-back)
- [Iterable & Array Capabilities](#iterable--array-capabilities)

### Create Connection

First create a connection from either a `Properties` file or just by manually passing in credentials:

```java
// Properties file from Class's resource folder:
TinySet.connectByResources("some-resource.properties");
// Properties file form normal path:
        TinySet.connectByFile("some-file.properties")
// Credential passing:
        TinySet.connect("url","user","password")
```

The properties file should contain the fields `url`, `user`, `password`.

### Statements

Statements are easy to create and retrieve data from.

```java
TinySet tinySet=new TinySet("SQL QUERY");
        while(tinySet.next()){
        int data=tinySet.integer();
        //...
        }
```

If you need to pass data into the query, that is also simple, using prepared statement syntax (`?`):

```java
TinySet tinySet=new TinySet("SQL QUERY WHERE `column` = ?").string("parameter");
        while(tinySet.next()){
        int data=tinySet.integer();
        //...
        }
```

It you only need to get a single field from a query, you can easily one line it:

```java
int quantity=new TinySet("SELECT `quantity` FROM products WHERE `name` = ?").string(name).integer();
```

### Dates

By default, JDBC uses `SQLDate` objects. There are old and not recommended for use. Yet, you have to use them when using
JDBC. TinySet automatically converts to/from these objects to the modern `LocalDate`, so you never have to use
an `SQLDate`.

*Note: I plan on adding more date support in the future.*

### Object Types

You can put and/or retrieve the following types with TinySet:

- `String`: `string()`, `string(String str)`
- `BigDecimal`: `bigDec()`, `bigDec(BigDecimal decimal)`
- `boolean`: `bool()`, `bool(boolean bool)`
- `LocalDate`: `date()`, `date(LocalDate date)`
- `int`: `integer()`, `integer(int i)`

If needed, you can also skip over a selected column in a set with `skip()`

### AutoCommit and Rolling Back

You can turn off `autoCommit` and easily collect several TinySet objects, which you can execute all at once. If any of
these return an error, all transactions will then be rolled back automatically.

```java
TinySet.setAutoCommit(false);
        TinySet set1=new TinySet("...");
        TinySet.collect(set1);
//...
        TinySet set2=new TinySet("...");
        TinySet.collect(set2);
// One line:
        TinySet.collect(new TinySet("...").integer(42));

// Commit:
        TinySet.commitCollection();
```

### Iterable & Array Capabilities

TinySet implements `Iterable`, so you can do all sorts of fun stuff with it. Instead of this:

```java
TinySet tinySet=new TinySet("SELECT * FROM products");
        while(tinySet.next()){
        int id=tinySet.integer();
        String name=tinySet.string();
        BigDecimal cost=tinySet.bigDec();
        int quantity=tinySet.integer();
        System.out.printf("ID: %d, Name: %s, Cost: %f, Quantity: %d",id,name,cost,quantity);
        }
```

You can do this:

```java
new TinySet("SELECT * FROM products").forEach(t->
        System.out.printf("ID: %d, Name: %s, Cost: %f, Quantity: %d",
        t.integer(),t.string(),t.bigDec(),t.integer()));
```

Of if you're just selecting a single item and want an array from that item:

```java
ArrayList<String> names=new ArrayList<String>;
new TinySet("SELECT `names` FROM products").forEach(t->names.add(t.string()));
```