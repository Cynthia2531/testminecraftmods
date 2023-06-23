package com.cynthia2531.testminecraftmods;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.ArrowNockEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TestMinecraftMods.MODID)
public class TestMinecraftMods
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "testminecraftmods";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "TestMinecraftMods" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "TestMinecraftMods" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    // Creates a new Block with the id "TestMinecraftMods:example_block", combining the namespace and path
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of(Material.STONE)));
    // Creates a new BlockItem with the id "TestMinecraftMods:example_block", combining the namespace and path
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));

    public TestMinecraftMods()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        modEventBus.addListener(this::registerOverlays);
    }
    
    private void registerOverlays(RegisterGuiOverlaysEvent event)
	{
		event.registerAboveAll(find("health_overlay").replace(":", "_"), (gui, mStack, partialTicks, screenWidth, screenHeight) -> onOverlayEvent(mStack, screenWidth, screenHeight));
	}

    private static Minecraft mc()
	{
		return Minecraft.getInstance();
	}

    public static void onOverlayEvent(PoseStack stack, int screenWidth, int screenHeight)
	{
		if (/*event.getType() == ElementType.ALL && */!mc().options.renderDebug)
		{
			Player player = mc().player;

			if (player == null || player != null && player.level == null)
				return;

			Level world = player.level;
			// Window res = mc().getWindow();

			try
			{
				List<Entity> entities = world.getEntities(
                    player,
                    player.getBoundingBox().inflate(7.0D),
                    NOT_SPECTATING_AND_LIVING.and(ENTITY_VISIBLE)
                );
				beginHealthRendering(stack, entities);
                
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

    private static void beginHealthRendering(PoseStack stack, List<Entity> entities)
	{
		if (!entities.isEmpty())
		{
            Minecraft instance =  Minecraft.getInstance();
            LocalPlayer player = instance.player;
            

            String message = "";

			for (int i = 0; i < entities.size(); ++i)
			{
				Entity entity = entities.get(i);
                if (entity != null && entity instanceof LivingEntity living){
                    int maxHealth = (int) (living.getMaxHealth());
                    int health = (int) (living.getHealth());
                    message += entity.getName().getString() + "(" + String.valueOf(health) +"/" + String.valueOf(maxHealth) + "), ";
                }
                
                
			}
            player.sendSystemMessage(Component.literal(message));
		}
	}

    public static final Predicate<Entity> ENTITY_VISIBLE = (entity) ->
	{
		if (entity instanceof LivingEntity living && living.hasEffect(MobEffects.INVISIBILITY))
			return false;

		return !entity.isCrouching() && !entity.isInvisible();
	};

    public static final Predicate<Entity> NOT_SPECTATING_AND_LIVING = (entity) ->
	{
		// TODO check for bosses
		return entity.isAlive() && !entity.isSpectator() && entity instanceof LivingEntity && !(entity instanceof ArmorStand)/* && entity.getType().is(Tags.Entity)*/ && !entity.isInvulnerable() && !entity.getDisplayName().getString().contains("click") && !entity.getDisplayName().getString().contains("join");
	};

    public static String find(String name)
	{
		return new String(MODID + ":" + name);
	}

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
    }

    private void addCreative(CreativeModeTabEvent.BuildContents event)
    {
        if (event.getTab() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(EXAMPLE_BLOCK_ITEM);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    @SubscribeEvent
    public void pickupItem(EntityItemPickupEvent event) {
        ItemEntity item = event.getItem();
        System.out.println("Item picked up!");
        Minecraft instance =  Minecraft.getInstance();
        LocalPlayer player = instance.player;
        player.sendSystemMessage(item.getName());
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
