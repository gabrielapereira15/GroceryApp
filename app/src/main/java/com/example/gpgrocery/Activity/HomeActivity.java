package com.example.gpgrocery.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.example.gpgrocery.Fragment.AddStockFragment;
import com.example.gpgrocery.Fragment.ListStockFragment;
import com.example.gpgrocery.Fragment.LogoutFragment;
import com.example.gpgrocery.Fragment.PurchaseFragment;
import com.example.gpgrocery.Fragment.SalesFragment;
import com.example.gpgrocery.Fragment.SearchStockFragment;
import com.example.gpgrocery.R;
import com.example.gpgrocery.databinding.ActivityHomeBinding;
import com.example.gpgrocery.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationView;

public class HomeActivity extends AppCompatActivity {

    ActivityHomeBinding homeBinding;
    SharedPreferences sharedPref1;
    ActionBarDrawerToggle mToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        homeBinding = ActivityHomeBinding.inflate(getLayoutInflater());
        View view = homeBinding.getRoot();
        setContentView(view);

        mToggle=new ActionBarDrawerToggle(this,homeBinding.drawerLayout,homeBinding.materialToolbar,R.string.nav_open,R.string.nav_close);
        homeBinding.drawerLayout.addDrawerListener(mToggle);
        mToggle.syncState();

        setSupportActionBar(homeBinding.materialToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setNavigationdrawer();

        sharedPref1 = getSharedPreferences("login_details", Context.MODE_PRIVATE);
        homeBinding.textView.setText("Welcome " + sharedPref1.getString("USERNAME", null));
    }

    private void setNavigationdrawer() {
        homeBinding.navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment frag = null;
                int itemId = item.getItemId();
                if(itemId==R.id.nav_add_stock_menu){
                    frag = new AddStockFragment();
                } else if(itemId==R.id.nav_sales_menu) {
                    frag = new SalesFragment();
                } else if(itemId==R.id.nav_purchase_menu){
                    frag = new PurchaseFragment();
                } else if(itemId==R.id.nav_search_stock_menu) {
                    frag = new SearchStockFragment();
                } else if(itemId==R.id.nav_list_stock_menu) {
                    frag = new ListStockFragment();
                } else if(itemId==R.id.nav_log_out_menu){
                    frag = new LogoutFragment();
                }
                if (frag != null) {
                    FragmentTransaction frgTrans = getSupportFragmentManager().beginTransaction();
                    frgTrans.replace(R.id.frame, frag);
                    frgTrans.commit();
                    homeBinding.drawerLayout.closeDrawers();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(mToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}