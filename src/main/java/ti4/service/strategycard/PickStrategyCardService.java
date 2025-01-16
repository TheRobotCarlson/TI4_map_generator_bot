package ti4.service.strategycard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import ti4.buttons.Buttons;
import ti4.helpers.ButtonHelper;
import ti4.helpers.ButtonHelperActionCards;
import ti4.helpers.ButtonHelperFactionSpecific;
import ti4.helpers.FoWHelper;
import ti4.helpers.Helper;
import ti4.helpers.PromissoryNoteHelper;
import ti4.helpers.StringHelper;
import ti4.image.BannerGenerator;
import ti4.map.Game;
import ti4.map.Player;
import ti4.message.MessageHelper;
import ti4.service.info.ListTurnOrderService;
import ti4.service.player.PlayerStatsService;
import ti4.service.turn.EndTurnService;
import ti4.service.turn.StartTurnService;
import ti4.settings.users.UserSettingsManager;

@UtilityClass
public class PickStrategyCardService {

    public static void secondHalfOfSCPick(GenericInteractionCreateEvent event, Player player, Game game, int scPicked) {
        boolean isFowPrivateGame = FoWHelper.isPrivateGame(game, event);

        String msgExtra = "";
        boolean allPicked = true;
        Player privatePlayer = null;
        List<Player> activePlayers = game.getPlayers().values().stream()
            .filter(Player::isRealPlayer)
            .collect(Collectors.toList());
        if (game.isReverseSpeakerOrder() || !game.getStoredValue("willRevolution").isEmpty()) {
            Collections.reverse(activePlayers);
        }
        int maxSCsPerPlayer = game.getStrategyCardsPerPlayer();
        if (maxSCsPerPlayer < 1) {
            maxSCsPerPlayer = 1;
        }
        if (!game.getStoredValue("exhaustedSC" + scPicked).isEmpty()) {
            game.setSCPlayed(scPicked, true);
        }

        boolean nextCorrectPing = false;
        Queue<Player> players = new ArrayDeque<>(activePlayers);
        while (players.iterator().hasNext()) {
            Player player_ = players.poll();
            if (player_ == null || !player_.isRealPlayer()) {
                continue;
            }
            int player_SCCount = player_.getSCs().size();
            if (nextCorrectPing && player_SCCount < maxSCsPerPlayer && player_.getFaction() != null) {
                msgExtra += player_.getRepresentationUnfogged() + " is up to pick their strategy card.";
                game.setPhaseOfGame("strategy");
                privatePlayer = player_;
                allPicked = false;
                break;
            }
            if (player_ == player) {
                nextCorrectPing = true;
            }
            if (player_SCCount < maxSCsPerPlayer && player_.getFaction() != null) {
                players.add(player_);
            }
        }

        //INFORM ALL PLAYER HAVE PICKED
        if (allPicked) {

            for (Player p2 : game.getRealPlayers()) {
                ButtonHelperActionCards.checkForAssigningCoup(game, p2);
                if (game.getStoredValue("Play Naalu PN") != null && game.getStoredValue("Play Naalu PN").contains(p2.getFaction())) {
                    if (!p2.getPromissoryNotesInPlayArea().contains("gift") && p2.getPromissoryNotes().containsKey("gift")) {
                        PromissoryNoteHelper.resolvePNPlay("gift", p2, game, event);
                    }
                }
            }

            msgExtra += "\nAll players picked strategy cards.";
            Set<Integer> scPickedList = new HashSet<>();
            for (Player player_ : activePlayers) {
                scPickedList.addAll(player_.getSCs());
            }

            //ADD A TG TO UNPICKED SC
            game.incrementScTradeGoods();

            for (int sc : scPickedList) {
                game.setScTradeGood(sc, 0);
            }

            Player nextPlayer = null;
            int lowestSC = 100;
            for (Player player_ : activePlayers) {
                int playersLowestSC = player_.getLowestSC();
                String scNumberIfNaaluInPlay = game.getSCNumberIfNaaluInPlay(player_, Integer.toString(playersLowestSC));
                if (scNumberIfNaaluInPlay.startsWith("0/")) {
                    nextPlayer = player_; //no further processing, this player has the 0 token
                    break;
                }
                if (playersLowestSC < lowestSC) {
                    lowestSC = playersLowestSC;
                    nextPlayer = player_;
                }
            }

            //INFORM FIRST PLAYER IS UP FOR ACTION
            if (nextPlayer != null) {
                msgExtra += "\n" + nextPlayer.getRepresentation() + " is first in initiative order.";
                privatePlayer = nextPlayer;
                game.updateActivePlayer(nextPlayer);
                ButtonHelperFactionSpecific.resolveMilitarySupportCheck(nextPlayer, game);
                ButtonHelperFactionSpecific.resolveKolleccAbilities(nextPlayer, game);
                if (game.isFowMode()) {
                    FoWHelper.pingAllPlayersWithFullStats(game, event, nextPlayer, "started turn");
                }
                game.setStoredValue("willRevolution", "");
                game.setPhaseOfGame("action");
                if (!game.isFowMode()) {
                    ButtonHelper.updateMap(game, event,
                        "Start of Action Phase For Round #" + game.getRound());
                }
            }
        }

        //SEND EXTRA MESSAGE
        if (isFowPrivateGame) {
            if (allPicked) {
                msgExtra = privatePlayer.getRepresentationUnfogged() + ", it is now your turn (your " 
                    + StringHelper.ordinal(privatePlayer.getInRoundTurnCount()) + " turn of round " + game.getRound() + ").";
            }
            String fail = "User for next faction not found. Report to ADMIN";
            String success = "The next player has been notified";
            MessageHelper.sendPrivateMessageToPlayer(privatePlayer, game, event, msgExtra, fail, success);
            game.updateActivePlayer(privatePlayer);

            if (!allPicked) {
                game.setPhaseOfGame("strategy");
                MessageHelper.sendMessageToChannelWithButtons(privatePlayer.getPrivateChannel(), "Use buttons to pick your strategy card.", Helper.getRemainingSCButtons(game, privatePlayer));
            } else {
                privatePlayer.setInRoundTurnCount(privatePlayer.getInRoundTurnCount() + 1);
                if (game.isShowBanners()) {
                    BannerGenerator.drawFactionBanner(privatePlayer);
                }
                MessageHelper.sendMessageToChannelWithButtons(privatePlayer.getPrivateChannel(), msgExtra + "\n Use Buttons to do turn.",
                    StartTurnService.getStartOfTurnButtons(privatePlayer, game, false, event));
                if (privatePlayer.getGenSynthesisInfantry() > 0) {
                    if (!ButtonHelper.getPlaceStatusInfButtons(game, privatePlayer).isEmpty()) {
                        MessageHelper.sendMessageToChannelWithButtons(privatePlayer.getCorrectChannel(),
                            "Use buttons to revive infantry. You have " + privatePlayer.getGenSynthesisInfantry() + " infantry left to revive.",
                            ButtonHelper.getPlaceStatusInfButtons(game, privatePlayer));
                    } else {
                        privatePlayer.setStasisInfantry(0);
                        MessageHelper.sendMessageToChannel(privatePlayer.getCorrectChannel(), privatePlayer.getRepresentation()
                            + ", you had infantry II to be revived, but the bot couldn't find any planets you control in your home system to place them on, so per the rules they now disappear into the ether.");

                    }
                }

            }

        } else {
            if (!allPicked) {
                game.updateActivePlayer(privatePlayer);
                game.setPhaseOfGame("strategy");
                checkForForcePickLastStratCard(event, privatePlayer, game, msgExtra);
            } else {
                MessageHelper.sendMessageToChannel(game.getMainGameChannel(), msgExtra);
                if (game.isShowBanners()) {
                    BannerGenerator.drawPhaseBanner("action", game.getRound(), game.getActionsChannel());
                }
                game.setPhaseOfGame("action");
                ListTurnOrderService.turnOrder(event, game);
                privatePlayer.setInRoundTurnCount(privatePlayer.getInRoundTurnCount() + 1);
                if (game.isShowBanners()) {
                    BannerGenerator.drawFactionBanner(privatePlayer);
                }
                String text = privatePlayer.getRepresentationUnfogged() + ", it is now your turn (your " 
                    + StringHelper.ordinal(privatePlayer.getInRoundTurnCount()) + " turn of round " + game.getRound() + ").";
                Player nextPlayer = EndTurnService.findNextUnpassedPlayer(game, privatePlayer);
                if (nextPlayer == privatePlayer) {
                    text += "\n-# All other players are passed; you will take consecutive turns until you pass, ending the action phase.";
                } else if (nextPlayer != null) {
                    String ping = UserSettingsManager.get(nextPlayer.getUserID()).isPingOnNextTurn() ? nextPlayer.getRepresentationUnfogged() : nextPlayer.getRepresentationNoPing();
                    text += "\n-# " + ping + " will start their turn once you've ended yours.";
                }
                MessageHelper.sendMessageToChannel(game.getMainGameChannel(), text);
                if (privatePlayer.getGenSynthesisInfantry() > 0) {
                    if (!ButtonHelper.getPlaceStatusInfButtons(game, privatePlayer).isEmpty()) {
                        MessageHelper.sendMessageToChannelWithButtons(privatePlayer.getCorrectChannel(),
                            "Use buttons to revive infantry. You have " + privatePlayer.getGenSynthesisInfantry() + " infantry left to revive.",
                            ButtonHelper.getPlaceStatusInfButtons(game, privatePlayer));
                    } else {
                        privatePlayer.setStasisInfantry(0);
                        MessageHelper.sendMessageToChannel(privatePlayer.getCorrectChannel(), privatePlayer.getRepresentation()
                            + ", you had infantry II to be revived, but the bot couldn't find any planets you control in your home system to place them on, so per the rules they now disappear into the ether.");

                    }
                }
                MessageHelper.sendMessageToChannelWithButtons(game.getMainGameChannel(), "Use buttons to do turn.",
                    StartTurnService.getStartOfTurnButtons(privatePlayer, game, false, event));
            }
        }
        if (allPicked) {
            for (Player p2 : game.getRealPlayers()) {
                List<Button> buttons = new ArrayList<>();
                if (p2.hasTechReady("qdn") && p2.getTg() > 2 && p2.getStrategicCC() > 0) {
                    buttons.add(Buttons.green("startQDN", "Use Quantum Datahub Node"));
                    buttons.add(Buttons.red("deleteButtons", "Decline"));
                    MessageHelper.sendMessageToChannelWithButtons(p2.getCorrectChannel(), p2.getRepresentationUnfogged() + " you have the opportunity to use _Quantum Datahub Node_.", buttons);
                }
                buttons = new ArrayList<>();
                if (game.getLaws().containsKey("arbiter") && game.getLawsInfo().get("arbiter").equalsIgnoreCase(p2.getFaction())) {
                    buttons.add(Buttons.green("startArbiter", "Use Imperial Arbiter"));
                    buttons.add(Buttons.red("deleteButtons", "Decline"));
                    MessageHelper.sendMessageToChannelWithButtons(p2.getCorrectChannel(),
                        p2.getRepresentationUnfogged() + " you have the opportunity to use _Imperial Arbiter_.", buttons);
                }
            }
        }
    }

