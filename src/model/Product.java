package model;

public class Product {
    private int id;
    private String name;
    private double price;
    private String description;
    private int categoryId;
    private int quantity;
    private String supplier;
    private int added_by;
    private String added_date;
    private int modified_by;
    private String modified_date;

    public Product(int id, String name, double price, String description, int categoryId){
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.categoryId = categoryId;
    }

    public Product(int id, String name, double price, String description, int categoryId, int quantity, String supplier, int added_by, String added_date, int modified_by, String modified_date){
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.categoryId = categoryId;
        this.quantity = quantity;
        this.supplier = supplier;
        this.added_by = added_by;
        this.added_date = added_date;
        this.modified_by = modified_by;
        this.modified_date = modified_date;
    }

    public Product(int id, String name, double price, String description, String supplier) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.supplier = supplier;
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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public int getAdded_by() {
        return added_by;
    }

    public void setAdded_by(int added_by) {
        this.added_by = added_by;
    }

    public String getAdded_date() {
        return added_date;
    }

    public void setAdded_date(String added_date) {
        this.added_date = added_date;
    }

    public int getModified_by() {
        return modified_by;
    }

    public void setModified_by(int modified_by) {
        this.modified_by = modified_by;
    }

    public String getModified_date() {
        return modified_date;
    }

    public void setModified_date(String modified_date) {
        this.modified_date = modified_date;
    }
}
