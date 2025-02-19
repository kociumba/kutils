package org.kociumba.kutils.client.tabList

import net.minecraft.client.network.PlayerListEntry
import net.minecraft.scoreboard.ReadableScoreboardScore
import net.minecraft.scoreboard.ScoreHolder
import net.minecraft.scoreboard.Scoreboard
import net.minecraft.scoreboard.ScoreboardCriterion
import net.minecraft.scoreboard.ScoreboardObjective
import net.minecraft.scoreboard.number.StyledNumberFormat
import net.minecraft.text.Text
import org.kociumba.kutils.client.client
import org.kociumba.kutils.mixin.client.PlayerListHudAccessor

data class ScoreboardKey(var key: String)
data class ScoreboardValue(var value: String)

var scoreboard: Scoreboard? = null
var objective: ScoreboardObjective? = null

fun collectPlayers(): List<PlayerListEntry> {
    return (client.inGameHud.playerListHud as PlayerListHudAccessor).invokeCollectPlayerEntries()
}

fun getHeader(): Text {
    return (client.inGameHud.playerListHud as PlayerListHudAccessor).getHeader()
}

fun getFooter(): Text {
    return (client.inGameHud.playerListHud as PlayerListHudAccessor).getFooter()
}

fun removeStyling(txt: Text): String {
    var s = txt.string
    return s.trim()
}

fun getTabListKeyPairs(): Map<ScoreboardKey, ScoreboardValue> {
    var r: LinkedHashMap<ScoreboardKey, ScoreboardValue> = LinkedHashMap()
    if (scoreboard == null) return r
    var players: List<PlayerListEntry> = collectPlayers()

    for (player in players) {
        var name = removeStyling(client.inGameHud.playerListHud.getPlayerName(player))
        var fscore: String? = null
        if (objective == null) return r

        var sh = ScoreHolder.fromProfile(player.profile)
        var score = scoreboard!!.getScore(sh, objective)
        if (score == null) return r

        if (objective!!.renderType == ScoreboardCriterion.RenderType.HEARTS) {
            var num = objective!!.getNumberFormatOr(StyledNumberFormat.YELLOW)
            fscore = removeStyling(ReadableScoreboardScore.getFormattedScore(score, num))
        } else {
            fscore = score.score.toString()
        }

        r.put(ScoreboardKey(name), ScoreboardValue(fscore))
    }

    var header = getHeader()
    r.put(ScoreboardKey("header"), ScoreboardValue(removeStyling(header)))

    var footer = getFooter()
    r.put(ScoreboardKey("footer"), ScoreboardValue(removeStyling(footer)))

    return r
}