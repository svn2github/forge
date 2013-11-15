package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.card.ability.AbilityFactory;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.TargetRestrictions;
import forge.game.ai.ComputerUtilCard;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;

public class TwoPilesEffect extends SpellAbilityEffect {

    // *************************************************************************
    // ***************************** TwoPiles **********************************
    // *************************************************************************

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        String valid = "";
        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        sb.append("Separate all ").append(valid).append(" cards ");

        for (final Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("controls into two piles.");
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getSourceCard();
        ZoneType zone = null;
        boolean pile1WasChosen = true;

        if (sa.hasParam("Zone")) {
            zone = ZoneType.smartValueOf(sa.getParam("Zone"));
        }

        String valid = "";
        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        Player separator = card.getController();
        if (sa.hasParam("Separator")) {
            separator = AbilityUtils.getDefinedPlayers(sa.getSourceCard(), sa.getParam("Separator"), sa).get(0);
        }

        Player chooser = tgtPlayers.get(0);
        if (sa.hasParam("Chooser")) {
            chooser = AbilityUtils.getDefinedPlayers(sa.getSourceCard(), sa.getParam("Chooser"), sa).get(0);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                final ArrayList<Card> pile1 = new ArrayList<Card>();
                final ArrayList<Card> pile2 = new ArrayList<Card>();
                List<Card> pool = new ArrayList<Card>();
                if (sa.hasParam("DefinedCards")) {
                    pool = new ArrayList<Card>(AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("DefinedCards"), sa));
                } else {
                    pool = p.getCardsIn(zone);
                }
                pool = CardLists.getValidCards(pool, valid, card.getController(), card);
                int size = pool.size();

                // first, separate the cards into piles
                if (separator.isHuman()) {
                    final List<Card> firstPile = GuiChoose.order("Place into two piles", "Pile 1", -1, pool, null, card);
                    for (final Object o : firstPile) {
                        pile1.add((Card) o);
                    }

                    for (final Card c : pool) {
                        if (!pile1.contains(c)) {
                            pile2.add(c);
                        }
                    }
                } else if (size > 0) {
                    //computer separates
                    Card biggest = null;
                    Card smallest = null;
                    biggest = pool.get(0);
                    smallest = pool.get(0);

                    for (Card c : pool) {
                        if (c.getCMC() >= biggest.getCMC()) {
                            biggest = c;
                        } else if (c.getCMC() <= smallest.getCMC()) {
                            smallest = c;
                        }
                    }
                    pile1.add(biggest);

                    if (size > 3 && !pile1.contains(smallest)) {
                        pile1.add(smallest);
                    }
                    for (Card c : pool) {
                        if (!pile1.contains(c)) {
                            pile2.add(c);
                        }
                    }
                }

                System.out.println("Pile 1:" + pile1);
                System.out.println("Pile 2:" + pile2);
                card.clearRemembered();

                pile1WasChosen = selectPiles(sa, pile1, pile2, chooser, card, pool);

                // take action on the chosen pile
                if (sa.hasParam("ChosenPile")) {
                    final SpellAbility action = AbilityFactory.getAbility(card.getSVar(sa.getParam("ChosenPile")), card);
                    action.setActivatingPlayer(sa.getActivatingPlayer());
                    ((AbilitySub) action).setParent(sa);

                    AbilityUtils.resolve(action);
                }

                // take action on the chosen pile
                if (sa.hasParam("UnchosenPile")) {
                    //switch the remembered cards
                    card.clearRemembered();
                    if (pile1WasChosen) {
                        for (final Card c : pile2) {
                            card.addRemembered(c);
                        }
                    } else {
                        for (final Card c : pile1) {
                            card.addRemembered(c);
                        }
                    }
                    final SpellAbility action = AbilityFactory.getAbility(card.getSVar(sa.getParam("UnchosenPile")), card);
                    action.setActivatingPlayer(sa.getActivatingPlayer());
                    ((AbilitySub) action).setParent(sa);

                    AbilityUtils.resolve(action);
                }
            }
        }
    } // end twoPiles resolve

    private boolean selectPiles(final SpellAbility sa, ArrayList<Card> pile1, ArrayList<Card> pile2,
            Player chooser, Card card, List<Card> pool) {
        boolean pile1WasChosen = true;
        // then, the chooser picks a pile

        if (sa.hasParam("FaceDown")) {
            // Used for Phyrexian Portal, FaceDown Pile choosing
            if (chooser.isHuman()) {
                final String p1Str = String.format("Pile 1 (%s cards)", pile1.size());
                final String p2Str = String.format("Pile 2 (%s cards)", pile2.size());
                final String[] possibleValues = { p1Str , p2Str };

                pile1WasChosen = GuiDialog.confirm(card, "Choose a Pile", possibleValues);
            }
            else {
                // AI will choose the first pile if it is larger or the same
                // TODO Improve this to be slightly more random to not be so predictable
                pile1WasChosen = pile1.size() >= pile2.size();
            }
        } else {
            if (chooser.isHuman()) {
                final Card[] disp = new Card[pile1.size() + pile2.size() + 2];
                disp[0] = new Card(-1);
                disp[0].setName("Pile 1");
                for (int i = 0; i < pile1.size(); i++) {
                    disp[1 + i] = pile1.get(i);
                }
                disp[pile1.size() + 1] = new Card(-2);
                disp[pile1.size() + 1].setName("Pile 2");
                for (int i = 0; i < pile2.size(); i++) {
                    disp[pile1.size() + i + 2] = pile2.get(i);
                }

                // make sure Pile 1 or Pile 2 is clicked on
                while (true) {
                    final Object o = GuiChoose.one("Choose a pile", disp);
                    final Card c = (Card) o;
                    String name = c.getName();

                    if (!(name.equals("Pile 1") || name.equals("Pile 2"))) {
                        continue;
                    }

                    pile1WasChosen = name.equals("Pile 1");
                    break;
                }
            } else {
                int cmc1 = ComputerUtilCard.evaluatePermanentList(new ArrayList<Card>(pile1));
                int cmc2 = ComputerUtilCard.evaluatePermanentList(new ArrayList<Card>(pile2));
                if (CardLists.getNotType(pool, "Creature").isEmpty()) {
                    cmc1 = ComputerUtilCard.evaluateCreatureList(new ArrayList<Card>(pile1));
                    cmc2 = ComputerUtilCard.evaluateCreatureList(new ArrayList<Card>(pile2));
                    System.out.println("value:" + cmc1 + " " + cmc2);
                }

                // for now, this assumes that the outcome will be bad
                // TODO: This should really have a ChooseLogic param to
                // figure this out
                pile1WasChosen = cmc1 >= cmc2;
                if ("Worst".equals(sa.getParam("AILogic"))) {
                    pile1WasChosen = !pile1WasChosen;
                }
                GuiDialog.message("Computer chooses the Pile " + (pile1WasChosen ? "1" : "2"));
            }
        }

        if (pile1WasChosen) {
            for (final Card z : pile1) {
                card.addRemembered(z);
            }
        } else {
            for (final Card z : pile2) {
                card.addRemembered(z);
            }
        }

        return pile1WasChosen;
    }
}