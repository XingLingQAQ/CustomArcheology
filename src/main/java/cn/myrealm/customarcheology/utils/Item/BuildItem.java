package cn.myrealm.customarcheology.utils.Item;

import cn.myrealm.customarcheology.CustomArcheology;
import cn.myrealm.customarcheology.managers.managers.BlockManager;
import cn.myrealm.customarcheology.managers.managers.HookManager;
import cn.myrealm.customarcheology.managers.managers.ToolManager;
import cn.myrealm.customarcheology.managers.managers.system.LanguageManager;
import cn.myrealm.customarcheology.utils.CommonUtil;
import cn.myrealm.customarcheology.utils.NBTUtil;
import com.google.common.base.Enums;
import com.google.common.collect.MultimapBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrushableBlock;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TropicalFish;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.meta.components.*;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.tag.DamageTypeTags;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

public class BuildItem {
    public static ItemStack buildItemStack(ConfigurationSection section,
                                           String... args) {
        ItemStack item = new ItemStack(Material.STONE);
        return editItemStack(item, section, args);
    }

    public static ItemStack editItemStack(ItemStack item,
                                          ConfigurationSection section,
                                          String... args) {

        // Material
        String materialKey = section.getString("material");
        if (materialKey != null) {
            Material material = Material.getMaterial(materialKey.toUpperCase());
            if (material != null) {
                item.setType(material);
            } else if (BlockManager.getInstance().isBlockExists(materialKey)) {
                item = BlockManager.getInstance().generateItemStack(materialKey, 1);
            } else if (ToolManager.getInstance().isToolExists(materialKey)) {
                item = ToolManager.getInstance().getTool(materialKey).generateItem();
            }
        } else {
            String pluginName = section.getString("hook-plugin");
            String itemID = section.getString("hook-item");
            if (pluginName != null && itemID != null) {
                if (pluginName.equals("MMOItems") && !itemID.contains(";;")) {
                    itemID = section.getString("hook-item-type") + ";;" + itemID;
                } else if (pluginName.equals("EcoArmor") && !itemID.contains(";;")) {
                    itemID = itemID + ";;" + section.getString("hook-item-type");
                }
                ItemStack hookItem = HookManager.getHookManager().getHookItem(pluginName, itemID);
                if (hookItem != null) {
                    item = hookItem;
                }
            }
        }

        // Amount
        int amountKey = section.getInt("amount");
        if (amountKey > 0) {
            item.setAmount(amountKey);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        // Custom Name
        String displayNameKey = section.getString("name", section.getString("display"));
        if (displayNameKey != null) {
            meta.setDisplayName(LanguageManager.parseColor(CommonUtil.modifyString(displayNameKey, args)));
        }

        // Item Name
        if (CommonUtil.getMinorVersion(20, 5)) {
            String itemNameKey = section.getString("item-name");
            if (itemNameKey != null) {
                if (itemNameKey.isEmpty()) {
                    meta.setItemName(" ");
                } else {
                    meta.setItemName(LanguageManager.parseColor(CommonUtil.modifyString(itemNameKey, args)));
                }
            }
        }

        // Lore
        List<String> lores = section.getStringList("lore");
        if (!lores.isEmpty()) {
            List<String> newLore = new ArrayList<>();
            for (String lore : lores) {
                lore = CommonUtil.modifyString(lore, args);
                for (String singleLore : lore.split("\n")) {
                    if (singleLore.isEmpty()) {
                        newLore.add(" ");
                        continue;
                    }
                    newLore.add(LanguageManager.parseColor(singleLore));
                }
            }
            if (!newLore.isEmpty()) {
                meta.setLore(newLore);
            }
        }


        // Custom Model Data
        int customModelDataKey = section.getInt("custom-model-data", section.getInt("cmd", -1));
        if (customModelDataKey > 0) {
            meta.setCustomModelData(customModelDataKey);
        }

        // Max Stack
        if (CommonUtil.getMinorVersion(20, 5)) {
            int maxStackKey = section.getInt("max-stack", -1);
            if (maxStackKey > 0 && maxStackKey < 100) {
                meta.setMaxStackSize(maxStackKey);
            }
        }

        // Food
        if (CommonUtil.getMinorVersion(20, 5)) {
            ConfigurationSection foodKey = section.getConfigurationSection("food");
            FoodComponent foodComponent = meta.getFood();
            if (foodKey != null) {
                if (foodKey.contains("can-always-eat")) {
                    foodComponent.setCanAlwaysEat(foodKey.getBoolean("can-always-eat"));
                }
                int foodNutrition = foodKey.getInt("nutrition", -1);
                if (foodNutrition > 0) {
                    foodComponent.setNutrition(foodNutrition);
                }
                double foodSaturation = foodKey.getDouble("saturation", -1);
                if (foodSaturation > 0) {
                    foodComponent.setSaturation((float) foodSaturation);
                }
                ConfigurationSection convertItem = section.getConfigurationSection("convert");
                if (!CommonUtil.getMinorVersion(21, 2)) {
                    double eatSecond = foodKey.getDouble("eat-seconds", -1);
                    if (eatSecond >= 0) {
                        foodComponent.setEatSeconds((float) eatSecond);
                    }
                    if (CommonUtil.getMajorVersion(21) && convertItem != null) {
                        foodComponent.setUsingConvertsTo(buildItemStack(convertItem, args));
                    }
                    for (String effects : foodKey.getStringList("effects")) {
                        String[] effectParseResult = effects.replace(" ", "").split(",");
                        if (effectParseResult.length < 4) {
                            continue;
                        }
                        PotionEffectType potionEffectType = null;
                        if (CommonUtil.getMajorVersion(20)) {
                            potionEffectType = Registry.POTION_EFFECT_TYPE.get(CommonUtil.parseNamespacedKey(effectParseResult[0]));
                        } else {
                            potionEffectType = PotionEffectType.getByName(effectParseResult[0]);
                        }
                        if (potionEffectType != null) {
                            PotionEffect potionEffect = new PotionEffect(potionEffectType,
                                    Integer.parseInt(effectParseResult[1]),
                                    Integer.parseInt(effectParseResult[2]),
                                    effectParseResult.length < 5 || Boolean.parseBoolean(effectParseResult[3]),
                                    effectParseResult.length < 6 || Boolean.parseBoolean(effectParseResult[4]),
                                    effectParseResult.length < 7 || Boolean.parseBoolean(effectParseResult[5]));
                            foodComponent.addEffect(potionEffect, Float.parseFloat(effectParseResult[effectParseResult.length - 1]));
                        }
                    }
                }
                meta.setFood(foodComponent);
            }
        }

        // Tool
        if (CommonUtil.getMajorVersion(21)) {
            ConfigurationSection toolKey = section.getConfigurationSection("tool");
            ToolComponent toolComponent = meta.getTool();
            if (toolKey != null) {
                int damagePerBlock = toolKey.getInt("damage-per-block", -1);
                if (damagePerBlock >= 0) {
                    toolComponent.setDamagePerBlock(damagePerBlock);
                }
                double miningSpeed = toolKey.getDouble("mining-speed", -1);
                if (miningSpeed > 0) {
                    toolComponent.setDefaultMiningSpeed((float) miningSpeed);
                }
                for (String rules : toolKey.getStringList("rules")) {
                    String[] ruleParseResult = rules.replace(" ", "").split(",");
                    if (ruleParseResult.length < 3) {
                        continue;
                    }
                    Collection<Material> materials = new ArrayList<>();
                    int i = 0;
                    for (String singleMaterial : ruleParseResult) {
                        Tag<Material> tempVal1 = Bukkit.getTag(Tag.REGISTRY_ITEMS, CommonUtil.parseNamespacedKey(singleMaterial), Material.class);
                        if (tempVal1 != null) {
                            float speed = Float.parseFloat(ruleParseResult[1]);
                            boolean correctForDrop = Boolean.parseBoolean(ruleParseResult[2]);
                            toolComponent.addRule(tempVal1, speed, correctForDrop);
                            continue;
                        }
                        Tag<Material> tempVal2 = Bukkit.getTag(Tag.REGISTRY_BLOCKS, CommonUtil.parseNamespacedKey(singleMaterial), Material.class);
                        if (tempVal2 != null) {
                            float speed = Float.parseFloat(ruleParseResult[1]);
                            boolean correctForDrop = Boolean.parseBoolean(ruleParseResult[2]);
                            toolComponent.addRule(tempVal2, speed, correctForDrop);
                            continue;
                        }
                        Material material = Material.getMaterial(singleMaterial.toUpperCase());
                        if (material == null) {
                            break;
                        }
                        materials.add(material);
                        i ++;
                    }
                    float speed = Float.parseFloat(ruleParseResult[i]);
                    boolean correctForDrop = Boolean.parseBoolean(ruleParseResult[i + 1]);
                    toolComponent.addRule(materials, speed, correctForDrop);
                }
                meta.setTool(toolComponent);
            }
        }

        // Jukebox
        if (CommonUtil.getMajorVersion(21)) {
            JukeboxPlayableComponent jukeboxPlayableComponent = meta.getJukeboxPlayable();
            String song = section.getString("song");
            if (song != null) {
                jukeboxPlayableComponent.setSongKey(CommonUtil.parseNamespacedKey(song));
                if (section.contains("show-song")) {
                    jukeboxPlayableComponent.setShowInTooltip(section.getBoolean("show-song"));
                }
                meta.setJukeboxPlayable(jukeboxPlayableComponent);
            }
        }

        // Fire Resistant
        if (CommonUtil.getMinorVersion(20, 5)) {
            if (section.get("fire-resistant") != null) {
                meta.setFireResistant(section.getBoolean("fire-resistant"));
            }
        }

        // Hide Tooltip
        if (CommonUtil.getMinorVersion(20, 5)) {
            if (section.get("hide-tool-tip") != null) {
                meta.setHideTooltip(section.getBoolean("hide-tool-tip"));
            }
        }

        // Glow
        if (CommonUtil.getMinorVersion(20, 5)) {
            if (section.get("glow") != null) {
                meta.setEnchantmentGlintOverride(section.getBoolean("glow"));
            }
        }

        // Unbreakable
        if (section.get("unbreakable") != null) {
            meta.setUnbreakable(section.getBoolean("unbreakable"));
        }

        // Rarity
        if (CommonUtil.getMinorVersion(20, 5)) {
            String rarityKey = section.getString("rarity");
            if (rarityKey != null) {
                meta.setRarity(Enums.getIfPresent(ItemRarity.class, rarityKey).or(ItemRarity.COMMON));
            }
        }

        // Flag
        List<String> itemFlagKey = section.getStringList("flags");
        if (!itemFlagKey.isEmpty()) {
            for (String flag : itemFlagKey) {
                flag = flag.toUpperCase();
                ItemFlag itemFlag = Enums.getIfPresent(ItemFlag.class, flag).orNull();
                if (itemFlag != null) {
                    meta.addItemFlags(itemFlag);
                }
                if (CommonUtil.getMinorVersion(20, 6) && itemFlag == ItemFlag.HIDE_ATTRIBUTES && meta.getAttributeModifiers() == null) {
                    meta.setAttributeModifiers(MultimapBuilder.hashKeys().hashSetValues().build());
                }
            }
        }

        // Enchantments
        ConfigurationSection enchantsKey = section.getConfigurationSection("enchants");
        if (enchantsKey != null) {
            for (String ench : enchantsKey.getKeys(false)) {
                Enchantment vanillaEnchant = Registry.ENCHANTMENT.get(CommonUtil.parseNamespacedKey(ench.toLowerCase()));
                if (vanillaEnchant != null) {
                    meta.addEnchant(vanillaEnchant, enchantsKey.getInt(ench), true);
                }
            }
        }

        // Attribute
        ConfigurationSection attributesKey = section.getConfigurationSection("attributes");
        if (attributesKey != null) {
            for (String attribute : attributesKey.getKeys(false)) {
                Attribute attributeInst;
                if (CommonUtil.getMinorVersion(21, 2)) {
                    attributeInst = Registry.ATTRIBUTE.get(CommonUtil.parseNamespacedKey(attribute));
                } else {
                    attributeInst = Enums.getIfPresent(Attribute.class, attribute.toUpperCase(Locale.ENGLISH)).orNull();
                }
                if (attributeInst == null) {
                    continue;
                }
                ConfigurationSection subSection = attributesKey.getConfigurationSection(attribute);
                if (subSection == null) {
                    continue;
                }
                String attribId = subSection.getString("id");
                UUID id = attribId != null ? UUID.fromString(attribId) : UUID.randomUUID();

                String attribName = subSection.getString("name");
                double attribAmount = subSection.getDouble("amount");
                String attribOperation = subSection.getString("operation");

                Collection<AttributeModifier> tempVal2 = meta.getAttributeModifiers(attributeInst);
                if (tempVal2 != null) {
                    for (AttributeModifier tempVal1 : tempVal2) {
                        meta.removeAttributeModifier(attributeInst, tempVal1);
                    }
                }

                if (CommonUtil.getMinorVersion(20, 5)) {
                    String attribSlot = subSection.getString("slot");

                    EquipmentSlotGroup slot = EquipmentSlotGroup.ANY;

                    if (attribSlot != null) {
                        EquipmentSlotGroup targetSlot = EquipmentSlotGroup.getByName(attribSlot);
                        if (targetSlot != null) {
                            slot = targetSlot;
                        }
                    }

                    if (attribName != null && attribOperation != null) {
                        AttributeModifier modifier;
                        if (CommonUtil.getMajorVersion(21)) {
                            modifier = new AttributeModifier(
                                    CommonUtil.parseNamespacedKey(attribName),
                                    attribAmount,
                                    Enums.getIfPresent(AttributeModifier.Operation.class, attribOperation)
                                            .or(AttributeModifier.Operation.ADD_NUMBER),
                                    slot);
                        } else {
                            modifier = new AttributeModifier(
                                    id,
                                    attribName,
                                    attribAmount,
                                    Enums.getIfPresent(AttributeModifier.Operation.class, attribOperation)
                                            .or(AttributeModifier.Operation.ADD_NUMBER),
                                    slot);
                        }

                        meta.addAttributeModifier(attributeInst, modifier);
                    }
                } else {
                    String attribSlot = subSection.getString("slot");

                    EquipmentSlot slot = attribSlot != null ? Enums.getIfPresent(EquipmentSlot.class, attribSlot).or(EquipmentSlot.HAND) : null;

                    if (attribName != null && attribOperation != null) {
                        AttributeModifier modifier = new AttributeModifier(
                                id,
                                attribName,
                                attribAmount,
                                Enums.getIfPresent(AttributeModifier.Operation.class, attribOperation)
                                        .or(AttributeModifier.Operation.ADD_NUMBER),
                                slot);

                        meta.addAttributeModifier(attributeInst, modifier);
                    }
                }
            }
        }

        // Damage
        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;
            int damageKey = section.getInt("damage", -1);
            if (damageKey > 0) {
                damageable.setDamage(damageKey);
            }
            if (CommonUtil.getMinorVersion(20, 5)) {
                int maxDamageKey = section.getInt("max-damage", -1);
                if (maxDamageKey > 0) {
                    damageable.setMaxDamage(maxDamageKey);
                }
            }
        }

        // Stored Enchantments
        if (meta instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta enchantmentStorageMeta = (EnchantmentStorageMeta) meta;
            ConfigurationSection storedEnchantsKey = section.getConfigurationSection("stored-enchants");
            if (storedEnchantsKey != null) {
                for (String ench : storedEnchantsKey.getKeys(false)) {
                    Enchantment vanillaEnchant = Registry.ENCHANTMENT.get(CommonUtil.parseNamespacedKey(ench.toLowerCase()));
                    if (vanillaEnchant != null) {
                        enchantmentStorageMeta.addStoredEnchant(vanillaEnchant, storedEnchantsKey.getInt(ench), true);
                    }
                }
            }
        }

        // Banner
        if (meta instanceof BannerMeta) {
            BannerMeta banner = (BannerMeta) meta;
            ConfigurationSection bannerPatternsKey = section.getConfigurationSection("patterns");

            if (bannerPatternsKey != null) {
                for (String pattern : bannerPatternsKey.getKeys(false)) {
                    PatternType type = null;
                    if (CommonUtil.getMajorVersion(21)) {
                        type = Registry.BANNER_PATTERN.get(CommonUtil.parseNamespacedKey(pattern));
                    } else {
                        type = Enums.getIfPresent(PatternType.class, pattern.toUpperCase()).or(PatternType.BASE);
                    }
                    String bannerColor = bannerPatternsKey.getString(pattern);
                    if (type != null && bannerColor != null) {
                        DyeColor color = Enums.getIfPresent(DyeColor.class, bannerColor.toUpperCase()).or(DyeColor.WHITE);
                        banner.addPattern(new Pattern(color, type));
                    }
                }
            }
        }

        // Potion
        if (meta instanceof PotionMeta) {
            PotionMeta potionMeta = (PotionMeta) meta;
            String basePotionType = section.getString("base-effect");
            if (basePotionType != null) {
                String[] singlePotion = basePotionType.replace(" ", "").split(",");
                PotionType potionType = Enums.getIfPresent(PotionType.class, singlePotion[0].toUpperCase()).orNull();
                if (CommonUtil.getMinorVersion(20, 5)) {
                    potionMeta.setBasePotionType(potionType);
                } else if (singlePotion.length == 3) {
                    potionMeta.setBasePotionData(new PotionData(potionType,
                            Boolean.parseBoolean(basePotionType.replace(" ", "").split(",")[1]),
                            Boolean.parseBoolean(basePotionType.replace(" ", "").split(",")[2])));
                }
            }
            for (String effects : section.getStringList("effects")) {
                String[] effectParseResult = effects.replace(" ", "").split(",");
                if (effectParseResult.length < 3) {
                    continue;
                }
                PotionEffectType potionEffectType = null;
                if (CommonUtil.getMajorVersion(20)) {
                    potionEffectType = Registry.POTION_EFFECT_TYPE.get(CommonUtil.parseNamespacedKey(effectParseResult[0]));
                } else {
                    potionEffectType = PotionEffectType.getByName(effectParseResult[0]);
                }
                if (potionEffectType != null) {
                    PotionEffect potionEffect = new PotionEffect(potionEffectType,
                            Integer.parseInt(effectParseResult[1]),
                            Integer.parseInt(effectParseResult[2]),
                            effectParseResult.length < 4 || Boolean.parseBoolean(effectParseResult[3]),
                            effectParseResult.length < 5 || Boolean.parseBoolean(effectParseResult[4]),
                            effectParseResult.length < 6 || Boolean.parseBoolean(effectParseResult[5]));
                    potionMeta.addCustomEffect(potionEffect, true);
                }
            }
            String potionColor = section.getString("color");
            if (potionColor != null) {
                potionMeta.setColor(CommonUtil.parseColor(potionColor));
            }
        }

        // Armor Trim
        if (CommonUtil.getMajorVersion(20)) {
            if (meta instanceof ArmorMeta) {
                ArmorMeta armorMeta = (ArmorMeta) meta;
                ConfigurationSection trim = section.getConfigurationSection("trim");
                if (trim != null) {
                    String trimMaterialKey = trim.getString("material");
                    String trimPatternKey = trim.getString("pattern");
                    if (trimMaterialKey != null && trimPatternKey != null) {
                        NamespacedKey trimMaterialNamespacedKey = CommonUtil.parseNamespacedKey(trimMaterialKey);
                        NamespacedKey trimPatternNamespacedKey = CommonUtil.parseNamespacedKey(trimPatternKey);
                        if (trimMaterialNamespacedKey != null && trimPatternNamespacedKey != null) {
                            TrimMaterial trimMaterial = Registry.TRIM_MATERIAL.get(trimMaterialNamespacedKey);
                            TrimPattern trimPattern = Registry.TRIM_PATTERN.get(trimPatternNamespacedKey);
                            if (trimMaterial != null && trimPattern != null) {
                                armorMeta.setTrim(new ArmorTrim(trimMaterial, trimPattern));
                            }
                        }
                    }
                }
            }
        }

        // Leather Armor Color
        if (meta instanceof LeatherArmorMeta leather) {
            String colorKey = section.getString("color");
            if (colorKey != null) {
                leather.setColor(CommonUtil.parseColor(colorKey));
            }
        }

        // Axolotl Bucket
        if (CommonUtil.getMinorVersion(17, 1)) {
            if (meta instanceof AxolotlBucketMeta bucket) {
                String variantStr = section.getString("color");
                if (variantStr != null) {
                    Axolotl.Variant variant = Enums.getIfPresent(Axolotl.Variant.class, variantStr.toUpperCase()).orNull();
                    if (variant != null) {
                        bucket.setVariant(variant);
                    }
                }
            }
        }

        // Tropical Fish Bucket
        if (meta instanceof TropicalFishBucketMeta tropical) {
            String colorKey = section.getString("color");
            String patternColorKey = section.getString("pattern-color");
            String patternKey = section.getString("pattern");
            if (colorKey != null && patternColorKey != null && patternKey != null) {
                DyeColor color = Enums.getIfPresent(DyeColor.class, colorKey).or(DyeColor.WHITE);
                DyeColor patternColor = Enums.getIfPresent(DyeColor.class, patternColorKey).or(DyeColor.WHITE);
                TropicalFish.Pattern pattern = Enums.getIfPresent(TropicalFish.Pattern.class, patternKey).or(TropicalFish.Pattern.BETTY);

                tropical.setBodyColor(color);
                tropical.setPatternColor(patternColor);
                tropical.setPattern(pattern);
            }
        }

        // Skull
        if (meta instanceof SkullMeta skullMeta) {
            String skullTextureNameKey = section.getString("skull-meta", section.getString("skull"));
            if (skullTextureNameKey != null) {
                if (CustomArcheology.newSkullMethod) {
                    try {
                        Class<?> profileClass = Class.forName("net.minecraft.world.item.component.ResolvableProfile");
                        Constructor<?> constroctor = profileClass.getConstructor(GameProfile.class);
                        GameProfile profile = new GameProfile(UUID.randomUUID(), "");
                        profile.getProperties().put("textures", new Property("textures", skullTextureNameKey));
                        try {
                            Method mtd = skullMeta.getClass().getDeclaredMethod("setProfile", profileClass);
                            mtd.setAccessible(true);
                            mtd.invoke(skullMeta, constroctor.newInstance(profile));
                        } catch (Exception exception) {
                            exception.printStackTrace();
                            Bukkit.getConsoleSender().sendMessage("§x§9§8§F§B§9§8[ManyouItems] §cError: Can not parse skull texture in a item!");
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    GameProfile profile = new GameProfile(UUID.randomUUID(), "");
                    profile.getProperties().put("textures", new Property("textures", skullTextureNameKey));
                    try {
                        Method mtd = skullMeta.getClass().getDeclaredMethod("setProfile", GameProfile.class);
                        mtd.setAccessible(true);
                        mtd.invoke(skullMeta, profile);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                        Bukkit.getConsoleSender().sendMessage("§x§9§8§F§B§9§8[ManyouItems] §cError: Can not parse skull texture in a item!");
                    }
                }
            }
        }

        // Firework
        if (meta instanceof FireworkMeta fireworkMeta) {

            int power = section.getInt("power");
            if (power > 0 && power < 128) {
                fireworkMeta.setPower(power);
            }

            ConfigurationSection fireworkKey = section.getConfigurationSection("firework");
            if (fireworkKey != null) {
                FireworkEffect.Builder builder = FireworkEffect.builder();
                for (String fws : fireworkKey.getKeys(false)) {
                    ConfigurationSection fw = fireworkKey.getConfigurationSection(fws);

                    if (fw != null) {
                        builder.flicker(fw.getBoolean("flicker"));
                        builder.trail(fw.getBoolean("trail"));
                        String fireworkType = fw.getString("type");
                        if (fireworkType != null) {
                            builder.with(Enums.getIfPresent(FireworkEffect.Type.class, fireworkType.toUpperCase())
                                    .or(FireworkEffect.Type.STAR));
                        }

                        ConfigurationSection colorsSection = fw.getConfigurationSection("colors");
                        if (colorsSection != null) {
                            List<Color> colors = new ArrayList<>();
                            for (String colorStr : colorsSection.getStringList("base")) {
                                colors.add(CommonUtil.parseColor(colorStr));
                            }
                            builder.withColor(colors);

                            colors = new ArrayList<>();
                            for (String colorStr : colorsSection.getStringList("fade")) {
                                colors.add(CommonUtil.parseColor(colorStr));
                            }
                            builder.withFade(colors);
                            fireworkMeta.addEffect(builder.build());
                        }
                    }
                }
            }
        }

        // Firework Effect
        if (meta instanceof FireworkEffectMeta fireworkEffectMeta) {

            ConfigurationSection fireworkKey = section.getConfigurationSection("firework");
            if (fireworkKey != null) {
                FireworkEffect.Builder builder = FireworkEffect.builder();
                builder.flicker(fireworkKey.getBoolean("flicker"));
                builder.trail(fireworkKey.getBoolean("trail"));
                String fireworkType = fireworkKey.getString("type");
                if (fireworkType != null) {
                    builder.with(Enums.getIfPresent(FireworkEffect.Type.class, fireworkType.toUpperCase())
                            .or(FireworkEffect.Type.STAR));
                }
                ConfigurationSection colorsSection = fireworkKey.getConfigurationSection("colors");
                if (colorsSection != null) {
                    List<Color> colors = new ArrayList<>();
                    for (String colorStr : colorsSection.getStringList("base")) {
                        colors.add(CommonUtil.parseColor(colorStr));
                    }
                    builder.withColor(colors);

                    colors = new ArrayList<>();
                    for (String colorStr : colorsSection.getStringList("fade")) {
                        colors.add(CommonUtil.parseColor(colorStr));
                    }
                    builder.withFade(colors);
                    fireworkEffectMeta.setEffect(builder.build());
                }
            }
        }

        // Suspicious Stew
        if (meta instanceof SuspiciousStewMeta stewMeta) {
            for (String effects : section.getStringList("effects")) {
                String[] effectParseResult = effects.replace(" ", "").split(",");
                if (effectParseResult.length < 3) {
                    continue;
                }
                PotionEffectType potionEffectType;
                if (CommonUtil.getMajorVersion(20)) {
                    potionEffectType = Registry.POTION_EFFECT_TYPE.get(CommonUtil.parseNamespacedKey(effectParseResult[0]));
                } else {
                    potionEffectType = PotionEffectType.getByName(effectParseResult[0]);
                }
                if (potionEffectType != null) {
                    PotionEffect potionEffect = new PotionEffect(potionEffectType,
                            Integer.parseInt(effectParseResult[1]),
                            Integer.parseInt(effectParseResult[2]),
                            effectParseResult.length < 4 || Boolean.parseBoolean(effectParseResult[3]),
                            effectParseResult.length < 5 || Boolean.parseBoolean(effectParseResult[4]),
                            effectParseResult.length < 6 || Boolean.parseBoolean(effectParseResult[5]));
                    stewMeta.addCustomEffect(potionEffect, true);
                }
            }
        }

        // Bundle
        if (CommonUtil.getMajorVersion(17)) {
            if (meta instanceof BundleMeta bundleMeta) {
                ConfigurationSection bundleContentKey = section.getConfigurationSection("contents");

                if (bundleContentKey != null) {
                    for (String key : bundleContentKey.getKeys(false)) {
                        ConfigurationSection contentItemSection = bundleContentKey.getConfigurationSection(key);
                        if (contentItemSection != null) {
                            bundleMeta.addItem(buildItemStack(contentItemSection,
                                    args));
                        }
                    }
                }
            }
        }

        // Block
        if (meta instanceof BlockStateMeta blockStateMeta) {
            BlockState state = blockStateMeta.getBlockState();

            if (state instanceof CreatureSpawner spawner) {
                String spawnerKey = section.getString("spawner");
                if (spawnerKey != null) {
                    EntityType entityType = Enums.getIfPresent(EntityType.class, spawnerKey.toUpperCase()).orNull();
                    if (entityType != null) {
                        spawner.setSpawnedType(entityType);
                    }
                }

                int spawnerMinDelayKey = section.getInt("min-delay", -1);
                if (spawnerMinDelayKey != -1) {
                    spawner.setMinSpawnDelay(spawnerMinDelayKey);
                }

                int spawnerMaxDelayKey = section.getInt("max-delay", -1);
                if (spawnerMaxDelayKey != -1) {
                    spawner.setMaxSpawnDelay(spawnerMaxDelayKey);
                }

                int spawnerMaxEntities = section.getInt("max-entities", -1);
                if (spawnerMaxEntities != -1) {
                    spawner.setMaxNearbyEntities(spawnerMaxEntities);
                }

                int spawnerPlayerRange = section.getInt("player-range", -1);
                if (spawnerPlayerRange != -1) {
                    spawner.setRequiredPlayerRange(spawnerPlayerRange);
                }

                int spawnerSpawnRange = section.getInt("spawn-range", -1);
                if (spawnerSpawnRange != -1) {
                    spawner.setSpawnRange(spawnerSpawnRange);
                }

                spawner.update(true);
                blockStateMeta.setBlockState(spawner);
            } else if (state instanceof ShulkerBox) {
                ConfigurationSection shulkerContentKey = section.getConfigurationSection("contents");
                if (shulkerContentKey != null) {
                    ShulkerBox box = (ShulkerBox) state;
                    for (String key : shulkerContentKey.getKeys(false)) {
                        ConfigurationSection contentItemSection = shulkerContentKey.getConfigurationSection(key);
                        if (contentItemSection != null) {
                            box.getInventory().setItem(Integer.parseInt(key), buildItemStack(contentItemSection,
                                    args));
                        }
                    }
                    box.update(true);
                    blockStateMeta.setBlockState(box);
                }
            } else if (CommonUtil.getMajorVersion(20) && state instanceof BrushableBlock brushableBlock) {
                ConfigurationSection brushableContentKey = section.getConfigurationSection("content");
                if (brushableContentKey != null) {
                    brushableBlock.setItem(buildItemStack(brushableContentKey, args));
                }
                blockStateMeta.setBlockState(brushableBlock);
            }
        }

        // Ominous Bottle
        if (CommonUtil.getMinorVersion(20, 5)) {
            if (meta instanceof OminousBottleMeta ominousBottleMeta) {
                int ominousPowerKey = section.getInt("power", -1);
                if (ominousPowerKey > 0) {
                    ominousBottleMeta.setAmplifier(ominousPowerKey);
                }
            }
        }

        // Music Instrument
        if (CommonUtil.getMajorVersion(19)) {
            if (meta instanceof MusicInstrumentMeta musicInstrumentMeta) {
                String musicKey = section.getString("music");
                if (musicKey != null) {
                    MusicInstrument musicInstrument;
                    if (CommonUtil.getMinorVersion(20, 4)) {
                        musicInstrument = Registry.INSTRUMENT.get(CommonUtil.parseNamespacedKey(musicKey));

                    } else {
                        musicInstrument = MusicInstrument.getByKey(CommonUtil.parseNamespacedKey(musicKey));
                    }
                    if (musicInstrument != null) {
                        musicInstrumentMeta.setInstrument(musicInstrument);
                    }
                }
            }
        }

        // Repairable
        if (meta instanceof Repairable) {
            Repairable repairableMeta = (Repairable) meta;
            int repairCost = section.getInt("repair-cost", -1);
            if (repairCost >= 0) {
                repairableMeta.setRepairCost(repairCost);
            }
        }

        if (CommonUtil.getMinorVersion(21, 2)) {
            // Enchantable
            int enchantable = section.getInt("enchantable", -1);
            if (enchantable >= 0) {
                meta.setEnchantable(enchantable);
            }

            // Glider
            if (section.getString("glider") != null) {
                meta.setGlider(section.getBoolean("glider"));
            }

            // Item Model
            String itemModel = section.getString("item-model", null);
            if (itemModel != null) {
                meta.setItemModel(CommonUtil.parseNamespacedKey(itemModel));
            }

            // Tooltip Style
            String tooltipStyle = section.getString("tooltip-style", null);
            if (tooltipStyle != null) {
                meta.setTooltipStyle(CommonUtil.parseNamespacedKey(tooltipStyle));
            }

            // Use Cooldown
            ConfigurationSection useCooldown = section.getConfigurationSection("use-cooldown");
            if (useCooldown != null) {
                UseCooldownComponent useCooldownComponent = meta.getUseCooldown();
                String cooldownGroup = useCooldown.getString("cooldown-group", null);
                if (cooldownGroup != null) {
                    useCooldownComponent.setCooldownGroup(CommonUtil.parseNamespacedKey(cooldownGroup));
                }
                int cooldownSeconds = useCooldown.getInt("cooldown-seconds", -1);
                if (cooldownSeconds >= 0) {
                    useCooldownComponent.setCooldownSeconds((float) cooldownSeconds);
                }
            }

            // Equippable
            ConfigurationSection equippable = section.getConfigurationSection("equippable");
            if (equippable != null) {
                EquippableComponent equippableComponent = meta.getEquippable();
                List<String> entities = equippable.getStringList("entities");
                if (!entities.isEmpty()) {
                    for (String entity : entities) {
                        Tag<EntityType> entityTag = Bukkit.getTag(Tag.REGISTRY_ENTITY_TYPES, CommonUtil.parseNamespacedKey(entity), EntityType.class);
                        if (entityTag != null) {
                            equippableComponent.setAllowedEntities(entityTag);
                        } else {
                            EntityType entityType = Registry.ENTITY_TYPE.get(CommonUtil.parseNamespacedKey(entity));
                            if (entityType != null) {
                                equippableComponent.setAllowedEntities(entityType);
                            }
                        }
                    }
                }
                if (equippable.contains("dispensable")) {
                    equippableComponent.setDispensable(equippable.getBoolean("dispensable"));
                }
                if (equippable.contains("swappable")) {
                    equippableComponent.setSwappable(equippable.getBoolean("swappable"));
                }
                if (equippable.contains("damage-on-hurt")) {
                    equippableComponent.setDamageOnHurt(equippable.getBoolean("damage-on-hurt"));
                }
                String cameraOverlay = equippable.getString("camera-overlay");
                if (cameraOverlay != null) {
                    equippableComponent.setCameraOverlay(CommonUtil.parseNamespacedKey(cameraOverlay));
                }
                String sound = equippable.getString("sound");
                if (sound != null) {
                    Sound equipSound = Registry.SOUNDS.get(CommonUtil.parseNamespacedKey(sound));
                    equippableComponent.setEquipSound(equipSound);
                }
                String model = equippable.getString("model");
                if (model != null) {
                    equippableComponent.setModel(CommonUtil.parseNamespacedKey(model));
                }
                String slot = equippable.getString("slot");
                if (slot != null) {
                    equippableComponent.setSlot(EquipmentSlot.valueOf(slot.toUpperCase()));
                }
                meta.setEquippable(equippableComponent);
            }

            // Damage Resistant
            String damageResistant = section.getString("damage-resistant");
            if (damageResistant != null) {
                Tag<DamageType> damageTypeTag = Bukkit.getTag(DamageTypeTags.REGISTRY_DAMAGE_TYPES, CommonUtil.parseNamespacedKey(damageResistant), DamageType.class);
                if (damageTypeTag != null) {
                    meta.setDamageResistant(damageTypeTag);
                }
            }
        }

        // NBT API
        ConfigurationSection nbtSection = section.getConfigurationSection("nbt");
        if (nbtSection != null && CommonUtil.checkPluginLoad("NBTAPI")) {
            for (String nbtType : nbtSection.getKeys(false)) {
                ConfigurationSection nbtTypeSection = nbtSection.getConfigurationSection(nbtType);
                if (nbtTypeSection != null) {
                    for (String nbtName : nbtTypeSection.getKeys(false)) {
                        item = NBTUtil.addNBT(item, nbtType, nbtName, nbtTypeSection.get(nbtName));
                    }
                }
            }
        }


        item.setItemMeta(meta);

        return item;
    }
}
