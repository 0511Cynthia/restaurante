package com.example.restauranteup;

import java.util.*;
import java.util.concurrent.*;

import com.example.restauranteup.domain.models.*;
import com.example.restauranteup.infrastructure.DistribucionPoisson;

public class Restaurante {
    private final Object lockMesas = new Object(); // Monitor para mesas
    private final Object lockComensalesEnEspera = new Object(); // Monitor para comensales en espera
    private final Object lockComensalesEnMesas = new Object(); // Monitor para comensales en mesas

    private final Queue<ComensalThread> comensalesEnEspera;
    private final Queue<ComensalThread> comensalesEnMesas;
    private final List<Boolean> mesas;
    private final Queue<Orden> bufferOrdenes;
    private final Queue<Comida> bufferComidas;

    private final List<MeseroThread> meseros;
    private final List<CocineroThread> cocineros;

    private final DistribucionPoisson distribucionPoisson;
    private final EventBus eventBus;

    private int comensalId;

    public Restaurante(int capacidad, int numMeseros, int numCocineros, int maximoComensales, EventBus eventBus) {
        this.comensalesEnEspera = new LinkedBlockingQueue<>();
        this.comensalesEnMesas = new LinkedBlockingQueue<>();
        this.mesas = new ArrayList<>(Collections.nCopies(capacidad, false)); // Todas las mesas libres
        this.bufferOrdenes = new LinkedBlockingQueue<>();
        this.bufferComidas = new LinkedBlockingQueue<>();
        this.eventBus = eventBus;

        this.meseros = new ArrayList<>();
        for (int i = 0; i < numMeseros; i++) {
            meseros.add(new MeseroThread(i + 1, bufferOrdenes, bufferComidas, comensalesEnMesas, eventBus));
        }

        this.cocineros = new ArrayList<>();
        for (int i = 0; i < numCocineros; i++) {
            cocineros.add(new CocineroThread(i + 1, bufferOrdenes, bufferComidas, eventBus));
        }

        this.distribucionPoisson = new DistribucionPoisson(2.0);
    }

    public void simular() {
        System.out.println("Iniciando simulación...");

        // Iniciar hilos de meseros
        meseros.forEach(Thread::start);

        // Iniciar hilos de cocineros
        cocineros.forEach(Thread::start);

        // Generar comensales
        generarComensales();
    }

    private void generarComensales() {
        new Thread(() -> {
            while (comensalId < 50) { // Generar hasta n comensales
                int intervalo = distribucionPoisson.generar(); // Generar intervalo en segundos

                ComensalThread nuevoComensal = new ComensalThread(
                        comensalId++,
                        this
                );

                synchronized (lockComensalesEnEspera) {
                    comensalesEnEspera.add(nuevoComensal);
                    eventBus.notifyObservers("NEW_QUEUE_COMENSAL", nuevoComensal);
                    System.out.println("Nuevo comensal " + nuevoComensal.getComensalId() + " llegando.");
                    lockComensalesEnEspera.notifyAll(); // Notificar que hay un nuevo comensal
                }

                nuevoComensal.start();

                try {
                    Thread.sleep(intervalo * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            System.out.println("Generación de comensales finalizada.");
        }).start();
    }


    public void detenerSimulacion() {
        // Interrumpir todos los hilos activos
        meseros.forEach(Thread::interrupt);
        cocineros.forEach(Thread::interrupt);
        comensalesEnMesas.forEach(Thread::interrupt);
        comensalesEnEspera.forEach(Thread::interrupt);

        System.out.println("Simulación detenida.");
    }

    public int asignarMesa(ComensalThread comensal) {
        synchronized (lockMesas) {
            for (int i = 0; i < mesas.size(); i++) {
                if (!mesas.get(i)) { // Si la mesa está libre
                    mesas.set(i, true);
                    comensal.setMesaId(i);
                    synchronized (comensalesEnMesas) {
                        comensalesEnMesas.add(comensal);
                        eventBus.notifyObservers("NEW_COMENSAL", comensal);
                        comensalesEnMesas.notifyAll(); // Notificar que hay un nuevo comensal en mesa
                    }
                    return i;
                }
            }
        }
        return -1;
    }

    public void liberarMesa(int mesaId, ComensalThread comensal) {
        synchronized (lockMesas) {
            mesas.set(mesaId, false); // Liberar la mesa
            System.out.println("Mesa " + mesaId + " liberada por comensal " + comensal.getComensalId());
            synchronized (lockComensalesEnMesas) {
                comensalesEnMesas.remove(comensal);
                eventBus.notifyObservers("EXIT_COMENSAL", comensal);
            }
        }
    }
}