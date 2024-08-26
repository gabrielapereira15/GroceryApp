package com.example.gpgrocery.Activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.gpgrocery.Util.DBHelper;
import com.example.gpgrocery.Model.User;
import com.example.gpgrocery.Util.Utils;
import com.example.gpgrocery.databinding.ActivitySignUpBinding;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {

    ActivitySignUpBinding signUpBinding;
    Intent intentHome;
    Intent intentLogin;
    DBHelper dbh;
    private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" +
                    "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    private static final Pattern pattern = Pattern.compile(EMAIL_PATTERN);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        signUpBinding = ActivitySignUpBinding.inflate(getLayoutInflater());
        View view = signUpBinding.getRoot();
        setContentView(view);

        dbh = new DBHelper(this);

        signUpBinding.btnSignUp.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == signUpBinding.btnSignUp.getId()) {
            if (isFormValidated()) {
                saveUser();
                intentHome = new Intent(this, HomeActivity.class);
                startActivity(intentHome);
            }
        }
    }

    private void saveUser() {
        Boolean isUserRegistered = Utils.isUserRegistered(dbh, signUpBinding.edtEmail, "emailId");
        if(isUserRegistered) {
            Utils.sendAlert(signUpBinding, "User already registered. Please login.");
            intentLogin = new Intent(this, MainActivity.class);
            startActivity(intentLogin);
        } else {
            registerAccount();
            Utils.saveSharedPreferenceUsername(this, signUpBinding.edtUsername);
        }
    }

    private void registerAccount() {
        User objUser = new User();
        objUser.setUsername(signUpBinding.edtUsername.getText().toString().trim());
        objUser.setEmailId(signUpBinding.edtEmail.getText().toString().trim());
        objUser.setPassword(signUpBinding.edtPassword.getText().toString().trim());
        dbh.InsertUserAccount(objUser);
    }
    private Boolean isFormValidated() {
        String username = signUpBinding.edtUsername.getText().toString().trim();
        String email = signUpBinding.edtEmail.getText().toString().trim();
        String password = signUpBinding.edtPassword.getText().toString().trim();

        if (username.isEmpty()) {
            signUpBinding.edtUsername.setError("Customer name is required");
            Utils.sendAlert(signUpBinding, "Please insert a username");
            return false;
        }
        if (email.isEmpty()) {
            signUpBinding.edtEmail.setError("Email address is required");
            Utils.sendAlert(signUpBinding, "Please insert your email address");
            return false;
        }
        if (!isValidEmail(email)) {
            signUpBinding.edtEmail.setError("Invalid email address");
            Utils.sendAlert(signUpBinding, "Please insert a valid email address");
            return false;
        }
        if(password.isEmpty()) {
            signUpBinding.edtPassword.setError("Invalid phone");
            Utils.sendAlert(signUpBinding, "Please insert a password");
            return false;
        }
        return true;
    }
    public static boolean isValidEmail(String email) {
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

}