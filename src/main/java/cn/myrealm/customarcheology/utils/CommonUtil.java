package cn.myrealm.customarcheology.utils;


import cn.myrealm.customarcheology.CustomArcheology;
import cn.myrealm.customarcheology.enums.Config;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.xikage.mythicmobs.MythicMobs;
import org.bukkit.Color;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.json.JSONObject;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author rzt1020
 */
public class CommonUtil {
    private CommonUtil() {
    }

    public static boolean checkPluginLoad(String pluginName){
        return CustomArcheology.plugin.getServer().getPluginManager().isPluginEnabled(pluginName);
    }

    public static List<Player> getNearbyPlayers(Location location) {
        int visibleDistance = Config.VISIBLE_DISTANCE.asInt();
        Collection<Entity> entities = Objects.requireNonNull(location.getWorld()).getNearbyEntities(location, visibleDistance, visibleDistance, visibleDistance);
        List<Player> players = new ArrayList<>();
        for (Entity entity : entities) {
            if (entity instanceof Player player) {
                players.add(player);
            }
        }
        return players;
    }

    public static boolean checkClass(String className, String methodName) {
        try {
            Class<?> targetClass = Class.forName(className);
            Method[] methods = targetClass.getDeclaredMethods();

            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    return true;
                }
            }

            return false;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean getClass(String className) {
        try {
            Class.forName(className);
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static final Pattern RANGE_PATTERN = Pattern.compile("(-?\\d+)(?:\\s*~\\s*(-?\\d+))?");

    public static Point parseRange(String input) {
        if (Objects.isNull(input)) {
            return null;
        }

        Matcher matcher = RANGE_PATTERN.matcher(input);

        if (matcher.find()) {
            int start = Integer.parseInt(matcher.group(1));
            int end;

            if (matcher.group(2) != null) {
                end = Integer.parseInt(matcher.group(2));
            } else {
                end = start;
            }
            return new Point(start, end);
        }

        return null;
    }

    public static Block getRandomBlock(Chunk chunk, Point range) {
        int x = CustomArcheology.RANDOM.nextInt(16);
        int z = CustomArcheology.RANDOM.nextInt(16);

        int y = range.x + CustomArcheology.RANDOM.nextInt(range.y - range.x + 1);

        World world = chunk.getWorld();

        int actualX = chunk.getX() * 16 + x;
        int actualZ = chunk.getZ() * 16 + z;

        return world.getBlockAt(actualX, y, actualZ);
    }

    public static Block getRandomBlock(World world, BoundingBox box) {
        int minX = (int) Math.floor(box.getMinX());
        int minY = (int) Math.floor(box.getMinY());
        int minZ = (int) Math.floor(box.getMinZ());
        int maxX = (int) Math.floor(box.getMaxX());
        int maxY = (int) Math.floor(box.getMaxY());
        int maxZ = (int) Math.floor(box.getMaxZ());

        int x = minX + CustomArcheology.RANDOM.nextInt(maxX - minX + 1);
        int y = minY + CustomArcheology.RANDOM.nextInt(maxY - minY + 1);
        int z = minZ + CustomArcheology.RANDOM.nextInt(maxZ - minZ + 1);

        return world.getBlockAt(x, y, z);
    }

    public static int getRandomIntFromPoint(Point point) {
        return CustomArcheology.RANDOM.nextInt((point.y - point.x) + 1) + point.x;
    }

    public static Block getGaussianRandomBlock(Chunk chunk, Point range, double gaussianMean, double gaussianStdDev) {
        int yValue = (int) Math.round(gaussianMean + CustomArcheology.RANDOM.nextGaussian() * gaussianStdDev);
        if (yValue < chunk.getWorld().getMinHeight() || yValue > chunk.getWorld().getMaxHeight()  || yValue < range.x || yValue > range.y) {
            return getGaussianRandomBlock(chunk, range, gaussianMean, gaussianStdDev);
        }
        Block newBlock = getRandomBlock(chunk, range);
        return newBlock.getWorld().getBlockAt(newBlock.getX(), yValue, newBlock.getZ());
    }

    public static void summonMythicMobs(Location location, String mobID, int level) {
        try {
            MythicMob mob = MythicBukkit.inst().getMobManager().getMythicMob(mobID).orElse(null);
            if (mob != null) {
                mob.spawn(BukkitAdapter.adapt(location), level);
            }
        }
        catch (NoClassDefFoundError ep) {
            io.lumine.xikage.mythicmobs.mobs.MythicMob mob = MythicMobs.inst().getMobManager().getMythicMob(mobID);
            if (mob != null) {
                mob.spawn(io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter.adapt(location), level);
            }
        }
    }


    public static String modifyString(String text, String... args) {
        for (int i = 0 ; i < args.length ; i += 2) {
            String var = "{" + args[i] + "}";
            if (args[i + 1] == null) {
                text = text.replace(var, "");
            }
            else {
                text = text.replace(var, args[i + 1]);
            }
        }
        return text;
    }

    public static boolean getMajorVersion(int version) {
        return CustomArcheology.majorVersion >= version;
    }

    public static boolean getMinorVersion(int majorVersion, int minorVersion) {
        return CustomArcheology.majorVersion > majorVersion || (CustomArcheology.majorVersion == majorVersion &&
                CustomArcheology.miniorVersion >= minorVersion);
    }

    public static NamespacedKey parseNamespacedKey(String key) {
        String[] keySplit = key.split(":");
        if (keySplit.length == 1) {
            return NamespacedKey.minecraft(key.toLowerCase());
        }
        return NamespacedKey.fromString(key);
    }

    public static org.bukkit.Color parseColor(String color) {
        String[] keySplit = color.replace(" ", "").split(",");
        if (keySplit.length == 3) {
            return org.bukkit.Color.fromRGB(Integer.parseInt(keySplit[0]), Integer.parseInt(keySplit[1]), Integer.parseInt(keySplit[2]));
        }
        return Color.fromRGB(Integer.parseInt(color));
    }

    public static JSONObject fetchJson(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return new JSONObject(response.toString());
        }
    }
}
