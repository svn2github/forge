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
package forge.game;

import forge.game.card.Card;
import forge.game.event.GameEventCardAttachment;
import forge.game.event.GameEventCardAttachment.AttachMethod;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TreeMap;


public abstract class GameEntity extends GameObject {
    private String name = "";
    private int preventNextDamage = 0;
    private LinkedHashSet<Card> enchantedBy;
    private TreeMap<Card, Map<String, String>> preventionShieldsWithEffects = new TreeMap<Card, Map<String, String>>();

    public String getName() {
        return name;
    }
    public void setName(final String s) {
        name = s;
    }

    public boolean addDamage(final int damage, final Card source) {
        int damageToDo = damage;

        damageToDo = replaceDamage(damageToDo, source, false);
        damageToDo = preventDamage(damageToDo, source, false);

        return addDamageAfterPrevention(damageToDo, source, false);
    }

    public boolean addDamageWithoutPrevention(final int damage, final Card source) {
        int damageToDo = replaceDamage(damage, source, false);
        return addDamageAfterPrevention(damageToDo, source, false);
    }

    // This function handles damage after replacement and prevention effects are applied
    public abstract boolean addDamageAfterPrevention(final int damage, final Card source, final boolean isCombat);

    // This should be also usable by the AI to forecast an effect (so it must
    // not change the game state)
    public int staticDamagePrevention(final int damage, final Card source, final boolean isCombat, final boolean isTest) {
        return 0;
    }

    // This should be also usable by the AI to forecast an effect (so it must
    // not change the game state)
    public int staticReplaceDamage(final int damage, final Card source, final boolean isCombat) {
        return 0;
    }

    public abstract int replaceDamage(final int damage, final Card source, final boolean isCombat);

    public abstract int preventDamage(final int damage, final Card source, final boolean isCombat);

    public int getPreventNextDamage() {
        return preventNextDamage;
    }
    public void setPreventNextDamage(final int n) {
        preventNextDamage = n;
    }
    public void addPreventNextDamage(final int n) {
        preventNextDamage += n;
    }
    public void subtractPreventNextDamage(final int n) {
        preventNextDamage -= n;
    }
    public void resetPreventNextDamage() {
        preventNextDamage = 0;
    }

    // PreventNextDamageWithEffect
    public TreeMap<Card, Map<String, String>> getPreventNextDamageWithEffect() {
        return preventionShieldsWithEffects;
    }
    public int getPreventNextDamageTotalShields() {
        int shields = preventNextDamage;
        for (final Map<String, String> value : preventionShieldsWithEffects.values()) {
            shields += Integer.valueOf(value.get("ShieldAmount"));
        }
        return shields;
    }
    /**
     * Adds a damage prevention shield with an effect that happens at time of prevention.
     * @param shieldSource - The source card which generated the shield
     * @param effectMap - A map of the effect occurring with the damage prevention
     */
    public void addPreventNextDamageWithEffect(final Card shieldSource, TreeMap<String, String> effectMap) {
        if (preventionShieldsWithEffects.containsKey(shieldSource)) {
            int currentShields = Integer.valueOf(preventionShieldsWithEffects.get(shieldSource).get("ShieldAmount"));
            currentShields += Integer.valueOf(effectMap.get("ShieldAmount"));
            effectMap.put("ShieldAmount", Integer.toString(currentShields));
            preventionShieldsWithEffects.put(shieldSource, effectMap);
        } else {
            preventionShieldsWithEffects.put(shieldSource, effectMap);
        }
    }
    public void subtractPreventNextDamageWithEffect(final Card shieldSource, final int n) {
        int currentShields = Integer.valueOf(preventionShieldsWithEffects.get(shieldSource).get("ShieldAmount"));
        if (currentShields > n) {
            preventionShieldsWithEffects.get(shieldSource).put("ShieldAmount", String.valueOf(currentShields - n));
        } else {
            preventionShieldsWithEffects.remove(shieldSource);
        }
    }
    public void resetPreventNextDamageWithEffect() {
        preventionShieldsWithEffects = new TreeMap<Card, Map<String, String>>();
    }

    public boolean hasKeyword(final String keyword) {
        return false;
    }

    // GameEntities can now be Enchanted
    public final Iterable<Card> getEnchantedBy(boolean allowModify) {
        if (enchantedBy == null) {
            return new LinkedHashSet<Card>();
        }
        if (allowModify) { //create copy to allow modifying original set while iterating
            return new LinkedHashSet<Card>(enchantedBy);
        }
        return enchantedBy;
    }
    public final void setEnchantedBy(final LinkedHashSet<Card> list) {
        enchantedBy = list;
        getView().updateEnchantedBy(this);
    }
    public final void setEnchantedBy(final Iterable<Card> list) {
        if (list == null) {
            enchantedBy = null;
        }
        else {
            enchantedBy = new LinkedHashSet<Card>();
            for (Card c : list) {
                enchantedBy.add(c);
            }
        }
        getView().updateEnchantedBy(this);
    }
    public final boolean isEnchanted() {
        return enchantedBy != null && !enchantedBy.isEmpty();
    }
    public final boolean isEnchantedBy(Card c) {
        return enchantedBy != null && enchantedBy.contains(c);
    }
    public final boolean isEnchantedBy(final String cardName) {
        for (final Card aura : getEnchantedBy(false)) {
            if (aura.getName().equals(cardName)) {
                return true;
            }
        }
        return false;
    }
    public final void addEnchantedBy(final Card c) {
        if (enchantedBy == null) {
            enchantedBy = new LinkedHashSet<Card>();
        }
        enchantedBy.add(c);
        getView().updateEnchantedBy(this);
        getGame().fireEvent(new GameEventCardAttachment(c, null, this, AttachMethod.Enchant));
    }
    public final void removeEnchantedBy(final Card c) {
        if (enchantedBy == null) { return; }

        if (enchantedBy.remove(c)) {
            getView().updateEnchantedBy(this);
            getGame().fireEvent(new GameEventCardAttachment(c, this, null, AttachMethod.Enchant));
        }
    }
    public final void unEnchantAllCards() {
        if (isEnchanted()) {
            for (Card c : getEnchantedBy(true)) {
                c.unEnchantEntity(this);
            }
        }
    }

    public boolean hasProtectionFrom(final Card source) {
        return false;
    }

    @Override
    public String toString() {
        return name;
    }

    public abstract Game getGame();
    public abstract GameEntityView<?> getView();
}
