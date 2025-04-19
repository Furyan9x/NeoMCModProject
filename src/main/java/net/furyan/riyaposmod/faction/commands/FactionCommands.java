package net.furyan.riyaposmod.faction.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.furyan.riyaposmod.RiyaposMod;
import net.furyan.riyaposmod.faction.Faction;
import net.furyan.riyaposmod.faction.FactionRegistry;
import net.furyan.riyaposmod.faction.capability.PlayerFactionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Optional;

/**
 * Registers and handles faction-related commands.
 */
public class FactionCommands {

    /**
     * Registers all faction commands.
     *
     * @param dispatcher The command dispatcher
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Main faction command
        LiteralArgumentBuilder<CommandSourceStack> factionCommand = Commands.literal("faction")
            .then(Commands.literal("join")
                .requires(source -> source.hasPermission(0)) // All players can use this
                .then(Commands.argument("faction", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        FactionRegistry.getAllFactions().forEach(faction -> 
                            builder.suggest(faction.getId())
                        );
                        return builder.buildFuture();
                    })
                    .executes(FactionCommands::joinFaction)
                )
            )
            .then(Commands.literal("leave")
                .requires(source -> source.hasPermission(0)) // All players can use this
                .executes(FactionCommands::leaveFaction)
            )
            .then(Commands.literal("info")
                .requires(source -> source.hasPermission(0)) // All players can use this
                .executes(FactionCommands::getFactionInfo)
                .then(Commands.argument("faction", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        FactionRegistry.getAllFactions().forEach(faction -> 
                            builder.suggest(faction.getId())
                        );
                        return builder.buildFuture();
                    })
                    .executes(FactionCommands::getFactionInfoForFaction)
                )
            )
            .then(Commands.literal("list")
                .requires(source -> source.hasPermission(0)) // All players can use this
                .executes(FactionCommands::listFactions)
            )
            .then(Commands.literal("reputation")
                .requires(source -> source.hasPermission(0)) // All players can use this
                .then(Commands.argument("faction", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        FactionRegistry.getAllFactions().forEach(faction -> 
                            builder.suggest(faction.getId())
                        );
                        return builder.buildFuture();
                    })
                    .executes(FactionCommands::getReputation)
                )
            );

        // Admin commands
        LiteralArgumentBuilder<CommandSourceStack> factionAdminCommand = Commands.literal("factionadmin")
            .requires(source -> source.hasPermission(2)) // Only ops can use this
            .then(Commands.literal("set")
                .then(Commands.argument("player", EntityArgument.players())
                    .then(Commands.argument("faction", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            FactionRegistry.getAllFactions().forEach(faction -> 
                                builder.suggest(faction.getId())
                            );
                            return builder.buildFuture();
                        })
                        .executes(FactionCommands::setPlayerFaction)
                    )
                )
            )
            .then(Commands.literal("clear")
                .then(Commands.argument("player", EntityArgument.players())
                    .executes(FactionCommands::clearPlayerFaction)
                )
            )
            .then(Commands.literal("reputation")
                .then(Commands.literal("set")
                    .then(Commands.argument("player", EntityArgument.players())
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                FactionRegistry.getAllFactions().forEach(faction -> 
                                    builder.suggest(faction.getId())
                                );
                                return builder.buildFuture();
                            })
                            .then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(FactionCommands::setPlayerReputation)
                            )
                        )
                    )
                )
                .then(Commands.literal("modify")
                    .then(Commands.argument("player", EntityArgument.players())
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                FactionRegistry.getAllFactions().forEach(faction -> 
                                    builder.suggest(faction.getId())
                                );
                                return builder.buildFuture();
                            })
                            .then(Commands.argument("amount", IntegerArgumentType.integer())
                                .executes(FactionCommands::modifyPlayerReputation)
                            )
                        )
                    )
                )
            );

        dispatcher.register(factionCommand);
        dispatcher.register(factionAdminCommand);
    }

    /**
     * Command handler for joining a faction.
     *
     * @param context The command context
     * @return The command result
     * @throws CommandSyntaxException If the command syntax is invalid
     */
    private static int joinFaction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String factionId = StringArgumentType.getString(context, "faction");

        if (!FactionRegistry.factionExists(factionId)) {
            context.getSource().sendFailure(Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.faction_not_found", factionId));
            return 0;
        }

        // Check if player already has a faction
        Optional<String> currentFaction = PlayerFactionProvider.getPlayerFactionId(player);
        if (currentFaction.isPresent()) {
            context.getSource().sendFailure(Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.already_in_faction", 
                FactionRegistry.getFaction(currentFaction.get()).map(Faction::getName).orElse(Component.literal(currentFaction.get()))));
            return 0;
        }

