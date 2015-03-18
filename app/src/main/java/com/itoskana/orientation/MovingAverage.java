package com.itoskana.orientation;

import java.util.LinkedList;
import java.util.Queue;


public class MovingAverage {
    private final Queue<Double> window = new LinkedList<Double> ();
    private final int window_size;
    private double sum;

    public MovingAverage(int window_size) {
        if(window_size <= 0)
            throw new RuntimeException("window_size must be positive");

        this.window_size = window_size;
    }

    public void input(double number) {
        sum += number;
        window.add(number);
        if(window.size() > window_size)
            sum -= window.remove();
    }

    public double getAverage() {
        if(window.isEmpty() == true)
            return 0;   // Technically the average is undefined

        return (sum / window.size());
    }
}
