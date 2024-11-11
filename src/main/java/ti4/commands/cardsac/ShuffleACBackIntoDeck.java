package ti4.commands.cardsac;

import java.util.Map;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.commands.GameStateSubcommand;
import ti4.generator.Mapper;
import ti4.helpers.Constants;
import ti4.map.Game;
import ti4.message.MessageHelper;

public class ShuffleACBackIntoDeck extends GameStateSubcommand {

    public ShuffleACBackIntoDeck() {
        super(Constants.SHUFFLE_AC_BACK_INTO_DECK, "Shuffle Action Card back into deck from the discard pile.", true, true);
        addOptions(new OptionData(OptionType.INTEGER, Constants.ACTION_CARD_ID, "Action Card ID that is sent between ()").setRequired(true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        int acIndex = event.getOption(Constants.ACTION_CARD_ID).getAsInt();
        Game game = getGame();
        String acID = null;
        for (Map.Entry<String, Integer> so : game.getDiscardActionCards().entrySet()) {
            if (so.getValue().equals(acIndex)) {
                acID = so.getKey();
            }
        }
        if (acID == null) {
            MessageHelper.sendMessageToChannel(event.getChannel(), "No such Action Card ID found, please retry");
            return;
        }
        boolean picked = game.shuffleActionCardBackIntoDeck(acIndex);
        if (!picked) {
            MessageHelper.sendMessageToChannel(event.getChannel(), "No such Action Card ID found, please retry");
            return;
        }
        String sb = "Game: " + game.getName() + " " +
            "Player: " + event.getUser().getName() + "\n" +
            "Card shuffled back into deck from discards: " +
            Mapper.getActionCard(acID).getRepresentation() + "\n";
        MessageHelper.sendMessageToChannel(event.getChannel(), sb);
    }
}
