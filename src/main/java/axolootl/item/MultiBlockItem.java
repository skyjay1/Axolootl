/**
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 **/

package axolootl.item;

import axolootl.AxRegistry;
import axolootl.Axolootl;
import axolootl.block.WaterloggedHorizontalMultiBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;

public class MultiBlockItem extends BlockItem {

    private static final RegistryObject<Item> GRAND_CASTLE_ITEM = RegistryObject.create(new ResourceLocation(Axolootl.MODID, "grand_castle"), ForgeRegistries.ITEMS);

    public MultiBlockItem(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }

    @Nullable
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext pContext) {
        final BlockPos blockpos = pContext.getClickedPos();
        final Direction direction = pContext.getHorizontalDirection();
        // determine the center position of a multiblock placed with the given position and rotation
        final Vec3i index = new Vec3i(-(direction.getStepX() - 1), 0, -(direction.getStepZ() - 1));
        final BlockPos center = WaterloggedHorizontalMultiBlock.getCenter(blockpos, index);
        // create a block place context at the center position
        return BlockPlaceContext.at(pContext, center, direction);
    }

    @Override
    protected boolean mustSurvive() {
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack pStack) {
        if(pStack.is(GRAND_CASTLE_ITEM.get())) {
            return true;
        }
        return super.isEnchantable(pStack);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if(stack.is(GRAND_CASTLE_ITEM.get()) && enchantment == Enchantments.FISHING_LUCK) {
            return true;
        }
        return super.canApplyAtEnchantingTable(stack, enchantment);
    }
}
