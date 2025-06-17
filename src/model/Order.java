package model;

import java.util.Date;
import java.util.List;

public class Order {
    private int orderId;
    private int clientId;
    private Date orderDate;
    private double totalCost;
    private String status;
    private Date lastUpdated;
    private String deliveryCity;
    private String deliveryAddress;
    private List<OrderItem> items;

    public Order() {
        // Default constructor
    }

    public Order(int orderId, int clientId, Date orderDate, double totalCost, String status, Date lastUpdated, String deliveryCity, String deliveryAddress) {
        this.orderId = orderId;
        this.clientId = clientId;
        this.orderDate = orderDate;
        this.totalCost = totalCost;
        this.status = status;
        this.lastUpdated = lastUpdated;
        this.deliveryCity = deliveryCity;
        this.deliveryAddress = deliveryAddress;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getDeliveryCity() {
        return deliveryCity;
    }

    public void setDeliveryCity(String deliveryCity) {
        this.deliveryCity = deliveryCity;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", clientId=" + clientId +
                ", orderDate=" + orderDate +
                ", totalCost=" + totalCost +
                ", status='" + status + '\'' +
                ", lastUpdated=" + lastUpdated +
                ", deliveryCity='" + deliveryCity + '\'' +
                ", deliveryAddress='" + deliveryAddress + '\'' +
                '}';
    }
} 