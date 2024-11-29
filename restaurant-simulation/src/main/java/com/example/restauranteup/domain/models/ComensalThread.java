package com.example.restauranteup.domain.models;

import java.util.List;
import java.util.Queue;
import java.util.Random;

public class ComensalThread extends Thread {

    private final int comensalId;
    private boolean atendido;
    private Orden orden;
    private int mesaId;
    private final Queue<ComensalThread> comensalesEnEspera;
    private final Queue<ComensalThread> comensalesEnMesas;
    private final List<Boolean> mesas;
    private final EventBus eventBus;

    public ComensalThread(int comensalId, 
                          Queue<ComensalThread> comensalesEnEspera,
                          Queue<ComensalThread> comensalesEnMesas, 
                          List<Boolean> mesas, 
                          EventBus eventBus) {
        this.comensalId = comensalId;
        this.comensalesEnEspera = comensalesEnEspera;
        this.comensalesEnMesas = comensalesEnMesas;
        this.mesas = mesas;
        this.eventBus = eventBus;
        this.atendido = false;
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

    public boolean isAtendido() {
        return atendido;
    }

    public void setAtendido(boolean atendido) {
        this.atendido = atendido;
    }

    public Orden getOrden() {
        return orden;
    }

    public void setOrden(Orden orden) {
        this.orden = orden;
    }

    @Override
    public void run() {
        esperarMesa();
        comer();
        liberarMesa();
    }

    /**
     * Ciclo de espera por una mesa disponible.
     */
    private void esperarMesa() {
        synchronized (comensalesEnEspera) {
            comensalesEnEspera.add(this);
            eventBus.notifyObservers("NEW_QUEUE_COMENSAL", this);
            System.out.println("Comensal " + comensalId + " esperando una mesa.");

            while (mesaId == -1) { // Esperar hasta que se asigne una mesa
                try {
                    comensalesEnEspera.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * Ciclo de comer en la mesa.
     */
    private void comer() {
        try {
            synchronized (comensalesEnMesas) {
                comensalesEnMesas.add(this);
                eventBus.notifyObservers("NEW_COMENSAL", this);
                System.out.println("Comensal " + comensalId + " asignado a mesa " + mesaId + " está comiendo.");

                int tiempoComida = new Random().nextInt(5) + 3; // Tiempo entre 3 y 7 segundos
                Thread.sleep(tiempoComida * 1000L);
                System.out.println("Comensal " + comensalId + " terminó de comer en la mesa " + mesaId + ".");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Liberar la mesa ocupada y notificar al sistema.
     */
    private void liberarMesa() {
        synchronized (mesas) {
            mesas.set(mesaId, false); // Marcar la mesa como libre
            System.out.println("Mesa " + mesaId + " liberada por comensal " + comensalId + ".");
            mesas.notifyAll(); // Notificar que hay una mesa disponible
        }

        synchronized (comensalesEnMesas) {
            comensalesEnMesas.remove(this);
            eventBus.notifyObservers("EXIT_COMENSAL", this);
        }
    }

    /**
     * Asigna una mesa libre al comensal.
     * @return true si se asigna una mesa, false si no hay disponibles.
     */
    public synchronized boolean asignarMesa() {
        synchronized (mesas) {
            for (int i = 0; i < mesas.size(); i++) {
                if (!mesas.get(i)) { // Si la mesa está libre
                    mesas.set(i, true); // Ocupar la mesa
                    setMesaId(i);
                    System.out.println("Mesa " + i + " asignada al comensal " + comensalId + ".");
                    synchronized (comensalesEnEspera) {
                        comensalesEnEspera.remove(this); // Retirar de la cola de espera
                        comensalesEnEspera.notifyAll(); // Notificar a otros comensales
                    }
                    return true;
                }
            }
        }
        return false; // No hay mesas disponibles
    }

    @Override
    public String toString() {
        return "Comensal{id=" + comensalId + ", atendido=" + atendido + ", mesaId=" + mesaId + "}";
    }
}