        // Join the faction
        if (PlayerFactionProvider.setPlayerFaction(player, factionId)) {
            Faction faction = FactionRegistry.getFaction(factionId).orElseThrow();
            context.getSource().sendSuccess(() -> 
                Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.joined_faction", faction.getName()), 
                false);
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.join_failed"));
            return 0;
        }
    }

    /**
     * Command handler for leaving a faction.
     *
     * @param context The command context
     * @return The command result
     * @throws CommandSyntaxException If the command syntax is invalid
     */
    private static int leaveFaction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        // Check if player has a faction
        Optional<String> currentFaction = PlayerFactionProvider.getPlayerFactionId(player);
        if (currentFaction.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.not_in_faction"));
            return 0;
        }

        // Leave the faction
        PlayerFactionProvider.clearPlayerFaction(player);
        context.getSource().sendSuccess(() -> 
            Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.left_faction", 
                FactionRegistry.getFaction(currentFaction.get()).map(Faction::getName).orElse(Component.literal(currentFaction.get()))), 
            false);
        return 1;
    }

    /**
     * Command handler for getting faction info.
     *
     * @param context The command context
     * @return The command result
     * @throws CommandSyntaxException If the command syntax is invalid
     */
    private static int getFactionInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();

        // Check if player has a faction
        Optional<String> currentFaction = PlayerFactionProvider.getPlayerFactionId(player);
        if (currentFaction.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.not_in_faction"));
            return 0;
        }

        // Get faction info
        Optional<Faction> faction = FactionRegistry.getFaction(currentFaction.get());
        if (faction.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.faction_not_found", currentFaction.get()));
            return 0;
        }

        sendFactionInfo(context.getSource(), faction.get(), player);
        return 1;
    }

    /**
     * Command handler for getting info about a specific faction.
     *
     * @param context The command context
     * @return The command result
     * @throws CommandSyntaxException If the command syntax is invalid
     */
    private static int getFactionInfoForFaction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String factionId = StringArgumentType.getString(context, "faction");

        if (!FactionRegistry.factionExists(factionId)) {
            context.getSource().sendFailure(Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.faction_not_found", factionId));
            return 0;
        }

        // Get faction info
        Optional<Faction> faction = FactionRegistry.getFaction(factionId);
        if (faction.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.faction_not_found", factionId));
            return 0;
        }

        sendFactionInfo(context.getSource(), faction.get(), player);
        return 1;
    }

    /**
     * Sends faction info to a command source.
     *
     * @param source The command source
     * @param faction The faction
     * @param player The player
     */
    private static void sendFactionInfo(CommandSourceStack source, Faction faction, ServerPlayer player) {
        source.sendSuccess(() -> Component.literal("=== ").append(faction.getName()).append(" ==="), false);
        source.sendSuccess(faction::getDescription, false);

        // Send perks
        if (!faction.getPerks().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.perks"), false);
            for (Faction.FactionPerk perk : faction.getPerks()) {
                source.sendSuccess(() -> Component.literal("- ").append(perk.getName()).append(": ").append(perk.getDescription()), false);
            }
        }

        // Send hindrances
        if (!faction.getHindrances().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.hindrances"), false);
            for (Faction.FactionPerk hindrance : faction.getHindrances()) {
                source.sendSuccess(() -> Component.literal("- ").append(hindrance.getName()).append(": ").append(hindrance.getDescription()), false);
            }
        }

        // Send reputation
        int reputation = PlayerFactionProvider.getReputation(player, faction.getId());
        Component reputationMessage = PlayerFactionProvider.getPlayerFaction(player).getReputationMessage(faction.getId());
        source.sendSuccess(() -> Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.reputation", 
            reputation, reputationMessage), false);
    }

    /**
     * Command handler for listing all factions.
     *
     * @param context The command context
     * @return The command result
     */
    private static int listFactions(CommandContext<CommandSourceStack> context) {
        Collection<Faction> factions = FactionRegistry.getAllFactions();

        context.getSource().sendSuccess(() -> Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.list_header"), false);
        for (Faction faction : factions) {
            context.getSource().sendSuccess(() -> Component.literal("- ").append(faction.getName()), false);
        }

        return 1;
    }

    /**
     * Command handler for getting reputation with a faction.
     *
     * @param context The command context
     * @return The command result
     * @throws CommandSyntaxException If the command syntax is invalid
     */
    private static int getReputation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String factionId = StringArgumentType.getString(context, "faction");

        if (!FactionRegistry.factionExists(factionId)) {
            context.getSource().sendFailure(Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.faction_not_found", factionId));
            return 0;
        }

        // Get faction
        Optional<Faction> faction = FactionRegistry.getFaction(factionId);
        if (faction.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.faction_not_found", factionId));
            return 0;
        }

        // Get reputation
        int reputation = PlayerFactionProvider.getReputation(player, factionId);
        Component reputationMessage = PlayerFactionProvider.getPlayerFaction(player).getReputationMessage(factionId);

        context.getSource().sendSuccess(() -> Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.reputation_with", 
            faction.get().getName(), reputation, reputationMessage), false);

        return 1;
    }

    /**
     * Command handler for setting a player's faction.
     *
     * @param context The command context
     * @return The command result
     * @throws CommandSyntaxException If the command syntax is invalid
     */
    private static int setPlayerFaction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
        String factionId = StringArgumentType.getString(context, "faction");

        if (!FactionRegistry.factionExists(factionId)) {
            context.getSource().sendFailure(Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.faction_not_found", factionId));
            return 0;
        }

        // Get faction
        Optional<Faction> faction = FactionRegistry.getFaction(factionId);
        if (faction.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.faction_not_found", factionId));
            return 0;
        }

        // Set faction for all players
        int count = 0;
        for (ServerPlayer player : players) {
            if (PlayerFactionProvider.setPlayerFaction(player, factionId)) {
                count++;
            }
        }

        if (count > 0) {
            final int finalCount = count;
            final Faction finalFaction = faction.get();
            context.getSource().sendSuccess(() -> Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.set_faction", 
                finalCount, finalFaction.getName()), true);
            return count;
        } else {
            context.getSource().sendFailure(Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.set_faction_failed"));
            return 0;
        }
    }

    /**
     * Command handler for clearing a player's faction.
     *
     * @param context The command context
     * @return The command result
     * @throws CommandSyntaxException If the command syntax is invalid
     */
    private static int clearPlayerFaction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");

        // Clear faction for all players
        int count = 0;
        for (ServerPlayer player : players) {
            Optional<String> currentFaction = PlayerFactionProvider.getPlayerFactionId(player);
            if (currentFaction.isPresent()) {
                PlayerFactionProvider.clearPlayerFaction(player);
                count++;
            }
        }

        if (count > 0) {
            final int finalCount = count;
            context.getSource().sendSuccess(() -> Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.clear_faction", finalCount), true);
            return count;
        } else {
            context.getSource().sendFailure(Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.clear_faction_failed"));
            return 0;
        }
    }

    /**
     * Command handler for setting a player's reputation with a faction.
     *
     * @param context The command context
     * @return The command result
     * @throws CommandSyntaxException If the command syntax is invalid
     */
    private static int setPlayerReputation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
        String factionId = StringArgumentType.getString(context, "faction");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        if (!FactionRegistry.factionExists(factionId)) {
            context.getSource().sendFailure(Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.faction_not_found", factionId));
            return 0;
        }

        // Get faction
        Optional<Faction> faction = FactionRegistry.getFaction(factionId);
        if (faction.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.faction_not_found", factionId));
            return 0;
        }

        // Set reputation for all players
        int count = 0;
        for (ServerPlayer player : players) {
            PlayerFactionProvider.setReputation(player, factionId, amount);
            count++;
        }

        if (count > 0) {
            final int finalCount = count;
            final int finalAmount = amount;
            final Faction finalFaction = faction.get();
            context.getSource().sendSuccess(() -> Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.set_reputation", 
                finalCount, finalAmount, finalFaction.getName()), true);
            return count;
        } else {
            context.getSource().sendFailure(Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.set_reputation_failed"));
            return 0;
        }
    }

    /**
     * Command handler for modifying a player's reputation with a faction.
     *
     * @param context The command context
     * @return The command result
     * @throws CommandSyntaxException If the command syntax is invalid
     */
    private static int modifyPlayerReputation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
        String factionId = StringArgumentType.getString(context, "faction");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        if (!FactionRegistry.factionExists(factionId)) {
            context.getSource().sendFailure(Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.faction_not_found", factionId));
            return 0;
        }

        // Get faction
        Optional<Faction> faction = FactionRegistry.getFaction(factionId);
        if (faction.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.faction_not_found", factionId));
            return 0;
        }

        // Modify reputation for all players
        int count = 0;
        for (ServerPlayer player : players) {
            PlayerFactionProvider.modifyReputation(player, factionId, amount);
            count++;
        }

        if (count > 0) {
            final int finalCount = count;
            final String amountStr = amount >= 0 ? "+" + amount : String.valueOf(amount);
            final Faction finalFaction = faction.get();
            context.getSource().sendSuccess(() -> Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.modify_reputation", 
                finalCount, amountStr, finalFaction.getName()), true);
            return count;
        } else {
            context.getSource().sendFailure(Component.translatable("faction." + RiyaposMod.MOD_ID + ".command.modify_reputation_failed"));
            return 0;
        }
    }
}
