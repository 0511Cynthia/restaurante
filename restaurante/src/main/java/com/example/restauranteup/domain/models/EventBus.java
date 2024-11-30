package com.example.restauranteup.domain.models;

import java.util.ArrayList;
import java.util.List;

import com.example.restauranteup.domain.interfaces.Observer;

public class EventBus {
    private final List<Observer> observers = new ArrayList<>();

    public void subscribe(Observer observer) {
        observers.add(observer);
    }

    public void unsubscribe(Observer observer) {
        observers.remove(observer);
    }

    public void notifyObservers(String event, Object data) {
        for (Observer observer : observers) {
            observer.update(event, data);
        }
    }
}
