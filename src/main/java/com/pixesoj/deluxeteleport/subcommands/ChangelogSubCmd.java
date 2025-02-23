package com.pixesoj.deluxeteleport.subcommands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pixesoj.deluxeteleport.DeluxeTeleport;
import com.pixesoj.deluxeteleport.managers.MessagesManager;
import com.pixesoj.deluxeteleport.managers.database.DataDatabase;
import com.pixesoj.deluxeteleport.managers.filesmanager.MessagesFileManager;
import com.pixesoj.deluxeteleport.utils.PlayerUtils;
import com.pixesoj.deluxeteleport.utils.SubCommand;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Pattern;

public class ChangelogSubCmd implements SubCommand {

    private final DeluxeTeleport plugin;

    public ChangelogSubCmd(DeluxeTeleport plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        mainSubCommand(sender, args, false);

        return true;
    }

    public void mainSubCommand(CommandSender sender, String[] args, boolean isNotify) {
        if (!PlayerUtils.hasPermission(plugin, sender, plugin.getMainPermissionsManager().getChangelog(), plugin.getMainPermissionsManager().isChangelogDefault(), true)) {
            return;
        }

        MessagesFileManager msg = plugin.getMainMessagesManager();
        MessagesManager m = new MessagesManager(msg.getPrefix(), plugin);
        String changelogUrl;
        String stringVersion;
        int pageNumber = 1;
        if (args.length == 0) {
            changelogUrl = "https://api.github.com/repos/MiniPixesoj/DeluxeTeleport/releases/tags/" + plugin.version;
            stringVersion = plugin.version;
        } else {
            if (!Pattern.matches("\\d+\\.\\d+\\.\\d+", args[0])) {
                m.sendMessage(sender, msg.getUpdateChangelogInvalidFormat(), true);
                return;
            }
            changelogUrl = "https://api.github.com/repos/MiniPixesoj/DeluxeTeleport/releases/tags/" + args[0];
            stringVersion = args[0];

            if (args.length > 1) {
                if ((sender instanceof Player) && args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")){
                    assert sender instanceof Player;
                    Player player = (Player) sender;
                    DataDatabase dataDatabase = new DataDatabase(plugin);
                    dataDatabase.saveData(player.getName(), args[0], Boolean.parseBoolean(args[1]));
                    if (args[1].equalsIgnoreCase("true")) {
                        m.sendMessage(sender, msg.getUpdateChangelogEnabledNotify().replace("%version%", stringVersion), true);
                    } else {
                        m.sendMessage(sender, msg.getUpdateChangelogDisabledNotify().replace("%version%", stringVersion), true);
                    }
                    return;
                }

                try {
                    pageNumber = Integer.parseInt(args[1]);
                    if (pageNumber < 1) {
                        pageNumber = 1;
                    }
                } catch (NumberFormatException e) {
                    m.sendMessage(sender, msg.getUpdateChangelogInvalidPage(), true);
                }
            }
        }

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(changelogUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            int responseCode = connection.getResponseCode();
            if (responseCode == 404) {
                m.sendMessage(sender, msg.getUpdateChangelogNoChangelogsFound().replace("%version%", stringVersion), true);
                return;
            } else if (responseCode != 200) {
                m.sendMessage(sender, msg.getUpdateChangelogErrorInfo().replace("%version%", stringVersion), true);
                return;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            JsonObject jsonResponse = new JsonParser().parse(response.toString()).getAsJsonObject();
            String changelogBody = jsonResponse.get("body").getAsString();

            String version = jsonResponse.get("name").getAsString();
            m.sendMessage(sender, msg.getUpdateChangelogRegister().replace("%version%", stringVersion), true);

            String[] lines = changelogBody.split("\\r?\\n");
            int linesPerPage = 10;
            int totalPages = (int) Math.ceil((double) lines.length / linesPerPage);

            int startLine = (pageNumber - 1) * linesPerPage;
            int endLine = Math.min(startLine + linesPerPage, lines.length);

            for (int i = startLine; i < endLine; i++) {
                String line = lines[i];
                String formattedLine = line.replaceAll("`([^`]*)`", "&a$1&7")
                        .replace("**", "")
                        .replace("[+]", "&a[+]&7")
                        .replace("[-]", "&4[-]&7")
                        .replace("[/]", "&e[/]&7");

                m.sendMessage(sender, "  &7" + formattedLine, false);
            }

            m.sendMessage(sender, " ", false);

            TextComponent seeFull = new TextComponent();
            seeFull.setText(MessagesManager.getColoredMessage(msg.getUpdateChangelogSeeFull()));
            seeFull.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, jsonResponse.get("html_url").getAsString()));
            seeFull.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(MessagesManager.getColoredMessage(msg.getUpdateChangelogSeeFullDescription())).create()));

            TextComponent ok = new TextComponent();
            ok.setText(MessagesManager.getColoredMessage(msg.getUpdateChangelogOk()));
            ok.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/deluxeteleport changelog " + version + " false"));
            ok.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(MessagesManager.getColoredMessage(msg.getUpdateChangelogOkDescription())).create()));

            if (totalPages > 1) {
                if (sender instanceof Player) {
                    m.sendMessage(sender, MessagesManager.getCenteredMessage(msg.getUpdateChangelogPages().replace("%page_number%", String.valueOf(pageNumber))
                            .replace("%total_pages%", String.valueOf(totalPages))), false);
                    Player player = (Player) sender;
                    TextComponent nextPage = new TextComponent();
                    if ((pageNumber + 1) <= totalPages) {
                        nextPage.setText(MessagesManager.getColoredMessage(msg.getUpdateChangelogNext()));
                        nextPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/deluxeteleport changelog " + version + " " + (pageNumber + 1)));
                        nextPage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(MessagesManager.getColoredMessage(msg.getUpdateChangelogNextDescription())).create()));
                    } else {
                        String nextStripColor = ChatColor.stripColor(msg.getUpdateChangelogNext());
                        nextPage.setText(MessagesManager.getColoredMessage("&8&l" + nextStripColor));
                    }

                    TextComponent previusPage = new TextComponent();
                    if ((pageNumber - 1) >= 1) {
                        previusPage.setText(MessagesManager.getColoredMessage(msg.getUpdateChangelogPrevius()));
                        previusPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/deluxeteleport changelog " + version + " " + (pageNumber - 1)));
                        previusPage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(MessagesManager.getColoredMessage(msg.getUpdateChangelogPreviusDescription())).create()));
                    } else {
                        String previusStripColor = ChatColor.stripColor(msg.getUpdateChangelogPrevius());
                        previusPage.setText(MessagesManager.getColoredMessage("&8&l" + previusStripColor));
                    }

                    TextComponent combined = new TextComponent(" ");
                    combined.addExtra(previusPage);
                    if (isNotify) {
                        combined.addExtra(MessagesManager.getColoredMessage("     "));
                        combined.addExtra(ok);
                    }
                    combined.addExtra(MessagesManager.getColoredMessage("     "));
                    combined.addExtra(seeFull);
                    combined.addExtra(MessagesManager.getColoredMessage("     "));
                    combined.addExtra(nextPage);
                    m.sendComponent(player, m.getCenteredComponent(combined));
                } else {
                    m.sendMessage(sender, MessagesManager.getCenteredMessage(msg.getUpdateChangelogPagesConsole().replace("%page_number%", String.valueOf(pageNumber))
                            .replace("%total_pages%", String.valueOf(totalPages))), false);
                }
            } else {
                if ((sender instanceof Player) && isNotify){
                    TextComponent combined = new TextComponent();
                    combined.addExtra(ok);
                    combined.addExtra(MessagesManager.getColoredMessage("     "));
                    combined.addExtra(seeFull);
                    m.sendComponent((Player) sender, m.getCenteredComponent(combined));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            m.sendMessage(sender, msg.getUpdateChangelogExeption(), true);
        }
    }
}
