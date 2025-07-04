package model;

public class User {
    private int userid;
    private String username;
    private String password;
    private String role;
    private String name;
    private String phone;
    private String address;
    private String registrationDate;
    private String photoPath;
    private byte[] photo;

    public User(int userId, String username, String password, String role, String name, String phone, String address, String registrationDate, String photoPath, byte[] photo) {
        this.userid = userId;
        this.username = username;
        this.password = password;
        this.role = role;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.registrationDate = registrationDate;
        this.photoPath = photoPath;
        this.photo = photo;
    }

    public  int getUserid() {
        return userid;
    }

    public String getUsername() {
        return username;

    }

    public String getPassword() {
        return password;
    }

    public String getRole(){
        return role;
    }

    public String getName(){
        return name;
    }

    public String getPhone(){
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setUserId(int userId) {
        this.userid = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }
}
