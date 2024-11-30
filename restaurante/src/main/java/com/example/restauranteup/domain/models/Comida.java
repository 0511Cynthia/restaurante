package com.example.restauranteup.domain.models;

public class Comida {
    private Orden orden;

    public Comida(Orden orden){
        this.orden = orden;
    }

    public Orden getOrden(){
        return orden;
    }
}