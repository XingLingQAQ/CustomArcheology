package cn.myrealm.customarcheology.hooks;

import cn.myrealm.customarcheology.utils.CommonUtil;
import com.ssomar.executableitems.executableitems.manager.ExecutableItemsManager;
import com.willfp.eco.core.items.Items;
import com.willfp.ecoarmor.sets.ArmorSet;
import com.willfp.ecoarmor.sets.ArmorSlot;
import com.willfp.ecoarmor.sets.ArmorUtils;
import com.willfp.ecoitems.items.EcoItem;
import com.willfp.ecoitems.items.ItemUtilsKt;
import dev.lone.itemsadder.api.CustomStack;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.th0rgal.oraxen.api.OraxenItems;
import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.inventory.ItemStack;
import pers.neige.neigeitems.manager.ItemManager;

public class CheckValidHook {

    public static String[] checkValid(ItemStack itemStack) {
        if (CommonUtil.checkPluginLoad("ItemsAdder")) {
            CustomStack customStack = CustomStack.byItemStack(itemStack);
            if (customStack != null) {
                return new String[]{"ItemsAdder", customStack.getId()};
            }
        }
        if (CommonUtil.checkPluginLoad("Oraxen")) {
            String tempVal1 = OraxenItems.getIdByItem(itemStack);
            if (tempVal1 != null) {
                return new String[]{"Oraxen", tempVal1};
            }
        }
        if (CommonUtil.checkPluginLoad("MMOItems")) {
            String tempVal1 = MMOItems.getID(itemStack);
            String tempVal2 = MMOItems.getTypeName(itemStack);
            if (tempVal1 != null && !tempVal1.isEmpty() && tempVal2 != null && !tempVal2.isEmpty()) {
                return new String[]{"MMOItems", tempVal2 + ";;" + tempVal1};
            }
        }
        if (CommonUtil.checkPluginLoad("EcoItems")) {
            EcoItem tempVal1 = ItemUtilsKt.getEcoItem(itemStack);
            if (tempVal1 != null) {
                return new String[]{"EcoItems", tempVal1.getID()};
            }
        }
        if (CommonUtil.checkPluginLoad("EcoArmor")) {
            ArmorSet tempVal1 = ArmorUtils.getSetOnItem(itemStack);
            ArmorSlot tempVal2 = ArmorSlot.getSlot(itemStack);
            if (tempVal1 != null && tempVal2 != null) {
                return new String[]{"EcoArmor", tempVal1.getId() + tempVal2};
            }
        }
        if (CommonUtil.checkPluginLoad("eco")) {
            if (Items.getCustomItem(itemStack) != null) {
                return new String[]{"eco", Items.getCustomItem(itemStack).getKey().toString()};
            }
        }
        if (CommonUtil.checkPluginLoad("MythicMobs")) {
            String tempVal1 = MythicBukkit.inst().getItemManager().getMythicTypeFromItem(itemStack);
            if (tempVal1 != null) {
                return new String[]{"MythicMobs", tempVal1};
            }
        }
        if (CommonUtil.checkPluginLoad("NeigeItems")) {
            if (ItemManager.INSTANCE.isNiItem(itemStack) != null) {
                return new String[]{"NeigeItems", ItemManager.INSTANCE.isNiItem(itemStack).getId()};
            }
        }
        if (CommonUtil.checkPluginLoad("ExecutableItems")) {
            if (ExecutableItemsManager.getInstance().getObject(itemStack).isPresent()) {
                return new String[]{"ExecutableItems", ExecutableItemsManager.getInstance().getObject(itemStack).get().getId()};
            }
        }
        return null;
    }
}
