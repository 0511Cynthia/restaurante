package com.example.restauranteup.infrastructure.views.controller;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.example.restauranteup.domain.models.EventBus;
import com.example.restauranteup.domain.models.threads.ComensalThread;
import com.example.restauranteup.domain.interfaces.Observer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import com.almasb.fxgl.animation.Animation;
import javafx.util.Duration;

public class FXGLRestauranteController implements Observer {

    private final List<Entity> mesas;
    private final Entity stage;
    private final Entity cocinero;
    private final Entity mesero; // Entidad para el mesero
    private final EventBus eventBus;

    private final List<Entity> platos = new ArrayList<>();
    private static final String[] CLIENT_ASSETS = {
            "cliente_1.png", "cliente_2.png", "cliente_3.png", "cliente_4.png", 
            "cliente_5.png", "cliente_6.png", "cliente_7.png", "cliente_8.png"
    };
    private Animation<?> chefAnimation;
    private Animation<?> meseroAnimation; // Animación del mesero
    private Random random = new Random();

    private final Map<Integer, Entity> comensalesEntities = new ConcurrentHashMap<>();
    private final Queue<ComensalThread> comensalQueue = new LinkedList<>();
    private final List<ComensalThread> comensalesEnMesas = new ArrayList<>();

    private static final Point2D MESERO_ORIGINAL_POSITION = new Point2D(750, 19);

    public FXGLRestauranteController(EventBus eventBus) {
        this.eventBus = eventBus;
        this.mesas = new ArrayList<>();

        eventBus.subscribe(this);

        stage = FXGL.entityBuilder()
                .view(new Rectangle(980, 560, Color.LIGHTGRAY))
                .buildAndAttach();

        cocinero = FXGL.entityBuilder()
                .zIndex(2)
                .at(621, 19)
                .view(FXGL.getAssetLoader().loadTexture("cocinero.png", 112, 120))
                .buildAndAttach();

        mesero = FXGL.entityBuilder()
                .zIndex(2)
                .at(MESERO_ORIGINAL_POSITION)
                .view(FXGL.getAssetLoader().loadTexture("mesero.png", 40, 80))
                .buildAndAttach();

        initMesasYPlatos();
    }

    private void initMesasYPlatos() {
        double[][] mesasCoords = {
            {277, 210}, {399, 210}, {519, 210}, {638, 210}, {755, 210},
            {277, 370}, {405, 370}, {527, 370}, {650, 370}, {762, 370}
        };

        for (int i = 0; i < mesasCoords.length; i++) {
            Entity mesa = FXGL.entityBuilder()
                    .at(mesasCoords[i][0], mesasCoords[i][1])
                    .with("type", "mesa")
                    .with("id", i + 1)
                    .buildAndAttach();
            mesas.add(mesa);

            Entity plato = FXGL.entityBuilder()
                    .zIndex(2)
                    .at(mesasCoords[i][0] + 10, mesasCoords[i][1] - 20)
                    .view(FXGL.getAssetLoader().loadTexture("aguachile.png", 30, 30))
                    .buildAndAttach();

            plato.getViewComponent().setVisible(false);
            platos.add(plato);
        }
    }

    public void addComensalToQueue(ComensalThread comensal) {
        Platform.runLater(() -> {
            // Agregar comensal a la cola lógica
            comensalQueue.add(comensal);
    
            double startX = 1000; // Posición inicial fuera de pantalla
            double startY = 450;
            double spacing = 40; // Espaciado entre comensales en la cola
    
            // Elegir textura aleatoria para el comensal
            String clienteAsset = CLIENT_ASSETS[random.nextInt(CLIENT_ASSETS.length)];
            Entity comensalEntity = FXGL.entityBuilder()
                    .zIndex(2)
                    .at(startX, startY)
                    .view(FXGL.getAssetLoader().loadTexture(clienteAsset, 40, 80))
                    .with("type", "comensal")
                    .with("id", comensal.getComensalId())
                    .buildAndAttach();
    
            // Asociar la entidad con el ID del comensal
            comensalesEntities.put(comensal.getComensalId(), comensalEntity);
    
            // Renderizar la cola basada en `comensalQueue`
            List<ComensalThread> queueList = new ArrayList<>(comensalQueue);
            for (int i = 0; i < queueList.size(); i++) {
                int comensalId = queueList.get(i).getComensalId();
                Entity entity = comensalesEntities.get(comensalId);
                if (entity != null) {
                    FXGL.animationBuilder()
                            .duration(Duration.seconds(1))
                            .translate(entity)
                            .to(new Point2D(100 + i * spacing, startY))
                            .buildAndPlay();
                }
            }
        });
    }

