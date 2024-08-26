package com.example.gpgrocery.Activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import com.example.gpgrocery.Util.DBHelper;
import com.example.gpgrocery.databinding.ActivityMainBinding;
import com.example.gpgrocery.Util.Utils;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    ActivityMainBinding mainBinding;
    Intent intentSignup;
    Intent intentHome;
    DBHelper dbh;
    SharedPreferences sharedPref1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = mainBinding.getRoot();
        setContentView(view);

        dbh = new DBHelper(this);
        sharedPref1 = getSharedPreferences("login_details", Context.MODE_PRIVATE);

        mainBinding.btnLogin.setOnClickListener(this);
        mainBinding.btnSignUp.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == mainBinding.btnSignUp.getId()) {
            intentSignup = new Intent(this, SignUpActivity.class);
            startActivity(intentSignup);
        } else if(v.getId() == mainBinding.btnLogin.getId()) {
            if(isFormValidated()) {
                Boolean isUserRegistered = Utils.isUserRegistered(dbh, mainBinding.edtUsername, "username");
                if(isUserRegistered) {
                    Utils.saveSharedPreferenceUsername(this, mainBinding.edtUsername);
                    intentHome = new Intent(this, HomeActivity.class);
                    startActivity(intentHome);
                } else {
                    Utils.sendAlert(mainBinding, "User not registered. Please signup.");
                }
            }
        }
    }

    private Boolean isFormValidated() {
        String username = mainBinding.edtUsername.getText().toString().trim();
        String password = mainBinding.edtPassword.getText().toString().trim();

        if (username.isEmpty()) {
            mainBinding.edtUsername.setError("Customer name is required");
            Utils.sendAlert(mainBinding, "Please insert the username");
            return false;
        }
        if(password.isEmpty()) {
            mainBinding.edtPassword.setError("Invalid phone");
            Utils.sendAlert(mainBinding, "Please insert the password");
            return false;
        }
        return true;
    }
}