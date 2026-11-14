package com.endo.gitanoland.entity;

import com.endo.gitanoland.Config;
import com.endo.gitanoland.GitanoLand;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Hace aparecer al WeedsGuy de vez en cuando cerca de un jugador, como el Wandering Trader de vanilla. */
@EventBusSubscriber(modid = GitanoLand.MODID)
public class WeedsGuySpawner {
    private static final long CHECK_AT = 2000L; // una vez por día, a primera hora de la mañana

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (level.getDayTime() % 24000L != CHECK_AT) return;
        int chance = Config.WEEDSGUY_SPAWN_CHANCE.get();
        if (chance <= 0 || level.random.nextInt(100) >= chance) return;
        if (!level.getEntities(ModEntities.WEEDSGUY.get(), e -> true).isEmpty()) return;
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;
        ServerPlayer player = players.get(level.random.nextInt(players.size()));
        for (int i = 0; i < 10; i++) {
            int dx = (level.random.nextBoolean() ? 1 : -1) * (24 + level.random.nextInt(24));
            int dz = (level.random.nextBoolean() ? 1 : -1) * (24 + level.random.nextInt(24));
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    player.blockPosition().offset(dx, 0, dz));
            if (!level.getBlockState(pos.below()).isSolidRender(level, pos.below())
                    || !level.getBlockState(pos).getFluidState().isEmpty()) continue;
            WeedsGuyEntity guy = ModEntities.WEEDSGUY.get().spawn(level, pos, MobSpawnType.EVENT);
            if (guy != null) {
                guy.setDespawnDelay(48000);
                return;
            }
        }
    }
}
