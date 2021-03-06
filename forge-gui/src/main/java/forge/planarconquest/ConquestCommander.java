package forge.planarconquest;

import forge.deck.Deck;
import forge.deck.generation.DeckGenPool;
import forge.item.PaperCard;
import forge.planarconquest.ConquestPlane.Region;

public class ConquestCommander {
    private final PaperCard card;
    private final Deck deck;

    private int stars; //think 1-star/4-star general
    private Region deployedRegion;
    private ConquestAction currentDayAction;

    public ConquestCommander(PaperCard card0, DeckGenPool cardPool0, boolean forAi, Region deployedRegion0) {
        card = card0;
        deck = ConquestUtil.generateDeck(card0, cardPool0, forAi);
        deployedRegion = deployedRegion0;
    }

    public String getName() {
        return card.getName();
    }

    public String getPlayerName() {
        String name = card.getName();
        int idx = name.indexOf(' ');
        if (idx != -1) {
            name = name.substring(0, idx);
        }
        if (name.endsWith(",") || name.endsWith("-")) {
            name = name.substring(0, name.length() - 1).trim();
        }
        return name;
    }

    public PaperCard getCard() {
        return card;
    }

    public Deck getDeck() {
        return deck;
    }

    public Region getDeployedRegion() {
        return deployedRegion;
    }
    public void setDeployedRegion(Region deployedRegion0) {
        deployedRegion = deployedRegion0;
    }

    public ConquestAction getCurrentDayAction() {
        return currentDayAction;
    }
    public void setCurrentDayAction(ConquestAction currentDayAction0) {
        currentDayAction = currentDayAction0;
    }

    public int getStars() {
        return stars;
    }

    public String toString() {
        return card.getName();
    }
}
