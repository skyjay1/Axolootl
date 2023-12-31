/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.data.resource_generator;

import com.google.common.collect.ImmutableList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.List;

@Immutable
public class ResourceDescription {

    private final ItemStack icon;
    private final int weight;
    private final int totalWeight;
    private final double percentChance;
    private final List<Component> descriptions;
    private final Component chanceDescription;

    /**
     * @param item the item stack
     */
    public ResourceDescription(final ItemStack item) {
        this(item, 1, 1);
    }

    /**
     * @param item the item stack
     * @param descriptions any additional components to add to the description tooltip
     */
    public ResourceDescription(final ItemStack item, final List<Component> descriptions) {
        this(item, 1, 1, descriptions);
    }

    /**
     * @param item the item stack
     * @param weight the entry weight
     * @param totalWeight the total weight of all entries
     */
    public ResourceDescription(final ItemStack item, final int weight, final int totalWeight) {
        this(item, weight, totalWeight, ImmutableList.of());
    }

    /**
     * @param item the item stack
     * @param weight the entry weight
     * @param totalWeight the total weight of all entries
     * @param descriptions any additional components to add to the description tooltip
     */
    public ResourceDescription(final ItemStack item, final int weight, final int totalWeight, final List<Component> descriptions) {
        this.icon = item;
        this.weight = weight;
        this.totalWeight = totalWeight;
        this.percentChance = weight / Math.max(1.0D, totalWeight);
        this.chanceDescription = createChanceDescription(this.percentChance, false);
        this.descriptions = ImmutableList.copyOf(descriptions);
    }

    //// GETTERS ////

    public boolean showChance() {
        return weight < totalWeight;
    }

    public ItemStack getItem() {
        return icon;
    }

    public int getWeight() {
        return weight;
    }

    public int getTotalWeight() {
        return totalWeight;
    }

    public double getPercentChance() {
        return percentChance;
    }

    public Component getChanceDescription() {
        return chanceDescription;
    }

    public List<Component> getDescriptions() {
        return descriptions;
    }

    //// HELPER METHODS ////

    /**
     * @param percentChance the percent chance between 0 and 1.0
     * @param wrapped true to wrap the percent chance in parentheses
     * @return a component with the percent chance as a string
     */
    public static Component createChanceDescription(final double percentChance, final boolean wrapped) {
        final String sPercentChance = String.format("%.1f", percentChance * 100.0D).replaceAll("\\.0+$", "");
        return Component.translatable("axolootl.resource_description.chance" + (wrapped ? ".wrapped" : ""), sPercentChance);
    }
}
