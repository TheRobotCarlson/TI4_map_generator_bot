package ti4.commands.special;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.commands.tokens.AddCC;
import ti4.commands.units.AddRemoveUnits;
import ti4.helpers.AliasHandler;
import ti4.helpers.Constants;
import ti4.helpers.FoWHelper;
import ti4.helpers.Helper;
import ti4.map.*;
import ti4.message.MessageHelper;

public class CheckDistance extends SpecialSubcommandData {
    public CheckDistance() {
        super(Constants.CHECK_DISTANCE, "Check Distance");
        addOptions(new OptionData(OptionType.STRING, Constants.TILE_NAME, "System/Tile name").setRequired(true).setAutoComplete(true));
        addOptions(new OptionData(OptionType.INTEGER, Constants.MAX_DISTANCE, "Max distance to check"));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Game activeGame = getActiveGame();
        Player player = activeGame.getPlayer(getUser().getId());
        player = Helper.getGamePlayer(activeGame, player, event, null);
        player = Helper.getPlayer(activeGame, player, event);
        if (player == null) {
            MessageHelper.sendMessageToChannel(event.getChannel(), "Player could not be found");
            return;
        }

        OptionMapping tileOption = event.getOption(Constants.TILE_NAME);
        if (tileOption == null){
            MessageHelper.sendMessageToChannel(event.getChannel(), "Specify a tile");
            return;
        }
        String tileID = AliasHandler.resolveTile(tileOption.getAsString().toLowerCase());
        Tile tile = AddRemoveUnits.getTile(event, tileID, activeGame);
        if (tile == null) {
            MessageHelper.sendMessageToChannel(event.getChannel(), "Could not resolve tileID:  `" + tileID + "`. Tile not found");
            return;
        }

        int maxDistance = event.getOption(Constants.MAX_DISTANCE, 8, OptionMapping::getAsInt);
        Map<String, Integer> distances = getTileDistances(activeGame, player, tile.getPosition(), maxDistance);

        sendMessage(distances.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .sorted()
                .reduce("Distances: \n", (a, b) -> a + "\n" + b));
    }

    public static int getDistanceBetweenTwoTiles(Game activeGame, Player player, String tilePosition1, String tilePosition2){
         Map<String, Integer> distances = getTileDistances(activeGame, player, tilePosition1, 8);
         if(distances.get(tilePosition2) != null){
            return distances.get(tilePosition2);
         }
         return 100;
    }


    public static Map<String, Integer> getTileDistancesRelativeToAllYourUnlockedTiles(Game activeGame, Player player){
        Map<String, Integer> distances = new HashMap<>();
        List<Tile> originTiles = new ArrayList<>();
        for(Tile tile : activeGame.getTileMap().values()){
            if(!AddCC.hasCC(player, tile) && FoWHelper.playerHasUnitsInSystem(player, tile)){
                distances.put(tile.getPosition(), 0);
                originTiles.add(tile);
            }
        }
        for(Tile tile : originTiles){
            Map<String, Integer> someDistances = getTileDistances(activeGame, player, tile.getPosition(), 8);
            for(String tilePos : someDistances.keySet()){
                if(AddCC.hasCC(player, activeGame.getTileByPosition(tilePos))){
                    continue;
                }
                if(distances.get(tilePos) == null){
                    distances.put(tilePos, someDistances.get(tilePos));
                }else{
                    if(distances.get(tilePos) > someDistances.get(tilePos)){
                        distances.put(tilePos, someDistances.get(tilePos));
                    }
                }
            }
        }
        return distances;
    }

    public static List<String> getAllTilesACertainDistanceAway(Game activeGame, Player player, Map<String, Integer> distances, int target){
        List<String> tiles = new ArrayList<>();
        for(String pos : distances.keySet()){
            if(distances.get(pos) != null && distances.get(pos)== target){
                tiles.add(pos);
            }
        }
        return tiles;
    }

    public static Map<String, Integer> getTileDistances(Game activeGame, Player player, String tilePosition, int maxDistance) {
        Map<String, Integer> distances = new HashMap<>();
        distances.put(tilePosition, 0);

        for (int i = 1; i <= maxDistance; i++) {
            Map<String, Integer> distancesCopy = new HashMap<>(distances);
            for (String existingPosition : distancesCopy.keySet()) {
                addAdjacentPositionsIfNotThereYet(activeGame, existingPosition, distances, player, i);
            }
        }

        for (String otherTilePosition : activeGame.getTileMap().keySet()) {
            distances.putIfAbsent(otherTilePosition, null);
        }

        return distances;
    }

    private static void addAdjacentPositionsIfNotThereYet(Game activeGame, String position, Map<String, Integer> distances, Player player, int distance) {
        for (String tilePosition : adjacentPositions(activeGame, position, player)) {
            distances.putIfAbsent(tilePosition, distance);
        }
    }

    private static Set<String> adjacentPositions(Game activeGame, String position, Player player) {
        return FoWHelper.getAdjacentTilesAndNotThisTile(activeGame, position, player, false);
    }
}
