package ti4.commands;

import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.unions.IThreadContainerUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import ti4.AsyncTI4DiscordBot;
import ti4.helpers.SlashCommandAcceptanceHelper;
import ti4.map.Game;
import ti4.map.GameManager;
import ti4.map.GameSaveLoadManager;

import static ti4.helpers.SlashCommandAcceptanceHelper.acceptIfHasRoles;

public abstract class GameStateSubcommand extends Subcommand {

    protected final boolean loadGame;
    protected final boolean saveGame;
    private final ThreadLocal<Game> game = new ThreadLocal<>();
    private final ThreadLocal<Long> gameLastModifiedDate = new ThreadLocal<>();

    public GameStateSubcommand(@NotNull String name, @NotNull String description, boolean loadGame, boolean saveGame) {
        super(name, description);
        this.loadGame = loadGame;
        this.saveGame = saveGame;
    }

    @Override
    public boolean accept(SlashCommandInteractionEvent event) {
        return name.equals(event.getInteraction().getSubcommandName()) &&
                acceptIfHasRoles(event, AsyncTI4DiscordBot.adminRoles) || SlashCommandAcceptanceHelper.acceptIfPlayerInGame(event);
    }

    public void preExecute(SlashCommandInteractionEvent event) {
        super.preExecute(event);
        if (loadGame) {
            game.set(GameManager.getGame(getGameName(event)));
            gameLastModifiedDate.set(game.get().getLastModifiedDate());
        }
    }

    private String getGameName(SlashCommandInteractionEvent event) {
        // try to get game name from channel name
        var channel = event.getChannel();
        String gameName = getGameNameFromChannelName(channel.getName());
        if (isValidGame(gameName)) {
            return gameName;
        }
        // if a thread, try to get game name from parent
        if (channel instanceof ThreadChannel) {
            IThreadContainerUnion parentChannel = ((ThreadChannel) channel).getParentChannel();
            gameName = getGameNameFromChannelName(parentChannel.getName());
        }
        if (isValidGame(gameName)) {
            return gameName;
        }
        throw new IllegalArgumentException("Invalid game name: " + gameName + " while attempting to run event " + event.getName() +
                " in channel " + channel.getName());
    }

    @NotNull
    private static String getGameNameFromChannelName(String channelName) {
        return StringUtils.substringBefore(channelName, "-");
    }

    private static boolean isValidGame(String gameName) {
        return gameName != null && GameManager.isValidGame(gameName);
    }

    public void postExecute(SlashCommandInteractionEvent event) {
        super.postExecute(event);
        if (loadGame && saveGame && gameLastModifiedDate.get() != game.get().getLastModifiedDate()) {
            GameSaveLoadManager.saveGame(game.get(), event);
        }
        game.remove();
        gameLastModifiedDate.remove();
    }

    @NotNull
    protected Game getGame() {
        return game.get();
    }
}
