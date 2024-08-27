package com.jeka8833.hypixelrequester.hypixel;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class HypixelJSONStructure {
    public @JSONField(name = "player") Optional<Player> player = Optional.empty();

    public boolean isEmpty() {
        return player.isEmpty() || player.get().isEmpty();
    }

    public static final class Player {
        public @JSONField(name = "stats") Optional<Stats> stats = Optional.empty();
        public @JSONField(name = "networkExp") Optional<Long> networkExp = Optional.empty();

        public boolean isEmpty() {
            return (stats.isEmpty() || stats.get().isEmpty()) && networkExp.isEmpty();
        }
    }

    public static final class Stats {
        public @JSONField(name = "TNTGames") Optional<TNTGames> tntGames = Optional.empty();
        public @JSONField(name = "Duels") Optional<Duels> duels = Optional.empty();

        public boolean isEmpty() {
            return (tntGames.isEmpty() || tntGames.get().isEmpty()) && (duels.isEmpty() || duels.get().isEmpty());
        }
    }

    public static final class TNTGames {
        // For TNTRun game
        public @JSONField(name = "wins_tntrun") Optional<Integer> tntRunWins = Optional.empty();
        public @JSONField(name = "deaths_tntrun") Optional<Integer> tntRunLosses = Optional.empty();

        // For PVPRun game
        public @JSONField(name = "wins_pvprun") Optional<Integer> pvpRunWins = Optional.empty();
        public @JSONField(name = "deaths_pvprun") Optional<Integer> pvpRunLosses = Optional.empty();
        public @JSONField(name = "kills_pvprun") Optional<Integer> pvpRunKills = Optional.empty();

        // For BowSpleef game
        public @JSONField(name = "wins_bowspleef") Optional<Integer> bowSpleefWins = Optional.empty();
        public @JSONField(name = "deaths_bowspleef") Optional<Integer> bowSpleefLosses = Optional.empty();

        // For TNTTag game
        public @JSONField(name = "wins_tntag") Optional<Integer> tntTagWins = Optional.empty();
        public @JSONField(name = "deaths_tntag") Optional<Integer> tntTagLosses = Optional.empty();
        public @JSONField(name = "kills_tntag") Optional<Integer> tntTagKills = Optional.empty();

        // For Wizards game
        public @JSONField(name = "wins_capture") Optional<Integer> wizardsWins = Optional.empty();
        public @JSONField(name = "deaths_capture") Optional<Integer> wizardsLosses = Optional.empty();
        public @JSONField(name = "kills_capture") Optional<Integer> wizardsKills = Optional.empty();

        // General stats
        public @JSONField(name = "winstreak") Optional<Integer> winStreak = Optional.empty();
        public @JSONField(name = "coins") Optional<Integer> coins = Optional.empty();

        public boolean isEmpty() {
            return tntRunWins.isEmpty() && tntRunLosses.isEmpty() && pvpRunWins.isEmpty() && pvpRunLosses.isEmpty() &&
                    pvpRunKills.isEmpty() && bowSpleefWins.isEmpty() && bowSpleefLosses.isEmpty() &&
                    tntTagWins.isEmpty() && tntTagLosses.isEmpty() && tntTagKills.isEmpty() && wizardsWins.isEmpty() &&
                    wizardsLosses.isEmpty() && wizardsKills.isEmpty() && winStreak.isEmpty() && coins.isEmpty();
        }
    }

    public static final class Duels {
        // For BowSpleef Duels game
        public @JSONField(name = "bowspleef_duel_wins") Optional<Integer> bowSpleefWins = Optional.empty();
        public @JSONField(name = "bowspleef_duel_losses") Optional<Integer> bowSpleefLosses = Optional.empty();
        public @JSONField(name = "current_winstreak_mode_bowspleef_duel") Optional<Integer> bowSpleefWinStreak =
                Optional.empty();

        public boolean isEmpty() {
            return bowSpleefWins.isEmpty() && bowSpleefLosses.isEmpty() && bowSpleefWinStreak.isEmpty();
        }
    }
}
