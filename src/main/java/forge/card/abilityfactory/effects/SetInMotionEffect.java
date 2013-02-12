package forge.card.abilityfactory.effects;


import forge.Card;
import forge.card.abilityfactory.AbilityUtils;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

public class SetInMotionEffect extends SpellEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        System.out.println("AF_SIM");
        Card source = sa.getSourceCard();
        Player controller = source.getController();

        int repeats = 1;

        if (sa.hasParam("RepeatNum")) {

            repeats = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("RepeatNum"), sa);
        }

        for (int i = 0; i < repeats; i++) {

            controller.setSchemeInMotion();
        }
    }

}
