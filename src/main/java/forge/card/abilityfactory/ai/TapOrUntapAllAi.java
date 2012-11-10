package forge.card.abilityfactory.ai;

import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class TapOrUntapAllAi extends SpellAiLogic {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        // Only Turnabout currently uses this, it's hardcoded to always return false
        // Looks like Faces of the Past could also use this
        return false;
    }

}
