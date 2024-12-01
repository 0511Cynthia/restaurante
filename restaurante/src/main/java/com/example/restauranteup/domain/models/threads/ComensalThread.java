package com.example.restauranteup.domain.models;

import java.util.List;
import java.util.Queue;
import java.util.Random;
import com.example.restauranteup.Restaurante;

public class ComensalThread extends Thread {
    private final int comensalId;
    private final Restaurante restaurante; // Referencia al restaurante
    private Orden orden;
    private int mesaId;

    public ComensalThread(int comensalId, Restaurante restaurante) {
        this.comensalId = comensalId;
        this.restaurante = restaurante;
        this.mesaId = -1;
    }

    public int getComensalId() {
        return comensalId;
    }

    public int getMesaId() {
        return mesaId;
    }

    public void setMesaId(int mesaId) {
        this.mesaId = mesaId;
    }

    public Orden getOrden() {
        return orden;
    }

    public void setOrden(Orden orden) {
        this.orden = orden;
    }

    @Override
    public void run() {
        if (!esperarYAsignarMesa()) {
            System.out.println("Comensal " + comensalId + " no pudo obtener mesa y abandonó el restaurante.");
            return;
        }
    }

    /**
     * Intentar asignar una mesa llamando al restaurante.
     * @return true si se asignó una mesa, false si no.
     */
    private boolean esperarYAsignarMesa() {
        synchronized (restaurante) {
            while (mesaId == -1) { // Mientras no tenga mesa asignada
                mesaId = restaurante.asignarMesa(this); // Solicitar mesa al restaurante
                if (mesaId == -1) { // Si no hay mesas disponibles
                    try {
                        restaurante.wait(); // Esperar que una mesa quede libre
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Simular el tiempo de comida del comensal.
     */
    public void comer() {
        System.out.println("Comensal " + comensalId + " asignado a mesa " + mesaId + " está comiendo.");
        try {
            int tiempoComida = new Random().nextInt(4) + 2; // Tiempo en segundos (entre 2 y 5)
            Thread.sleep(tiempoComida * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Comensal " + comensalId + " terminó de comer en la mesa " + mesaId + ".");
        liberarMesa();
    }

    /**
     * Liberar la mesa después de comer.
     */
    private void liberarMesa() {
        restaurante.liberarMesa(mesaId, this); // Notificar al restaurante
    }

    @Override
    public String toString() {
        return "Comensal{id=" + comensalId + ", mesaId=" + mesaId + "}";
    }
}
