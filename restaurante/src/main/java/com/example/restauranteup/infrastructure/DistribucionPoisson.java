package com.example.restauranteup.infrastructure;

import java.util.Random;

public class DistribucionPoisson {
    private final double lambda;

    public DistribucionPoisson(double lambda) {
        this.lambda = lambda;
    }

    public int generar() {
        Random random = new Random();
        double l = Math.exp(-lambda);
        int k = 0;
        double p = 1.0;

        do {
            k++;
            p *= random.nextDouble();
        } while (p > l);

        return k - 1;
    }
}
