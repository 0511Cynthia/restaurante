package com.example.restauranteup.domain.models;

import java.util.Queue;
import java.util.Random;

public class CocineroThread extends Thread {

    private final int id;
    private boolean ocupado;
    private int tiempoCoccion;
    private final Queue<Orden> bufferOrdenes;
    private final Queue<Comida> bufferComidas;
    private final EventBus eventBus;

    public CocineroThread(int id, Queue<Orden> bufferOrdenes, Queue<Comida> bufferComidas, EventBus eventBus) {
        this.id = id;
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
        tiempoCoccion = random.nextInt(3001) + 3000; // Tiempo de cocción entre 3000 y 6000 ms

        ocupado = true;
        System.out.println("Cocinero " + id + " cocinando orden " + orden.getId() +
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
        return "Cocinero{id=" + id + ", ocupado=" + ocupado + "}";
    }
}