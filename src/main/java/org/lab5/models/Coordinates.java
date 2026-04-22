package org.lab5.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class Coordinates implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Integer x;
    private final float y;

    @JsonCreator
    public Coordinates(
            @JsonProperty("x") Integer x,
            @JsonProperty("y") float y
    ) {
        this.x = x;
        this.y = y;
    }

    public Integer getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    @Override
    public String toString() {
        return "Coordinates{x=" + x + ", y=" + y + "}";
    }
}