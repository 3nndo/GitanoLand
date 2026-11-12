package com.endo.gitanoland.event;

import com.endo.gitanoland.Config;
import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.reward.Rewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import com.endo.gitanoland.net.RewardToastPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Los logros dan Kirkys pendientes (normales 5, de desafío 10); se reclaman en el menú de recompensas. */
@EventBusSubscriber(modid = GitanoLand.MODID)
public class RewardEvents {
    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        var adv = event.getAdvancement().value();
        DisplayInfo d = adv.display().orElse(null);
        if (d == null || adv.parent().isEmpty()) return; // recetas y pestañas raíz no cuentan
        int n = d.getType() == AdvancementType.CHALLENGE ? Config.ACHIEVEMENT_CHALLENGE_KIRKYS.get() : Config.ACHIEVEMENT_KIRKYS.get();
        if (n <= 0) return;
        Rewards.addPending(p, n);
        PacketDistributor.sendToPlayer(p, new RewardToastPayload(n, d.getType() == AdvancementType.CHALLENGE));
    }

    /** Al morir, lo pendiente y la recompensa diaria pasan al jugador nuevo. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        var o = event.getOriginal().getPersistentData();
        var n = event.getEntity().getPersistentData();
        n.putInt(Rewards.PENDING, o.getInt(Rewards.PENDING));
        if (o.contains(Rewards.DAILY)) n.putLong(Rewards.DAILY, o.getLong(Rewards.DAILY));
    }
}
