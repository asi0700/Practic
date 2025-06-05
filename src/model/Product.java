package model;

public class Product {
    private int product_id;
    private String name;
    private String description;
    private int quantity;
    private double price;
    private String supplier;
    private int added_by;
    private String added_date;
    private int modified_by;
    private String modified_date;

    public Product(int product_id, String name, String description, int quantity, double price, String supplier, int added_by, String added_date, int modified_by, String modified_date){
        this.product_id = product_id;
        this.name = name;
        this.description = description;
        this.quantity = quantity;
        this.price = price;
        this.supplier = supplier;
        this.added_by = added_by;
        this.added_date = added_date;
        this.modified_by = modified_by;
        this.modified_date = modified_date;
    }



    public int getProduct_id() {
        return product_id;
    }

    public void setProduct_id(int product_id) {
        this.product_id = product_id;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
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


