package com.example.restauranteup.domain.models;

public class Orden {
    private int id;
    private final ComensalThread comensal;
    private final int idMesa; // Identificador de la mesa

    public Orden(ComensalThread comensal, int idMesa) {
        this.id = comensal.getComensalId();
        this.comensal = comensal;
        this.idMesa = idMesa;
    }

    public int getId() {
        return id;
    }

    public ComensalThread getComensal() {
        return comensal;
    }

    public int getIdMesa() {
        return idMesa;
    }

    @Override
    public String toString() {
        return "Orden{id=" + id + "}";
    }
}
