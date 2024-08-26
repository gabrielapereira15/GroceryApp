package com.example.gpgrocery.Model;

import java.util.Date;

public class Purchase {
    private int invoiceNumber, itemCode, qtyPurchased;
    private Date dateOfPurchase;
    public void setInvoiceNumber(int invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }
    public void setItemCode(int itemCode) {this.itemCode = itemCode;}
    public void setQtyPurchased(int qtyPurchased) {this.qtyPurchased = qtyPurchased;}
    public void setDateOfPurchase(Date dateOfPurchase) {this.dateOfPurchase = dateOfPurchase;}
    public int getInvoiceNumber() { return invoiceNumber; }
    public int getItemCode() {return itemCode;}
    public int getQtyPurchased() {return qtyPurchased;}
    public Date getDateOfPurchase() {return dateOfPurchase;}
}
