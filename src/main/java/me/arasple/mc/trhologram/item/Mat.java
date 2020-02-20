package me.arasple.mc.trhologram.item;

import io.izzel.taboolib.internal.apache.lang3.math.NumberUtils;
import io.izzel.taboolib.util.Strings;
import io.izzel.taboolib.util.lite.Materials;
import me.arasple.mc.trhologram.TrHologram;
import me.arasple.mc.trhologram.utils.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Arasple
 * @date 2020/1/14 21:48
 * 物品材质 + 物品选项
 * 物品选项：
 * 头颅玩家（变量支持）<head:>
 * 自定义纹理 <skull:>
 * [旧] 数据值 Datavalue
 * [新] ModelData
 * 染色 [皮革]
 * 染色
 * JavaScript
 */
public class Mat {

    private ItemStack staticItem;
    private String mat;
    private Option option;
    private String optionValue;

    public Mat(String rawMat) {
        String[] options = readOption(rawMat);
        if (options != null) {
            this.option = Option.valueOf(options[0]);
            this.optionValue = options[1];
        }
        if (option == null) {
            option = Option.ORIGINAL;
        }
        this.mat = rawMat.replaceAll("<([^<>].+)>", "").replaceAll(" ", "");

        if (JsonItem.isJson(this.mat)) {
            option = option != Option.VARIABLE ? Option.JSON : Option.VARIABLE;
            if (option == Option.JSON) {
                staticItem = JsonItem.fromJson(rawMat);
            }
        }
    }

    public static Mat mat(String text) {
        if (text.toLowerCase().startsWith("item:") || text.toLowerCase().startsWith("icon:")) {
            return new Mat(text.substring(5));
        }
        return null;
    }

    public static String[] readOption(String option) {
        for (Option value : Option.values()) {
            Matcher matcher = value.matcher(option);
            if (matcher.find()) {
                String optValue = null;
                String[] args = matcher.group().split(":", 2);
                if (args.length >= 2) {
                    optValue = args[1];
                }
                return new String[]{value.name(), optValue != null ? optValue.substring(0, optValue.length() - 1) : null};
            }
        }
        return null;
    }

    public ItemStack createItem(Player player) {
        if (staticItem != null) {
            return staticItem.clone();
        }
        if (option == Option.VARIABLE && JsonItem.isJson(mat)) {
            return JsonItem.fromJson(Vars.replace(player, mat));
        }
        ItemStack item;
        ItemMeta meta;
        // ORIGINAL
        if (Strings.nonEmpty(mat)) {
            String[] args = (option == Option.VARIABLE ? Vars.replace(player, mat) : mat).split(":");
            String[] read = readMaterial(new String[]{args[0], args.length > 1 ? args[1] : null});
            item = new ItemStack(Material.valueOf(read[0]));
            meta = item.getItemMeta();
            if (!Materials.isNewVersion() && !Strings.isEmpty(read[1]) && option != Option.DATA_VALUE) {
                item.setDurability(NumberUtils.toShort(read[1], (short) 0));
            } else if (option == Option.DATA_VALUE) {
                item.setDurability(NumberUtils.toShort(optionValue, (short) 0));
            } else if (option == Option.MODEL_DATA) {
                meta.setCustomModelData(NumberUtils.toInt(optionValue, 0));
            } else if (option == Option.DYE_LEATHER) {
                if (meta instanceof LeatherArmorMeta) {
                    Dyer.setLeather((LeatherArmorMeta) meta, optionValue);
                }
            } else if (option == Option.BANNER) {
                item = Materials.WHITE_BANNER.parseItem();
                meta = item.getItemMeta();
                if (meta instanceof BannerMeta) {
                    Dyer.setBanner((BannerMeta) meta, Arrays.asList(optionValue.split(",")));
                }
            }
            item.setItemMeta(meta);
            if (option != Option.VARIABLE) {
                staticItem = item;
            }
            return item;
        }
        // TEXTURED SKULL
        else if (option == Option.TEXTURE_SKULL) {
            staticItem = Skulls.getCustomSkull(optionValue);
            return staticItem.clone();
        }
        // PLAYER HEAD
        else if (option == Option.HEAD) {
            return Skulls.getPlayerSkull(Vars.replace(player, optionValue));
        }
        // HDB
        else if (option == Option.HEAD_DATABASE) {
//            if (!HookHeadDatabase.isHoooked()) {
//                TLocale.sendToConsole("ERROR.HDB", mat);
//                return null;
//            }
//            return HookHeadDatabase.getItem(optionValue);
        }
        // JAVASCRIPT
        else if (option == Option.JAVA_SCRIPT) {
            Object object = JavaScript.run(player, optionValue);
            if (object instanceof ItemStack) {
                return ((ItemStack) object).clone();
            }
        }
        return null;
    }

