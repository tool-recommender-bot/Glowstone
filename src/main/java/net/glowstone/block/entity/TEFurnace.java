package net.glowstone.block.entity;

import net.glowstone.EventFactory;
import net.glowstone.GlowServer;
import net.glowstone.block.GlowBlock;
import net.glowstone.block.GlowBlockState;
import net.glowstone.block.state.GlowFurnace;
import net.glowstone.inventory.GlowFurnaceInventory;
import net.glowstone.inventory.crafting.CraftingManager;
import net.glowstone.util.nbt.CompoundTag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public class TEFurnace extends TEContainer {

    private short burnTime = 0;
    private short cookTime = 0;
    private short burnTimeFuel = 0;

    public TEFurnace(GlowBlock block) {
        super(block, new GlowFurnaceInventory(new GlowFurnace(block, (short) 0, (short) 0)));
        setSaveId("Furnace");
    }

    @Override
    public GlowBlockState getState() {
        return new GlowFurnace(block);
    }

    @Override
    public void saveNbt(CompoundTag tag) {
        super.saveNbt(tag);
        tag.putShort("BurnTime", burnTime);
        tag.putShort("CookTime", cookTime);
    }

    @Override
    public void loadNbt(CompoundTag tag) {
        super.loadNbt(tag);
        if (tag.isShort("BurnTime")) {
            burnTime = tag.getShort("BurnTime");
        }
        if (tag.isShort("CookTime")) {
            cookTime = tag.getShort("CookTime");
        }
    }

    public short getBurnTime() {
        return burnTime;
    }

    public void setBurnTime(short burnTime) {
        this.burnTime = burnTime;
    }

    public short getCookTime() {
        return cookTime;
    }

    public void setCookTime(short cookTime) {
        this.cookTime = cookTime;
    }

    public void burn() {
        GlowFurnaceInventory inv = (GlowFurnaceInventory) getInventory();
        if (burnTime > 0) burnTime--;
        boolean isBurnable = isBurnable();
        if (cookTime > 0 && isBurnable) {
            cookTime++;
        } else if (burnTime != 0) {
            cookTime = 0;
        }

        if (cookTime == 0 && isBurnable) {
            cookTime = 1;
        }

        if (burnTime == 0) {
            if (isBurnable) {
                CraftingManager cm = ((GlowServer) Bukkit.getServer()).getCraftingManager();
                FurnaceBurnEvent burnEvent = new FurnaceBurnEvent(block, inv.getFuel(), cm.getFuelTime(inv.getFuel().getType()));
                EventFactory.callEvent(burnEvent);
                if (!burnEvent.isCancelled()) {
                    burnTime = (short) cm.getFuelTime(inv.getFuel().getType());
                    burnTimeFuel = burnTime;
                    if (inv.getFuel().getAmount() == 1) {
                        inv.setFuel(new ItemStack(Material.AIR));
                    } else {
                        inv.getFuel().setAmount(inv.getFuel().getAmount() - 1);
                    }
                } else if (cookTime != 0) {
                    if (cookTime % 2 == 0) {
                        cookTime = (short) (cookTime - 2);
                    } else {
                        cookTime--;
                    }
                }
            } else if (cookTime != 0) {
                if (cookTime % 2 == 0) {
                    cookTime = (short) (cookTime - 2);
                } else {
                    cookTime--;
                }
            }
        }

        if (cookTime == 200) {
            CraftingManager cm = ((GlowServer) Bukkit.getServer()).getCraftingManager();
            Recipe recipe = cm.getFurnaceRecipe(inv.getSmelting());
            if (recipe != null) {
                FurnaceSmeltEvent smeltEvent = new FurnaceSmeltEvent(block, inv.getSmelting(), recipe.getResult());
                EventFactory.callEvent(smeltEvent);
                if (!smeltEvent.isCancelled()) {
                    if (inv.getResult() == null || inv.getResult().getType().equals(Material.AIR)) {
                        inv.setResult(recipe.getResult());
                    } else {
                        inv.getResult().setAmount(inv.getResult().getAmount() + recipe.getResult().getAmount());
                    }
                    if (inv.getSmelting().getAmount() == 1) {
                        inv.setSmelting(new ItemStack(Material.AIR));
                    } else {
                        inv.getSmelting().setAmount(inv.getSmelting().getAmount() - 1);
                    }
                }
                cookTime = 0;
            }
        }
        inv.getViewersSet().stream().forEach((human) -> {
            human.setWindowProperty(InventoryView.Property.BURN_TIME, burnTime);
            human.setWindowProperty(InventoryView.Property.TICKS_FOR_CURRENT_FUEL, burnTimeFuel);
            human.setWindowProperty(InventoryView.Property.COOK_TIME, cookTime);
            human.setWindowProperty(InventoryView.Property.TICKS_FOR_CURRENT_SMELTING, 200);
        });
        if (!isBurnable && burnTime == 0 && cookTime == 0) {
            getState().getBlock().getWorld().requestPulse(getState().getBlock(), 0);
        }
    }

    private boolean isBurnable() {
        GlowFurnaceInventory inv = (GlowFurnaceInventory) getInventory();
        if ((burnTime != 0 || inv.getFuel() != null) && inv.getSmelting() != null) {
            if (((inv.getFuel() == null || inv.getFuel().getType().equals(Material.AIR)) || inv.getSmelting().getType().equals(Material.AIR)) && burnTime == 0) {
                return false;
            }
            CraftingManager cm = ((GlowServer) Bukkit.getServer()).getCraftingManager();
            if (burnTime != 0 || cm.isFuel(inv.getFuel().getType())) {
                Recipe recipe = cm.getFurnaceRecipe(inv.getSmelting());
                if (recipe != null && (inv.getResult() == null || !inv.getResult().getType().equals(Material.AIR) || (inv.getResult().getType().equals(recipe.getResult().getType()) && (inv.getResult().getAmount() + recipe.getResult().getAmount()) <= recipe.getResult().getMaxStackSize()))) {
                    return true;
                }
            }
        }
        return false;
    }
}
