package com.example.gpgrocery.Model;

public class Sale {
    private int orderNumber, itemCode, qtySold;
    String customerName, customerEmail;
    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }
    public void setItemCode(int itemCode) {this.itemCode = itemCode;}
    public void setCustomerName(String customerName) {this.customerName = customerName;}
    public void setCustomerEmail(String customerEmail) {this.customerEmail = customerEmail;}
    public void setQtySold(int qtySold) {this.qtySold = qtySold;}
    public int getOrderNumber() { return orderNumber; }
    public int getItemCode() {return itemCode;}
    public String getCustomerName() {return customerName;}
    public String getCustomerEmail() {return customerEmail;}
    public int getQtySold() {return qtySold;}
}
