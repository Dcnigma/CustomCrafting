package me.wolfyscript.customcrafting.listeners;

import me.wolfyscript.customcrafting.CustomCrafting;
import me.wolfyscript.customcrafting.handlers.RecipeHandler;
import me.wolfyscript.customcrafting.listeners.customevents.CustomPreCraftEvent;
import me.wolfyscript.customcrafting.recipes.types.ICraftingRecipe;
import me.wolfyscript.customcrafting.utils.RecipeUtils;
import me.wolfyscript.utilities.api.WolfyUtilities;
import me.wolfyscript.utilities.api.custom_items.CustomItem;
import me.wolfyscript.utilities.api.utils.inventory.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

public class CraftListener implements Listener {

    private final CustomCrafting customCrafting;
    private final RecipeUtils recipeUtils;
    private final WolfyUtilities api;

    public CraftListener(CustomCrafting customCrafting) {
        this.customCrafting = customCrafting;
        this.recipeUtils = customCrafting.getRecipeUtils();
        this.api = WolfyUtilities.getAPI(customCrafting);
    }

    @EventHandler
    public void onAdvancedWorkbench(CustomPreCraftEvent event) {
        if (!event.isCancelled() && event.getRecipe().getNamespacedKey().toString().equals("customcrafting:advanced_workbench")) {
            if (!customCrafting.getConfigHandler().getConfig().isAdvancedWorkbenchEnabled()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraft(InventoryClickEvent event) {
        if (event.getClickedInventory() == null || !(event.getClickedInventory() instanceof CraftingInventory)) return;
        Player player = (Player) event.getWhoClicked();
        CraftingInventory inventory = (CraftingInventory) event.getClickedInventory();
        ItemStack resultItem = inventory.getResult();

        if (event.getSlot() == 0) {
            if (resultItem == null) {
                event.setCancelled(true);
                return;
            } else if (!ItemUtils.isAirOrNull(event.getCursor()) && !event.getCursor().isSimilar(resultItem) && !event.isShiftClick()) {
                event.setCancelled(true);
                return;
            }

            if (recipeUtils.getPreCraftedRecipes().containsKey(event.getWhoClicked().getUniqueId())) {
                inventory.setResult(new ItemStack(Material.AIR));
                ItemStack[] matrix = inventory.getMatrix().clone();
                recipeUtils.consumeRecipe(resultItem, matrix, event);
                Bukkit.getScheduler().runTask(customCrafting, () -> inventory.setMatrix(matrix));
                player.updateInventory();
                recipeUtils.getPreCraftedRecipes().put(event.getWhoClicked().getUniqueId(), null);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(customCrafting, () -> {
                PrepareItemCraftEvent event1 = new PrepareItemCraftEvent(inventory, event.getView(), false);
                Bukkit.getPluginManager().callEvent(event1);
            }, 1);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreCraft(PrepareItemCraftEvent e) {
        Player player = (Player) e.getView().getPlayer();
        try {
            RecipeHandler recipeHandler = customCrafting.getRecipeHandler();
            ItemStack[] matrix = e.getInventory().getMatrix();
            ItemStack result = recipeUtils.preCheckRecipe(matrix, player, e.isRepair(), e.getInventory(), false, true);
            if (result != null) {
                e.getInventory().setResult(result);
            } else {
                //No valid custom recipes found
                if (e.getRecipe() != null) {
                    if (e.getRecipe() instanceof Keyed) {
                        //Vanilla Recipe is available.
                        api.sendDebugMessage("Detected recipe: " + ((Keyed) e.getRecipe()).getKey());
                        //Check for custom recipe that overrides the vanilla recipe
                        ICraftingRecipe recipe = recipeHandler.getAdvancedCraftingRecipe(((Keyed) e.getRecipe()).getKey().toString());
                        if (recipeHandler.getDisabledRecipes().contains(((Keyed) e.getRecipe()).getKey().toString()) || recipe != null) {
                            //Recipe is disabled or it is a custom recipe!
                            e.getInventory().setResult(new ItemStack(Material.AIR));
                        } else {
                            //Check for items that are not allowed in vanilla recipes.
                            //If one is found, then cancel the recipe.
                            for (ItemStack itemStack : matrix) {
                                CustomItem customItem = CustomItem.getByItemStack(itemStack);
                                if (customItem != null && customItem.isBlockVanillaRecipes()) {
                                    e.getInventory().setResult(new ItemStack(Material.AIR));
                                    return;
                                }
                            }
                            //At this point the vanilla recipe is valid and can be crafted
                            api.sendDebugMessage("Use vanilla recipe output!");
                        }
                    }
                }
            }
            //player.updateInventory();
        } catch (Exception ex) {
            System.out.println("-------- WHAT HAPPENED? Please report! --------");
            ex.printStackTrace();
            System.out.println("-------- WHAT HAPPENED? Please report! --------");
            recipeUtils.getPreCraftedRecipes().remove(player.getUniqueId());
            e.getInventory().setResult(new ItemStack(Material.AIR));
        }
    }
}