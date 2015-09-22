/* 
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 * 
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.items;

import buildcraft.api.tools.IToolWrench;
import ic2.api.item.IBoxable;

import java.util.*;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.world.World;
import mods.railcraft.api.core.items.IToolCrowbar;
import mods.railcraft.common.blocks.tracks.BlockTrackElevator;
import mods.railcraft.common.blocks.tracks.TrackTools;
import mods.railcraft.common.core.RailcraftConfig;
import mods.railcraft.common.items.enchantment.RailcraftEnchantments;
import mods.railcraft.common.plugins.forge.*;
import mods.railcraft.common.util.inventory.InvTools;
import mods.railcraft.common.util.misc.MiscTools;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockRailBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;

public class ItemCrowbar extends ItemTool implements IToolCrowbar, IBoxable, IToolWrench {

    public static final byte BOOST_DAMAGE = 3;
    private static final String ITEM_TAG = "railcraft.tool.crowbar";
    private static Item item;
    private final Set<Class<? extends Block>> shiftRotations = new HashSet<Class<? extends Block>>();
    private final Set<Class<? extends Block>> bannedRotations = new HashSet<Class<? extends Block>>();

    public static void registerItem() {
        if (item == null && RailcraftConfig.isItemEnabled(ITEM_TAG)) {
            item = new ItemCrowbar(ToolMaterial.IRON);
            item.setUnlocalizedName(ITEM_TAG);
            RailcraftRegistry.register(item);
            HarvestPlugin.setToolClass(item, "crowbar", 0);

            CraftingPlugin.addShapedRecipe(new ItemStack(item),
                    " RI",
                    "RIR",
                    "IR ",
                    'I', "ingotIron",
                    'R', "dyeRed");

            LootPlugin.addLootTool(new ItemStack(item), 1, 1, ITEM_TAG);
            LootPlugin.addLootWorkshop(new ItemStack(item), 1, 1, ITEM_TAG);
        }
    }

    public static ItemStack getItem() {
        if (item == null)
            return null;
        return new ItemStack(item);
    }

    public static Item getItemObj() {
        return item;
    }

    protected ItemCrowbar(ToolMaterial material) {
        super(3, material, new HashSet<Block>(Arrays.asList(new Block[]{
            Blocks.rail, Blocks.detector_rail, Blocks.golden_rail, Blocks.activator_rail
        })));
        setCreativeTab(CreativePlugin.RAILCRAFT_TAB);
        shiftRotations.add(BlockLever.class);
        shiftRotations.add(BlockButton.class);
        shiftRotations.add(BlockChest.class);
        bannedRotations.add(BlockRailBase.class);
    }

    @Override
    public boolean doesSneakBypassUse(World world, int x, int y, int z, EntityPlayer player) {
        return true;
    }

    @Override
    public void registerIcons(IIconRegister iconRegister) {
        itemIcon = iconRegister.registerIcon("railcraft:" + MiscTools.cleanTag(getUnlocalizedName()));
    }

    private boolean isShiftRotation(Class<? extends Block> cls) {
        for (Class<? extends Block> shift : shiftRotations) {
            if (shift.isAssignableFrom(cls))
                return true;
        }
        return false;
    }

    private boolean isBannedRotation(Class<? extends Block> cls) {
        for (Class<? extends Block> banned : bannedRotations) {
            if (banned.isAssignableFrom(cls))
                return true;
        }
        return false;
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        Block block = world.getBlock(x, y, z);

        if (block == null)
            return false;

        if (player.isSneaking() != isShiftRotation(block.getClass()))
            return false;

        if (isBannedRotation(block.getClass()))
            return false;

        if (block.rotateBlock(world, x, y, z, ForgeDirection.getOrientation(side))) {
            player.swingItem();
            return !world.isRemote;
        }
        return false;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int x, int y, int z, EntityLivingBase entity) {
        if (!world.isRemote)
            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                if (!player.isSneaking()) {
                    int level = EnchantmentHelper.getEnchantmentLevel(RailcraftEnchantments.destruction.effectId, stack) * 2 + 1;
                    if (level > 0) {
                        BlockMatrix matrix = new BlockMatrix(world, player, level, x, y, z);
                        matrix.breakBlocks();
                    }
                }
            }
        return super.onBlockDestroyed(stack, world, block, x, y, z, entity);
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.block;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack par1ItemStack) {
        return 72000;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        player.setItemInUse(stack, this.getMaxItemUseDuration(stack));
        return stack;
    }

    @Override
    public boolean canBeStoredInToolbox(ItemStack itemstack) {
        return true;
    }

    @Override
    public boolean canWrench(EntityPlayer player, int x, int y, int z) {
        return true;
    }

    @Override
    public void wrenchUsed(EntityPlayer player, int x, int y, int z) {
        player.getCurrentEquippedItem().damageItem(1, player);
        player.swingItem();
    }

    @Override
    public boolean canWhack(EntityPlayer player, ItemStack crowbar, int x, int y, int z) {
        return true;
    }

    @Override
    public void onWhack(EntityPlayer player, ItemStack crowbar, int x, int y, int z) {
        crowbar.damageItem(1, player);
        player.swingItem();
    }

    @Override
    public boolean canLink(EntityPlayer player, ItemStack crowbar, EntityMinecart cart) {
        return player.isSneaking();
    }

    @Override
    public void onLink(EntityPlayer player, ItemStack crowbar, EntityMinecart cart) {
        crowbar.damageItem(1, player);
        player.swingItem();
    }

    @Override
    public boolean canBoost(EntityPlayer player, ItemStack crowbar, EntityMinecart cart) {
        return !player.isSneaking();
    }

    @Override
    public void onBoost(EntityPlayer player, ItemStack crowbar, EntityMinecart cart) {
        crowbar.damageItem(BOOST_DAMAGE, player);
        player.swingItem();
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List info, boolean advInfo) {
        info.add(LocalizationPlugin.translate("item.railcraft.tool.crowbar.tip"));
    }

    private class BlockMatrix {
        private World world;
        private EntityPlayer player;
        private int[][][] matrix;
        private Queue<int[]> queue;
        private int origin;
        private int size;
        private int startX;
        private int startY;
        private int startZ;

        BlockMatrix(World world, EntityPlayer player, int size, int startX, int startY, int startZ) {
            this.world = world;
            this.player = player;
            this.origin = size / 2;
            this.size = size;
            this.startX = startX;
            this.startY = startY;
            this.startZ = startZ;
            
            createMatrix();
        }

        private void createMatrix() {
            matrix = new int[size][size][size];
            matrix[origin][origin][origin] = 1;
            queue = new LinkedList<int[]>();
            queue.add(new int[]{startX, startY, startZ});

            while (!queue.isEmpty()) {
                int[] b = queue.remove();
                checkBlocks(b[0], b[1], b[2]);
            }
        }

        public void breakBlocks() {
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix.length; j++) {
                    for (int k = 0; k < matrix.length; k++) {
                        if (matrix[i][j][k] == 1) {
                            int posX = startX - origin + i;
                            int postY = startY - origin + j;
                            int posZ = startZ - origin+ k;
                            Block block = WorldPlugin.getBlock(world, posX, postY, posZ);
                            int meta = WorldPlugin.getBlockMetadata(world, posX, postY, posZ);
                            BreakEvent event = new BreakEvent(posX, postY, posZ, world, block, meta, player);
                            MinecraftForge.EVENT_BUS.post(event);
                            if (event.isCanceled())
                                return;
                            List<ItemStack> drops = block.getDrops(world, posX, postY, posZ, meta, 0);
                            InvTools.dropItems(drops, world, posX, postY, posZ);
                            world.setBlockToAir(posX, postY, posZ);
                        }
                    }
                }
            }
        }

        private void checkBlock(int x, int y, int z) {
            Block block = WorldPlugin.getBlock(world, x, y, z);
            if (TrackTools.isRailBlock(block) || block instanceof BlockTrackElevator || block.isToolEffective("crowbar", WorldPlugin.getBlockMetadata(world, x, y, z)))
                try {
                    if (matrix[x-startX+origin][y-startY+origin][z-startZ+origin] != 1) {
                        matrix[x-startX+origin][y-startY+origin][z-startZ+origin] = 1;
                        queue.add(new int[] {x, y, z});
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    return;
                }
        }

        private void checkBlocks(int x, int y, int z) {
            //NORTH
            checkBlock(x, y, z - 1);
            checkBlock(x, y + 1, z - 1);
            checkBlock(x, y - 1, z - 1);
            //SOUTH
            checkBlock(x, y, z + 1);
            checkBlock(x, y + 1, z + 1);
            checkBlock(x, y - 1, z + 1);
            //EAST
            checkBlock(x + 1, y, z);
            checkBlock(x + 1, y + 1, z);
            checkBlock(x + 1, y - 1, z);
            //WEST
            checkBlock(x - 1, y, z);
            checkBlock(x - 1, y + 1, z);
            checkBlock(x - 1, y - 1, z);
            //UP_DOWN
            checkBlock(x, y + 1, z);
            checkBlock(x, y - 1, z);
        }

    }

}