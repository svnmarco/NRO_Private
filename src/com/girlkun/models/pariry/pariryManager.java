package com.girlkun.models.pariry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.girlkun.database.GirlkunDB;
import com.girlkun.models.npc.Npc;
import com.girlkun.models.npc.NpcManager;
import com.girlkun.models.player.Player;
import com.girlkun.server.Client;
import com.girlkun.server.Maintenance;
import com.girlkun.server.ServerManager;
import com.girlkun.server.ServerNotify;
import com.girlkun.services.ChatGlobalService;
import com.girlkun.services.MapService;
import com.girlkun.services.Service;
import com.girlkun.utils.Logger;
import com.girlkun.utils.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class pariryManager implements Runnable {
    public int goldChan;
    public int goldLe;
    private static pariryManager instance;
    public static List<Pariry> pariryList;
    public static Pariry currentPariry;
    public static int TIME_RENEW = 100; // seconds

    private static int[] numbersBefore = new int[3];
    private static boolean resultBefore = false;
    private static int idBefore = -1;

    public static int time; // seconds


    private pariryManager() {
        pariryList = new ArrayList<>();
    }

    public static pariryManager gI() {
        if (instance == null) {
            instance = new pariryManager();
        }
        return instance;
    }

    public void addPlayerEven(Player player) {
        if (currentPariry != null) {
            currentPariry.addPlayerEven(player);
        }
    }

    public void addPlayerOdd(Player player) {
        if (currentPariry != null) {
            currentPariry.addPlayerOdd(player);
        }
    }

    public String getHistoryGame() {
        StringBuffer sb = new StringBuffer("");

        try (
                Connection conn = GirlkunDB.getConnection(); PreparedStatement ps = conn
                .prepareStatement("SELECT * FROM pariry_session ORDER BY time DESC LIMIT 20");) {

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int i = rs.getInt("id");
                    sb.append("#").append(i).append(". ");
                    java.sql.Timestamp timestamp = rs.getTimestamp("time");
                    int hour = timestamp.getHours();
                    int minute = timestamp.getMinutes();
                    int second = timestamp.getSeconds();
                    int day = timestamp.getDate();
                    int month = timestamp.getMonth() + 1;
                    int year = timestamp.getYear() + 1900;
                    sb.append(hour).append(":").append(minute).append(":").append(second).append(" - ");
                    sb.append(day).append("/").append(month).append("/").append(year).append(" | ");
                    int number_1 = rs.getInt("number_1");
                    int number_2 = rs.getInt("number_2");
                    int number_3 = rs.getInt("number_3");
                    sb.append(number_1).append(" - ").append(number_2).append(" - ").append(number_3).append(" | ");
                    sb.append(" Kết quả: ");
                    if (rs.getBoolean("result")) {
                        sb.append("Lẻ");
                    } else {
                        sb.append("Chẵn");
                    }
                    sb.append("\n");
                    // i++;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    public String getHistoryPlayer(Player player) {
        StringBuffer sb = new StringBuffer("");

        try (
                Connection conn = GirlkunDB.getConnection(); PreparedStatement ps = conn
                .prepareStatement("SELECT * FROM pariry_players WHERE id_player = ? ORDER BY time DESC LIMIT 10");) {
            ps.setInt(1, (int) player.id);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int i = rs.getInt("id_session");
                    sb.append("#").append(i).append(". ");
                    java.sql.Timestamp timestamp = rs.getTimestamp("time");
                    int hour = timestamp.getHours();
                    int minute = timestamp.getMinutes();
                    int second = timestamp.getSeconds();
                    int day = timestamp.getDate();
                    int month = timestamp.getMonth() + 1;
                    int year = timestamp.getYear() + 1900;
                    sb.append(hour).append(":").append(minute).append(":").append(second).append(" - ");
                    sb.append(day).append("/").append(month).append("/").append(year).append(" | ");
                    if (rs.getBoolean("is_odd")) {
                        sb.append("Lẻ").append(" | ");
                    } else {
                        sb.append("Chẵn").append(" | ");
                    }
                    if (rs.getBoolean("is_win")) {
                        sb.append("Thắng").append(" | ");
                    } else {
                        sb.append("Thua").append(" | ");
                    }
                    sb.append(rs.getInt("ruby_quantity")).append("\n");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    public void addPariry(Pariry pariry) {
        pariryList.add(pariry);
    }

    public void removePariry(Pariry pariry) {
        pariryList.remove(pariry);
    }

    public List<Pariry> getAllPariries() {
        return pariryList;
    }

    @Override
    public void run() {
        Npc npc = NpcManager.getNpc((byte) 80);
        while (ServerManager.isRunning) {
            try {
                if (currentPariry == null && !Maintenance.isRuning) {
                    PlayNewSession();
                }

                while (time > 0) {
                    time--;
                    List<Player> players = MapService.gI().getAllPlayerInMap(5);
                    Thread.sleep(1000);
                }

                finishSession();
                setRubyWin();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void setRubyWin() {
        try {
            getRubyWinAllPlayerNotYetReward();
        } catch (Exception e) {
            Logger.error(e.getMessage());
        }
    }

    public boolean addSession(Pariry pariry) {
        int[] numbers = pariry.getNumbers();
        boolean result = pariry.getResult();
        try (Connection conn = GirlkunDB.getConnection(); PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO `pariry_session`(`number_1`, `number_2`, `number_3`, `result`) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) { // Set the option to return generated keys

            ps.setInt(1, numbers[0]);
            ps.setInt(2, numbers[1]);
            ps.setInt(3, numbers[2]);
            ps.setBoolean(4, result);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Lỗi khi add session");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    pariry.setId(generatedId);
                } else {
                    throw new SQLException("Creating pariry session failed, no ID obtained.");
                }
            }
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public void PlayNewSession() {
        Pariry newPariry = new Pariry(-1, -1, -1, -1, false);
        currentPariry = newPariry;
        addPariry(currentPariry);
        time = TIME_RENEW;
    }

    public void finishSession() {
        if (currentPariry == null) {
            return;
        }
        currentPariry.generatedResult();
        if (addSession(currentPariry)) {
            // System.out.println(currentPariry.getId() + "\n");
            // System.out.println("Kết quả phiên chẵn lẻ: " + numbers[0] + " - " +
            // numbers[1] + " - " + numbers[2] + " - "
            // + currentPariry.getResult() + "\n");
            numbersBefore = currentPariry.getNumbers();
            resultBefore = currentPariry.getResult();
            idBefore = currentPariry.getId();
            if (currentPariry.getResult() == true) {
                if (currentPariry.getPlayersOdd() != null && currentPariry.getPlayersOdd().size() > 0) {
                    for (Player pl : currentPariry.getPlayersOdd()) {
                        // System.out.println("Player: " + pl.id + " - " + pl.cuoc + " lẻ - thắng");
                        addPlayerToNewSession(pl, true, true);
                        ChatGlobalService.gI().chat(pl,pl.name + " đã thắng " + Util.format(pl.cuoc * 1.8) + " thỏi vàng");
                        Service.gI().sendThongBaoOK(pl,"Bạn đã thắng " + Util.format(pl.cuoc * 1.8) + " thỏi vàng");
                        pl.cuoc = 0;
                        pariryManager.gI().goldChan = 0;
                        pariryManager.gI().goldLe = 0;
                    }
                }
                if (currentPariry.getPlayersEven() != null && currentPariry.getPlayersEven().size() > 0) {
                    for (Player pl : currentPariry.getPlayersEven()) {
                        // System.out.println("Player: " + pl.id + " - " + pl.cuoc + " chẵn - thua");
                        addPlayerToNewSession(pl, false, false);
                        Service.gI().sendThongBaoOK(pl, "Kết quả là lẻ, bạn đã thua " + pl.cuoc1 + " thỏi vàng");
                        pl.cuoc1 = 0;
                        pariryManager.gI().goldChan = 0;
                        pariryManager.gI().goldLe = 0;
                    }
                }
            } else {
                if (currentPariry.getPlayersEven() != null && currentPariry.getPlayersEven().size() > 0) {
                    for (Player pl : currentPariry.getPlayersEven()) {
                        // System.out.println("Player: " + pl.id + " - " + pl.cuoc + " chẵn - thắng");
                        addPlayerToNewSession(pl, true, false);
                        ChatGlobalService.gI().chat(pl,pl.name + " đã thắng " + Util.format(pl.cuoc1 * 1.8) + " thỏi vàng");
                        Service.gI().sendThongBaoOK(pl, "Bạn đã thắng " + Util.format(pl.cuoc1 * 1.8) + " thỏi vàng");
                        pl.cuoc1 = 0;
                        pariryManager.gI().goldChan = 0;
                        pariryManager.gI().goldLe = 0;
                    }
                }
                if (currentPariry.getPlayersOdd() != null && currentPariry.getPlayersOdd().size() > 0) {
                    for (Player pl : currentPariry.getPlayersOdd()) {
                        // System.out.println("Player: " + pl.id + " - " + pl.cuoc + " lẻ - thua");
                        addPlayerToNewSession(pl, false, true);
                        Service.gI().sendThongBaoOK(pl, "Kết quả là chẵn, bạn đã thua " + pl.cuoc + " thỏi vàng");
                        pl.cuoc = 0;
                        pariryManager.gI().goldChan = 0;
                        pariryManager.gI().goldLe = 0;
                    }
                }
            }
            currentPariry = null;
            // PlayNewSession();
        } else {
            System.out.println("Lỗi khi kết thúc session");
        }
    }

    public void addPlayerToNewSession(Player player, boolean isWin, boolean isOdd) {
        try (Connection conn = GirlkunDB.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO `pariry_players`(`id_session`, `id_player`, `ruby_quantity`,`is_odd`, `is_win`, `reward`) VALUES (?,?,?,?,?,?)")) {
                ps.setInt(1, currentPariry.getId());
                ps.setInt(2, (int) player.id);
                ps.setInt(3, player.cuoc + player.cuoc1);
                ps.setBoolean(4, isOdd);
                ps.setBoolean(5, isWin);
                ps.setBoolean(6, false);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            // Handle the exception (e.g., log the error for debugging)
            e.printStackTrace();
        }
    }

    private void getRubyWinAllPlayerNotYetReward() {
        Map<Integer, Integer> playerRubies = new HashMap<>();
        try (Connection conn = GirlkunDB.getConnection(); PreparedStatement ps = conn
                .prepareStatement("SELECT * FROM `pariry_players` WHERE `is_win` = 1 AND `reward` = 0")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) { // Dùng vòng lặp để xử lý tất cả các dòng
                    int id_player = rs.getInt("id_player");
                    int ruby = rs.getInt("ruby_quantity");
                    boolean isWin = rs.getBoolean("is_win");
                    boolean isReward = rs.getBoolean("reward");
                    if (isWin && !isReward) {
                        playerRubies.put(id_player, playerRubies.getOrDefault(id_player, 0) + ruby);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Iterator<Map.Entry<Integer, Integer>> itr = playerRubies.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<Integer, Integer> entry = itr.next();
            Integer id_player = entry.getKey();
            Integer ruby = entry.getValue();

            Player player = Client.gI().getPlayer(id_player);
            if (player != null) {
                player.rubyWin = 0;
                player.rubyWin += ruby;
            } else {
                itr.remove();
            }
        }
    }

    public void setAfterPlayerReward(Player player) {
        try (Connection conn = GirlkunDB.getConnection(); PreparedStatement ps = conn
                .prepareStatement("UPDATE `pariry_players` SET `reward` = ? WHERE `is_win` = 1 AND `id_player` = ?")) {
            ps.setBoolean(1, true);
            ps.setInt(2, (int) player.id);
            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Không thể cập nhât cho player: " + player.id + "\n");
                // You can add additional error handling or logging here if needed.
            }
        } catch (SQLException e) {
            // Handle the exception (e.g., log the error for debugging)
            e.printStackTrace();
        }
    }

}
