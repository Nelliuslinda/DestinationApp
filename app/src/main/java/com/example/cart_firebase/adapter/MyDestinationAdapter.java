package com.example.cart_firebase.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cart_firebase.R;
import com.example.cart_firebase.eventbus.MyUpdateCartEvent;
import com.example.cart_firebase.liveServer.iCartLoadListener;
import com.example.cart_firebase.liveServer.iRecyclerViewClickListener;
import com.example.cart_firebase.model.CartModel;
import com.example.cart_firebase.model.DestinationModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyDestinationAdapter extends RecyclerView.Adapter<MyDestinationAdapter.MyDestinationViewHolder> {

    private Context context;
    private List<DestinationModel> destinationModelList;
    private iCartLoadListener icartLoadListener;

    public MyDestinationAdapter(Context context, List<DestinationModel> destinationModelList, iCartLoadListener icartLoadListener) {
        this.context = context;
        this.destinationModelList = destinationModelList;
        this.icartLoadListener = icartLoadListener;
    }

    @NonNull
    @Override
    public MyDestinationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyDestinationViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.layout_destination_item,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyDestinationViewHolder holder, int position) {
        Glide.with(context)
                .load(destinationModelList.get(position).getPhoto())
                .into(holder.imageView);
        holder.txtPrice.setText(new StringBuilder("KES ").append(destinationModelList.get(position).getPrice()));
        holder.txtName.setText(new StringBuilder().append(destinationModelList.get(position).getDestinationname()));
        holder.txtLocation.setText(new StringBuilder().append(destinationModelList.get(position).getLocation()));
        holder.txtDescription.setText(new StringBuilder().append(destinationModelList.get(position).getDescription()));

        holder.setListener((view, adapterPosition) -> {
            addToCart(destinationModelList.get(position));
        });
    }

    private void addToCart(DestinationModel destinationModel) {
        DatabaseReference userCart = FirebaseDatabase
                .getInstance()
                .getReference("Cart")
                .child("UNIQUE_USER_ID");

        userCart.child(destinationModel.getKey())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){//if the user has the destination in the cart
                            //update the quantity and total price
                            CartModel cartModel = snapshot.getValue(CartModel.class);
                            cartModel.setDays(cartModel.getDays()+1);
                            Map<String,Object> updateData = new HashMap<>();
                            updateData.put("days",cartModel.getDays());
                            updateData.put("totalPrice", cartModel.getDays()*Float.parseFloat(cartModel.getPrice()));

                            userCart.child(destinationModel.getKey())
                                    .updateChildren(updateData)
                                    .addOnSuccessListener(aVoid -> {
                                        icartLoadListener.onCartLoadFailed("Destination added successfully to the cart");
                                    })
                                    .addOnFailureListener(e -> icartLoadListener.onCartLoadFailed(e.getMessage()));
                        }
                        else//If item is not in the cart add new
                        {
                            CartModel cartModel = new CartModel();
                            cartModel.setDestinationname(destinationModel.getDestinationname());
                            cartModel.setPhoto(destinationModel.getPhoto());
                            cartModel.setKey(destinationModel.getKey());
                            cartModel.setPrice(destinationModel.getPrice());
                            cartModel.setDays(1);
                            cartModel.setTotalPrice(Float.parseFloat(destinationModel.getPrice()));

                            userCart.child(destinationModel.getKey())
                                    .setValue(cartModel)
                                    .addOnSuccessListener(aVoid -> {
                                        icartLoadListener.onCartLoadFailed("Add To Cart Success");
                                    })
                                    .addOnFailureListener(e -> icartLoadListener.onCartLoadFailed(e.getMessage()));

                        }

                        EventBus.getDefault().postSticky(new MyUpdateCartEvent());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        //iCartLoadListener.onCartLoadFailed(error.getMessage());
                    }
                });
    }

    @Override
    public int getItemCount() {
        return destinationModelList.size();
    }

    public class MyDestinationViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.imageView)
        ImageView imageView;
        @BindView(R.id.txtName)
        TextView txtName;
        @BindView(R.id.txtPrice)
        TextView txtPrice;
        @BindView(R.id.txtLocation)
        TextView txtLocation;
        @BindView(R.id.txtDescription)
        TextView txtDescription;

        iRecyclerViewClickListener listener;

        public void setListener(iRecyclerViewClickListener listener) {
            this.listener = listener;
        }

        private Unbinder unbinder;
        public MyDestinationViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this,itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            listener.onRecyclerClick(view,getAdapterPosition());
        }
    }
}
