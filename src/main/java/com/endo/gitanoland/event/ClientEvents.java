package com.endo.gitanoland.event;

import com.endo.gitanoland.GitanoLand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.endo.gitanoland.client.ShopScreen;
import com.endo.gitanoland.entity.ModEntities;
import com.endo.gitanoland.shop.ModMenus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import com.endo.gitanoland.item.KirkyBagItem;

@EventBusSubscriber(modid = GitanoLand.MODID, value = Dist.CLIENT)
public class ClientEvents {
    /** Oscurece la textura del bundle de vanilla multiplicándola por un gris. */
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // Huevos de aldeano: mismo modelo que un huevo de vanilla, con dos colores.
        event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFF4A5C30 : 0xFFD8C47A, GitanoLand.AMMO_MAKER_EGG.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFF3A3A40 : 0xFF966E28, GitanoLand.GUNSMITH_EGG.get());
    }

    /** El WeedsGuy se dibuja igual que el Wandering Trader. */
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WEEDSGUY.get(), WanderingTraderRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SHOP.get(), ShopScreen::new);
        event.register(ModMenus.REWARDS.get(), com.endo.gitanoland.client.RewardsScreen::new);
    }

    /** Propiedad "fill" de las bolsas: elige el modelo vacío / medio / lleno. */
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        ResourceLocation fill = ResourceLocation.fromNamespaceAndPath(GitanoLand.MODID, "fill");
        event.enqueueWork(() -> {
            for (var bag : new net.minecraft.world.item.Item[]{GitanoLand.KIRKY_BAG_SMALL.get(),
                    GitanoLand.KIRKY_BAG.get(), GitanoLand.KIRKY_BAG_LARGE.get()}) {
                ItemProperties.register(bag, fill, (stack, level, entity, seed) -> KirkyBagItem.fillState(stack));
            }
        });
    }
}
