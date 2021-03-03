package net.skycade.skycadechunkcollectors.util;


import net.skycade.api.localization.Localization;

public class Messages {

    public static final Localization.Message USAGE = new Localization.Message("usage", "Usage: /collector give <name> <amount>");
    public static final Localization.Message PLACED = new Localization.Message("placed", "&aYou placed a &bChunk Collector&a!");
    public static final Localization.Message CANNOT_PLACE = new Localization.Message("cannot-place", "&cYou cannot place a chunk collector in this world.");
    public static final Localization.Message SUCCESS = new Localization.Message("success", "&aSuccess!");
    public static final Localization.Message ALREADY_VIEWING = new Localization.Message("already-viewing", "&cAnother player is already viewing this chunk collector.");
    public static final Localization.Message NOT_ENOUGH_MONEY = new Localization.Message("not-enough-money", "&cYou do not have enough money!");
    public static final Localization.Message LINK_SESSION_START = new Localization.Message("link-session-start", "&aStarting to link Chests to this Collector! &bShift-Left Click Chests to link them.");
    public static final Localization.Message CANNOT_LINK = new Localization.Message("cannot-link", "&cCannot link this Chest! The Collector is too far away.");
    public static final Localization.Message LINKED = new Localization.Message("linked", "&aChest linked!");
    public static final Localization.Message LIMIT_EXCEEDED = new Localization.Message("limit-exceeded", "&cYou reached the limit of linkable Chests for this Collector.");
    public static final Localization.Message LINK_SESSION_PAUSE = new Localization.Message("link-session-paused", "&cYour linking session was paused because you broke a block. &bShift-left click the Collector again to restart!");


    public static void init() {
        Localization.getInstance().registerMessages("skycade.chunkcollectors",
                USAGE,
                PLACED,
                CANNOT_PLACE,
                SUCCESS,
                ALREADY_VIEWING,
                NOT_ENOUGH_MONEY,
                LINK_SESSION_START,
                CANNOT_LINK,
                LINKED,
                LIMIT_EXCEEDED,
                LINK_SESSION_PAUSE
        );
    }
}
