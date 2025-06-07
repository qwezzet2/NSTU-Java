package com.example.pokeapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

//Активити, которая отображает детали конкретного покемона.
//Получает URL покемона из интента и вызывает метод fetchPokemonDetail для получения данных.
//Использует OkHttp для выполнения сетевого запроса и Jackson ObjectMapper для парсинга JSON.

public class PokemonDetailActivity extends AppCompatActivity {
    private TextView detailTextView;
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokemon_detail);

        // Инициализация TextView для отображения деталей покемона
        detailTextView = findViewById(R.id.detailTextView);

        // Инициализация кнопки "Назад"
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            finish(); // Закрывает текущую активность и возвращает к MainActivity
        });

        // Получение URL покемона из интента
        String pokemonUrl = getIntent().getStringExtra("POKEMON_URL");

        // Проверка наличия URL и запрос деталей покемона
        if (pokemonUrl != null) {
            fetchPokemonDetail(pokemonUrl);
        } else {
            showNoPokemonUrlError();
        }
    }

    /**
     * Запрос деталей покемона по указанному URL.
     * @param url URL покемона.
     */
    private void fetchPokemonDetail(String url) {
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                showNetworkError();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    PokemonDetail pokemonDetail = objectMapper.readValue(jsonData, PokemonDetail.class);
                    updatePokemonDetails(pokemonDetail);
                } else {
                    showDataLoadError();
                }
            }
        });
    }

    /**
     * Обновляет текстовое поле с деталями покемона.
     * @param pokemonDetail объект с деталями покемона.
     */
    private void updatePokemonDetails(PokemonDetail pokemonDetail) {
        runOnUiThread(() -> {
            String detailText = "ID: " + pokemonDetail.getId() + "\n" +
                    "Имя: " + pokemonDetail.getName() + "\n" +
                    "Базовый опыт: " + pokemonDetail.getBase_experience() + "\n" +
                    "Высота: " + pokemonDetail.getHeight() + "\n" +
                    "Вес: " + pokemonDetail.getWeight();
            detailTextView.setText(detailText);
        });
    }

    //Отображает сообщение об ошибке загрузки данных.

    private void showDataLoadError() {
        runOnUiThread(() ->
                Toast.makeText(PokemonDetailActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show()
        );
    }

    //Отображает сообщение об ошибке сети.

    private void showNetworkError() {
        runOnUiThread(() ->
                Toast.makeText(PokemonDetailActivity.this, "Failed to load necessary data", Toast.LENGTH_SHORT).show()
        );
    }

    //Отображает сообщение об отсутствии URL покемона.

    private void showNoPokemonUrlError() {
        detailTextView.setText("No Pokemon URL!");
    }
}