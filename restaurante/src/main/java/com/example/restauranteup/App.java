package com.example.restauranteup;

import java.io.File;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.example.restauranteup.domain.models.EventBus;
import com.example.restauranteup.infrastructure.ui.controller.FXGLRestauranteController;
import java.net.URL;

import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.scene.image.Image;

import com.almasb.fxgl.dsl.FXGL;

public class App extends GameApplication {

    private EventBus eventBus;
    private FXGLRestauranteController restauranteController;
    private Restaurante restaurante;

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(980);
        settings.setHeight(560);
        settings.setTitle("Restaurant Simulation");
    }

    @Override
    protected void initGame() {
        FXGL.entityBuilder()
        .view("escenario.png")
        .zIndex(1)
        .at(0, 0)
        .buildAndAttach();

        // Crear botón de inicio
        Button startButton = FXGL.getUIFactoryService().newButton("Iniciar Simulación");

        // Configurar posición y acción del botón
        startButton.setTranslateX(20); // Posición X
        startButton.setTranslateY(20); // Posición Y
        startButton.setOnAction(e -> restaurante.simular());

        startButton.setStyle("-fx-background-color: white; -fx-background-radius: 5px; -fx-font-size: 8px; -fx-text-fill: black; -fx-padding: 5px 10px;");

        // Agregar el botón a la escena de FXGL
        FXGL.getGameScene().addUINode(startButton);


        // Inicializar EventBus y Controller
        eventBus = new EventBus();
        restauranteController = new FXGLRestauranteController(eventBus);
        restaurante = new Restaurante(10, 1, 1, 100, eventBus);
    }

    public static void main(String[] args) {
        launch(args);
    }
}