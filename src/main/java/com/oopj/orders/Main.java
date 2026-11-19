package com.oopj.orders;

import com.oopj.orders.dao.OrderDAO;
import com.oopj.orders.model.Order;
import com.oopj.orders.util.DatabaseConnection;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;

public class Main {
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final OrderDAO ORDER_DAO = new OrderDAO();
    private static final List<String> UPDATE_STATUSES = List.of("Pending", "Shipped", "Delivered");
    private static final List<String> INITIAL_STATUSES = List.of("Pending", "Shipped", "Delivered", "Cancelled");

    public static void main(String[] args) {
        System.out.println("===============================================");
        System.out.println("            ORDER MANAGEMENT SYSTEM");
        System.out.println("              Java JDBC + MySQL");
        System.out.println("===============================================");
        try (var connection = DatabaseConnection.getConnection()) {
            System.out.println("Database connection successful: ecommerce_db");
        } catch (Exception e) {
            System.err.println("Could not connect to MySQL: " + e.getMessage());
            System.err.println("Check MySQL is running and update src/main/resources/db.properties.");
            return;
        }

        boolean running = true;
        while (running) {
            showMenu();
            int choice = readInt("Enter your choice: ");
            try {
                switch (choice) {
                    case 1 -> insertOrders();
                    case 2 -> retrieveOrdersByStatus();
                    case 3 -> increaseQuantity();
                    case 4 -> updateOrderStatus();
                    case 5 -> showTotalAndDeleteCancelled();
                    case 0 -> running = false;
                    default -> System.out.println("Invalid choice. Please select 0 to 5.");
                }
            } catch (SQLException e) {
                System.err.println("Database operation failed: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid input: " + e.getMessage());
            }
            System.out.println();
        }
        System.out.println("Thank you for using the Order Management System.");
    }

    private static void showMenu() {
        System.out.println("\n--------------- MAIN MENU ---------------");
        System.out.println("1. Insert at least three order records");
        System.out.println("2. Retrieve orders by status (quantity > 2)");
        System.out.println("3. Add quantity to an existing order");
        System.out.println("4. Search order and update its status");
        System.out.println("5. Show total order value and delete cancelled order");
        System.out.println("0. Exit");
        System.out.println("-----------------------------------------");
    }

    private static void insertOrders() throws SQLException {
        System.out.println("\n--- INSERT ORDER RECORDS ---");
        int count;
        do {
            count = readInt("How many orders do you want to insert? (minimum 3): ");
            if (count < 3) System.out.println("Please enter at least three orders.");
        } while (count < 3);
        int inserted = 0, duplicates = 0;
        for (int i = 1; i <= count; i++) {
            System.out.println("\nOrder " + i + " of " + count);
            int id = readInt("Order ID: ");
            String customer = readNonBlank("Customer name: ");
            String product = readNonBlank("Product name: ");
            int quantity = readInt("Quantity: ");
            if (quantity < 0) throw new IllegalArgumentException("Quantity cannot be negative.");
            double price = readDouble("Unit price: ");
            if (price < 0) throw new IllegalArgumentException("Unit price cannot be negative.");
            String status = readStatus("Status (Pending/Shipped/Delivered/Cancelled): ", INITIAL_STATUSES);
            boolean ok = ORDER_DAO.insert(new Order(id, customer, product, quantity, price, status));
            if (ok) { inserted++; System.out.println("Order inserted successfully."); }
            else { duplicates++; System.out.println("Duplicate order ID " + id + "; record was not inserted."); }
        }
        System.out.printf(Locale.US, "\nInsertion summary: %d inserted, %d duplicate ID(s) handled.%n", inserted, duplicates);
    }

    private static void retrieveOrdersByStatus() throws SQLException {
        System.out.println("\n--- RETRIEVE ORDERS BY STATUS ---");
        String status = readNonBlank("Enter order status: ");
        List<Order> orders = ORDER_DAO.findByStatusAndQuantityGreaterThanTwo(status);
        printOrders(orders);
        if (orders.isEmpty()) System.out.println("No matching orders found.");
    }

