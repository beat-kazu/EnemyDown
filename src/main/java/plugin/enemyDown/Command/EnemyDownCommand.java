package plugin.enemyDown.Command;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import plugin.enemyDown.Main;
import plugin.enemyDown.data.PlayerScore;

/**
 * 　制限時間内にランダムで出現する敵を倒して、スコアを獲得するゲームを起動するコマンド。
 * 　スコアは敵によって変わり、倒せた敵の合計によってスコアが変動します。
 * 　結果じゃプレーヤー名、点数、日時などで保存されます。
 */
public class EnemyDownCommand extends BaseCommand implements  Listener {

  public static final int GAME_TIME = 20;
  private Main main;
  private List<PlayerScore> playerScoreList = new ArrayList<>();

  public EnemyDownCommand(Main main){
    this.main= main;
  }

  @Override
  public boolean onExecutePlayerCommand(Player player) {
    PlayerScore nowPlayerScore = getPlayerScore(player);
    player.sendTitle("**　Game start　**","5秒間の挑戦です！",0,2*20,0);

    initPlayerStatus(player);

    EntityType enemy_dbg = getEnemy();
    player.sendMessage("敵は" + enemy_dbg + "に設定されました");

    gamePlay(player, nowPlayerScore, enemy_dbg);
    return true;
  }



  @Override
  public boolean onExecuteNPCCommand(CommandSender sender) {
    return false;
  }

  @EventHandler
    public void onEnemyDeath(EntityDeathEvent e) {
    LivingEntity enemy = e.getEntity();
    Player player = enemy.getKiller();
    if (Objects.isNull(player) || playerScoreList.isEmpty()) {
      return;
    }
    for (PlayerScore playerScore : playerScoreList) {
      if (playerScore.getPlayerName().equals(player.getName())) {
        if(playerScore.getGameTime()>0){
          int point = switch (enemy.getType()) {
            case ZOMBIE -> 10;
            case SKELETON, SPIDER -> 20;
            default -> 0;
          };
         playerScore.setScore(playerScore.getScore() + point);
         player.sendMessage("敵を倒した！　現在のスコアは" + playerScore.getScore() + "点！");
        }
      }
    }
  }

  /**
   * 現在実行しているプレーヤーのスコア情報を取得する
   * @param player　コマンドを実行したプレーヤー
   * @return　現在実行しているプレーヤーのスコア情報
   */
  private PlayerScore getPlayerScore(Player player) {
    PlayerScore playerScore = new PlayerScore(player.getName());
    if(playerScoreList.isEmpty()) {
      playerScore = addNewPlayer(player);
    }else{
       playerScore = playerScoreList.stream()
           .findFirst()
           .map(ps -> ps.getPlayerName().equals(player.getName())
          ? ps
          : addNewPlayer(player)).orElse(playerScore);
    }
    playerScore.setGameTime(GAME_TIME);
    return playerScore;
  }
  /**
   * 新規のプレーヤー情報をリストに追加します。
   * @param player コマンドを実行したプレーヤー
   * @return 新規プレーヤー
   */
  private PlayerScore addNewPlayer(Player player) {
    PlayerScore newPlayer = new PlayerScore(player.getName());
    playerScoreList.add(newPlayer);
    return newPlayer;
  }

  /**
   *  ゲームを始める前にプレーヤーの状態を設定する。
   *  体力と空腹度を最大にして、装備はネザライト一式になる。
   * @param player　コマンドを実行したプレーヤー
   */
  private static void initPlayerStatus(Player player) {
    player.setHealth(20);
    player.setFoodLevel(20);
    PlayerInventory inventory = player.getInventory();
    inventory.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
    inventory.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
    inventory.setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
    inventory.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
    inventory.setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
  }

  /**
   * ゲームを実行します。規定の時間内に敵を倒すとスコアが加算されます。合計スコアを時間経過後に表示します。
   * @param player コマンドを実行したプレイヤー
   * @param nowPlayer　プレーヤースコア情報
   * @param enemy_dbg　デバッグ表示用の敵情報
   */
  private void gamePlay(Player player, PlayerScore nowPlayer, EntityType enemy_dbg) {
    Bukkit.getScheduler().runTaskTimer(main,Runnable -> {
      if(nowPlayer.getGameTime() <=0){
        Runnable.cancel();
        player.sendTitle("ゲームが終了しました。",
            nowPlayer.getPlayerName()+" 合計 "+ nowPlayer.getScore() + "点！",
            0,60,0);
        nowPlayer.setScore(0);
        List<Entity> nearbyEntities = player.getNearbyEntities(50, 0, 50);
        for(Entity enemy : nearbyEntities){
          switch (enemy.getType()) {
            case ZOMBIE, SKELETON, SPIDER -> enemy.remove();
          }
        }
        return;
      }
      player.getWorld().spawnEntity(getEnemySpawnLocation(player), enemy_dbg);
      nowPlayer.setGameTime(nowPlayer.getGameTime()-5);
    },0,5*20);
  }

  /**
   * 敵の出現場所を取得します。
   * 出現エリアはX軸とZ軸は自分の位置からプラス、ランダムで-10~9の値が設定されます。
   * Y軸はプレイヤーと同じ位置になります。
   * @param player　コマンドを実行したプレイヤー
   * @return　敵の出現場所
   */
  private  Location getEnemySpawnLocation(Player player) {
    Location playerLocation = player.getLocation();
    int randomX = new SplittableRandom().nextInt(10) -4;
    int randomZ = new SplittableRandom().nextInt(10) -4;
    double x = playerLocation.getX()+randomX;
    double y = playerLocation.getY();
    double z = playerLocation.getZ()+randomZ;

    player.sendMessage("X軸は" + String.valueOf(randomX) + "に設定されました");
    player.sendMessage("Z軸は" + String.valueOf(randomZ) + "に設定されました");
    return new Location(player.getWorld(),x,y,z);
  }
  /**
   * ランダムで敵を抽出して、その結果を敵を取得します。
   * @return　敵
   */
  private  EntityType getEnemy() {
    List<EntityType>enemyList = List.of(EntityType.ZOMBIE, EntityType.SKELETON,EntityType.SPIDER);
    return enemyList.get(new SplittableRandom().nextInt(enemyList.size()));

  }
}


