package com.battleship.persistence;

import com.battleship.model.GameState;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * handles serialization/deserialization of full game state to/from json,
 * per the save format in the spec (version, timestamp, boards, turnhistory...).
 */
public class SaveGameService {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /** serializes the current game state and writes it to [timestamp]_battleship_save.json */
    public Path save(GameSaveDTO saveData, Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
        String timestamp = saveData.getTimestamp() != null && !saveData.getTimestamp().isBlank()
                ? saveData.getTimestamp()
                : Instant.now().toString();
        String safeTimestamp = timestamp.replace(":", "-");
        Path file = directory.resolve(safeTimestamp + "_battleship_save.json");
        String json = gson.toJson(saveData);
        Files.writeString(file, json);
        return file;
    }

    /** parses a save file back into dtos for gamecontroller reconstruction. */
    public GameSaveDTO load(Path file) throws IOException {
        String json = Files.readString(file);
        return gson.fromJson(json, GameSaveDTO.class);
    }
}
