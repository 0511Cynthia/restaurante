package com.example.restauranteup;

import java.util.*;
import java.util.concurrent.*;

import com.example.restauranteup.domain.models.EventBus;
import com.example.restauranteup.domain.models.Orden;
import com.example.restauranteup.domain.models.Comida;
import com.example.restauranteup.domain.models.CocineroThread;
import com.example.restauranteup.domain.models.ComensalThread;
import com.example.restauranteup.domain.models.MeseroThread;
import com.example.restauranteup.infrastructure.DistribucionPoisson;

public class Restaurante {
    private final int maximoComensales;
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
        this.maximoComensales = maximoComensales;
        this.comensalId = 0;
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

        // Hilo para generar comensales
        new Thread(() -> {
            while (comensalId < maximoComensales) { // Generar hasta n comensales
                int intervalo = distribucionPoisson.generar(); // Genera el intervalo en segundos

                // Crear un nuevo comensal y asignarlo a la espera
                ComensalThread nuevoComensal = new ComensalThread(
                        comensalId++,
                        comensalesEnEspera,
                        comensalesEnMesas,
                        mesas,
                        eventBus
                );

                synchronized (comensalesEnEspera) {
                    comensalesEnEspera.add(nuevoComensal);
                    System.out.println("Nuevo comensal " + nuevoComensal.getComensalId() + " llegando.");
                    comensalesEnEspera.notifyAll();
                }

                // Iniciar el hilo del comensal
                nuevoComensal.start();

                // Simular el intervalo entre llegadas
                try {
                    Thread.sleep(intervalo * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            System.out.println("Generación de comensales finalizada. Total: " + comensalId);
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
}