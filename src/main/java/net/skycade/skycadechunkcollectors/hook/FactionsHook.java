package net.skycade.skycadechunkcollectors.hook;

import com.massivecraft.factions.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class FactionsHook extends Hook {
    public FactionsHook() {
        super("Factions");
    }

    @Override
    public boolean canBuild(Player player, Block block) {
        FPlayer fp = FPlayers.getInstance().getByPlayer(player);

        Faction faction = Board.getInstance().getFactionAt(new FLocation(block));

        if (faction.isWarZone())
            return false;

        if (faction.isWilderness())
            return true;

        return faction.getFPlayers().contains(fp);
    }
}
