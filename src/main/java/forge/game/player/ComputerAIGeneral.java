/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.esotericsoftware.minlog.Log;

import forge.Card;

import forge.card.abilityfactory.ApiType;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.replacement.ReplaceMoved;
import forge.card.replacement.ReplacementEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellPermanent;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerType;
import forge.game.GameState;
import forge.game.zone.ZoneType;

/**
 * <p>
 * ComputerAI_General class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ComputerAIGeneral implements Computer {

    private final Player player;
    private final GameState game;
    /**
     * <p>
     * Constructor for ComputerAI_General.
     * </p>
     */
    public ComputerAIGeneral(final Player computerPlayer, final GameState game0) {
        player = computerPlayer;
        game = game0;
    }

    /**
     * <p>
     * main.
     * </p>
     */
    @Override
    public final void playLands()
    {
        List<Card> landsWannaPlay = ComputerUtil.getLandsToPlay(player);
        
        while(landsWannaPlay != null && !landsWannaPlay.isEmpty() && player.canPlayLand()) {
            Card land = ComputerUtil.chooseBestLandToPlay(landsWannaPlay, player);
            landsWannaPlay.remove(land);
            player.playLand(land);
        }
    }
    

    /**
     * <p>
     * hasACardGivingHaste.
     * </p>
     * 
     * @return a boolean.
     */
    public static boolean hasACardGivingHaste(final Player ai) {
        final List<Card> all = new ArrayList<Card>(ai.getCardsIn(ZoneType.Battlefield));
        all.addAll(CardFactoryUtil.getExternalZoneActivationCards(ai));
        all.addAll(ai.getCardsIn(ZoneType.Hand));

        for (final Card c : all) {
            for (final SpellAbility sa : c.getSpellAbility()) {

                if (sa.getApi() == null) {
                    continue;
                }

                /// ????
                // if ( sa.isAbility() || sa.isSpell() && sa.getApi() != ApiType.Pump ) continue
                if (sa.hasParam("AB") && !sa.getParam("AB").equals("Pump")) {
                    continue;
                }
                if (sa.hasParam("SP") && !sa.getParam("SP").equals("Pump")) {
                    continue;
                }

                if (sa.hasParam("KW") && sa.getParam("KW").contains("Haste")) {
                    return true;
                }
            }
        }

        return false;
    } // hasACardGivingHaste

    /**
     * <p>
     * getAvailableSpellAbilities.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    private List<Card> getAvailableCards() {

        final Player opp = player.getOpponent();
        List<Card> all = new ArrayList<Card>(player.getCardsIn(ZoneType.Hand));
        all.addAll(player.getCardsIn(ZoneType.Battlefield));
        all.addAll(player.getCardsIn(ZoneType.Exile));
        all.addAll(player.getCardsIn(ZoneType.Graveyard));
        all.addAll(player.getCardsIn(ZoneType.Command));
        if (!player.getCardsIn(ZoneType.Library).isEmpty()) {
            all.add(player.getCardsIn(ZoneType.Library).get(0));
        }
        all.addAll(opp.getCardsIn(ZoneType.Exile));
        all.addAll(opp.getCardsIn(ZoneType.Battlefield));
        return all;
    }

    /**
     * <p>
     * hasETBTrigger.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean hasETBTrigger(final Card card) {
        for (final Trigger tr : card.getTriggers()) {
            final HashMap<String, String> params = tr.getMapParams();
            if (tr.getMode() != TriggerType.ChangesZone) {
                continue;
            }

            if (!params.get("Destination").equals(ZoneType.Battlefield.toString())) {
                continue;
            }

            if (params.containsKey("ValidCard") && !params.get("ValidCard").contains("Self")) {
                continue;
            }
            return true;
        }
        return false;
    }

    /**
     * <p>
     * hasETBTrigger.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean hasETBReplacement(final Card card) {
        for (final ReplacementEffect re : card.getReplacementEffects()) {
            final HashMap<String, String> params = re.getMapParams();
            if (!(re instanceof ReplaceMoved)) {
                continue;
            }

            if (!params.get("Destination").equals(ZoneType.Battlefield.toString())) {
                continue;
            }

            if (params.containsKey("ValidCard") && !params.get("ValidCard").contains("Self")) {
                continue;
            }
            return true;
        }
        return false;
    }

    /**
     * <p>
     * getPossibleETBCounters.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    private ArrayList<SpellAbility> getPossibleETBCounters() {

        final Player opp = player.getOpponent();
        List<Card> all = new ArrayList<Card>(player.getCardsIn(ZoneType.Hand));
        all.addAll(player.getCardsIn(ZoneType.Exile));
        all.addAll(player.getCardsIn(ZoneType.Graveyard));
        if (!player.getCardsIn(ZoneType.Library).isEmpty()) {
            all.add(player.getCardsIn(ZoneType.Library).get(0));
        }
        all.addAll(opp.getCardsIn(ZoneType.Exile));

        final ArrayList<SpellAbility> spellAbilities = new ArrayList<SpellAbility>();
        for (final Card c : all) {
            for (final SpellAbility sa : c.getNonManaSpellAbilities()) {
                if (sa instanceof SpellPermanent) {
                    // TODO ArsenalNut (13 Oct 2012) added line to set activating player to fix NPE problem
                    // in checkETBEffects.  There is SpellPermanent.checkETBEffects where the player can be
                    // directly input but it is currently a private method.
                    sa.setActivatingPlayer(player);
                    if (SpellPermanent.checkETBEffects(c, sa, ApiType.Counter)) {
                        spellAbilities.add(sa);
                    }
                }
            }
        }
        return spellAbilities;
    }

    /**
     * Returns the spellAbilities from the card list.
     * 
     * @param l
     *            a {@link forge.CardList} object.
     * @return an array of {@link forge.card.spellability.SpellAbility} objects.
     */
    private ArrayList<SpellAbility> getSpellAbilities(final List<Card> l) {
        final ArrayList<SpellAbility> spellAbilities = new ArrayList<SpellAbility>();
        for (final Card c : l) {
            for (final SpellAbility sa : c.getNonManaSpellAbilities()) {
                spellAbilities.add(sa);
            }
        }
        return spellAbilities;
    }

    /**
     * <p>
     * getPlayableCounters.
     * </p>
     * 
     * @param l
     *            a {@link forge.CardList} object.
     * @return a {@link java.util.ArrayList} object.
     */
    private ArrayList<SpellAbility> getPlayableCounters(final List<Card> l) {
        final ArrayList<SpellAbility> spellAbility = new ArrayList<SpellAbility>();
        for (final Card c : l) {
            for (final SpellAbility sa : c.getNonManaSpellAbilities()) {
                // Check if this AF is a Counterpsell
                if (sa.getApi() == ApiType.Counter) {
                    spellAbility.add(sa);
                }
            }
        }

        return spellAbility;
    }

    /**
     * <p>
     * declare_attackers.
     * </p>
     */
    @Override
    public final void declareAttackers() {
        // 12/2/10(sol) the decision making here has moved to getAttackers()
        game.setCombat(ComputerUtil.getAttackers(player));

        final List<Card> att = game.getCombat().getAttackers();
        if (!att.isEmpty()) {
            game.getPhaseHandler().setCombat(true);
        }

        for (final Card element : att) {
            // tapping of attackers happens after Propaganda is paid for
            final StringBuilder sb = new StringBuilder();
            sb.append("Computer just assigned ").append(element.getName()).append(" as an attacker.");
            Log.debug(sb.toString());
        }

        player.getZone(ZoneType.Battlefield).updateObservers();

        game.getPhaseHandler().setPlayersPriorityPermission(false);

        // ai is about to attack, cancel all phase skipping
        for (Player p : game.getPlayers()) {
            p.getController().autoPassCancel();
        }
    }


    /**
     * <p>
     * stack_not_empty.
     * </p>
     */
    @Override
    public final List<SpellAbility> getSpellAbilitiesToPlay() {
        // if top of stack is owned by me
        if (!game.getStack().isEmpty() && game.getStack().peekInstance().getActivatingPlayer().equals(player)) {
            // probably should let my stuff resolve
            return null;
        }
        final List<Card> cards = getAvailableCards();

        if ( !game.getStack().isEmpty() ) {
            List<SpellAbility> counter = ComputerUtil.playCounterSpell(player, getPlayableCounters(cards), game);
            if( counter == null || counter.isEmpty())
                return counter;
    
            List<SpellAbility> counterETB = ComputerUtil.playSpellAbilities(player, this.getPossibleETBCounters(), game);
            if( counterETB == null || counterETB.isEmpty())
                return counterETB;
        }

        return ComputerUtil.playSpellAbilities(player, getSpellAbilities(cards), game);
    }
    
    @Override
    public Player getPlayer()
    {
        return player;
    }
}
