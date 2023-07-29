package axolootl.data.axolootl_variant.condition;

import axolootl.AxRegistry;
import com.mojang.serialization.Codec;
import net.minecraftforge.fml.ModList;

public class ModLoadedForgeCondition extends ForgeCondition {

    public static final Codec<ModLoadedForgeCondition> CODEC = Codec.STRING
            .xmap(ModLoadedForgeCondition::new, ModLoadedForgeCondition::getModId).fieldOf("modid").codec();

    private final String modId;

    public ModLoadedForgeCondition(String modId) {
        this.modId = modId;
    }

    public String getModId() {
        return modId;
    }

    @Override
    public boolean test(ForgeConditionContext context) {
        return ModList.get().isLoaded(getModId());
    }

    @Override
    public Codec<? extends ForgeCondition> getCodec() {
        return AxRegistry.ForgeConditionsReg.MOD_LOADED.get();
    }
}
