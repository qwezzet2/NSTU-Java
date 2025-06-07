package com.example.pokeapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

//Класс используется для хранения деталей покемона, он соответствует структуре JSON, который возвращает API, за исключением неизвестных полей.

@JsonIgnoreProperties(ignoreUnknown = true)
public class PokemonDetail {
    private int id;
    private String name;
    private int base_experience;
    private int height;
    private int weight;

    // Конструктор по умолчанию (если нужен пустой объект)
    public PokemonDetail() {}

    // Полный конструктор для удобного создания объекта
    public PokemonDetail(int id, String name, int base_experience, int height, int weight) {
        this.id = id;
        this.name = name;
        this.base_experience = base_experience;
        this.height = height;
        this.weight = weight;
    }

    // Геттеры и сеттеры
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getBase_experience() {
        return base_experience;
    }

    public void setBase_experience(int base_experience) {
        this.base_experience = base_experience;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    // Метод toString для удобства вывода объекта в строковом виде
    @Override
    public String toString() {
        return "PokemonDetail{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", base_experience=" + base_experience +
                ", height=" + height +
                ", weight=" + weight +
                '}';
    }
}