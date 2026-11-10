package com.endo.gitanoland.event;

import com.endo.gitanoland.Config;
import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.mission.Missions;
import com.endo.gitanoland.shop.Wallet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Misiones de los aldeanos normales. Clic derecho: la barra de acción dice qué piden. Shift + clic derecho con los
 * objetos en el inventario: los entregas y cobras Kirkys. Cada aldeano paga una sola vez.
 */
@EventBusSubscriber(modid = GitanoLand.MODID)
public class MissionEvents {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!Config.MISSIONS_ENABLED.get()) return;
        if (!(event.getTarget() instanceof Villager villager) || !(event.getEntity() instanceof ServerPlayer player)) return;
        ResourceLocation prof = BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession());
        if (GitanoLand.MODID.equals(prof.getNamespace())) return; // los de GitanoLand son tiendas

        Missions.Mission m = Missions.of(villager);
        if (m == null || Missions.isDone(villager)) return;

        int reward = Missions.reward(m, player);
        Component item = new ItemStack(m.item()).getHoverName();

        if (player.isShiftKeyDown()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(false));
            if (Wallet.countItem(player, m.item()) < m.count()) {
                player.displayClientMessage(Component.translatable("gitanoland.mission.missing", m.count(), item), true);
                villager.playSound(SoundEvents.VILLAGER_NO, 1f, 1f);
                return;
            }
            Wallet.takeItem(player, m.item(), m.count());
            ItemStack coins = new ItemStack(GitanoLand.KIRKY.get(), reward);
            if (!player.getInventory().add(coins)) player.drop(coins, false);
            Missions.markDone(villager);
            player.displayClientMessage(Component.translatable("gitanoland.mission.done", reward), true);
            player.level().playSound(null, villager.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1f, 1f);
            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getEyeY(), villager.getZ(), 8, 0.4, 0.3, 0.4, 0.0);
            }
        } else if (Config.MISSION_HINTS.get()) {
            // Sin cancelar: el comercio normal se abre igual; solo avisamos en la barra de acción.
            player.displayClientMessage(Component.translatable("gitanoland.mission.hint", m.count(), item, reward), true);
        }
    }
}
