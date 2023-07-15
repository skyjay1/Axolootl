package axolootl.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BubblerBlock extends WaterloggedHorizontalBlock {

    public BubblerBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
        // add bubble particles
        Vec3 vec = Vec3.atBottomCenterOf(pPos).add(0, 1.1D, 0);
        addParticles(pLevel, vec, 0.4D, ParticleTypes.BUBBLE, 2, 0.09D, pRandom);
        addParticles(pLevel, vec, 0.4D, ParticleTypes.BUBBLE_COLUMN_UP, 6, 0.09D, pRandom);
    }

    public static void addParticles(final Level level, final Vec3 pos, final double radius, final ParticleOptions particle,
                                    final int count, final double motion, final RandomSource random) {
        for(int i = 0; i < count; i++) {
            double dx = (random.nextDouble() - 0.5D) * 2.0D * radius;
            double dy = (random.nextDouble() - 0.5D) * 2.0D * radius;
            double dz = (random.nextDouble() - 0.5D) * 2.0D * radius;
            double vx = (random.nextDouble() - 0.5D) * 2.0D * motion;
            double vy = (random.nextDouble()) * motion;
            double vz = (random.nextDouble() - 0.5D) * 2.0D * motion;
            level.addParticle(particle, pos.x() + dx, pos.y() + dy, pos.z() + dz, vx, vy, vz);
        }
    }
}
