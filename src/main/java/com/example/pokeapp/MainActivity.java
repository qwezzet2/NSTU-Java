package com.example.pokeapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String POKEMON_API_URL = "https://pokeapi.co/api/v2/pokemon/";

    private RecyclerView recyclerView;
    private PokemonAdapter adapter;
    private List<Pokemon> pokemonList = new ArrayList<>();

    private OkHttpClient client = new OkHttpClient();
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация RecyclerView и адаптера
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PokemonAdapter(pokemonList, this::onPokemonClick);
        recyclerView.setAdapter(adapter);

        // Загрузка данных покемонов
        fetchPokemonData(POKEMON_API_URL);
    }

    private void fetchPokemonData(String url) {
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Обработка ошибки загрузки данных
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    PokemonResponse pokemonResponse = objectMapper.readValue(jsonData, PokemonResponse.class);

                    // Обновление списка покемонов и адаптера
                    runOnUiThread(() -> {
                        pokemonList.addAll(pokemonResponse.getResults());
                        adapter.notifyDataSetChanged();
                    });

                    // Рекурсивная загрузка следующей страницы данных
                    if (pokemonResponse.getNext() != null) {
                        fetchPokemonData(pokemonResponse.getNext());
                    }
                }
            }
        });
    }

    private void onPokemonClick(Pokemon pokemon) {
        // Запуск PokemonDetailActivity при клике на покемона
        Intent intent = new Intent(MainActivity.this, PokemonDetailActivity.class);
        intent.putExtra("POKEMON_URL", pokemon.getUrl());
        startActivity(intent);
    }
}