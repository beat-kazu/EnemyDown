package plugin.enemyDown.data;

import lombok.Getter;
import lombok.Setter;

/**
 * EnemyDown のゲームを実行する際のプレイヤー情報を扱うプロジェクト
 * プレーヤー名、合計点数、日時などの情報を持つ。
 */
@Getter
@Setter
public class ExecutingPlayer {

  private String playerName;
  private int score;
  private int gameTime;

  public ExecutingPlayer(String playerName) {
    this.playerName = playerName;
  }
}