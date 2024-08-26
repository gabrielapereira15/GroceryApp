//package com.example.gpgrocery.Util;
//
//import android.annotation.SuppressLint;
//import android.content.Context;
//import android.content.Intent;
//import android.database.Cursor;
//import android.graphics.Color;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import androidx.appcompat.app.AlertDialog;
//import android.content.DialogInterface;
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//import java.util.List;
//
//public class ListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
//
//    private List<Place> dataList;
//    PlaceItemLayoutBinding placeItemBinding;
//    boolean isSaved;
//    DBHelper dbh;
//
//    public ListAdapter(List placesList, Context context) {
//        super();
//        this.dataList = placesList;
//        this.dbh = new DBHelper(context);
//    }
//
//    @NonNull
//    @Override
//    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
//        placeItemBinding = PlaceItemLayoutBinding.inflate(layoutInflater, parent, false);
//        return new ViewHolder(placeItemBinding);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
//        Cursor cursor1 = dbh.readFavorites();
//        ((ViewHolder) holder). bindView(dataList.get(position), cursor1);
//
//        // Set OnClickListener to favorite icon
//        ((ViewHolder) holder).recyclerRowBinding.imgSaveLocation.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (isUserLogged()) {
//                    int clickedPosition = holder.getAdapterPosition();
//                    Place clickedPlace = dataList.get(clickedPosition);
//                    Favorite placeFavorite = CreateFavorite(clickedPlace);
//                    if (!isSaved) {
//                        isSaved = true;
//                        ((ViewHolder) holder).recyclerRowBinding.imgSaveLocation.setColorFilter(Color.RED);
//                        dbh.InsertFavorite(placeFavorite);
//                    } else {
//                        isSaved = false;
//                        dbh.DeleteFavorite(placeFavorite);
//                        ((ViewHolder) holder).recyclerRowBinding.imgSaveLocation.setColorFilter(Color.LTGRAY);
//                    }
//                } else {
//                    new AlertDialog.Builder(placeItemBinding.getRoot().getContext())
//                            .setMessage("User not registered, please login to favorite places!")
//                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int id) {
//                                    dialog.dismiss();
//                                    Intent intentMain = new Intent(placeItemBinding.getRoot().getContext(), MainActivity.class);
//                                    placeItemBinding.getRoot().getContext().startActivity(intentMain);
//                                }
//
//                            })
//                            .setCancelable(false)
//                            .show();
//                }
//            }
//        });
//
//    }
//
//    private boolean isUserLogged() {
//        Cursor cursor1 = dbh.readProfile();
//        if (cursor1.getCount() == 0) {
//            return false;
//        }
//        return true;
//    }
//
//    public Favorite CreateFavorite (Place placeData) {
//        Favorite objFavorite = new Favorite();
//        objFavorite.setPlaceName(placeData.getName().toString().trim());
//        objFavorite.setPlaceAddress(placeData.getAddress().toString().trim());
//        objFavorite.setType(placeData.getPlaceTypes().toString().trim());
//        return objFavorite;
//    }
//
//    @Override
//    public int getItemCount() {
//        return dataList.size();
//    }
//
//    class ViewHolder extends RecyclerView.ViewHolder {
//        PlaceItemLayoutBinding recyclerRowBinding;
//        public ViewHolder(PlaceItemLayoutBinding recyclerRowBinding) {
//            super(recyclerRowBinding.getRoot());
//            this.recyclerRowBinding = recyclerRowBinding;
//        }
//
//        public void bindView(Place placeData, Cursor cursor1) {
//            recyclerRowBinding.txtPlaceDRating.setText(String.valueOf(placeData.getRating()));
//            recyclerRowBinding.txtPlaceName.setText(placeData.getName());
//            recyclerRowBinding.txtPlaceAddress.setText(placeData.getAddress());
//
//            if (cursor1.moveToFirst()) {
//                do {
//                    @SuppressLint("Range") String favoritePlaceName = cursor1.getString(cursor1.getColumnIndex("placeName"));
//                    if (placeData.getName().equals(favoritePlaceName)) {
//                        recyclerRowBinding.imgSaveLocation.setColorFilter(Color.RED);
//                    }
//                } while (cursor1.moveToNext());
//            }
//            cursor1.close();
//
//        }
//    }
//}