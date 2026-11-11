package com.endo.gitanoland.event;

import com.endo.gitanoland.Config;
import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.mission.Missions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;

/**
 * Tratos especiales de los aldeanos normales dentro de su GUI de comercio (sin tocar la GUI): al hacerle clic
 * derecho se añaden los tratos de su nivel (un trato especial + un trueque normal con Kirkys). Cada especial se
 * puede hacer 2 veces (3 si le caes muy bien) y no se reabastece. El de nivel 4 lo sube a Maestro, da descuento y
 * reabastece sus tratos normales.
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
        if (!Missions.hasMissions(villager)) return;
        Missions.ensure(villager, player);
    }

    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        if (!(event.getAbstractVillager() instanceof Villager v) || !(event.getEntity() instanceof ServerPlayer player)) return;
        int level = Missions.levelOf(v, event.getMerchantOffer());
        if (level < 0) return;
        Missions.Special s = Missions.of(v, level);
        if (s == null) return;
        int used = Missions.addUse(v, level);

        // Más de un stack de Kirkys no cabe en la casilla de resultado: el resto va directo al inventario.
        int extra = Missions.reward(v, s) - Missions.STACK;
        if (extra > 0) {
            ItemStack coins = new ItemStack(GitanoLand.KIRKY.get(), extra);
            if (!player.getInventory().add(coins)) player.drop(coins, false);
        }
        // Repeticiones: la 2.ª y la 3.ª solo dan la mitad de lo que falta para el siguiente nivel.
        if (used >= 2 && level != 4) {
            int cur = v.getVillagerData().getLevel();
            if (VillagerData.canLevelUp(cur)) {
                int gap = Math.max(0, VillagerData.getMaxXpPerLevel(cur) - v.getVillagerXp());
                v.setVillagerXp(v.getVillagerXp() + gap / 2);
            }
        }
        // Nivel 4 (el penúltimo): descuento y todos los tratos normales reabastecidos (los especiales no).
        if (level == 4 && used == 1) {
            v.getGossips().add(player.getUUID(), GossipType.MAJOR_POSITIVE, 20);
            for (MerchantOffer o : v.getOffers()) {
                if (Missions.levelOf(v, o) < 0) o.resetUses();
            }
        }
        player.level().playSound(null, v.blockPosition(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1f, 1f);
        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, v.getX(), v.getEyeY(), v.getZ(), 8, 0.4, 0.3, 0.4, 0.0);
        }
    }
}
