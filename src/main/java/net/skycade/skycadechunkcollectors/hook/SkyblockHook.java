package net.skycade.skycadechunkcollectors.hook;

import net.skycade.skycadeskyblock.common.data.Coop;
import net.skycade.skycadeskyblock.common.data.Island;
import net.skycade.skycadeskyblock.common.data.TeamMember;
import net.skycade.skycadeskyblock.server.SkycadeSkyblockServerPlugin;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SkyblockHook extends Hook {

    public SkyblockHook() {
        super("SkycadeSkyblock");
    }

    @Override
    public boolean canBuild(Player player, Block block) {
        Island island = SkycadeSkyblockServerPlugin.getInstance().getIslandListener().getIsland(block.getX(), block.getZ());
        if (island == null) return false;
        UUID uuid = player.getUniqueId();

        return island.getOwner().equals(uuid) || Coop.isCooped(uuid, island) || island.getTeamMembers().stream()
                .filter(i -> i.getExpiry() == null || i.getExpiry() < System.currentTimeMillis())
                .map(TeamMember::getUuid)
                .anyMatch(uuid::equals);
    }
}