    public static void checkForForcePickLastStratCard(GenericInteractionCreateEvent event, Player privatePlayer, Game game, String msgExtra){
        List<Button> scButtons = Helper.getRemainingSCButtons(game, privatePlayer);
        if (scButtons.size() == 1){ // if there is only one SC left to pick (4p/8p games), force pick last SC
            MessageHelper.sendMessageToChannel(event.getMessageChannel(), privatePlayer.getRepresentation() + 
                ", you have only one available Strategy Card to pick. Bot will force pick for you.");
            int unpickedStrategyCard = 0;
            for (Integer sc : game.getSCList()) {
                if (sc <= 0)
                    continue; // some older games have a 0 in the list of SCs
                boolean held = false;
                for (Player p : game.getPlayers().values()) {
                    if (p == null || p.getFaction() == null) {
                        continue;
                    }
                    if (p.getSCs() != null && p.getSCs().contains(sc) && !game.isFowMode()) {
                        held = true;
                        break;
                    }
                }
                if (held)
                    continue;
                unpickedStrategyCard = sc;
            }    
            PlayerStatsService.secondHalfOfPickSC(event, game, privatePlayer, unpickedStrategyCard);
            secondHalfOfSCPick(event, privatePlayer, game, unpickedStrategyCard);
        } else {
            MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(), msgExtra, scButtons);
        }
    }
}
