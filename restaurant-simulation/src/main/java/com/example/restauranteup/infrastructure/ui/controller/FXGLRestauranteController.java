package com.example.restauranteup.infrastructure.ui.controller;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.example.restauranteup.domain.models.EventBus;
import com.example.restauranteup.domain.interfaces.Observer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class FXGLRestauranteController implements Observer {

    private final List<Entity> mesas;
    private final Entity stage;
    private final Entity cocinero;
    private final EventBus eventBus;

    // Mapa para manejar comensales y sus entidades gráficas
    private final Map<Integer, Entity> comensalesEntities = new ConcurrentHashMap<>();
    private final List<Entity> platos = new ArrayList<>();
    private static final String[] CLIENT_ASSETS = {
            "cliente_1.png", "cliente_2.png", "cliente_3.png", "cliente_4.png", 
            "cliente_5.png", "cliente_6.png", "cliente_7.png", "cliente_8.png"
    };
    private int controlMesas = 1; // Control de rotación de mesas
    private Random random = new Random();

    public FXGLRestauranteController(EventBus eventBus) {
        this.eventBus = eventBus;
        this.mesas = new ArrayList<>();

        //FXGL.getGameScene().setBackgroundRepeat("bg.png");

        // Crear la escena estática
        stage = FXGL.entityBuilder()
                .view(new Rectangle(980, 560, Color.LIGHTGRAY))
                .buildAndAttach();

        // Crear y agregar el cocinero
        cocinero = FXGL.entityBuilder()
                .zIndex(2)
                .at(621, 19)
                .view(FXGL.getAssetLoader().loadTexture("cocinero.png", 112, 120))
                .buildAndAttach();

        // Crear y agregar las mesas y platos
        initMesasYPlatos();
    }

    /**
     * Inicializar las mesas y los platos.
     */
    private void initMesasYPlatos() {
        // Coordenadas y tamaños de las mesas y sus platos
        double[][] mesasCoords = {
                {277, 210}, {399, 210}, {519, 210}, {638, 210}, {755, 210}, // Fila superior
                {277, 370}, {405, 370}, {527, 370}, {650, 370}, {762, 370}  // Fila inferior
        };

        for (int i = 0; i < mesasCoords.length; i++) {
            // Crear mesa
            // Entity mesa = FXGL.entityBuilder()
            //         .at(mesasCoords[i][0], mesasCoords[i][1])
            //         .view(FXGL.getAssetLoader().loadTexture("mesa.png", 60, 60))
            //         .buildAndAttach();

                        // Crear una entidad lógica para la mesa (sin imagen)
        Entity mesa = FXGL.entityBuilder()
                .at(mesasCoords[i][0], mesasCoords[i][1])
                .with("type", "mesa")
                .with("id", i + 1)
                .buildAndAttach();
            mesas.add(mesa);

            // Crear plato
            Entity plato = FXGL.entityBuilder()
                    .zIndex(2)
                    .at(mesasCoords[i][0] + 10, mesasCoords[i][1] - 20) // Ajuste de posición
                    .view(FXGL.getAssetLoader().loadTexture("aguachile.png", 30, 30))
                    .build();
            plato.setProperty("visible", false); // Inicialmente invisible
            platos.add(plato);
            FXGL.getGameWorld().addEntity(plato);
        }
    }

    /**
     * Agregar un comensal en la cola.
     */
    public void addComensalToQueue(int comensalId) {
        double startX = 1000 + comensalesEntities.size() * 150; // Posición inicial en X
        double startY = 450; // Línea de entrada

        String clienteAsset = CLIENT_ASSETS[random.nextInt(CLIENT_ASSETS.length)];

        // Crear entidad gráfica del comensal
        Entity comensalEntity = FXGL.entityBuilder()
                .zIndex(2)
                .at(startX, startY)
                .view(FXGL.getAssetLoader().loadTexture(clienteAsset, 40, 80))
                .with("type", "comensal")
                .with("id", comensalId)
                .buildAndAttach();

        comensalesEntities.put(comensalId, comensalEntity);
        System.out.println("Comensal " + comensalId + " añadido a la fila.");
    }

    /**
     * Mover un comensal a una mesa.
     */
    public void moveComensalToMesa(int comensalId) {
        Entity comensalEntity = comensalesEntities.get(comensalId);
        if (comensalEntity == null) {
            System.out.println("Comensal no encontrado: " + comensalId);
            return;
        }

        // Obtener la mesa asignada
        if (controlMesas > mesas.size()) controlMesas = 1; // Reiniciar el control de mesas
        Entity mesa = mesas.get(controlMesas - 1);

        // Mover el comensal a la mesa
        comensalEntity.setPosition(mesa.getPosition().add(10, 10)); // Ajustar posición a la mesa
        System.out.println("Comensal " + comensalId + " asignado a la mesa " + controlMesas);

        // Mostrar el plato correspondiente
        Entity plato = platos.get(controlMesas - 1);
        plato.setProperty("visible", true);
        controlMesas++;
    }

    /**
     * Liberar la mesa ocupada y ocultar el plato.
     */
    public void liberarMesa(int comensalId) {
        Entity comensalEntity = comensalesEntities.get(comensalId);
        if (comensalEntity != null) {
            comensalEntity.removeFromWorld();
            comensalesEntities.remove(comensalId);
            System.out.println("Comensal " + comensalId + " dejó el restaurante.");
        }

        if (controlMesas > 0 && controlMesas <= platos.size()) {
            Entity plato = platos.get(controlMesas - 1);
            plato.setProperty("visible", false);
            System.out.println("Mesa " + controlMesas + " liberada.");
        }
    }

    @Override
    public void update(String event, Object data) {
        // Manejo de eventos del sistema
        switch (event) {
            case "NEW_QUEUE_COMENSAL" -> {
                int comensalId = (int) data;
                addComensalToQueue(comensalId);
            }
            case "NEW_COMENSAL" -> {
                int comensalId = (int) data;
                moveComensalToMesa(comensalId);
            }
            case "EXIT_COMENSAL" -> {
                int comensalId = (int) data;
                liberarMesa(comensalId);
            }
            default -> System.out.println("Evento desconocido: " + event);
        }
    }
}
