package com.chatillon7.tameablelimit.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// JSON Implementation
public class JsonStorage implements IStorage {
    private final File dataFolder;
    private final Gson gson;

    public JsonStorage(File dataFolder) {
        this.dataFolder = new File(dataFolder, "data");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        if (!this.dataFolder.exists()) {
            this.dataFolder.mkdirs();
        }
    }

    @Override
    public void initialize() {
        // JSON doesn't need initialization
    }

    @Override
    public void saveTameableData(UUID playerUUID, Map<String, Integer> tameableCounts) {
        File playerFile = new File(dataFolder, playerUUID.toString() + ".json");
        try (FileWriter writer = new FileWriter(playerFile)) {
            gson.toJson(tameableCounts, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	@Override
	public Map<String, Integer> loadTameableData(UUID playerUUID) {
		File playerFile = new File(dataFolder, playerUUID.toString() + ".json");
		if (!playerFile.exists()) {
			return new HashMap<>();
		}
    
		try (FileReader reader = new FileReader(playerFile)) {
			// Gson'dan gelen Map<String, Double>'yi Map<String, Integer>'a dönüştür
			Map<String, Double> rawData = gson.fromJson(reader, new HashMap<String, Double>().getClass());
			Map<String, Integer> data = new HashMap<>();
        
			for (Map.Entry<String, Double> entry : rawData.entrySet()) {
				data.put(entry.getKey(), entry.getValue().intValue()); // Double'ı Integer'a dönüştür
			}
        
			return data;
		} catch (IOException e) {
			e.printStackTrace();
			return new HashMap<>();
		}
	}

    @Override
    public void close() {
        // JSON doesn't need closing
    }
}
