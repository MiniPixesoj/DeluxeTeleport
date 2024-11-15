package com.pixesoj.deluxeteleport.managers;

import com.pixesoj.deluxeteleport.DeluxeTeleport;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class LocationsManager {
    private DeluxeTeleport plugin;
    private String fileName = "locations.yml";
    private FileConfiguration fileConfiguration;
    private File file;

    public LocationsManager(DeluxeTeleport plugin) {
        this.plugin = plugin;
    }

    public void loadLocationsFile() {
        file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource(fileName, false);
        }
        fileConfiguration = YamlConfiguration.loadConfiguration(file);
    }

    public void saveLocationsFile() {
        if (fileConfiguration != null && file != null) {
            try {
                fileConfiguration.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public FileConfiguration getLocationsFile() {
        if (fileConfiguration == null) {
            loadLocationsFile();
        }
        return fileConfiguration;
    }

    public void reloadLocationsFile() {
        if (file != null) {
            fileConfiguration = YamlConfiguration.loadConfiguration(file);
        } else {
            loadLocationsFile();
        }
    }
}