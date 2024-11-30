package com.example.restauranteup;

import java.util.*;
import java.util.concurrent.*;

import com.example.restauranteup.domain.models.*;
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

        // Iniciar hilo recepcionista
        Thread recepcionista = new Thread(this::asignarMesas);
        recepcionista.setName("Recepcionista");
        recepcionista.start();

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

    /**
     * Hilo recepcionista: asigna mesas a los comensales en espera.
     */
    private void asignarMesas() {
        while (true) {
            synchronized (comensalesEnEspera) {
                while (comensalesEnEspera.isEmpty()) {
                    try {
                        comensalesEnEspera.wait(); // Esperar si no hay comensales en la cola
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                ComensalThread comensal = comensalesEnEspera.poll(); // Obtener el siguiente comensal
                if (comensal != null) {
                    synchronized (mesas) {
                        int mesaDisponible = buscarMesaLibre();
                        if (mesaDisponible != -1) {
                            mesas.set(mesaDisponible, true); // Marcar la mesa como ocupada
                            comensal.setMesaId(mesaDisponible);
                            synchronized (comensalesEnMesas) {
                                comensalesEnMesas.add(comensal);
                                comensalesEnMesas.notifyAll();
                            }
                            System.out.println("Recepcionista asignó mesa " + mesaDisponible + " al comensal " + comensal.getComensalId());
                        } else {
                            //System.out.println("Recepcionista no encontró mesas libres para el comensal " + comensal.getComensalId());
                            comensalesEnEspera.add(comensal); // Volver a poner al comensal en la cola
                        }
                    }
                }
            }
        }
    }

    /**
     * Buscar una mesa libre.
     * @return El índice de la mesa libre o -1 si no hay ninguna disponible.
     */
    private int buscarMesaLibre() {
        for (int i = 0; i < mesas.size(); i++) {
            if (!mesas.get(i)) { // Si la mesa está libre
                return i;
            }
        }
        return -1; // No hay mesas disponibles
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
