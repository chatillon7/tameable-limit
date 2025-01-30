package com.chatillon7.tameablelimit.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.entity.Player;
import java.sql.*;
import java.util.*;
import java.io.*;

// Storage Interface
public interface IStorage {
    void saveTameableData(UUID playerUUID, Map<String, Integer> tameableCounts);
    Map<String, Integer> loadTameableData(UUID playerUUID);
    void initialize();
    void close();
}

