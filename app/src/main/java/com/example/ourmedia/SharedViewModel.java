package com.example.ourmedia;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private MutableLiveData<String> actionParameter = new MutableLiveData<>();

    public void setActionParameter(String parameter) {
        actionParameter.setValue(parameter);
    }

    public LiveData<String> getActionParameter() {
        return actionParameter;
    }
}