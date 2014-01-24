package forge.game.ability.effects;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.game.ability.SpellAbilityEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

public class DrainManaEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        sb.append(StringUtils.join(tgtPlayers, ", "));
        sb.append(" empties his or her mana pool.");

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        for (final Player p : getTargetPlayers(sa)) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                p.getManaPool().clearPool(false);
            }
        }
    }

}