    private static void increaseQuantity() throws SQLException {
        System.out.println("\n--- INCREASE ORDER QUANTITY ---");
        int id = readInt("Enter order ID: ");
        int amount = readInt("Enter additional quantity: ");
        if (amount <= 0) throw new IllegalArgumentException("Additional quantity must be greater than zero.");
        if (ORDER_DAO.addQuantity(id, amount)) {
            System.out.println("Order quantity updated successfully.");
            ORDER_DAO.findById(id).ifPresent(Main::printOrder);
        } else {
            System.out.println("Order does not exist.");
        }
    }

    private static void updateOrderStatus() throws SQLException {
        System.out.println("\n--- SEARCH AND UPDATE ORDER STATUS ---");
        int id = readInt("Enter order ID: ");
        Optional<Order> found = ORDER_DAO.findById(id);
        if (found.isEmpty()) { System.out.println("Order does not exist."); return; }
        System.out.println("Current order details:");
        printOrder(found.get());
        String status = readStatus("New status (Pending/Shipped/Delivered): ", UPDATE_STATUSES);
        if (ORDER_DAO.updateStatus(id, status)) {
            System.out.println("Order status updated successfully. Updated details:");
            ORDER_DAO.findById(id).ifPresent(Main::printOrder);
        } else {
            System.out.println("Order does not exist.");
        }
    }

    private static void showTotalAndDeleteCancelled() throws SQLException {
        System.out.println("\n--- TOTAL ORDER VALUE AND CONDITIONAL DELETE ---");
        System.out.printf(Locale.US, "Total order value: %.2f%n", ORDER_DAO.getTotalOrderValue());
        int id = readInt("Enter order ID to delete (only Cancelled orders can be deleted): ");
        Optional<Order> found = ORDER_DAO.findById(id);
        if (found.isEmpty()) { System.out.println("Order does not exist."); return; }
        if (!"cancelled".equalsIgnoreCase(found.get().getStatus())) {
            System.out.println("Order cannot be deleted because its status is " + found.get().getStatus()
                    + ". Only Cancelled orders can be deleted.");
            return;
        }
        if (ORDER_DAO.deleteIfCancelled(id)) System.out.println("Cancelled order deleted successfully.");
        else System.out.println("Order could not be deleted.");
    }

    private static String readStatus(String prompt, List<String> allowed) {
        while (true) {
            String input = readNonBlank(prompt);
            for (String status : allowed) if (status.equalsIgnoreCase(input)) return status;
            System.out.println("Invalid status. Allowed values: " + String.join(", ", allowed));
        }
    }

    private static String readNonBlank(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = SCANNER.nextLine().trim();
            if (!value.isEmpty()) return value;
            System.out.println("Value cannot be blank.");
        }
    }

    private static int readInt(String prompt) {
        while (true) {
            String value = readNonBlank(prompt);
            try { return Integer.parseInt(value); }
            catch (NumberFormatException e) { System.out.println("Enter a valid whole number."); }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            String value = readNonBlank(prompt);
            try { return Double.parseDouble(value); }
            catch (NumberFormatException e) { System.out.println("Enter a valid number."); }
        }
    }

    private static void printOrders(List<Order> orders) {
        System.out.printf("%-9s %-22s %-20s %-10s %-12s %-12s%n",
                "ORDER ID", "CUSTOMER", "PRODUCT", "QUANTITY", "UNIT PRICE", "STATUS");
        System.out.println("-----------------------------------------------------------------------------------------------");
        for (Order order : orders) printOrder(order);
    }

    private static void printOrder(Order order) {
        System.out.printf(Locale.US, "%-9d %-22s %-20s %-10d %-12.2f %-12s%n",
                order.getOrderId(), shorten(order.getCustomerName(), 21), shorten(order.getProductName(), 19),
                order.getQuantity(), order.getUnitPrice(), order.getStatus());
    }

    private static String shorten(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
