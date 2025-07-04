package plugin.enemyDown.Command;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SplittableRandom;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
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
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import plugin.enemyDown.Main;
import plugin.enemyDown.data.PlayerScore;

/**
 * 　制限時間内にランダムで出現する敵を倒して、スコアを獲得するゲームを起動するコマンド。
 * 　スコアは敵によって変わり、倒せた敵の合計によってスコアが変動します。
 * 　結果はプレーヤー名、点数、日時などで保存されます。
 */
public class EnemyDownCommand extends BaseCommand implements  Listener {

  public static final int GAME_TIME = 20;
  public static final String EASY = "easy";
  public static final String NORMAL = "normal";
  public static final String HARD = "hard";
  public static final String NONE = "none";
  public static final String LIST = "list";

  private Main main;
  private List<PlayerScore> playerScoreList = new ArrayList<>();
  private List<Entity> spawnEntityList = new ArrayList<>();

  public EnemyDownCommand(Main main){
    this.main= main;
  }

  @Override
  public boolean onExecutePlayerCommand(Player player, @NotNull Command command,
      @NotNull String label, @NotNull String[] args) {
    if(args.length ==1 && LIST.equals(args[0]) ){
      try(Connection con = DriverManager.getConnection(
          "jdbc:mysql://localhost:3306/spigot_server",
          "root",
          "password");

          Statement statement = con.createStatement();
          ResultSet resultset = statement.executeQuery( "select * from player_score;")){
        while(resultset.next()){
          int id = resultset.getInt("id");
          String name = resultset.getString("player_name");
          int score = resultset.getInt("score");
          String difficulty = resultset.getString("difficulty");

          DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
          LocalDateTime date = LocalDateTime.parse(resultset.getString("registered_at"), formatter);

          player.sendMessage(id + " | " + name + " | " + difficulty + " | " + date.format(formatter));
        }
      }catch (SQLException e){
        e.printStackTrace();
      }
      return  false;
    }

    String difficulty = getdifficulty(player, args);
    if(difficulty.equals(NONE)){
      return false;
    }

    PlayerScore nowPlayerScore = getPlayerScore(player);
    player.sendTitle("**　Game start　**","5秒間の挑戦です！",0,2*20,0);

    initPlayerStatus(player);

    EntityType enemy_dbg = getEnemy(difficulty);
    player.sendMessage("敵は" + enemy_dbg + "に設定されました");

    gamePlay(player, nowPlayerScore, enemy_dbg,difficulty);
    return true;
  }

  /**
   * 難易度をコマンド引数から取得します。
   * @param player　コマンドを実行したプレイヤー
   * @param args　　コマンド引数
   * @return
   */
  private String getdifficulty(Player player, String[] args) {
    if(args.length ==1 && (EASY.equals(args[0]) || NORMAL.equals(args[0])  || HARD.equals(args[0]))){
      return args[0];
    }
    player.sendMessage(ChatColor.RED + "実行できません。コマンド引数の1つ目に難易度設定が必要です。[easy ,normal,hard]");
    return NONE;
  }


  @Override
  public boolean onExecuteNPCCommand(CommandSender sender, @NotNull Command command,
      @NotNull String label, @NotNull String[] args) {
    return false;
  }

  @EventHandler
    public void onEnemyDeath(EntityDeathEvent e) {
    LivingEntity enemy = e.getEntity();
    Player player = enemy.getKiller();


    if (Objects.isNull(player) || spawnEntityList.stream().noneMatch(entity -> entity.equals(enemy))) {
      return;
    }

    playerScoreList.stream()
        .filter(p -> p.getPlayerName().equals(player.getName()))
        .findFirst()
        .ifPresent(p -> {
          int point = switch (enemy.getType()) {
            case ZOMBIE -> 10;
            case SKELETON, WITCH -> 20;
            default -> 0;
          };
          p.setScore(p.getScore() + point);
          player.sendMessage("敵を倒した！　現在のスコアは" + p.getScore() + "点！");
        });
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
    playerScore.setScore(0);
    removePotionEffect(player);
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
   * @param nowPlayerScore　プレーヤースコア情報
   * @param enemy_dbg　デバッグ表示用の敵情報
   * @param difficulty 難易度
   */
  private void gamePlay(Player player, PlayerScore nowPlayerScore, EntityType enemy_dbg, String difficulty) {
    Bukkit.getScheduler().runTaskTimer(main,Runnable -> {
      if(nowPlayerScore.getGameTime() <=0){
        Runnable.cancel();

        player.sendTitle("ゲームが終了しました。",
            nowPlayerScore.getPlayerName()+" 合計 "+ nowPlayerScore.getScore() + "点！",
            0,60,0);

        try(Connection con = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/spigot_server",
            "root",
            "password");
            Statement statement = con.createStatement()) {

          statement.executeUpdate(
              "insert player_score(player_name,score,difficulty,registered_at)"
                  + "values('" + nowPlayerScore.getPlayerName() + "', " + nowPlayerScore.getScore() + ", '"
                  + difficulty + "', now());");
        }catch (SQLException e){
          e.printStackTrace();
        }


        spawnEntityList.forEach(Entity::remove);
        spawnEntityList.clear();

        removePotionEffect(player);
        return;
      }
      Entity spwanEntity = player.getWorld().spawnEntity(getEnemySpawnLocation(player), enemy_dbg);
      spawnEntityList.add(spwanEntity);
      nowPlayerScore.setGameTime(nowPlayerScore.getGameTime()-5);
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
   * @param difficulty 難易度
   * @return　敵
   */
  private  EntityType getEnemy(String difficulty) {
    List<EntityType>enemyList = switch (difficulty) {
      case NORMAL -> List.of(EntityType.ZOMBIE, EntityType.SKELETON);
      case HARD -> List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.WITCH);
      default -> List.of(EntityType.ZOMBIE);
      };
     return enemyList.get(new SplittableRandom().nextInt(enemyList.size()));

  }

  /**
   * プレイヤーに設定されている特殊効果を除外します。
   * @param player コマンドを実行したプレーヤ
   */
  private void removePotionEffect(Player player) {
    player.getActivePotionEffects().stream()
        .map(PotionEffect::getType)
        .forEach(player::removePotionEffect);
  }
}


