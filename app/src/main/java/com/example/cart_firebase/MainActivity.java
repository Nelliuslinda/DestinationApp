package com.example.cart_firebase;

import android.content.Intent;
import android.os.Bundle;
import android.telephony.CarrierConfigManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cart_firebase.adapter.MyDestinationAdapter;
import com.example.cart_firebase.eventbus.MyUpdateCartEvent;
import com.example.cart_firebase.liveServer.iCartLoadListener;
import com.example.cart_firebase.liveServer.iDestinationLoadListener;
import com.example.cart_firebase.model.CartModel;
import com.example.cart_firebase.model.DestinationModel;
import com.example.cart_firebase.utils.SpaceItemDecoration;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nex3z.notificationbadge.NotificationBadge;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements iDestinationLoadListener, iCartLoadListener {

    @BindView(R.id.recycler_destination)
    RecyclerView recyclerDestination;
    @BindView(R.id.mainLayout)
    RelativeLayout mainLayout;
    @BindView(R.id.badge)
    NotificationBadge badge;
    @BindView(R.id.btnCart)
    FrameLayout btnCart;

    iDestinationLoadListener destinationLoadListener;
    iCartLoadListener cartLoadListener;

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        if(EventBus.getDefault().hasSubscriberForEvent(MyUpdateCartEvent.class))
            EventBus.getDefault().removeStickyEvent(MyUpdateCartEvent.class);
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe(threadMode = ThreadMode.MAIN,sticky = true)
    public void  onUpdateCart(MyUpdateCartEvent event){
        countCartItem();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        loadDestinationFromFirebase();
        countCartItem();
    }

    private void loadDestinationFromFirebase() {
        List<DestinationModel> destinationModels = new ArrayList<>();
        FirebaseDatabase.getInstance()
                .getReference("Destination")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot destinationSnapshot : snapshot.getChildren()) {
                                DestinationModel destinationModel = destinationSnapshot.getValue(DestinationModel.class);
                                destinationModel.setKey(destinationSnapshot.getKey());
                                destinationModels.add(destinationModel);
                            }
                            destinationLoadListener.onDestinationLoadSuccess(destinationModels);
                        } else {
                            destinationLoadListener.onDestinationLoadFailed("Can't find destination");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        destinationLoadListener.onDestinationLoadFailed(error.getMessage());
                    }
                });
    }

    private void init() {
        ButterKnife.bind(this);

        destinationLoadListener = this;
        cartLoadListener = this;

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerDestination.setLayoutManager(gridLayoutManager);
        recyclerDestination.addItemDecoration(new SpaceItemDecoration());

        btnCart.setOnClickListener(v->startActivity(new Intent(this, CartActivity.class)));

    }

    @Override
    public void onDestinationLoadSuccess(List<DestinationModel> destinationModelList) {
        MyDestinationAdapter adapter = new MyDestinationAdapter(this, destinationModelList, cartLoadListener);
        recyclerDestination.setAdapter(adapter);
    }

    @Override
    public void onDestinationLoadFailed(String message) {
        Snackbar.make(mainLayout, message, Snackbar.LENGTH_LONG).show();

    }

    @Override
    public void onCartLoadSuccess(List<CartModel> cartModelList) {
        int cartSum = 0;
        for (CartModel cartModel : cartModelList)
            cartSum += cartModel.getDays();
        badge.setNumber(cartSum);

    }

    @Override
    public void onCartLoadFailed(String message) {
        Snackbar.make(mainLayout, message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        countCartItem();
    }

    private void countCartItem() {
        List<CartModel> cartModels = new ArrayList<>();
        FirebaseDatabase
                .getInstance().getReference("Cart")
                .child("UNIQUE_USER_ID")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for(DataSnapshot cartSnapshot:snapshot.getChildren()){
                            CartModel cartModel = cartSnapshot.getValue(CartModel.class);
                            cartModel.setKey(cartSnapshot.getKey());
                            cartModels.add(cartModel);
                        }
                        cartLoadListener.onCartLoadSuccess(cartModels);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        cartLoadListener.onCartLoadFailed(error.getMessage());
                    }
                });
    }
}
