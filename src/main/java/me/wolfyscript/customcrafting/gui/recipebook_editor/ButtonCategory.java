package me.wolfyscript.customcrafting.gui.recipebook_editor;

import me.wolfyscript.customcrafting.CustomCrafting;
import me.wolfyscript.customcrafting.configs.recipebook.Category;
import me.wolfyscript.customcrafting.configs.recipebook.CategoryFilter;
import me.wolfyscript.customcrafting.configs.recipebook.CategorySettings;
import me.wolfyscript.customcrafting.data.CCCache;
import me.wolfyscript.utilities.api.inventory.gui.button.ButtonState;
import me.wolfyscript.utilities.api.inventory.gui.button.buttons.ActionButton;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;

class ButtonCategory extends ActionButton<CCCache> {

    ButtonCategory(String categoryID, CustomCrafting customCrafting) {
        super("category_" + categoryID, new ButtonState<>("category", Material.CHEST, (cache, guiHandler, player, inventory, slot, event) -> {
            if (!categoryID.isEmpty() && event instanceof InventoryClickEvent clickEvent) {
                var recipeBookEditor = cache.getRecipeBookEditor();
                var recipeBook = customCrafting.getConfigHandler().getRecipeBookConfig();
                if (clickEvent.isRightClick() && clickEvent.isShiftClick()) {
                    //Delete Category
                    recipeBook.getCategories().removeCategory(categoryID);
                    return true;
                } else if (clickEvent.isLeftClick()) {
                    //Edit Category
                    CategorySettings category = recipeBookEditor.isFilters() ? recipeBook.getCategories().getFilter(categoryID) : recipeBook.getCategories().getCategory(categoryID);
                    recipeBookEditor.setCategoryID(categoryID);
                    if (category instanceof CategoryFilter filter) {
                        recipeBookEditor.setFilter(new CategoryFilter(filter));
                        guiHandler.openWindow("filter");
                    } else {
                        recipeBookEditor.setCategory(new Category((Category) category));
                        guiHandler.openWindow("category");
                    }
                    return true;
                }
            }
            return true;
        }, (values, cache, guiHandler, player, inventory, itemStack, slot, help) -> {
            var recipeBookEditor = cache.getRecipeBookEditor();
            var recipeBook = customCrafting.getConfigHandler().getRecipeBookConfig();
            var category = recipeBookEditor.isFilters() ? recipeBook.getCategories().getFilter(categoryID) : recipeBook.getCategories().getCategory(categoryID);
            itemStack.setType(category.getIcon());
            values.put("%name%", category.getName());
            values.put("%description%", category.getDescription());
            return itemStack;
        }));
    }
}
