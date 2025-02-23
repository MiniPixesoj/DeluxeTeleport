package com.pixesoj.deluxeteleport.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.yggdrasil.response.Response;
import com.pixesoj.deluxeteleport.model.internal.UpdateCheckResult;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class UpdateCheckManager {
    private final String version;
    private final List<String> versions = new ArrayList<>();

    public UpdateCheckManager(String version) {
        this.version = version;
    }

    public UpdateCheckResult check() {
        try {
            URL url = new URL("https://api.github.com/repos/MiniPixesoj/DeluxeTeleport/releases/latest");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == 200) {
                try (InputStream inputStream = connection.getInputStream();
                     InputStreamReader reader = new InputStreamReader(inputStream)) {

                    JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                    String latestVersion = json.get("name").getAsString();
                    if (latestVersion.contains("-")) latestVersion = latestVersion.split("-")[0];
                    String downloadUrl = null;

                    if (json.has("assets") && json.getAsJsonArray("assets").size() > 0) {
                        JsonArray assets = json.getAsJsonArray("assets");
                        downloadUrl = assets.get(0).getAsJsonObject().get("browser_download_url").getAsString();
                    }

                    if (!latestVersion.isEmpty() && isNewerVersion(version, latestVersion)) {
                        return UpdateCheckResult.updateAvailable(latestVersion, downloadUrl);
                    } else {
                        return UpdateCheckResult.noUpdate(latestVersion);
                    }
                }
            } else {
                throw new IllegalStateException("Response code was " + connection.getResponseCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return UpdateCheckResult.error();
        }
    }

    public List<String> getAvailableVersions() {
        String urlString = "https://api.github.com/repos/MiniPixesoj/DeluxeTeleport/releases";

        try {
            URL url = new URL(urlString);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                JsonObject json = new JsonParser().parse(response.toString()).getAsJsonObject();
                JsonArray releases = json.getAsJsonArray("releases");

                for (int i = 0; i < releases.size(); i++) {
                    JsonObject release = releases.get(i).getAsJsonObject();
                    String versionName = release.get("name").getAsString();
                    versions.add(versionName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return versions;
    }

    private boolean isNewerVersion(String currentVersion, String latestVersion) {
        String[] currentParts = currentVersion.split("\\.");
        String[] latestParts = latestVersion.split("\\.");

        int length = Math.max(currentParts.length, latestParts.length);

        for (int i = 0; i < length; i++) {
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

            if (latestPart > currentPart) {
                return true;
            } else if (latestPart < currentPart) {
                return false;
            }
        }
        return false;
    }
}
