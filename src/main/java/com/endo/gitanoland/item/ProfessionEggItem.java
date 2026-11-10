package com.endo.gitanoland.item;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** "Huevo" que invoca un aldeano ya con su profesión de GitanoLand, que no despawnea (es una tienda). */
public class ProfessionEggItem extends Item {
    private final Supplier<VillagerProfession> profession;

    public ProfessionEggItem(Properties properties, Supplier<VillagerProfession> profession) {
        super(properties);
        this.profession = profession;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        Villager villager = EntityType.VILLAGER.spawn(level, stack, player, pos, MobSpawnType.SPAWN_EGG, true, false);
        if (villager == null) return InteractionResult.FAIL;

        VillagerData data = villager.getVillagerData().setProfession(profession.get()).setLevel(1);
        villager.setVillagerData(data);
        // Con 1 de XP el aldeano no pierde la profesión por no tener mesa (vanilla solo la quita si XP = 0).
        villager.setVillagerXp(1);
        villager.setPersistenceRequired();
        if (player != null && !player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.CONSUME;
    }
}
