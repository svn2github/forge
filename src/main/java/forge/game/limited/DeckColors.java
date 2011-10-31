package forge.game.limited;

import forge.Constant;

/**
 * Created by IntelliJ IDEA. User: dhudson Date: 6/24/11 Time: 8:42 PM To change
 * this template use File | Settings | File Templates.
 */
class DeckColors {

    /** The Color1. */
    public String Color1 = "none";

    /** The Color2. */
    public String Color2 = "none";
    // public String Splash = "none";
    /** The Mana1. */
    public String Mana1 = "";

    /** The Mana2. */
    public String Mana2 = "";

    // public String ManaS = "";

    /**
     * <p>
     * Constructor for deckColors.
     * </p>
     * 
     * @param c1
     *            a {@link java.lang.String} object.
     * @param c2
     *            a {@link java.lang.String} object.
     * @param sp
     *            a {@link java.lang.String} object.
     */
    public DeckColors(final String c1, final String c2, final String sp) {
        this.Color1 = c1;
        this.Color2 = c2;
        // Splash = sp;
    }

    /**
     * <p>
     * Constructor for DeckColors.
     * </p>
     */
    public DeckColors() {

    }

    /**
     * <p>
     * ColorToMana.
     * </p>
     * 
     * @param color
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String ColorToMana(final String color) {
        final String Mana[] = { "W", "U", "B", "R", "G" };

        for (int i = 0; i < Constant.Color.ONLY_COLORS.length; i++) {
            if (Constant.Color.ONLY_COLORS[i].equals(color)) {
                return Mana[i];
            }
        }

        return "";
    }

}
