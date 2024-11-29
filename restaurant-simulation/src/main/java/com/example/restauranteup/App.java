package com.example.restauranteup;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.example.restauranteup.domain.models.EventBus;
import com.example.restauranteup.infrastructure.ui.controller.FXGLRestauranteController;
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
        FXGL.getGameScene().setBackgroundRepeat("floor.png");

        // Inicializar EventBus y Controller
        eventBus = new EventBus();
        restauranteController = new FXGLRestauranteController(eventBus);
        restaurante = new Restaurante(10, 1, 1, 100, eventBus);

        restaurante.simular();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