    public Option getOption() {
        return option;
    }

    private String[] readMaterial(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                continue;
            }
            args[i] = args[i].replace(' ', '_').toUpperCase();
        }
        if (args.length >= 1) {
            String id = args.length >= 2 && NumberUtils.isCreatable(args[1]) ? args[1] : null;
            if (NumberUtils.isCreatable(args[0])) {
                return new String[]{
                        Arrays.stream(Material.values()).filter(m -> m.getId() == NumberUtils.toInt(args[0], 1)).findFirst().orElse(Material.STONE).name(),
                        id
                };
            } else {
                try {
                    return new String[]{Material.valueOf(args[0]).name(), id};
                } catch (IllegalArgumentException e) {
                    Materials mats = Arrays.stream(Materials.values())
                            .filter(m -> Strings.similarDegree(m.name(), args[0]) > TrHologram.SETTINGS.getDouble("OPTIONS.MATERIAL-SIMILAR-DEGREE", 0.8))
                            .max(Comparator.comparingDouble(x -> Strings.similarDegree(x.name(), args[0])))
                            .orElse(Materials.STONE);
                    return new String[]{mats
                            .parseMaterial()
                            .name(),
                            id == null ? String.valueOf(mats.getData()) : id
                    };
                }
            }
        }
        return new String[]{Material.STONE.name(), null};
    }

    @Override
    public String toString() {
        return "Mat{" +
                "staticItem=" + staticItem +
                ", mat='" + mat + '\'' +
                ", option=" + option +
                ", optionValue='" + optionValue + '\'' +
                '}';
    }

    public enum Option {

        /**
         * Example
         * *
         * <head:Arasple>
         * <player-head:Arasple>
         */
        HEAD("<((player|variable)?(-)?head):(.+)?>"),

        /**
         * Example
         * *
         * <skull:Arasple>
         * <custom-head:Arasple>
         */
        TEXTURE_SKULL("<(((custom|texture)?(-)?skull)|custom-head)(:)?(.+)?>"),

        /**
         * Example
         * *
         * <id-value:3>
         * <data-value:2>
         * <data:1>
         * <value:1>
         */
        DATA_VALUE("<(((data|id)?(-)?value)|data|value)(:)?([0-9]+[.]?[0-9]*>)"),

        /**
         * Example
         * *
         * <model-value:3>
         * <model-data:3>
         */
        MODEL_DATA("<((model(-)?(value|data)))(:)?([0-9]+[.]?[0-9]*>)"),

        /**
         * Example
         * *
         * <dye:0,0,0>
         * <dye-leather:3,2>
         */

        DYE_LEATHER("<dye(-)?(leather)?:( )?([0-9]+[,]+[0-9]+[,]+[0-9]*>)"),

        //

        /**
         * Example
         * *
         * <banner:RED SQUARE_BOTTOM_LEFT,WHITE>
         * <banner-style:RED SQUARE_BOTTOM_LEFT,WHITE>
         */
        BANNER("<banner(-)?(dye|color|style)?:( )?(.+>)"),

        /**
         * Example
         * *
         * <head-database:13434>
         * <hdb:435345534>
         */
        HEAD_DATABASE("<((head(-)?(database))|(hdb)):( )?(([0-9]|random)+>)"),

        /**
         * Example
         * *
         * <oraxen:ID>
         */
        ORAXEN("<oraxen:(.+)?>"),

        /**
         * Get ItemStack by JavaScript
         * *
         * <js-item:SCRIPT>
         */
        JAVA_SCRIPT("<((javascript|js)?(-)?item):(.+)?>"),

        /**
         * Example
         * *
         * <Variable>
         */
        VARIABLE("<variable>"),

        JSON("<JSON>"),

        ORIGINAL("<ORIGINAL>");

        private Pattern pattern;

        Option(String pattern) {
            this.pattern = Pattern.compile("(?i)" + pattern);
        }

        public Matcher matcher(String content) {
            return pattern.matcher(content);
        }
    }

}
