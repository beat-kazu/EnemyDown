package plugin.enemyDown;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import plugin.enemyDown.Command.EnemyDownCommand;
import plugin.enemyDown.Command.EnemySpawnCommand;

public final class Main extends JavaPlugin  {

  @Override
  public void onEnable() {
    EnemyDownCommand enemyDownCommand = new EnemyDownCommand(this);
    Bukkit.getPluginManager().registerEvents(enemyDownCommand, this);
    getCommand("enemyDown").setExecutor(enemyDownCommand);

    EnemySpawnCommand enemySpawnCommand = new EnemySpawnCommand();
    Bukkit.getPluginManager().registerEvents(enemySpawnCommand, this);
    getCommand("enemySpawn").setExecutor(enemySpawnCommand);
   }
}
