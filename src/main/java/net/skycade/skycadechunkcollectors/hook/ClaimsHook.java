package net.skycade.skycadechunkcollectors.hook;

import net.skycade.skycadeclaims.data.ClaimedRegion;
import net.skycade.skycadeclaims.data.Claims;
import net.skycade.skycadeclaims.listener.ProtectionListener;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class ClaimsHook extends Hook {
    public ClaimsHook() {
        super("SkycadeClaims");
    }

    @Override
    public boolean canBuild(Player player, Block block) {
        ClaimedRegion region = Claims.getInstance().getApplicableRegion(block);

        return ProtectionListener.canBuild(player.getUniqueId(), region, block);
    }
}
