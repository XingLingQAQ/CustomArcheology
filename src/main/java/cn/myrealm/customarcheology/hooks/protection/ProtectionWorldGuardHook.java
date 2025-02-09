package cn.myrealm.customarcheology.hooks.protection;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ProtectionWorldGuardHook extends AbstractProtectionHook {

    public ProtectionWorldGuardHook() {
        super("WorldGuard");
    }

    @Override
    public boolean canBreak(Player player, Location location) {
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        return WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().testBuild(BukkitAdapter.adapt(location), localPlayer, Flags.BLOCK_BREAK)
                || WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(localPlayer, BukkitAdapter.adapt(player.getWorld()));
    }

    @Override
    public boolean canPlace(Player player, Location location) {
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        return WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().testBuild(BukkitAdapter.adapt(location), localPlayer, Flags.BLOCK_PLACE)
                || WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(localPlayer, BukkitAdapter.adapt(player.getWorld()));
    }
}
