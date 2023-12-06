package ti4.draft.items;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.jetbrains.annotations.NotNull;
import ti4.draft.DraftItem;
import ti4.generator.Mapper;
import ti4.helpers.Emojis;
import ti4.helpers.Helper;
import ti4.model.FactionModel;
import ti4.model.TechnologyModel;

import java.util.List;

public class StartingTechDraftItem extends DraftItem {
    public StartingTechDraftItem(String itemId) {
        super(Category.STARTINGTECH, itemId);
    }

    private FactionModel getFaction() {
        if (ItemId.equals("keleres")) {
            return Mapper.getFaction("keleresa");
        }
        return Mapper.getFaction(ItemId);
    }
    @Override
    public MessageEmbed getItemCard() {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle(getItemEmoji() + getItemName());
        eb.addField("Starting tech:", getTechString(), true);
        return eb.build();
    }

    @Override
    public String getItemName() {
        return getFaction().getFactionName() + " Starting Tech";
    }

    @NotNull
    private String getTechString() {
        if (ItemId.equals("winnu")) {
            return "Choose any 1 technology that has no prerequisites.";
        } else if (ItemId.equals("argent")) {
            return "Choose TWO of the following: :Biotictech: Neural Motivator, :Cybernetictech: Sarween Tools, :Warfaretech: Plasma Scoring";
        } else if (ItemId.equals("keleres")) {
            return "Choose 2 non-faction technologies owned by other players.";
        } else if (ItemId.equals("bentor")) {
            return "Choose 2 of the following: Psychoarchaeology, Dark Energy Tap, and Scanlink Drone Network.";
        } else if (ItemId.equals("celdauri")) {
            return "Choose 2 of the following: Antimass Deflectors, Sarween Tools, Plasma Scoring";
        } else if (ItemId.equals("cheiran")) {
            return "Choose 1 of the following: Magen Defense Grid, Self-Assembly Routines";
        } else if (ItemId.equals("edyn")) {
            return "Choose any 3 technologies that have different colors and no prerequisites.";
        } else if (ItemId.equals("ghoti")) {
            return "Choose 1 of the following: Gravity Drive, Sling Relay.";
        } else if (ItemId.equals("gledge")) {
            return "Choose 2 of the following: Psychoarchaeology, Scanlink Drone Network, AI Development Algorithm.";
        } else if (ItemId.equals("kjalengard")) {
            return "Choose 1 non-faction unit upgrade.";
        } else if (ItemId.equals("kolume")) {
            return "Choose 1 of the following: Graviton Laser System, Predictive Intelligence.";
        } else if (ItemId.equals("kyro")) {
            return "Choose 1 of the following: Daxcive Animators, Bio-Stims.";
        } else if (ItemId.equals("lanefir")) {
            return "Choose 2 of the following: Dark Energy Tap, Scanlink Drone Network, AI Development Algorithm.";
        } else if (ItemId.equals("nokar")) {
            return "Choose 2 of the following: Psychoarchaeology, Dark Energy Tap, AI Development Algorithm.";
        } else if (ItemId.equals("tnelis")) {
            return "Choose 2 of the following: Neural Motivator, Antimass Deflectors, Plasma Scoring.";
        } else if (ItemId.equals("vaden")) {
            return "Choose 2 of the following: Neural Motivator, Antimass Deflectors, Sarween Tools.";
        }
        List<String> techs = startingTechs();
        StringBuilder builder = new StringBuilder();
        TechnologyModel tech;
        for (int i = 0; i < techs.size() - 1; i++) {
            tech = Mapper.getTech(techs.get(i));
            builder.append(Emojis.getEmojiFromDiscord(tech.getType().toString().toLowerCase() + "tech"));
            builder.append(" ");
            builder.append(tech.getName());
            builder.append(", ");
        }
        tech = Mapper.getTech(techs.get(techs.size() - 1));
        builder.append(Emojis.getEmojiFromDiscord(tech.getType().toString().toLowerCase() + "tech"));
        builder.append(" ");
        builder.append(tech.getName());
        return String.join(",\n", builder.toString());
    }

    private List<String> startingTechs() {
        return getFaction().getStartingTech();
    }

    @Override
    public String getItemEmoji() {
        return Emojis.UnitTechSkip;
    }
}
