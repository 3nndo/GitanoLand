package com.endo.gitanoland.safe;

import com.endo.gitanoland.GitanoLand;
import com.endo.gitanoland.item.KirkyBagItem;
import com.mojang.serialization.MapCodec;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Caja fuerte: el almacen de Kirkys de su dueno. Clic derecho con Kirkys o bolsas: los guarda; agachado + mano vacia: saca un stack;
 * mano vacia: muestra el saldo. Con la llave se enlaza a maquinas del casino y tiendas (ver SafeKeyItem).
 * Solo el dueno (o un OP) puede usarla y romperla; es indestructible para explosiones.
 */
public class SafeBlock extends BaseEntityBlock {
    public static final MapCodec<SafeBlock> CODEC = simpleCodec(SafeBlock::new);

    public SafeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SafeBlockEntity(pos, state);
    }

    // ---- datos ----
    @Nullable
    private static SafeVaults.Vault vaultAt(Level level, BlockPos pos) {
        if (level.getServer() == null || !(level.getBlockEntity(pos) instanceof SafeBlockEntity be)) return null;
        return SafeVaults.get(level.getServer()).vault(be.vaultId());
    }

    private static boolean mayUse(Player p, SafeVaults.Vault v) {
        return v.owner.equals(p.getUUID()) || p.hasPermissions(2);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || level.getServer() == null || !(level.getBlockEntity(pos) instanceof SafeBlockEntity be)) return;
        SafeVaults vaults = SafeVaults.get(level.getServer());
        UUID id = stack.get(ModSafe.SAFE_ID.get());
        SafeVaults.Vault v = vaults.vault(id);
        if (v == null || (placer != null && !v.owner.equals(placer.getUUID()))) {
            // caja nueva, o un objeto de la caja de otro jugador (copiado): se crea una vacia para quien la coloca
            UUID owner = placer != null ? placer.getUUID() : new UUID(0, 0);
            String name = placer instanceof Player p ? p.getGameProfile().getName() : "?";
            v = vaults.create(owner, name);
        }
        be.vaultId(v.id);
    }

    // ---- uso ----
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        boolean ours = stack.is(GitanoLand.KIRKY.get()) || stack.getItem() instanceof KirkyBagItem || stack.is(ModSafe.SAFE_KEY.get());
        if (!ours) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return ItemInteractionResult.SUCCESS;
        SafeVaults.Vault v = vaultAt(level, pos);
        if (v == null) return ItemInteractionResult.SUCCESS;
        if (!mayUse(player, v)) {
            sp.displayClientMessage(Component.translatable("gitanoland.safe.not_yours", v.ownerName), true);
            return ItemInteractionResult.SUCCESS;
        }
        if (stack.is(ModSafe.SAFE_KEY.get())) {
            stack.set(ModSafe.SAFE_ID.get(), v.id);
            sp.displayClientMessage(Component.translatable("gitanoland.safe.key_linked"), true);
            return ItemInteractionResult.SUCCESS;
        }
        long moved;
        if (stack.getItem() instanceof KirkyBagItem) {
            moved = KirkyBagItem.getCoins(stack);
            KirkyBagItem.setCoins(stack, 0);
        } else {
            moved = stack.getCount();
            stack.shrink(stack.getCount());
        }
        v.balance += moved;
        SafeVaults.get(level.getServer()).dirty();
        sp.displayClientMessage(Component.translatable("gitanoland.safe.deposit", moved, v.balance), true);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        SafeVaults.Vault v = vaultAt(level, pos);
        if (v == null) return InteractionResult.SUCCESS;
        if (!mayUse(player, v)) {
            sp.displayClientMessage(Component.translatable("gitanoland.safe.not_yours", v.ownerName), true);
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && v.balance > 0) {
            int out = (int) Math.min(64, v.balance);
            v.balance -= out;
            SafeVaults.get(level.getServer()).dirty();
            ItemStack give = new ItemStack(GitanoLand.KIRKY.get(), out);
            if (!sp.getInventory().add(give)) sp.drop(give, false);
            sp.displayClientMessage(Component.translatable("gitanoland.safe.withdraw", out, v.balance), true);
        } else {
            sp.displayClientMessage(Component.translatable("gitanoland.safe.balance", v.balance), true);
        }
        return InteractionResult.SUCCESS;
    }

    // ---- romper: solo el dueno/OP; el saldo se queda en la caja y la caja vuelve al inventario con su id ----
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter getter, BlockPos pos) {
        if (player.getAbilities().instabuild) return 1.0F;
        if (!(getter instanceof Level level)) return 0.0F;
        SafeVaults.Vault v = vaultAt(level, pos);
        if (v == null || !mayUse(player, v)) return 0.0F;
        return player.getDigSpeed(state, pos) / 6.0F / 30.0F;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.getAbilities().instabuild) {
            SafeVaults.Vault v = vaultAt(level, pos);
            if (v != null) {
                ItemStack drop = new ItemStack(ModSafe.SAFE_ITEM.get());
                drop.set(ModSafe.SAFE_ID.get(), v.id);
                popResource(level, pos, drop);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
