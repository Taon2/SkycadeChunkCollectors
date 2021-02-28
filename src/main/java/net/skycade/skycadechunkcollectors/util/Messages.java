package net.skycade.skycadechunkcollectors.util;


import net.skycade.api.localization.Localization;

public class Messages {

    public static final Localization.Message USAGE = new Localization.Message("usage", "Usage: /collector give <name> <amount>");
    public static final Localization.Message PLACED = new Localization.Message("placed", "&aYou placed a &bChunk Collector&a!");
    public static final Localization.Message CANNOT_PLACE = new Localization.Message("cannot-place", "&cYou cannot place a chunk collector in this world.");
    public static final Localization.Message SUCCESS = new Localization.Message("success", "&aSuccess!");
    public static final Localization.Message ALREADY_VIEWING = new Localization.Message("already-viewing", "&cAnother player is already viewing this chunk collector.");
    public static final Localization.Message NOT_ENOUGH_MONEY = new Localization.Message("not-enough-money", "&cYou do not have enough money!");

    public static void init() {
        Localization.getInstance().registerMessages("skycade.chunkcollectors",
                USAGE,
                PLACED,
                CANNOT_PLACE,
                SUCCESS,
                ALREADY_VIEWING,
                NOT_ENOUGH_MONEY
        );
    }
}