    public void moveComensalToMesa(ComensalThread comensal) {
        Platform.runLater(() -> {
            // Obtener y eliminar al comensal más antiguo de la cola
            ComensalThread nextComensal = comensalQueue.poll(); 
            if (nextComensal == null) {
                System.out.println("No hay comensales en la cola.");
                return;
            }
    
            Entity comensalEntity = comensalesEntities.get(nextComensal.getComensalId());
            if (comensalEntity == null) {
                System.out.println("Entidad de comensal no encontrada: " + nextComensal.getComensalId());
                return;
            }
    
            comensalesEnMesas.add(nextComensal); // Agregar a lista de comensales en mesas
    
            int mesaId = comensal.getMesaId();
            if (mesaId >= 0 && mesaId <= mesas.size()) {
                Entity mesa = mesas.get(mesaId);
    
                // Animar movimiento del comensal hacia la mesa
                FXGL.animationBuilder()
                        .duration(Duration.seconds(2))
                        .translate(comensalEntity)
                        .to(mesa.getPosition().add(10, 10))
                        .buildAndPlay();
            }
        });
    }
    
    public void liberarMesa(ComensalThread comensal) {
        Platform.runLater(() -> {
            // Eliminar la entidad gráfica del comensal
            Entity comensalEntity = comensalesEntities.get(comensal.getComensalId());
            if (comensalEntity != null) {
                comensalEntity.removeFromWorld(); // Eliminar del mundo
                comensalesEntities.remove(comensal.getComensalId()); // Remover de entidades activas
                System.out.println("FXGL!!! Comensal " + comensal.getComensalId() + " dejó el restaurante.");
            }
    
            // Liberar la mesa y ocultar el plato
            int mesaId = comensal.getMesaId();
            if (mesaId >= 0 && mesaId <= mesas.size()) {
                //comensalesEnMesas.remove(comensal.getComensalId());
                Entity plato = platos.get(mesaId);
                plato.getViewComponent().setVisible(false); // Ocultar el plato
                System.out.println("FXGL!!! Mesa " + mesaId + " liberada.");
            }
        });
    }
    

    private void handleChefCooking() {
        System.out.println("FXGL!!! Chef está cocinando.");
    
        // Crear la animación de movimiento en loop
        FXGL.animationBuilder()
            .duration(Duration.seconds(1))
            .repeatInfinitely()
            .autoReverse(true)
            .translate(cocinero)
            .from(cocinero.getPosition())
            .to(cocinero.getPosition().add(40, 0)) // Reducir el movimiento
            .buildAndPlay(); // Cambiar a buildAndPlay()
    }
    
    /**
     * Maneja el evento CHEF_COOKED: Detener el movimiento del cocinero.
     */
    private void handleChefCooked() {
        System.out.println("FXGL!!! Chef terminó de cocinar.");
    
        if (chefAnimation != null) {
            chefAnimation.stop(); // Detener la animación en ejecución
            chefAnimation = null; // Liberar referencia
        }
    
        // Restaurar la posición original del cocinero
        FXGL.animationBuilder()
                .duration(Duration.seconds(1))
                .translate(cocinero)
                .to(new Point2D(621, 19)) // Posición inicial fija
                .buildAndPlay();
    }
    
    private void handleAttendClient(Object data) {
        int mesaId = (int) data;
        if (mesaId < 0 || mesaId >= mesas.size()) return;
    
        Platform.runLater(() -> {
            Entity mesa = mesas.get(mesaId);
    
            // Crear y ejecutar la animación directamente
            FXGL.animationBuilder()
                .duration(Duration.seconds(1))
                .translate(mesero)
                .from(mesero.getPosition())
                .to(mesa.getPosition().subtract(20, 0))
                .buildAndPlay();
        });
    }
    

    private void regresarMesero() {
        Platform.runLater(() -> FXGL.animationBuilder()
                .duration(Duration.seconds(1))
                .translate(mesero)
                .to(MESERO_ORIGINAL_POSITION)
                .buildAndPlay());
    }

    private void handleServeDish(Object data) {
        int mesaId = (int) data;
        if (mesaId < 0 || mesaId >= mesas.size()) return;

        Platform.runLater(() -> {
            Entity mesa = mesas.get(mesaId);
            
            FXGL.animationBuilder()
                    .duration(Duration.seconds(0.6))
                    .translate(mesero)
                    .to(mesa.getPosition().subtract(20, 0))
                    .buildAndPlay();

            regresarMesero();

            Entity plato = platos.get(mesaId);
            plato.getViewComponent().setVisible(true);
        });
    }


    @Override
    public void update(String event, Object data) {
        Platform.runLater(() -> {
            switch (event) {
                case "NEW_QUEUE_COMENSAL" -> {
                    ComensalThread comensal = (ComensalThread) data;
                    addComensalToQueue(comensal);
                }
                case "NEW_COMENSAL" -> {
                    ComensalThread comensal = (ComensalThread) data;
                    moveComensalToMesa(comensal);
                }
                case "EXIT_COMENSAL" -> {
                    ComensalThread comensal = (ComensalThread) data;
                    liberarMesa(comensal);
                }
                case "CHEF_COOKING" -> handleChefCooking();
                case "CHEF_COOKED" -> handleChefCooked();
                case "SERVE_DISH" -> handleServeDish(data);
                case "ATTEND_CLIENT" -> handleAttendClient(data);
                default -> System.out.println("Evento desconocido: " + event);
            }
        });
    }
    
}