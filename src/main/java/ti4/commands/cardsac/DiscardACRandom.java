package ti4.commands.cardsac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.commands.PlayerGameStateSubcommand;
import ti4.generator.Mapper;
import ti4.helpers.Constants;
import ti4.map.Game;
import ti4.map.Player;
import ti4.message.MessageHelper;

public class DiscardACRandom extends PlayerGameStateSubcommand {

    public DiscardACRandom() {
        super(Constants.DISCARD_AC_RANDOM, "Discard a random Action Card", true, true);
        addOptions(new OptionData(OptionType.INTEGER, Constants.COUNT, "Count of how many to discard, default 1"));
        addOptions(new OptionData(OptionType.STRING, Constants.FACTION_COLOR, "Faction or Color").setAutoComplete(true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        int count = event.getOption(Constants.COUNT, 1, OptionMapping::getAsInt);
        count = Math.max(count, 1);

        Player player = getPlayer();
        Map<String, Integer> actionCardsMap = player.getActionCards();
        if (actionCardsMap.isEmpty()) {
            MessageHelper.sendMessageToChannel(event.getChannel(), "No Action Cards in hand");
            return;
        }

        Game game = getGame();
        discardRandomAC(event, game, player, count);

    }

    public void discardRandomAC(GenericInteractionCreateEvent event, Game game, Player player, int count) {
        if (count < 1) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Player: ").append(player.getUserName()).append(" - ");
        sb.append("Discarded Action Card:").append("\n");
        while (count > 0 && !player.getActionCards().isEmpty()) {
            Map<String, Integer> actionCards_ = player.getActionCards();
            List<String> cards_ = new ArrayList<>(actionCards_.keySet());
            Collections.shuffle(cards_);
            String acID = cards_.getFirst();
            boolean removed = game.discardActionCard(player.getUserID(), actionCards_.get(acID));
            if (!removed) {
                MessageHelper.sendMessageToChannel(event.getMessageChannel(), "No such Action Cards found, please retry");
                return;
            }
            sb.append(Mapper.getActionCard(acID).getRepresentation()).append("\n");
            count--;
        }
        MessageHelper.sendMessageToChannel(player.getCorrectChannel(), sb.toString());
        ACInfo.sendActionCardInfo(game, player);
    }
}
