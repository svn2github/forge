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
package forge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.esotericsoftware.minlog.Log;

import forge.Constant.Zone;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.replacement.ReplacementEffect;
import forge.card.spellability.SpellAbility;
import forge.card.staticability.StaticAbility;

/**
 * <p>
 * StaticEffects class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class StaticEffects {

    // **************** StaticAbility system **************************
    /**
     * staticEffects.
     */
    private ArrayList<StaticEffect> staticEffects;

    /**
     * clearStaticEffect. TODO Write javadoc for this method.
     */
    public final void clearStaticEffects() {
        // remove all static effects
        for (int i = 0; i < this.staticEffects.size(); i++) {
            this.removeStaticEffect(this.staticEffects.get(i));
        }
        this.staticEffects = new ArrayList<StaticEffect>();

        AllZone.getTriggerHandler().cleanUpTemporaryTriggers();
    }

    /**
     * addStaticEffect. TODO Write javadoc for this method.
     * 
     * @param staticEffect
     *            a StaticEffect
     */
    public final void addStaticEffect(final StaticEffect staticEffect) {
        this.staticEffects.add(staticEffect);
    }

    /**
     * removeStaticEffect TODO Write javadoc for this method.
     * 
     * @param se
     *            a StaticEffect
     */
    final void removeStaticEffect(final StaticEffect se) {
        final CardList affectedCards = se.getAffectedCards();
        final ArrayList<Player> affectedPlayers = se.getAffectedPlayers();
        final HashMap<String, String> params = se.getParams();

        int powerBonus = 0;
        int toughnessBonus = 0;
        boolean setPT = false;
        String[] addKeywords = null;
        String[] addHiddenKeywords = null;
        String addColors = null;

        if (params.containsKey("SetPower") || params.containsKey("SetToughness")) {
            setPT = true;
        }

        if (params.containsKey("AddPower")) {
            if (params.get("AddPower").equals("X")) {
                powerBonus = se.getXValue();
            } else if (params.get("AddPower").equals("Y")) {
                powerBonus = se.getYValue();
            } else {
                powerBonus = Integer.valueOf(params.get("AddPower"));
            }
        }

        if (params.containsKey("AddToughness")) {
            if (params.get("AddToughness").equals("X")) {
                toughnessBonus = se.getXValue();
            } else if (params.get("AddToughness").equals("Y")) {
                toughnessBonus = se.getYValue();
            } else {
                toughnessBonus = Integer.valueOf(params.get("AddToughness"));
            }
        }

        if (params.containsKey("AddHiddenKeyword")) {
            addHiddenKeywords = params.get("AddHiddenKeyword").split(" & ");
        }

        if (params.containsKey("AddColor")) {
            addColors = CardUtil.getShortColorsString(new ArrayList<String>(Arrays.asList(params.get("AddColor").split(
                    " & "))));
        }

        if (params.containsKey("SetColor")) {
            addColors = CardUtil.getShortColorsString(new ArrayList<String>(Arrays.asList(params.get("SetColor").split(
                    " & "))));
        }

        // modify players
        for (final Player p : affectedPlayers) {
            if (params.containsKey("AddKeyword")) {
                addKeywords = params.get("AddKeyword").split(" & ");
            }

            // add keywords
            if (addKeywords != null) {
                for (final String keyword : addKeywords) {
                    p.removeKeyword(keyword);
                }
            }
        }

        // modify the affected card
        for (int i = 0; i < affectedCards.size(); i++) {
            final Card affectedCard = affectedCards.get(i);

            // remove set P/T
            if (!params.containsKey("CharacteristicDefining") && setPT) {
                affectedCard.removeNewPT(se.getTimestamp());
            }

            // remove P/T bonus
            affectedCard.addSemiPermanentAttackBoost(powerBonus * -1);
            affectedCard.addSemiPermanentDefenseBoost(toughnessBonus * -1);

            // remove keywords
            if (params.containsKey("AddKeyword") || params.containsKey("RemoveKeyword")
                    || params.containsKey("RemoveAllAbilities")) {
                affectedCard.removeChangedCardKeywords(se.getTimestamp());
            }

            // remove abilities
            if (params.containsKey("AddAbility")) {
                final SpellAbility[] spellAbility = affectedCard.getSpellAbility();
                for (final SpellAbility s : spellAbility) {
                    if (s.getType().equals("Temporary")) {
                        affectedCard.removeSpellAbility(s);
                    }
                }
            }

            if (addHiddenKeywords != null) {
                for (final String k : addHiddenKeywords) {
                    affectedCard.removeExtrinsicKeyword(k);
                }
            }

            // remove abilities
            if (params.containsKey("RemoveAllAbilities")) {
                final ArrayList<SpellAbility> abilities = affectedCard.getSpellAbilities();
                for (final SpellAbility ab : abilities) {
                    ab.setTemporarilySuppressed(false);
                }
                final ArrayList<StaticAbility> staticAbilities = affectedCard.getStaticAbilities();
                for (final StaticAbility stA : staticAbilities) {
                    stA.setTemporarilySuppressed(false);
                }
                final ArrayList<ReplacementEffect> replacementEffects = affectedCard.getReplacementEffects();
                for (final ReplacementEffect rE : replacementEffects) {
                    rE.setTemporarilySuppressed(false);
                }
            }

            // remove Types
            if (params.containsKey("AddType") || params.containsKey("RemoveType")) {
                affectedCard.removeChangedCardTypes(se.getTimestamp());
            }

            // remove colors
            if (addColors != null) {
                affectedCard.removeColor(addColors, affectedCard, !se.isOverwriteColors(),
                        se.getTimestamp(affectedCard));
            }
        }
        se.clearTimestamps();
    }

    // **************** End StaticAbility system **************************

    // this is used to keep track of all state-based effects in play:
    private final HashMap<String, Integer> stateBasedMap = new HashMap<String, Integer>();

    // this is used to define all cards that are state-based effects, and map
    // the
    // corresponding commands to their cardnames
    /** Constant <code>cardToEffectsList</code>. */
    private static HashMap<String, String[]> cardToEffectsList = new HashMap<String, String[]>();

    /**
     * <p>
     * Constructor for StaticEffects.
     * </p>
     */
    public StaticEffects() {
        this.initStateBasedEffectsList();
        this.staticEffects = new ArrayList<StaticEffect>();
    }

    /**
     * <p>
     * initStateBasedEffectsList.
     * </p>
     */
    public final void initStateBasedEffectsList() {
        // value has to be an array, since certain cards have multiple commands
        // associated with them

        StaticEffects.cardToEffectsList.put("Avatar", new String[] { "Ajani_Avatar_Token" });
        StaticEffects.cardToEffectsList.put("Alpha Status", new String[] { "Alpha_Status" });
        StaticEffects.cardToEffectsList.put("Coat of Arms", new String[] { "Coat_of_Arms" });
        StaticEffects.cardToEffectsList.put("Homarid", new String[] { "Homarid" });
        StaticEffects.cardToEffectsList.put("Liu Bei, Lord of Shu", new String[] { "Liu_Bei" });

        StaticEffects.cardToEffectsList.put("Old Man of the Sea", new String[] { "Old_Man_of_the_Sea" });
        StaticEffects.cardToEffectsList.put("Tarmogoyf", new String[] { "Tarmogoyf" });
        StaticEffects.cardToEffectsList.put("Umbra Stalker", new String[] { "Umbra_Stalker" });
        StaticEffects.cardToEffectsList.put("Wolf", new String[] { "Sound_the_Call_Wolf" });

    }

    /**
     * <p>
     * Getter for the field <code>cardToEffectsList</code>.
     * </p>
     * 
     * @return a {@link java.util.HashMap} object.
     */
    public final HashMap<String, String[]> getCardToEffectsList() {
        return StaticEffects.cardToEffectsList;
    }

    /**
     * <p>
     * addStateBasedEffect.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void addStateBasedEffect(final String s) {
        if (this.stateBasedMap.containsKey(s)) {
            this.stateBasedMap.put(s, this.stateBasedMap.get(s) + 1);
        } else {
            this.stateBasedMap.put(s, 1);
        }
    }

    /**
     * <p>
     * removeStateBasedEffect.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void removeStateBasedEffect(final String s) {
        if (this.stateBasedMap.containsKey(s)) {
            this.stateBasedMap.put(s, this.stateBasedMap.get(s) - 1);
            if (this.stateBasedMap.get(s) == 0) {
                this.stateBasedMap.remove(s);
            }
        }
    }

    /**
     * <p>
     * Getter for the field <code>stateBasedMap</code>.
     * </p>
     * 
     * @return a {@link java.util.HashMap} object.
     */
    public final HashMap<String, Integer> getStateBasedMap() {
        return this.stateBasedMap;
    }

    /**
     * <p>
     * reset.
     * </p>
     */
    public final void reset() {
        this.stateBasedMap.clear();
    }

    /**
     * <p>
     * rePopulateStateBasedList.
     * </p>
     */
    public final void rePopulateStateBasedList() {
        this.reset();

        final CardList cards = AllZoneUtil.getCardsIn(Zone.Battlefield);

        Log.debug("== Start add state effects ==");
        for (int i = 0; i < cards.size(); i++) {
            final Card c = cards.get(i);
            if (StaticEffects.cardToEffectsList.containsKey(c.getName())) {
                final String[] effects = this.getCardToEffectsList().get(c.getName());
                for (final String effect : effects) {
                    this.addStateBasedEffect(effect);
                    Log.debug("Added " + effect);
                }
            }
            if (c.isEmblem() && !CardFactoryUtil.checkEmblemKeyword(c).equals("")) {
                final String s = CardFactoryUtil.checkEmblemKeyword(c);
                this.addStateBasedEffect(s);
                Log.debug("Added " + s);
            }
        }
        Log.debug("== End add state effects ==");

    }

} // end class StaticEffects
