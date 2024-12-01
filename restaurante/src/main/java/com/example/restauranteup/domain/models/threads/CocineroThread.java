package com.example.restauranteup.domain.models.threads;

import java.util.Queue;
import java.util.Random;

import com.example.restauranteup.domain.models.Comida;
import com.example.restauranteup.domain.models.EventBus;
import com.example.restauranteup.domain.models.Orden;

public class CocineroThread extends Thread {

    private final int cocineroId;
    private boolean ocupado;
    private int tiempoCoccion;
    private final Queue<Orden> bufferOrdenes;
    private final Queue<Comida> bufferComidas;
    private final EventBus eventBus;

    public CocineroThread(int cocineroId, Queue<Orden> bufferOrdenes, Queue<Comida> bufferComidas, EventBus eventBus) {
        this.cocineroId = cocineroId;
        this.bufferOrdenes = bufferOrdenes;
        this.bufferComidas = bufferComidas;
        this.eventBus = eventBus;
        this.ocupado = false;
    }

    public int getTiempoCoccion() {
        return tiempoCoccion;
    }

    public boolean isOcupado() {
        return ocupado;
    }

    @Override
    public void run() {
        while (true) {
            synchronized (bufferOrdenes) {
                while (bufferOrdenes.isEmpty()) {
                    try {
                        bufferOrdenes.wait(); // Esperar si no hay órdenes disponibles
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Propagar la interrupción
                        return;
                    }
                }

                if (!ocupado) {
                    // Tomar la orden del buffer
                    Orden orden = bufferOrdenes.poll();

                    if (orden != null) {
                        prepararOrden(orden);
                    }
                }
            }
        }
    }

    private void prepararOrden(Orden orden) {
        Random random = new Random();
        tiempoCoccion = (random.nextInt(2) + 2) * 1000; // Tiempo de cocción entre 2 y 3 s

        ocupado = true;
        System.out.println("Cocinero " + cocineroId + " cocinando orden " + orden.getId() +
                " para la mesa " + orden.getIdMesa() + " durante " + (tiempoCoccion / 1000) + " segundos");

        // Notificar que el cocinero está cocinando
        eventBus.notifyObservers("CHEF_COOKING", this);

        // Simular la preparación del platillo
        try {
            Thread.sleep(tiempoCoccion);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        // Añadir la comida al bufferComidas
        synchronized (bufferComidas) {
            bufferComidas.add(new Comida(orden));
            System.out.println("Platillo de la orden " + orden.getId() + " listo para la mesa " + orden.getIdMesa());
            bufferComidas.notifyAll(); // Notificar que hay comida lista
        }

        ocupado = false;
        tiempoCoccion = 0;

        // Notificar que el cocinero terminó
        eventBus.notifyObservers("CHEF_COOKED", this);
    }

    @Override
    public String toString() {
        return "Cocinero{id=" + cocineroId + ", ocupado=" + ocupado + "}";
    }
}