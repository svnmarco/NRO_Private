package com.girlkun.models.pariry;

import java.util.ArrayList;
import java.util.List;

import com.girlkun.models.player.Player;
import com.girlkun.utils.Util;

public class Pariry {

  private int id;
  private String session;
  private int number_1;
  private int number_2;
  private int number_3;

  private List<Player> playersOdd;
  private List<Player> playersEven;

  private int sumEven = 0;
  private int sumOdd = 0;

  // 0 = even, 1 = odd
  private boolean isOdd;

  public Pariry() {

  }

  public Pariry(int id, int number_1, int number_2, int number_3, boolean isOdd) {
    this.id = id;
    this.session = "#" + id;
    this.number_1 = number_1;
    this.number_2 = number_2;
    this.number_3 = number_3;
    this.isOdd = isOdd;
    playersEven = new ArrayList<>();
    playersOdd = new ArrayList<>();
  }

  public void setNumber(int[] numbers) {
    this.number_1 = numbers[0];
    this.number_2 = numbers[1];
    this.number_3 = numbers[2];
    if ((number_1 + number_2 + number_3) % 2 == 0) {
      isOdd = false;
    } else {
      isOdd = true;
    }
  }

  public int checkPlayer(Player player) {
    for (Player p : playersEven) {
      if (p.equals(player)) {
        return 0;
      }
    }
    for (Player p : playersOdd) {
      if (p.equals(player)) {
        return 1;
      }
    }
    return -1;
  }

  public void generatedResult() {
    number_1 = Util.nextInt(1, 6);
    number_2 = Util.nextInt(1, 6);
    number_3 = Util.nextInt(1, 6);
    setNumber(getNumbers());
  }

  public void setId(int id) {
    this.id = id;
  }

  public void addPlayerEven(Player player) {
    playersEven.add(player);
    sumEven += player.cuoc;
  }

  public void addPlayerOdd(Player player) {
    playersOdd.add(player);
    sumOdd += player.cuoc;
  }

  public int getSumEven() {
    return sumEven;
  }

  public int getSumOdd() {
    return sumOdd;
  }

  public List<Player> getPlayersEven() {
    return playersEven;
  }

  public List<Player> getPlayersOdd() {
    return playersOdd;
  }

  public int getId() {
    return id;
  }

  public String getSession() {
    return session;
  }

  public int[] getNumbers() {
    return new int[] { number_1, number_2, number_3 };
  }

  public boolean getResult() {
    // 0 = even, 1 = odd
    return isOdd;
  }

}
