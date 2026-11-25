package dev.thorstatue;

import dev.thorstatue.block.ThorStatueBlock;
import dev.thorstatue.block.ThorStatueBlockEntity;
import dev.thorstatue.net.StartCutscenePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Registers the Thor statue block, its GeckoLib block entity and the cutscene packet.
 * The block item is intentionally NOT added to any creative tab: obtain it with
 *   /give @s thorstatue:thor_statue
 */
public class ThorStatueMod implements ModInitializer {
    public static final String MOD_ID = "thorstatue";

    public static Block THOR_STATUE;
    public static BlockEntityType<ThorStatueBlockEntity> THOR_STATUE_BE;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id("thor_statue"));
        THOR_STATUE = Registry.register(BuiltInRegistries.BLOCK, blockKey,
            new ThorStatueBlock(BlockBehaviour.Properties.of()
                .strength(5.0f, 1200.0f).noOcclusion().requiresCorrectToolForDrops().setId(blockKey)));

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id("thor_statue"));
        Registry.register(BuiltInRegistries.ITEM, itemKey,
            new BlockItem(THOR_STATUE, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));

        THOR_STATUE_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("thor_statue"),
            FabricBlockEntityTypeBuilder.create(ThorStatueBlockEntity::new, THOR_STATUE).build());

        PayloadTypeRegistry.clientboundPlay().register(StartCutscenePayload.TYPE, StartCutscenePayload.CODEC);
    }
}
