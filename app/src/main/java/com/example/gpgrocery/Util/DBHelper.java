package com.example.gpgrocery.Util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.gpgrocery.Model.StockItem;
import com.example.gpgrocery.Model.User;

public class DBHelper extends SQLiteOpenHelper {
    static String DBNAME = "GPGrocery.db";
    static int VERSION = 1;
    static String USER_TABLE_NAME = "user";
    static String STOCK_TABLE_NAME = "stock";
    static String SALES_TABLE_NAME = "sales";
    static String PURCHASE_TABLE_NAME = "purchase";
    static String USER_COL1 = "id";
    static String USER_COL2 = "username";
    static String USER_COL3 = "emailId";
    static String USER_COL4 = "password";
    static String STOCK_COL1 = "itemCode";
    static String STOCK_COL2 = "itemName";
    static String STOCK_COL3 = "qtyStock";
    static String STOCK_COL4 = "price";
    static String STOCK_COL5 = "taxable";
    static String SALES_COL1 = "orderNumber";
    static String SALES_COL2 = "itemCode";
    static String SALES_COL3 = "customerName";
    static String SALES_COL4 = "customerEmail";
    static String SALES_COL5 = "qtySold";
    static String SALES_COL6 = "dateOfSales";
    static String PURCHASE_COL1 = "invoiceNumber";
    static String PURCHASE_COL2 = "itemCode";
    static String PURCHASE_COL3 = "qtyPurchased";
    static String PURCHASE_COL4 = "dateOfPurchase";
    static final String CREATE_USER_TABLE_NAME = "create table " + USER_TABLE_NAME + " (" + USER_COL1 + " INTEGER PRIMARY KEY AUTOINCREMENT, " + USER_COL2 + " TEXT NOT NULL, "
            + USER_COL3 + " TEXT NOT NULL, "  + USER_COL4 + " TEXT NOT NULL); ";
    static final String CREATE_STOCK_TABLE_NAME = "create table " + STOCK_TABLE_NAME + " (" + STOCK_COL1 + " INTEGER PRIMARY KEY AUTOINCREMENT, " + STOCK_COL2 + " TEXT NOT NULL, "
            + STOCK_COL3 + " INTEGER NOT NULL, " + STOCK_COL4 + " FLOAT NOT NULL, "  + STOCK_COL5 + " BOOLEAN NOT NULL); ";
    static final String CREATE_SALES_TABLE_NAME = "create table " + SALES_TABLE_NAME + " (" + SALES_COL1 + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SALES_COL2 + " INTEGER NOT NULL, "
            + SALES_COL3 + " TEXT NOT NULL, " + SALES_COL4 + " TEXT NOT NULL, " + SALES_COL5 + " INTEGER NOT NULL, "  + SALES_COL6 + " DATE NOT NULL); ";
    static final String CREATE_PURCHASE_TABLE_NAME = "create table " + PURCHASE_TABLE_NAME + " (" + PURCHASE_COL1 + " INTEGER PRIMARY KEY AUTOINCREMENT, " + PURCHASE_COL2 + " INTEGER NOT NULL, "
            + PURCHASE_COL3 + " INTEGER NOT NULL, " + PURCHASE_COL4 + " DATE NOT NULL); ";
    static final String DROP_USER_TABLE_NAME = "DROP TABLE IF EXISTS " + USER_TABLE_NAME;
    static final String DROP_STOCK_TABLE_NAME = "DROP TABLE IF EXISTS " + STOCK_TABLE_NAME;
    static final String DROP_SALES_TABLE_NAME = "DROP TABLE IF EXISTS " + SALES_TABLE_NAME;
    static final String DROP_PURCHASE_TABLE_NAME = "DROP TABLE IF EXISTS " + PURCHASE_TABLE_NAME;

    public DBHelper(Context context) {
        super(context, DBNAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USER_TABLE_NAME);
        db.execSQL(CREATE_STOCK_TABLE_NAME);
        db.execSQL(CREATE_SALES_TABLE_NAME);
        db.execSQL(CREATE_PURCHASE_TABLE_NAME);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_USER_TABLE_NAME);
        db.execSQL(DROP_STOCK_TABLE_NAME);
        db.execSQL(DROP_SALES_TABLE_NAME);
        db.execSQL(DROP_PURCHASE_TABLE_NAME);
        onCreate(db);
    }

    public void InsertUserAccount(User objUser) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(USER_COL2, objUser.getUsername());
        cv.put(USER_COL3, objUser.getEmailId());
        cv.put(USER_COL4, objUser.getPassword());
        db.insert(USER_TABLE_NAME, null, cv);
    }

    public Cursor readAccounts() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursorObj;
        cursorObj = db.rawQuery("select * from " + USER_TABLE_NAME, null);
        if (cursorObj != null) {
            cursorObj.moveToFirst();
        }
        return cursorObj;
    }

    public Boolean InsertStockItem(StockItem objStockItem) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(STOCK_COL2, objStockItem.getItemName());
        cv.put(STOCK_COL3, objStockItem.getQtyStock());
        cv.put(STOCK_COL4, objStockItem.getPrice());
        cv.put(STOCK_COL5, objStockItem.getTaxable());
        long result = db.insert(STOCK_TABLE_NAME, null, cv);
        return (result != -1);
    }
}
