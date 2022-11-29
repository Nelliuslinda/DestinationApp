package com.example.cart_firebase.liveServer;

import com.example.cart_firebase.model.CartModel;
import java.util.List;

public interface iCartLoadListener {
    void onCartLoadSuccess(List<CartModel> cartModelList);
     void onCartLoadFailed(String message);
}
