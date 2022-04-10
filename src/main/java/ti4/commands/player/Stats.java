package ti4.commands.player;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.helpers.AliasHandler;
import ti4.helpers.Constants;
import ti4.map.Map;
import ti4.map.Player;
import ti4.message.MessageHelper;

import java.util.LinkedHashMap;
import java.util.StringTokenizer;

public class Stats extends PlayerSubcommandData {
    public Stats() {
        super(Constants.STATS, "Player Stats: CC,TG,Commodities");
        addOptions(new OptionData(OptionType.STRING, Constants.CC, "CC's Example: 3/3/2"))
                .addOptions(new OptionData(OptionType.INTEGER, Constants.TACTICAL, "Tactical command counter count"))
                .addOptions(new OptionData(OptionType.INTEGER, Constants.FLEET, "Fleet command counter count"))
                .addOptions(new OptionData(OptionType.INTEGER, Constants.STRATEGY, "Strategy command counter count"))
                .addOptions(new OptionData(OptionType.INTEGER, Constants.TG, "Trade goods count"))
                .addOptions(new OptionData(OptionType.INTEGER, Constants.COMMODITIES, "Commodity count"))
                .addOptions(new OptionData(OptionType.INTEGER, Constants.COMMODITIES_TOTAL, "Commodity total count"))
                .addOptions(new OptionData(OptionType.INTEGER, Constants.PN, "Promissory Note count"))
                .addOptions(new OptionData(OptionType.INTEGER, Constants.CRF, "Cultural Relic Fragment count"))
                .addOptions(new OptionData(OptionType.INTEGER, Constants.HRF, "Hazardous Relic Fragment count"))
                .addOptions(new OptionData(OptionType.INTEGER, Constants.IRF, "Industrial Relic Fragment count"))
                .addOptions(new OptionData(OptionType.INTEGER, Constants.VRF, "Unknown Relic Fragment count"))
                .addOptions(new OptionData(OptionType.INTEGER, Constants.SC, "Strategy Card Number count"))
                .addOptions(new OptionData(OptionType.STRING, Constants.SC_PLAYED, "Strategy Card played y/n"))
                .addOptions(new OptionData(OptionType.STRING, Constants.PASSED, "Player passed y/n"))
                .addOptions(new OptionData(OptionType.STRING, Constants.SPEAKER, "Player is speaker y/n"))
                .addOptions(new OptionData(OptionType.USER, Constants.PLAYER, "Player for which you set up faction"));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {

        Map activeMap = getActiveMap();
        Player player = activeMap.getPlayer(getUser().getId());
        if (player == null) {
            MessageHelper.sendMessageToChannel(event.getChannel(), "Player could not be found");
            return;
        }

        OptionMapping playerOption = event.getOption(Constants.PLAYER);
        if (playerOption != null) {
            String playerID = playerOption.getAsUser().getId();
            if (activeMap.getPlayer(playerID) != null) {
                player = activeMap.getPlayers().get(playerID);
            } else {
                MessageHelper.sendMessageToChannel(event.getChannel(), "Player:" + playerOption.getAsUser().getName() + " could not be found in map:" + activeMap.getName());
                return;
            }
        }

        OptionMapping optionCC = event.getOption(Constants.CC);
        OptionMapping optionT = event.getOption(Constants.TACTICAL);
        OptionMapping optionF = event.getOption(Constants.FLEET);
        OptionMapping optionS = event.getOption(Constants.STRATEGY);
        if (optionCC != null && (optionT != null || optionF != null && optionS != null)) {
            MessageHelper.sendMessageToChannel(event.getChannel(), "Use format 3/3/3 for command counters or individual values, not both");
        } else {

            if (optionCC != null) {
                @SuppressWarnings("ConstantConditions")
                String cc = AliasHandler.resolveFaction(optionCC.getAsString().toLowerCase());
                StringTokenizer tokenizer = new StringTokenizer(cc, "/");
                if (tokenizer.countTokens() != 3) {
                    MessageHelper.sendMessageToChannel(event.getChannel(), "Wrong format for tokens count. Must be 3/3/3");
                } else {
                    try {
                        player.setTacticalCC(Integer.parseInt(tokenizer.nextToken()));
                        player.setFleetCC(Integer.parseInt(tokenizer.nextToken()));
                        player.setStrategicCC(Integer.parseInt(tokenizer.nextToken()));
                    } catch (Exception e) {
                        MessageHelper.sendMessageToChannel(event.getChannel(), "Not number entered, check CC count again");
                    }
                }
            }
            if (optionT != null) {
                player.setTacticalCC(optionT.getAsInt());
            }
            if (optionF != null) {
                player.setFleetCC(optionF.getAsInt());
            }
            if (optionS != null) {
                player.setStrategicCC(optionS.getAsInt());
            }
        }
        OptionMapping optionTG = event.getOption(Constants.TG);
        if (optionTG != null) {
            player.setTg(optionTG.getAsInt());
        }
        OptionMapping optionC = event.getOption(Constants.COMMODITIES);
        if (optionC != null) {
            player.setCommodities(optionC.getAsInt());
        }
        OptionMapping optionCT = event.getOption(Constants.COMMODITIES_TOTAL);
        if (optionCT != null) {
            player.setCommoditiesTotal(optionCT.getAsInt());
        }
        OptionMapping optionPN = event.getOption(Constants.PN);
        if (optionPN != null) {
            player.setPn(optionPN.getAsInt());
        }
        OptionMapping optionCRF = event.getOption(Constants.CRF);
        if (optionCRF != null) {
            player.setCrf(optionCRF.getAsInt());
        }
        OptionMapping optionHRF = event.getOption(Constants.HRF);
        if (optionHRF != null) {
            player.setHrf(optionHRF.getAsInt());
        }
        OptionMapping optionIRF = event.getOption(Constants.IRF);
        if (optionIRF != null) {
            player.setIrf(optionIRF.getAsInt());
        }
        OptionMapping optionVRF = event.getOption(Constants.VRF);
        if (optionVRF != null) {
            player.setVrf(optionVRF.getAsInt());
        }

        OptionMapping optionSpeaker = event.getOption(Constants.SPEAKER);
        if (optionSpeaker != null) {
            String value = optionSpeaker.getAsString().toLowerCase();
            if ("y".equals(value) || "yes".equals(value)) {
                activeMap.setSpeaker(player.getUserID());
            }
        }

        OptionMapping optionPassed = event.getOption(Constants.PASSED);
        if (optionPassed != null) {
            String value = optionPassed.getAsString().toLowerCase();
            if ("y".equals(value) || "yes".equals(value)) {
                player.setPassed(true);
            } else if ("n".equals(value) || "no".equals(value)) {
                player.setPassed(false);
            }
        }
        OptionMapping optionSC = event.getOption(Constants.SC);
        if (optionSC != null) {
            int scNumber = optionSC.getAsInt();
            LinkedHashMap<Integer, Integer> scTradeGoods = activeMap.getScTradeGoods();
            if (!scTradeGoods.containsKey(scNumber)){
                MessageHelper.sendMessageToChannel(event.getChannel(), "Strategy Card must be from possible ones in Game");
                return;
            }
            if (scNumber > 0) {
                LinkedHashMap<String, Player> players = activeMap.getPlayers();
                for (Player playerStats : players.values()) {
                    if (playerStats.getSC() == scNumber) {
                        MessageHelper.sendMessageToChannel(event.getChannel(), "SC is already picked.");
                        return;
                    }
                }
                player.setSC(scNumber);
                Integer tgCount = scTradeGoods.get(scNumber);
                if (tgCount != null) {
                    int tg = player.getTg();
                    tg += tgCount;
                    player.setTg(tg);
                }
            } else if (scNumber == 0){
                int sc = player.getSC();
                player.setSC(scNumber);
                activeMap.setSCPlayed(sc, false);
            }
        }

        OptionMapping optionSCPlayed = event.getOption(Constants.SC_PLAYED);
        if (optionSCPlayed != null) {
            int sc = player.getSC();
            if (sc > 0) {
                String value = optionSCPlayed.getAsString().toLowerCase();
                if ("y".equals(value) || "yes".equals(value)) {
                    activeMap.setSCPlayed(sc, true);
                } else if ("n".equals(value) || "no".equals(value)) {
                    activeMap.setSCPlayed(sc, false);
                }
            }
        }
    }
}
