package com.example.gpgrocery.Util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.viewbinding.ViewBinding;

public class Utils {

    static SharedPreferences sharedPref1;

    public static void sendAlert(ViewBinding binding, String message) {
        new AlertDialog.Builder(binding.getRoot().getContext())
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }

                })
                .setCancelable(false)
                .show();
    }

    public static Boolean isUserRegistered(DBHelper dbh, EditText field, String columnName) {
        Boolean isRegistered = false;
        Cursor cursor1 = dbh.readAccounts();
        if (cursor1.moveToFirst()) {
            do {
                @SuppressLint("Range") String userEmail = cursor1.getString(cursor1.getColumnIndex(columnName));
                if (field.getText().toString().trim().equals(userEmail)) {
                    isRegistered = true;
                    return isRegistered;
                }
            } while (cursor1.moveToNext());
        }
        cursor1.close();
        return isRegistered;
    }

    public static void saveSharedPreferenceUsername(Context context, EditText editText) {
        sharedPref1 = context.getSharedPreferences("login_details", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor1 = sharedPref1.edit();
        editor1.putString("USERNAME", editText.getText().toString().trim());
        editor1.apply();
    }
}
