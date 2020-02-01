package me.arasple.mc.trhologram.action.base;

import io.izzel.taboolib.internal.apache.lang3.math.NumberUtils;
import io.izzel.taboolib.util.lite.Numbers;
import me.arasple.mc.trhologram.TrHologram;
import me.arasple.mc.trhologram.utils.JavaScript;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Arasple
 * @date 2019/12/21 20:39
 */
public abstract class AbstractAction {

    private String content;
    private HashMap<EnumOption, String> options;

    public abstract String getName();

    public void run(Player player) {
        if (options.containsKey(EnumOption.CHANCE) && !Numbers.random(NumberUtils.toDouble(options.get(EnumOption.CHANCE), 1))) {
            return;
        }
        if (options.containsKey(EnumOption.REQUIREMENT) && !(boolean) JavaScript.run(player, options.get(EnumOption.REQUIREMENT))) {
            return;
        }
        if (options.containsKey(EnumOption.PLAYERS)) {
            Bukkit.getOnlinePlayers().stream().filter(p -> (boolean) JavaScript.run(p, options.get(EnumOption.PLAYERS))).collect(Collectors.toList()).forEach(x -> onExecute(x, getContent() != null ? PlaceholderAPI.setBracketPlaceholders(player, getContent()) : null));
            return;
        }
        if (options.containsKey(EnumOption.DELAY)) {
            int delay = NumberUtils.toInt(options.get(EnumOption.DELAY), -1);
            if (delay > 0) {
                Bukkit.getScheduler().runTaskLaterAsynchronously(TrHologram.getPlugin(), () -> {
                    if (options.containsKey(EnumOption.PLAYERS)) {
                        Bukkit.getOnlinePlayers().stream().filter(p -> (boolean) JavaScript.run(p, getContent())).collect(Collectors.toList()).forEach(this::onExecute);
                        return;
                    }
                    onExecute(player);
                }, delay);
            }
            return;
        }

        onExecute(player);
    }

    public void onExecute(Player player, String content) {
        if (content != null) {
            String temp = getContent();
            setContent(content);
            onExecute(player);
            setContent(temp);
        } else {
            onExecute(player);
        }
    }

    /**
     * 供重写的动作内容
     *
     * @param player 执行玩家
     */
    public void onExecute(Player player) {
    }

    /*
    GETTERS && SETTERS
     */

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public HashMap<EnumOption, String> getOptions() {
        return options;
    }

    public void setOptions(HashMap<EnumOption, String> options) {
        this.options = options;
    }

    public String getOptionsAsString() {
        List<String> s = new ArrayList<>();
        options.forEach((type, value) -> s.add(type.toString(value)));
        return s.toString();
    }

    public AbstractAction create() {
        try {
            return getClass().getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        return "AbstractAction{" +
                "name='" + this.getClass().getName() + '\'' +
                "content='" + content + '\'' +
                ", options=" + options +
                '}';
    }

}
