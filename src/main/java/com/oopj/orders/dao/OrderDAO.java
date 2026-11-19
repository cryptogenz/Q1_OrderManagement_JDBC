package com.oopj.orders.dao;

import com.oopj.orders.model.Order;
import com.oopj.orders.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrderDAO {
    private static final String COLUMNS = "order_id, customer_name, product_name, quantity, unit_price, status";

    public boolean insert(Order order) throws SQLException {
        String sql = "INSERT INTO `Orders` (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, order.getOrderId());
            statement.setString(2, order.getCustomerName());
            statement.setString(3, order.getProductName());
            statement.setInt(4, order.getQuantity());
            statement.setDouble(5, order.getUnitPrice());
            statement.setString(6, order.getStatus());
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState()) || e.getErrorCode() == 1062) {
                return false; // Duplicate primary key / integrity constraint.
            }
            throw e;
        }
    }

    public List<Order> findByStatusAndQuantityGreaterThanTwo(String status) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM `Orders` "
                + "WHERE LOWER(status) = LOWER(?) AND quantity > 2 ORDER BY unit_price DESC";
        List<Order> orders = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) orders.add(mapRow(rs));
            }
        }
        return orders;
    }

    public boolean addQuantity(int orderId, int additionalQuantity) throws SQLException {
        String sql = "UPDATE `Orders` SET quantity = quantity + ? WHERE order_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, additionalQuantity);
            statement.setInt(2, orderId);
            return statement.executeUpdate() > 0;
        }
    }

    public Optional<Order> findById(int orderId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM `Orders` WHERE order_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public boolean updateStatus(int orderId, String status) throws SQLException {
        String sql = "UPDATE `Orders` SET status = ? WHERE order_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, orderId);
            return statement.executeUpdate() > 0;
        }
    }

    public double getTotalOrderValue() throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantity * unit_price), 0) AS total_value FROM `Orders`";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getDouble("total_value") : 0.0;
        }
    }

    public boolean deleteIfCancelled(int orderId) throws SQLException {
        String sql = "DELETE FROM `Orders` WHERE order_id = ? AND LOWER(status) = 'cancelled'";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, orderId);
            return statement.executeUpdate() > 0;
        }
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        return new Order(rs.getInt("order_id"), rs.getString("customer_name"),
                rs.getString("product_name"), rs.getInt("quantity"),
                rs.getDouble("unit_price"), rs.getString("status"));
    }
}
