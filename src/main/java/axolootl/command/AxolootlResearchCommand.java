/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.command;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import axolootl.data.axolootl_variant.AxolootlVariant;
import com.google.common.collect.Sets;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class AxolootlResearchCommand {

    private static final String COMMAND = "axresearch";

    private static final DynamicCommandExceptionType UNKNOWN_VARIANT = new DynamicCommandExceptionType(o -> Component.translatable("commands.axresearch.failure.unknown_variant", o));
    private static final DynamicCommandExceptionType INVALID_VARIANT = new DynamicCommandExceptionType(o -> Component.translatable("commands.axresearch.failure.invalid_variant", o));

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_AXOLOOTL_VARIANT = (context, builder) -> {
        Set<ResourceLocation> set = Sets.difference(AxolootlVariant.getRegistry(context.getSource().registryAccess()).keySet(), AxRegistry.AxolootlVariantsReg.getInvalidEntries());
        return SharedSuggestionProvider.suggest(set.stream().map(ResourceLocation::toString), builder);
    };

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        // create command
        final LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(COMMAND)
                .requires(c -> c.hasPermission(2))
                .then(Commands.literal("grant")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.literal("all")
                                        .executes(context -> addAll(context.getSource(), EntityArgument.getPlayers(context, "targets"))))
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(SUGGEST_AXOLOOTL_VARIANT)
                                        .executes(context -> add(context.getSource(), EntityArgument.getPlayers(context, "targets"), ResourceLocationArgument.getId(context, "id"))))))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.literal("all")
                                        .executes(context -> removeAll(context.getSource(), EntityArgument.getPlayers(context, "targets"))))
                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                        .suggests(SUGGEST_AXOLOOTL_VARIANT)
                                        .executes(context -> remove(context.getSource(), EntityArgument.getPlayers(context, "targets"), ResourceLocationArgument.getId(context, "id"))))));
        // register command
        dispatcher.register(builder);
    }

    private static int add(final CommandSourceStack context, final Collection<ServerPlayer> targets, final ResourceLocation id) throws CommandSyntaxException {
        // validate ID
        final Registry<AxolootlVariant> registry = AxolootlVariant.getRegistry(context.registryAccess());
        final Optional<AxolootlVariant> oVariant = registry.getOptional(id);
        if(oVariant.isEmpty()) {
            throw UNKNOWN_VARIANT.create(id);
        }
        if(!AxRegistry.AxolootlVariantsReg.isValid(id)) {
            throw INVALID_VARIANT.create(id);
        }
        // add to the given players
        for(ServerPlayer target : targets) {
            target.getCapability(Axolootl.AXOLOOTL_RESEARCH_CAPABILITY).ifPresent(c -> {
                c.addAxolootl(target, id);
            });
        }
        // send feedback
        if(targets.size() == 1) {
            context.sendSuccess(Component.translatable("commands.axresearch.add.single.success", id, targets.iterator().next().getDisplayName()), true);
        } else {
            context.sendSuccess(Component.translatable("commands.axresearch.add.multiple.success", id, targets.size()), true);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int addAll(final CommandSourceStack context, final Collection<ServerPlayer> targets) {
        // add to the given players
        final Set<ResourceLocation> ids = Sets.difference(AxolootlVariant.getRegistry(context.registryAccess()).keySet(), AxRegistry.AxolootlVariantsReg.getInvalidEntries());
        // add to the given players
        for(ServerPlayer target : targets) {
            target.getCapability(Axolootl.AXOLOOTL_RESEARCH_CAPABILITY).ifPresent(c -> {
                c.addAxolootls(ids);
                c.syncToClient(target);
            });
        }
        // send feedback
        if(targets.size() == 1) {
            context.sendSuccess(Component.translatable("commands.axresearch.addall.single.success", ids.size(), targets.iterator().next().getDisplayName()), true);
        } else {
            context.sendSuccess(Component.translatable("commands.axresearch.addall.multiple.success", ids.size(), targets.size()), true);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int remove(final CommandSourceStack context, final Collection<ServerPlayer> targets, final ResourceLocation id) throws CommandSyntaxException {
        // validate ID
        if(!AxRegistry.AxolootlVariantsReg.isValid(id)) {
            throw INVALID_VARIANT.create(id);
        }
        final Registry<AxolootlVariant> registry = AxolootlVariant.getRegistry(context.registryAccess());
        final Optional<AxolootlVariant> oVariant = registry.getOptional(id);
        if(oVariant.isEmpty()) {
            throw UNKNOWN_VARIANT.create(id);
        }
        // remove from the given players
        for(ServerPlayer target : targets) {
            target.getCapability(Axolootl.AXOLOOTL_RESEARCH_CAPABILITY).ifPresent(c -> {
                c.removeAxolootl(target, id);
            });
        }
        // send feedback
        if(targets.size() == 1) {
            context.sendSuccess(Component.translatable("commands.axresearch.remove.single.success", id, targets.iterator().next().getDisplayName()), true);
        } else {
            context.sendSuccess(Component.translatable("commands.axresearch.remove.multiple.success", id, targets.size()), true);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int removeAll(final CommandSourceStack context, final Collection<ServerPlayer> targets) {
        // clear for the given players
        for(ServerPlayer target : targets) {
            target.getCapability(Axolootl.AXOLOOTL_RESEARCH_CAPABILITY).ifPresent(c -> {
                c.clear(target);
            });
        }
        // send feedback
        if(targets.size() == 1) {
            context.sendSuccess(Component.translatable("commands.axresearch.removeall.single.success", targets.iterator().next().getDisplayName()), true);
        } else {
            context.sendSuccess(Component.translatable("commands.axresearch.removeall.multiple.success", targets.size()), true);
        }
        return Command.SINGLE_SUCCESS;
    }
}
