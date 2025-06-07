package com.example.pokeapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

//Класс для парсинга ответа от API, возвращающего список покемонов.

@JsonIgnoreProperties(ignoreUnknown = true)
public class PokemonResponse {
    @JsonProperty("results")
    private List<Pokemon> results;
    private int count;
    private String next;

    //Конструктор по умолчанию

    public PokemonResponse() {}

    //
    // Полный конструктор для удобства создания объекта
    // @param results Список покемонов
    // @param count Общее количество покемонов
    // @param next URL для следующей страницы

    public PokemonResponse(List<Pokemon> results, int count, String next) {
        this.results = results;
        this.count = count;
        this.next = next;
    }

    // Получить список покемонов.
    // @return список покемонов

    public List<Pokemon> getResults() {
        return results;
    }

    // Установить список покемонов.
    // @param results список покемонов

    public void setResults(List<Pokemon> results) {
        this.results = results;
    }

    //Получить общее количество покемонов.
    // @return общее количество покемонов

    public int getCount() {
        return count;
    }

    //Установить общее количество покемонов.
    // @param count общее количество покемонов

    public void setCount(int count) {
        this.count = count;
    }

    //Получить URL для следующей страницы.
    // @return URL для следующей страницы

    public String getNext() {
        return next;
    }

    // Установить URL для следующей страницы.
    // @param next URL для следующей страницы

    public void setNext(String next) {
        this.next = next;
    }

    //Преобразовать объект в строковое представление.
    // @return строковое представление объекта

    @Override
    public String toString() {
        return "PokemonResponse{" +
                "results=" + results +
                ", count=" + count +
                ", next='" + next + '\'' +
                '}';
    }
}