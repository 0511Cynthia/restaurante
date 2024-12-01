package com.example.restauranteup;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.example.restauranteup.domain.models.EventBus;
import com.example.restauranteup.infrastructure.views.controller.FXGLRestauranteController;
import javafx.scene.control.Button;
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

        Button startButton = FXGL.getUIFactoryService().newButton("Iniciar Simulación");

        startButton.setTranslateX(20);
        startButton.setTranslateY(20);
        startButton.setOnAction(e -> restaurante.simular());

        startButton.setStyle("-fx-background-color: white; -fx-background-radius: 5px; -fx-font-size: 8px; -fx-text-fill: black; -fx-padding: 5px 10px;");

        FXGL.getGameScene().addUINode(startButton);

        eventBus = new EventBus();
        restauranteController = new FXGLRestauranteController(eventBus);
        restaurante = new Restaurante(10, 1, 1, 100, eventBus);
    }

    public static void main(String[] args) {
        launch(args);
    }
}