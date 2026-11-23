package com.endo.gitanoland.safe;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Solo recuerda el id de su caja (el saldo esta en SafeVaults). */
public class SafeBlockEntity extends BlockEntity {
    @Nullable
    private UUID vaultId;

    public SafeBlockEntity(BlockPos pos, BlockState state) {
        super(ModSafe.SAFE_BE.get(), pos, state);
    }

    @Nullable
    public UUID vaultId() {
        return vaultId;
    }

    public void vaultId(UUID id) {
        this.vaultId = id;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (vaultId != null) tag.putUUID("vault", vaultId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        vaultId = tag.hasUUID("vault") ? tag.getUUID("vault") : null;
    }
}
