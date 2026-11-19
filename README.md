# Q1 - Order Management System (Java JDBC + MySQL)

This project follows Q1 of the uploaded OOPJ MSE-2 paper.

## Requirements
- JDK 21
- IntelliJ IDEA
- MySQL Server 8.x
- MySQL Workbench
- The faculty-supplied MySQL Connector/J JAR is included in `lib/`.

## Setup
1. Extract this folder and open it in IntelliJ using **File > Open**.
2. If IntelliJ detects the Maven project, choose **Load**. The connector JAR is included locally and referenced by `pom.xml`.
3. In MySQL Workbench, connect to your local MySQL instance.
4. Open `database/01_create_database.sql` and execute the full script. This creates `ecommerce_db` and the `Orders` table.
5. Open `src/main/resources/db.properties` and replace `CHANGE_ME` with your local MySQL root password. Keep the file private; do not submit a real password.
6. Open `src/main/java/com/oopj/orders/Main.java` and run `Main.main()`.

## Menu operations
1. Insert at least three order records using `PreparedStatement`. Duplicate primary keys are caught and reported.
2. Search by status and `quantity > 2`, ordered by `unit_price DESC`.
3. Add an additional quantity to an existing order; reports when the ID does not exist.
4. Show an order and update status only to Pending, Shipped, or Delivered.
5. Show `SUM(quantity * unit_price)` and delete only if the status is Cancelled.

All SQL values are parameterized with `PreparedStatement`; resources use try-with-resources.

## Suggested test data
| order_id | customer_name | product_name | quantity | unit_price | status |
|---:|---|---|---:|---:|---|
| 2001 | Asha Verma | Laptop | 3 | 55000 | Pending |
| 2002 | Rohan Singh | Headphones | 5 | 1800 | Shipped |
| 2003 | Meera Khan | Keyboard | 2 | 1200 | Cancelled |

To test the conditional deletion, create an order with status `Cancelled` and try deleting it. Try a Pending/Shipped order too; deletion must be rejected.

## Build
The project targets Java 21. If using Maven from a terminal:

```bash
mvn clean package
```

The MySQL driver is bundled locally in `lib/`, so a network download is not required for that driver.
