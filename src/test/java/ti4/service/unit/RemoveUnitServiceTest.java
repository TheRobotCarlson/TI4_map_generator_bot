package ti4.service.unit;

import org.junit.jupiter.api.Test;
import ti4.helpers.Units;
import ti4.map.Game;
import ti4.map.Planet;
import ti4.map.Space;
import ti4.map.Tile;
import ti4.map.UnitHolder;
import ti4.testUtils.BaseTi4Test;

import static org.assertj.core.api.Assertions.assertThat;

class RemoveUnitServiceTest extends BaseTi4Test {

    @Test
    void removeUnits() {
        beforeAll();

        var unitKey = new Units.UnitKey(Units.UnitType.Mech, "black");
        Tile tile = createTile("ixth", unitKey, 4);
        Game game = createGame(tile);
        ParsedUnit parsedUnit = createParsedUnit("ixth", unitKey, 4);

        RemoveUnitService.removeUnit(null, tile, game, parsedUnit);

        assertThat(tile.getPlanetUnitHolders().getFirst().getUnits().get(unitKey)).isNull();
    }

    private static Tile createTile(String location, Units.UnitKey unitKey, int amount) {
        Tile tile = new Tile("id", "position");
        tile.getUnitHolders().put(location, createUnitHolder(location, unitKey, amount));
        return tile;
    }

    private static UnitHolder createUnitHolder(String location, Units.UnitKey unitKey, int amount) {
        UnitHolder unitHolder;
        if (!"space".equals(location)) {
            unitHolder = new Planet(location, null);
        } else {
            unitHolder = new Space(location, null);
        }
        unitHolder.addUnit(unitKey, amount);
        return unitHolder;
    }

    private static Game createGame(Tile tile) {
        Game game = new Game();
        game.getTileMap().put(tile.getTileID(), tile);
        return game;
    }

    private static ParsedUnit createParsedUnit(String location, Units.UnitKey unitKey, int amount) {
        return new ParsedUnit(unitKey, amount, location);
    }
}