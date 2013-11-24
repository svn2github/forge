package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import forge.ai.ComputerUtilCard;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class ChooseColorEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        for (final Player p : getTargetPlayers(sa)) {
            sb.append(p).append(" ");
        }
        sb.append("chooses a color");
        if (sa.hasParam("OrColors")) {
            sb.append(" or colors");
        }
        sb.append(".");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getSourceCard();

        List<String> colorChoices = new ArrayList<String>(MagicColor.Constant.ONLY_COLORS);
        if (sa.hasParam("Choices")) {
            String[] restrictedChoices = sa.getParam("Choices").split(",");
            colorChoices = Arrays.asList(restrictedChoices);
        }

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        final TargetRestrictions tgt = sa.getTargetRestrictions();

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (p.isHuman()) {
                    if (sa.hasParam("OrColors")) {
                        final List<String> o = GuiChoose.getChoices("Choose a color or colors", 1, colorChoices.size(), colorChoices);
                        card.setChosenColor(new ArrayList<String>(o));
                    } else if (sa.hasParam("TwoColors")) {
                        final List<String> o = GuiChoose.getChoices("Choose two colors", 2, 2, colorChoices);
                        card.setChosenColor(new ArrayList<String>(o));
                    } else {
                        final Object o = GuiChoose.one("Choose a color", colorChoices);
                        if (null == o) {
                            return;
                        }
                        final String choice = (String) o;
                        final ArrayList<String> tmpColors = new ArrayList<String>();
                        tmpColors.add(choice);
                        card.setChosenColor(tmpColors);
                    }
                } else {
                    List<String> chosen = new ArrayList<String>();
                    Player ai = sa.getActivatingPlayer();
                    final Game game = ai.getGame();
                    Player opp = ai.getOpponent();
                    if (sa.hasParam("AILogic")) {
                        final String logic = sa.getParam("AILogic");
                        if (logic.equals("MostProminentInHumanDeck")) {
                            chosen.add(ComputerUtilCard.getMostProminentColor(CardLists.filterControlledBy(game.getCardsInGame(),
                                    opp), colorChoices));
                        } else if (logic.equals("MostProminentInComputerDeck")) {
                            chosen.add(ComputerUtilCard.getMostProminentColor(CardLists.filterControlledBy(game.getCardsInGame(),
                                    ai), colorChoices));
                        } else if (logic.equals("MostProminentDualInComputerDeck")) {
                            List<String> prominence = ComputerUtilCard.getColorByProminence(CardLists.filterControlledBy(game.getCardsInGame(), ai));
                            chosen.add(prominence.get(0));
                            chosen.add(prominence.get(1));
                        }
                        else if (logic.equals("MostProminentInGame")) {
                            chosen.add(ComputerUtilCard.getMostProminentColor(game.getCardsInGame(), colorChoices));
                        }
                        else if (logic.equals("MostProminentHumanCreatures")) {
                            List<Card> list = opp.getCreaturesInPlay();
                            if (list.isEmpty()) {
                                list = CardLists.filter(CardLists.filterControlledBy(game.getCardsInGame(), opp), CardPredicates.Presets.CREATURES);
                            }
                            chosen.add(ComputerUtilCard.getMostProminentColor(list, colorChoices));
                        }
                        else if (logic.equals("MostProminentComputerControls")) {
                            chosen.add(ComputerUtilCard.getMostProminentColor(ai.getCardsIn(ZoneType.Battlefield), colorChoices));
                        }
                        else if (logic.equals("MostProminentHumanControls")) {
                            chosen.add(ComputerUtilCard.getMostProminentColor(ai.getOpponent().getCardsIn(ZoneType.Battlefield), colorChoices));
                        }
                        else if (logic.equals("MostProminentPermanent")) {
                            final List<Card> list = game.getCardsIn(ZoneType.Battlefield);
                            chosen.add(ComputerUtilCard.getMostProminentColor(list, colorChoices));
                        }
                        else if (logic.equals("MostProminentAttackers") && game.getPhaseHandler().inCombat()) {
                            chosen.add(ComputerUtilCard.getMostProminentColor(game.getCombat().getAttackers(), colorChoices));
                        }
                        else if (logic.equals("MostProminentKeywordInComputerDeck")) {
                            List<Card> list = ai.getAllCards();
                            int max = 0;
                            String chosenColor = MagicColor.Constant.WHITE;

                            for (final String c : MagicColor.Constant.ONLY_COLORS) {
                                final int cmp = CardLists.filter(list, CardPredicates.containsKeyword(c)).size();
                                if (cmp > max) {
                                    max = cmp;
                                    chosenColor = c;
                                }
                            }
                            chosen.add(chosenColor);
                        }
                    }
                    if (chosen.size() == 0) {
                        chosen.add(MagicColor.Constant.GREEN);
                    }
                    GuiChoose.one("Computer picked: ", chosen);
                    final ArrayList<String> colorTemp = new ArrayList<String>();
                    colorTemp.addAll(chosen);
                    card.setChosenColor(colorTemp);
                }
            }
        }
    }

}