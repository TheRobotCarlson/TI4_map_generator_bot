package ti4.commands.game;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.AsyncTI4DiscordBot;
import ti4.commands.CommandHelper;
import ti4.commands.GameStateSubcommand;
import ti4.helpers.Constants;
import ti4.helpers.FoWHelper;
import ti4.helpers.Helper;
import ti4.map.Game;
import ti4.map.Player;
import ti4.message.BotLogger;
import ti4.message.MessageHelper;

class Replace extends GameStateSubcommand {

    public Replace() {
        super(Constants.REPLACE, "Replace player in game", true, false);
        addOptions(new OptionData(OptionType.USER, Constants.PLAYER, "Player being replaced @playerName"));
        addOptions(new OptionData(OptionType.STRING, Constants.FACTION_COLOR, "Faction being replaced").setAutoComplete(true));
        addOptions(new OptionData(OptionType.USER, Constants.PLAYER2, "Replacement player @playerName"));
        addOptions(new OptionData(OptionType.STRING, Constants.GAME_NAME, "Game name").setAutoComplete(true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        boolean isAdmin = false;
        if (member != null) {
            List<Role> roles = member.getRoles();
            for (Role role : AsyncTI4DiscordBot.bothelperRoles) {
                if (roles.contains(role)) {
                    isAdmin = true;
                    break;
                }
            }
        }

        Game game = getGame();
        if (game.getPlayer(event.getUser().getId()) == null && !isAdmin) {
            MessageHelper.sendMessageToChannel(event.getChannel(), "Only game players or Bothelpers can replace a player.");
            return;
        }

        OptionMapping replacedPlayerFactionOption = event.getOption(Constants.FACTION_COLOR);
        OptionMapping replacedPlayerOption = event.getOption(Constants.PLAYER);
        if (replacedPlayerFactionOption == null && replacedPlayerOption == null) {
            MessageHelper.replyToMessage(event, "Specify player to remove.");
            return;
        }

        Player replacedPlayer = CommandHelper.getPlayerFromEvent(game, event);
        if (replacedPlayer == null) {
            if (replacedPlayerOption != null) {
                MessageHelper.replyToMessage(event, "Could not find the player to be replaced, try using the faction/color option.");
            } else {
                MessageHelper.replyToMessage(event, "Could not find the specified faction/color, try using the player option.");
            }
            return;
        }

        OptionMapping replacementPlayerOption = event.getOption(Constants.PLAYER2);
        if (replacementPlayerOption == null) {
            MessageHelper.replyToMessage(event, "Specify player to be replaced.");
            return;
        }

        User replacementUser = replacementPlayerOption.getAsUser();
        Player possibleSpectatorToRemove = game.getPlayer(replacementUser.getId());
        if (possibleSpectatorToRemove != null && possibleSpectatorToRemove.getFaction() != null) {
            MessageHelper.replyToMessage(event, "Specify player that is **__not__** in the game to be the replacement");
            return;
        } else if (possibleSpectatorToRemove != null) {
            game.removePlayer(possibleSpectatorToRemove.getUserID());
        }

        Guild guild = game.getGuild();
        Member newMember = guild.getMemberById(replacementUser.getId());
        if (newMember == null) {
            MessageHelper.replyToMessage(event, "Added player must be on the game's server.");
            return;
        }

        //REMOVE ROLE
        Member oldMember = guild.getMemberById(replacedPlayer.getUserID());
        List<Role> roles = guild.getRolesByName(game.getName(), true);
        if (oldMember != null && roles.size() == 1) {
            guild.removeRoleFromMember(oldMember, roles.getFirst()).queue();
        }

        //ADD ROLE
        if (roles.size() == 1) {
            guild.addRoleToMember(newMember, roles.getFirst()).queue();
        }

        Map<String, List<String>> scoredPublicObjectives = game.getScoredPublicObjectives();
        for (Map.Entry<String, List<String>> poEntry : scoredPublicObjectives.entrySet()) {
            List<String> value = poEntry.getValue();
            boolean removed = value.remove(replacedPlayer.getUserID());
            if (removed) {
                value.add(replacementUser.getId());
            }
        }

        String oldPlayerUserId = replacedPlayer.getUserID();
        String oldPlayerUserName = replacedPlayer.getUserName();
        replacedPlayer.setUserID(replacementUser.getId());
        replacedPlayer.setUserName(replacementUser.getName());
        replacedPlayer.setTotalTurnTime(0);
        replacedPlayer.setNumberTurns(0);
        if (oldPlayerUserId.equals(game.getSpeakerUserID())) {
            game.setSpeakerUserID(replacementUser.getId());
        }
        if (oldPlayerUserId.equals(game.getActivePlayerID())) {
            // do not update stats for this action
            game.setActivePlayerID(replacementUser.getId());
        }
        Map<String, Player> playersById = game.getPlayers();
        Map<String, Player> updatedPlayersById = new HashMap<>();
        for (String userId: game.getPlayers().keySet()) {
            if(userId.equalsIgnoreCase(oldPlayerUserId)) {
                updatedPlayersById.put(replacedPlayer.getUserID(), replacedPlayer);
            }
            else {
                updatedPlayersById.put(userId, playersById.get(userId));
            }
        }
        game.setPlayers(updatedPlayersById);

        //UPDATE FOW PERMISSIONS
        if (game.isFowMode()) {
            long permission = Permission.MESSAGE_MANAGE.getRawValue() | Permission.VIEW_CHANNEL.getRawValue();
            TextChannel privateChannel = (TextChannel)replacedPlayer.getPrivateChannel();

            PermissionOverride oldOverride = privateChannel.getMemberPermissionOverrides().stream()
                .filter(override -> override.getMember().equals(oldMember)).findFirst().orElse(null);
            if (oldOverride != null) {
                oldOverride.delete().queue();
            }

            //Update private channel
            String newPrivateChannelName = privateChannel.getName().replace(getNormalizedName(oldMember), getNormalizedName(newMember));
            privateChannel.getManager().setName(newPrivateChannelName).queue();
            privateChannel.getManager().putMemberPermissionOverride(newMember.getIdLong(), permission, 0)
                .queue(success -> accessMessage(privateChannel, newMember));

            //Update Cards Info
            ThreadChannel cardsInfo = replacedPlayer.getCardsInfoThread();
            if (cardsInfo != null) {
                String newCardsInfoName = cardsInfo.getName().replace(oldPlayerUserName.replace("/", ""), replacedPlayer.getUserName().replace("/", ""));
                cardsInfo.getManager().setName(newCardsInfoName).queue();
                cardsInfo.removeThreadMember(oldMember).queue();
                cardsInfo.addThreadMember(newMember).queue(success -> accessMessage(cardsInfo, newMember));
            }

            //Update private threads
            for (ThreadChannel thread : game.getMainGameChannel().getThreadChannels()) {
                if (thread.getThreadMember(oldMember) != null) {
                    thread.removeThreadMember(oldMember).queue(success -> {
                        thread.addThreadMember(newMember).queue(success2 -> {
                            accessMessage(thread, newMember);
                        });
                    });
                }
            }
        }

        Helper.fixGameChannelPermissions(event.getGuild(), game);
        ThreadChannel mapThread = game.getBotMapUpdatesThread();
        if (mapThread != null && !mapThread.isLocked()) {
            mapThread.getManager().setArchived(false).queue(success -> mapThread.addThreadMember(newMember).queueAfter(5, TimeUnit.SECONDS), BotLogger::catchRestError);
        }

        game.getMiltyDraftManager().replacePlayer(oldPlayerUserId, replacedPlayer.getUserID());

        if (game.getMiltyDraftManager().getDraftIndex() < game.getMiltyDraftManager().getDraftOrder().size()) {
            game.getMiltyDraftManager().repostDraftInformation(game);
        }

        String message = "Game: " + game.getName() + "  Player: " + oldPlayerUserId + " replaced by player: " + replacementUser.getName();
        if (FoWHelper.isPrivateGame(game)) {
            MessageHelper.sendMessageToChannel(event.getChannel(), message);
        } else {
            MessageHelper.sendMessageToChannel(game.getActionsChannel(), message);
        }
    }

    private void accessMessage(MessageChannel channel, Member member) {
        MessageHelper.sendMessageToChannel(channel, 
            "Access to " + channel.getName() + " granted for " + member.getAsMention());
    }

    private String getNormalizedName(Member member) {
        String name = member.getNickname();
        if (name == null) {
            name = member.getEffectiveName();
        }
        name = name.toLowerCase()
            .replaceAll("[\\s]+", "-")
            .replaceAll("[^a-z0-9-]", "")
            .replaceAll("-{2,}", "-")
            .replaceAll("^-|-$", "");
        return name;
    }
}