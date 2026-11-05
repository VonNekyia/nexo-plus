package de.mcterranova.nexocoreprotect;

import org.bukkit.plugin.java.JavaPlugin;

public final class NexoCoreProtect extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new FurnitureLog(), this);
    }
}
