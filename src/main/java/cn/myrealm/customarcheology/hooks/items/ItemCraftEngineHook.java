package cn.myrealm.customarcheology.hooks.items;

import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.inventory.ItemStack;

public class ItemCraftEngineHook extends AbstractItemHook {

    public ItemCraftEngineHook() {
        super("CraftEngine");
    }

    @Override
    public ItemStack getHookItemByID(String hookItemID) {
        CustomItem<ItemStack> customItem = CraftEngineItems.byId(new Key(hookItemID.split(";;")[0], hookItemID.split(";;")[1]));
        if (customItem == null) {
            return null;
        }
        return customItem.buildItemStack();
    }

    @Override
    public String getIDByItemStack(ItemStack hookItem) {
        Key key = CraftEngineItems.getCustomItemId(hookItem);
        if (key == null) {
            return null;
        }
        return key.namespace() + ";;" + key.value();
    }
}
