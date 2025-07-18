package plugin.enemyDown.Command;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import plugin.enemyDown.Main;

class EnemyDownCommandTest {

  EnemyDownCommand sut;

  @Mock
  Main main;

  @Mock
  Player player;

  @BeforeEach
  void before(){
     sut = new EnemyDownCommand(main);

  }

  @Test
  void getdifficultyに渡す引数のargsの最初の文字列がeasyの時にeasyの文字列が返ること(){
    String actual = sut.getdifficulty(player, new String[]{"easy"});
    Assertions.assertEquals("easy" , actual);
  }

  @Test
  void getdifficultyに渡す引数のargsの最初の文字列がnormalの時にnormalの文字列が返ること(){
    String actual = sut.getdifficulty(player, new String[]{"normal"});
    Assertions.assertEquals("normal" , actual);
  }

}