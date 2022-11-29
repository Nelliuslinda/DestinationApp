package com.example.cart_firebase.liveServer;

import com.example.cart_firebase.model.DestinationModel;

import java.util.List;

public interface iDestinationLoadListener {
    void onDestinationLoadSuccess(List<DestinationModel> destinationModelList);
    void onDestinationLoadFailed(String message);
}
