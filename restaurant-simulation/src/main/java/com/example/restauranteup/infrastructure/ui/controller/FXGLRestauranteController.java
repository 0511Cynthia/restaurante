package com.example.restauranteup.infrastructure.ui.controller;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.example.restauranteup.domain.models.EventBus;
import com.example.restauranteup.domain.interfaces.Observer;
import com.example.restauranteup.domain.models.ComensalThread;
import com.example.restauranteup.domain.models.CocineroThread;
import com.example.restauranteup.domain.models.MeseroThread;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.*;

public class FXGLRestauranteController implements Observer {

    private final List<Entity> mesas = new ArrayList<>();
    private final Queue<ComensalThread> queueComensales = new LinkedList<>();
    private final Map<ComensalThread, Entity> comensalesEntities = new HashMap<>();
    private Entity entrance;
    private Entity kitchen;
    private Entity reception;

    public FXGLRestauranteController(EventBus eventBus) {
        eventBus.subscribe(this);
        initScenes();
    }

    // Inicialización de las escenas
    private void initScenes() {
        // Crear entrada
        entrance = FXGL.entityBuilder()
                .at(180, 580)
                .view(new Rectangle(100, 20, Color.BROWN))
                .build();
        FXGL.getGameWorld().addEntity(entrance);

        // Crear cocina
        kitchen = FXGL.entityBuilder()
                .at(40, 50)
                .view(FXGL.getAssetLoader().loadTexture("kitchen.png", 250, 200))
                .build();
        FXGL.getGameWorld().addEntity(kitchen);

        FXGL.getGameWorld().addEntity(FXGL.entityBuilder()
                .at(15, 135)
                .view(FXGL.getAssetLoader().loadTexture("chef.png", 50, 80))
                .build());
        FXGL.getGameWorld().addEntity(FXGL.entityBuilder()
                .at(175, 135)
                .view(FXGL.getAssetLoader().loadTexture("chef.png", 50, 80))
                .build());

        // Crear recepción
        reception = FXGL.entityBuilder()
                .at(100, 500)
                .view(FXGL.getAssetLoader().loadTexture("table.png"))
                .build();
        FXGL.getGameWorld().addEntity(reception);

        FXGL.getGameWorld().addEntity(FXGL.entityBuilder()
                .at(35, 480)
                .view(FXGL.getAssetLoader().loadTexture("receptionist.png", 50, 80))
                .build());

        // Crear mesas
        int rows = 5, cols = 4, startX = 350, startY = 50, separation = 40, tableSize = 70;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Entity table = FXGL.entityBuilder()
                        .at(startX + col * (tableSize + separation), startY + row * (tableSize + separation))
                        .view(FXGL.getAssetLoader().loadTexture("tables.png", tableSize, tableSize))
                        .build();
                mesas.add(table);
                FXGL.getGameWorld().addEntity(table);
            }
        }
    }

    @Override
    public void update(String event, Object data) {
        switch (event) {
            case "NEW_COMENSAL":
                addComensalToMesa((ComensalThread) data);
                break;
            case "NEW_QUEUE_COMENSAL":
                addComensalToQueue((ComensalThread) data);
                break;
            case "EXIT_COMENSAL":
                liberarMesa((ComensalThread) data);
                break;
            case "ATTEND_CLIENT":
                moveMeseroToClient((MeseroThread) data);
                break;
            case "SERVE_DISH":
                servePlato((MeseroThread) data);
                break;
            case "CHEF_COOKING":
                setChefCooking((CocineroThread) data);
                break;
            case "CHEF_COOKED":
                setChefIdle((CocineroThread) data);
                break;
            default:
                System.out.println("Evento desconocido: " + event);
        }
    }

    private void addComensalToQueue(ComensalThread comensal) {
        Platform.runLater(() -> {
            queueComensales.add(comensal);

            Entity comensalEntity = FXGL.entityBuilder()
                    .at(entrance.getX(), entrance.getY() - queueComensales.size() * 50)
                    .view(FXGL.getAssetLoader().loadTexture("boy.png", 50, 70))
                    .build();
            FXGL.getGameWorld().addEntity(comensalEntity);
            comensalesEntities.put(comensal, comensalEntity);

            System.out.println("Comensal " + comensal.getComensalId() + " añadido a la fila.");
        });
    }

    private void addComensalToMesa(ComensalThread comensal) {
        Platform.runLater(() -> {
            int mesaId = comensal.getMesaId();
            if (mesaId >= 0 && mesaId < mesas.size()) {
                Entity mesa = mesas.get(mesaId);

                Entity comensalEntity = FXGL.entityBuilder()
                        .at(mesa.getX() + 10, mesa.getY() + 10)
                        .view(FXGL.getAssetLoader().loadTexture("boy.png", 50, 70))
                        .build();
                FXGL.getGameWorld().addEntity(comensalEntity);

                comensalesEntities.put(comensal, comensalEntity);
                System.out.println("Comensal " + comensal.getComensalId() + " añadido a la mesa " + mesaId);
            }
        });
    }

    private void liberarMesa(ComensalThread comensal) {
        Platform.runLater(() -> {
            Entity comensalEntity = comensalesEntities.remove(comensal);
            if (comensalEntity != null) {
                comensalEntity.removeFromWorld();
                System.out.println("Mesa " + comensal.getMesaId() + " liberada.");
            }
        });
    }

    private void moveMeseroToClient(MeseroThread mesero) {
        Platform.runLater(() -> {
            ComensalThread comensal = mesero.getComensalActual();
            if (comensal != null) {
                System.out.println("Mesero " + mesero.getMeseroId() + " moviéndose hacia comensal " + comensal.getComensalId());
            }
        });
    }

    private void servePlato(MeseroThread mesero) {
        Platform.runLater(() -> System.out.println("Mesero " + mesero.getMeseroId() + " sirviendo plato."));
    }

    private void setChefCooking(CocineroThread cocinero) {
        Platform.runLater(() -> System.out.println("Cocinero " + cocinero.getId() + " está cocinando."));
    }

    private void setChefIdle(CocineroThread cocinero) {
        Platform.runLater(() -> System.out.println("Cocinero " + cocinero.getId() + " está inactivo."));
    }
}