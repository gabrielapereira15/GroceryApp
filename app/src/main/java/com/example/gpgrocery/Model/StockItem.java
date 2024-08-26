package com.example.gpgrocery.Model;

public class StockItem {
    private int itemCode, qtyStock;
    String itemName;
    Float price;
    Boolean taxable;
    public void setItemCode(int itemCode) {this.itemCode = itemCode;}
    public void setQtyStock(int qtyStock) {
        this.qtyStock = qtyStock;
    }
    public void setItemName(String itemName) {this.itemName = itemName;}
    public void setPrice(Float price) {this.price = price;}
    public void setTaxable(Boolean taxable) {this.taxable = taxable;}
    public int getItemCode() {return itemCode;}
    public int getQtyStock() { return qtyStock; }
    public String getItemName() {return itemName;}
    public Float getPrice() {return price;}
    public Boolean getTaxable() {return taxable;}
}
