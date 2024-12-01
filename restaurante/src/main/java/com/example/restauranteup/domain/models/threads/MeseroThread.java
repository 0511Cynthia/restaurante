package com.example.restauranteup.domain.models;

import java.util.Queue;

public class MeseroThread extends Thread {

    private final int meseroId;
    private boolean ocupado;
    private ComensalThread comensalActual;
    private final Queue<Orden> bufferOrdenes;
    private final Queue<Comida> bufferComidas;
    private final Queue<ComensalThread> comensalesEnMesas;
    private final EventBus eventBus;

    public MeseroThread(int meseroId,
                        Queue<Orden> bufferOrdenes,
                        Queue<Comida> bufferComidas,
                        Queue<ComensalThread> comensalesEnMesas,
                        EventBus eventBus) {
        this.meseroId = meseroId;
        this.bufferOrdenes = bufferOrdenes;
        this.bufferComidas = bufferComidas;
        this.comensalesEnMesas = comensalesEnMesas;
        this.eventBus = eventBus;
        this.ocupado = false;
        this.comensalActual = null;
    }

    public int getMeseroId() {
        return meseroId;
    }

    public boolean isOcupado() {
        return ocupado;
    }

    public ComensalThread getComensalActual() {
        return comensalActual;
    }

    @Override
    public void run() {
        while (true) {
            atenderComensal();
            esperarComidaYServir();
        }
    }

    /**
     * Atender a un comensal y generar su orden.
     */
    private void atenderComensal() {
        synchronized (comensalesEnMesas) {
            
            while (comensalesEnMesas.isEmpty() || ocupado) {
                try {
                    comensalesEnMesas.wait(); // Esperar si no hay comensales o si el mesero está ocupado
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            // Obtener el siguiente comensal en la cola
            ComensalThread comensal = comensalesEnMesas.poll();
            
            if (comensal != null) {
                atenderComensalInterno(comensal);
            }
        }
    }

    private void atenderComensalInterno(ComensalThread comensal) {
        ocupado = true;
        this.comensalActual = comensal;

        System.out.println("Mesero " + meseroId + " atendiendo al comensal " + comensal.getComensalId() + " (1 seg)");
        eventBus.notifyObservers("ATTEND_CLIENT", comensal.getMesaId());

        try {
            Thread.sleep(1000); // Simular tiempo de atención
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Generar una orden para el comensal
        Orden orden = generarOrden(comensal.getMesaId());
        if (orden != null) {
            agregarOrdenAlBuffer(orden);
        }
    }

    /**
     * Generar una orden para el comensal actual.
     */
    private Orden generarOrden(int idMesa) {
        if (comensalActual != null) {
            Orden orden = new Orden(comensalActual, idMesa);
            comensalActual.setOrden(orden);
            return orden;
        }
        return null;
    }

    /**
     * Agregar la orden generada al buffer de órdenes.
     */
    private void agregarOrdenAlBuffer(Orden orden) {
        synchronized (bufferOrdenes) {
            bufferOrdenes.add(orden);
            System.out.println("Mesero " + meseroId + " añadió una nueva orden al buffer: Orden " + orden.getId());
            bufferOrdenes.notifyAll();
        }
    }

    /**
     * Esperar comida y servirla al comensal.
     */
    private void esperarComidaYServir() {
        synchronized (bufferComidas) {
            while (bufferComidas.isEmpty() || comensalActual == null) {
                try {
                    bufferComidas.wait(); // Esperar si no hay comida lista
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            // Verificar si hay comida lista para el comensal actual
            Comida comidaLista = obtenerComidaLista();
            if (comidaLista != null) {
                bufferComidas.remove(comidaLista); // Eliminar la comida del buffer
                servirComida(comidaLista);
            }
        }
    }

    private Comida obtenerComidaLista() {
        for (Comida comida : bufferComidas) {
            if (comida.getOrden().getId() == comensalActual.getOrden().getId()) {
                return comida;
            }
        }
        return null;
    }

    /**
     * Servir comida al comensal actual.
     */
    private void servirComida(Comida comida) {
        System.out.println("Mesero " + meseroId + " sirviendo comida al comensal " + comensalActual.getComensalId());
        eventBus.notifyObservers("SERVE_DISH", comensalActual.getMesaId());

        comensalActual.comer();

        // Finalizar interacción con el comensal actual
        comensalActual = null;
        ocupado = false;

        synchronized (comensalesEnMesas) {
            comensalesEnMesas.notifyAll(); // Notificar que el mesero está libre
        }
    }

    @Override
    public String toString() {
        return "MeseroThread{id=" + meseroId + ", ocupado=" + ocupado + "}";
    }
}
