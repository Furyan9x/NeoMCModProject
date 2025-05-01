package net.furyan.riyaposmod.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class DumpItemsCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("dumpitems")
                .requires(cs -> cs.hasPermission(2)) // OP level 2+
                .executes(ctx -> dumpItems(ctx.getSource()))
        );
    }

    private static int dumpItems(CommandSourceStack source) {
        // Get all item registry names
        var items = BuiltInRegistries.ITEM.stream()
            .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
            .sorted()
            .collect(Collectors.toList());

        // Choose output file location (in world save folder)
        Path outputPath = source.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
            .resolve("all_items_dump.txt");

        try (FileWriter writer = new FileWriter(outputPath.toFile())) {
            for (String itemName : items) {
                writer.write(itemName + System.lineSeparator());
            }
            source.sendSuccess(() -> Component.literal("Dumped " + items.size() + " items to " + outputPath), false);
            return Command.SINGLE_SUCCESS;
        } catch (IOException e) {
            source.sendFailure(Component.literal("Failed to write item dump: " + e.getMessage()));
            return 0;
        }
    }
}
