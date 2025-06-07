package com.example.pokeapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PokemonAdapter extends RecyclerView.Adapter<PokemonAdapter.PokemonViewHolder> {
    private final List<Pokemon> pokemonList;
    private final OnPokemonClickListener onPokemonClickListener;

    public PokemonAdapter(List<Pokemon> pokemonList, OnPokemonClickListener onPokemonClickListener) {
        this.pokemonList = pokemonList;
        this.onPokemonClickListener = onPokemonClickListener;
    }

    @NonNull
    @Override
    public PokemonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Создание нового ViewHolder
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pokemon, parent, false);
        return new PokemonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PokemonViewHolder holder, int position) {
        // Привязка данных покемона к ViewHolder
        Pokemon pokemon = pokemonList.get(position);
        holder.pokemonNameTextView.setText(pokemon.getName());
        holder.itemView.setOnClickListener(v -> onPokemonClickListener.onPokemonClick(pokemon));
    }

    @Override
    public int getItemCount() {
        // Возвращает количество элементов в списке покемонов
        return pokemonList.size();
    }

    public interface OnPokemonClickListener {
        // Интерфейс для обработки клика на покемона
        void onPokemonClick(Pokemon pokemon);
    }

    static class PokemonViewHolder extends RecyclerView.ViewHolder {
        // ViewHolder для элемента списка покемонов
        TextView pokemonNameTextView;

        public PokemonViewHolder(@NonNull View itemView) {
            super(itemView);
            pokemonNameTextView = itemView.findViewById(R.id.pokemonNameTextView);
        }
    }
}

