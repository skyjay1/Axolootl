package axolootl.capability;

import axolootl.Axolootl;
import axolootl.network.AxNetwork;
import axolootl.network.ClientBoundSyncAxolootlResearchCapabilityPacket;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class AxolootlResearchCapability implements INBTSerializable<ListTag> {

    public static final ResourceLocation REGISTRY_NAME = new ResourceLocation(Axolootl.MODID, "research");

    public Collection<ResourceLocation> axolootls;
    public Collection<ResourceLocation> axolootlsView;

    public AxolootlResearchCapability() {
        this.axolootls = new HashSet<>();
        this.axolootlsView = Collections.unmodifiableCollection(axolootls);
    }

    public static AxolootlResearchCapability.Provider provider() {
        return new AxolootlResearchCapability.Provider();
    }

    /**
     * @return an unmodifiable view of the known axolootls
     */
    public Collection<ResourceLocation> getAxolootls() {
        return this.axolootlsView;
    }

    /**
     * @param id the axolootl ID to add
     * @return true if the collection changed as a result of this operation
     */
    public boolean addAxolootl(final ResourceLocation id) {
        return this.axolootls.add(id);
    }

    /**
     * @param id the axolootl ID to remove
     * @return true if the collection changed as a result of this operation
     */
    public boolean removeAxolootl(final ResourceLocation id) {
        return this.axolootls.remove(id);
    }

    /**
     * @param id the axolootl ID to query
     * @return true if the collection contains the given axolootl ID
     */
    public boolean containsAxolootl(final ResourceLocation id) {
        return this.axolootls.contains(id);
    }

    //// NBT ////

    @Override
    public ListTag serializeNBT() {
        final ListTag listTag = new ListTag();
        for(ResourceLocation id : axolootls) {
            listTag.add(StringTag.valueOf(id.toString()));
        }
        return listTag;
    }

    @Override
    public void deserializeNBT(ListTag tag) {
        // prepare to read list
        this.axolootls.clear();
        // validate type
        if(tag.getElementType() != Tag.TAG_STRING) {
            return;
        }
        // read list
        for(int i = 0, n = tag.size(); i < n; i++) {
            this.axolootls.add(new ResourceLocation(tag.getString(i)));
        }
    }

    public void syncToClient(ServerPlayer serverPlayer) {
        // send packet to client
        AxNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new ClientBoundSyncAxolootlResearchCapabilityPacket(this));
    }

    //// PROVIDER ////

    public static class Provider implements ICapabilitySerializable<ListTag> {
        private final AxolootlResearchCapability instance;
        private final LazyOptional<AxolootlResearchCapability> storage;

        public Provider() {
            instance = new AxolootlResearchCapability();
            storage = LazyOptional.of(() -> instance);
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            if(cap == Axolootl.AXOLOOTL_RESEARCH_CAPABILITY) {
                return storage.cast();
            }
            return LazyOptional.empty();
        }

        @Override
        public ListTag serializeNBT() {
            return instance.serializeNBT();
        }

        @Override
        public void deserializeNBT(ListTag tag) {
            instance.deserializeNBT(tag);
        }
    }
}
