package com.example.restauranteup.domain.interfaces;

public interface Observer {
    void update(String event, Object data);
}