package com.example.gpgrocery.Fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.RadioGroup;

import com.example.gpgrocery.Activity.HomeActivity;
import com.example.gpgrocery.R;
import com.example.gpgrocery.Model.StockItem;
import com.example.gpgrocery.Util.DBHelper;
import com.example.gpgrocery.Util.Utils;
import com.example.gpgrocery.databinding.FragmentAddStockBinding;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AddStockFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AddStockFragment extends Fragment implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    FragmentAddStockBinding addStockBinding;
    DBHelper dbh;
    Boolean insertStatus;
    Boolean isTaxable;
    Intent intentHome;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public AddStockFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AddStockFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AddStockFragment newInstance(String param1, String param2) {
        AddStockFragment fragment = new AddStockFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        addStockBinding = FragmentAddStockBinding.inflate(inflater,container,false);
        View view = addStockBinding.getRoot();
        init();
        return view;
    }

    private void init() {
        dbh = new DBHelper(getActivity());
        addStockBinding.rdnonTaxable.setChecked(false);
        addStockBinding.rdTaxable.setChecked(false);
        addStockBinding.rgTaxable.setOnCheckedChangeListener(this);
        addStockBinding.btnSubmit.setOnClickListener(this);
        addStockBinding.btnCancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId()==addStockBinding.btnSubmit.getId()) {
            hideKeyboard();
            if(isFormValidated()){
                StockItem objStockItem = CreateStockItem();
                insertStatus = dbh.InsertStockItem(objStockItem);
                if (insertStatus) {
                    Utils.sendAlert(addStockBinding, "Stock Item added successfully");
                    clearForm();
                } else {
                    Utils.sendAlert(addStockBinding, "Record insertion failure");
                }
            }
        } else if(v.getId()==addStockBinding.btnCancel.getId()) {
            intentHome = new Intent(requireContext(), HomeActivity.class);
            startActivity(intentHome);
        }
    }

    private void clearForm() {
        addStockBinding.edtItemName.setText(null);
        addStockBinding.edtQty.setText(null);
        addStockBinding.edtPrice.setText(null);
        addStockBinding.rdnonTaxable.setChecked(false);
        addStockBinding.rdTaxable.setChecked(false);
    }

    private boolean isFormValidated() {
        String itemName = addStockBinding.edtItemName.getText().toString().trim();
        String qnty = String.valueOf(addStockBinding.edtQty.getText());
        String price = String.valueOf(addStockBinding.edtPrice.getText());

        if (itemName.isEmpty()) {
            addStockBinding.edtItemName.setError("Item Name name is required");
            Utils.sendAlert(addStockBinding, "Please insert the item name");
            return false;
        }
        if (qnty.isEmpty()) {
            addStockBinding.edtQty.setError("Quantity is required");
            Utils.sendAlert(addStockBinding, "Please insert the quantity");
            return false;
        }
        if (Integer.parseInt(String.valueOf(addStockBinding.edtQty.getText())) <= 0) {
            addStockBinding.edtQty.setError("Quantity is required");
            Utils.sendAlert(addStockBinding, "Please insert the quantity greater than zero");
            return false;
        }
        if (price.isEmpty()) {
            addStockBinding.edtPrice.setError("Price is required");
            Utils.sendAlert(addStockBinding, "Please insert the price");
            return false;
        }
        if (Float.parseFloat(String.valueOf(addStockBinding.edtPrice.getText())) <= 0.0) {
            addStockBinding.edtPrice.setError("Price is required");
            Utils.sendAlert(addStockBinding, "Please insert the price greater than zero");
            return false;
        }
        if (!isTaxableChecked()) {
            Utils.sendAlert(addStockBinding, "Please select Taxable or Non-Taxable");
            return false;
        }
        return true;
    }

    private boolean isTaxableChecked() {
        return addStockBinding.rgTaxable.getCheckedRadioButtonId() != -1;
    }

    private StockItem CreateStockItem() {
        StockItem objStockItem =  new StockItem();
        objStockItem.setItemName(addStockBinding.edtItemName.getText().toString().trim());
        objStockItem.setQtyStock(Integer.parseInt(String.valueOf(addStockBinding.edtQty.getText())));
        objStockItem.setPrice(Float.parseFloat(String.valueOf(addStockBinding.edtPrice.getText())));
        objStockItem.setTaxable(isTaxable);
        return objStockItem;
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && getActivity().getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if(addStockBinding.rgTaxable.getCheckedRadioButtonId()==R.id.rdTaxable) {
            isTaxable = true;
        } else if(addStockBinding.rgTaxable.getCheckedRadioButtonId()==R.id.rdnonTaxable) {
            isTaxable = false;
        }
    }
}