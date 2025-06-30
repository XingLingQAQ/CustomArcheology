package cn.myrealm.customarcheology.listeners.bukkit;


import cn.myrealm.customarcheology.enums.Config;
import cn.myrealm.customarcheology.listeners.BaseListener;
import cn.myrealm.customarcheology.managers.managers.ChunkManager;
import cn.myrealm.customarcheology.mechanics.cores.ArcheologyBlock;
import cn.myrealm.customarcheology.utils.PacketUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.Objects;

import static org.bukkit.Material.BARRIER;

/**
 * @author rzt1020
 */
public class BreakListener extends BaseListener {

    public BreakListener(JavaPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onBreakBlock(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        ChunkManager chunkManager = ChunkManager.getInstance();
        if (chunkManager.isArcheologyBlock(loc)) {
            chunkManager.removeBlock(loc);
        }
    }

    @EventHandler
    public void onBlockBreakByTnt(EntityExplodeEvent event) {
        ChunkManager chunkManager = ChunkManager.getInstance();
        for (Block block : event.blockList()) {
            Location loc = block.getLocation();
            if (chunkManager.isArcheologyBlock(loc)) {
                chunkManager.removeBlock(loc);
            }
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Location loc = Objects.requireNonNull(event.getClickedBlock()).getLocation();
        ChunkManager chunkManager = ChunkManager.getInstance();
        ArcheologyBlock block = chunkManager.getArcheologyBlock(loc);
        if (block != null) {
            loc.getBlock().setType(BARRIER);
        }
    }

    @EventHandler
    public void onLeftClick(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            return;
        }
        if (!event.getPlayer().getGameMode().equals(GameMode.SURVIVAL) && !event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
            return;
        }
        Location loc = Objects.requireNonNull(event.getClickedBlock()).getLocation();
        ChunkManager chunkManager = ChunkManager.getInstance();
        if (chunkManager.isArcheologyBlock(loc)) {
            event.getPlayer().playSound(event.getPlayer(), chunkManager.getArcheologyBlock(loc).getBrushSound(), 1, 1);
            chunkManager.removeBlock(loc);
            if (Config.DISAPPEAR_AFTER_BREAK.asBoolean()) {
                loc.getBlock().setType(Material.AIR);
            }
        }
    }

    @EventHandler
    public void onPistonMove(BlockPistonExtendEvent event) {
        ChunkManager chunkManager = ChunkManager.getInstance();
        event.getBlocks().forEach(block -> {
            if (chunkManager.isArcheologyBlock(block.getLocation())) {
                chunkManager.removeBlock(block.getLocation());
            }
        });
    }
}
