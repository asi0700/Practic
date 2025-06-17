package model;

import java.util.Date;

public class Product {
    private int id;
    private String name;
    private String description;
    private double price;
    private int quantity;
    private String category;
    private String supplier;
    private Date added_date;
    private int added_by;
    private Date modified_date;
    private int modified_by;

    public Product() {
        this.id = 0;
        this.name = "";
        this.description = "";
        this.price = 0.0;
        this.quantity = 0;
        this.category = "";
        this.supplier = "";
        this.added_date = new Date();
        this.added_by = 0;
        this.modified_date = new Date();
        this.modified_by = 0;
    }

    public Product(int id, String name, double price, String description, int quantity, String supplier, int added_by, String added_date, int modified_by, String modified_date) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.quantity = quantity;
        this.supplier = supplier;
        this.added_by = added_by;
        this.modified_by = modified_by;
        this.added_date = new Date();
        this.modified_date = new Date();
    }

    public Product(int productId, String name, double price, String description, int i, int quantity, String supplier, int addedBy, String addedDate, int modifiedBy, String modifiedDate) {
    }

    public Product(int productId, String name, double price, String s, String supplier) {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public Date getAdded_date() {
        return added_date;
    }

    public void setAdded_date(Date added_date) {
        this.added_date = added_date;
    }

    public int getAdded_by() {
        return added_by;
    }

    public void setAdded_by(int added_by) {
        this.added_by = added_by;
    }

    public Date getModified_date() {
        return modified_date;
    }

    public void setModified_date(Date modified_date) {
        this.modified_date = modified_date;
    }

    public int getModified_by() {
        return modified_by;
    }

    public void setModified_by(int modified_by) {
        this.modified_by = modified_by;
    }
}
