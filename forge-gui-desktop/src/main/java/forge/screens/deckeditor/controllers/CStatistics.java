package forge.screens.deckeditor.controllers;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.UiCommand;
import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.MagicColor;
import forge.deck.DeckBase;
import forge.gui.framework.ICDoc;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.itemmanager.SItemManagerUtil.StatTypes;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.views.VStatistics;
import forge.util.ItemPool;

import javax.swing.*;

import java.util.Map.Entry;


/** 
 * Controls the "analysis" panel in the deck editor UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CStatistics implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public UiCommand getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        analyze();
    }

    private void setLabelValue(JLabel label, ItemPool<PaperCard> deck, Predicate<CardRules> predicate, int total) {
        int tmp = deck.countAll(Predicates.compose(predicate, PaperCard.FN_GET_RULES));
        label.setText(tmp + " (" + calculatePercentage(tmp, total) + "%)");
    }

    //========== Other methods
    @SuppressWarnings("unchecked")
    private <T extends InventoryItem, TModel extends DeckBase> void analyze() {
        final ACEditorBase<T, TModel> ed = (ACEditorBase<T, TModel>)
                CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();

        if (ed == null) { return; }

        final ItemPool<PaperCard> deck = ItemPool.createFrom(ed.getDeckManager().getPool(), PaperCard.class);

        int total = deck.countAll();
        int[] shardCount = calculateShards(deck);

        // Hack-ish: avoid /0 cases, but still populate labels :)
        if (total == 0) { total = 1; }

        setLabelValue(VStatistics.SINGLETON_INSTANCE.getLblCreature(), deck, CardRulesPredicates.Presets.IS_CREATURE, total);
        setLabelValue(VStatistics.SINGLETON_INSTANCE.getLblLand(), deck, CardRulesPredicates.Presets.IS_LAND, total);
        setLabelValue(VStatistics.SINGLETON_INSTANCE.getLblEnchantment(), deck, CardRulesPredicates.Presets.IS_ENCHANTMENT, total);
        setLabelValue(VStatistics.SINGLETON_INSTANCE.getLblArtifact(), deck, CardRulesPredicates.Presets.IS_ARTIFACT, total);
        setLabelValue(VStatistics.SINGLETON_INSTANCE.getLblInstant(), deck, CardRulesPredicates.Presets.IS_INSTANT, total);
        setLabelValue(VStatistics.SINGLETON_INSTANCE.getLblSorcery(), deck, CardRulesPredicates.Presets.IS_SORCERY, total);
        setLabelValue(VStatistics.SINGLETON_INSTANCE.getLblPlaneswalker(), deck, CardRulesPredicates.Presets.IS_PLANESWALKER, total);

        setLabelValue(VStatistics.SINGLETON_INSTANCE.getLblMulti(), deck, CardRulesPredicates.Presets.IS_MULTICOLOR, total);
        setLabelValue(VStatistics.SINGLETON_INSTANCE.getLblColorless(), deck, CardRulesPredicates.Presets.IS_COLORLESS, total);
        setLabelValue(VStatistics.SINGLETON_INSTANCE.getLblBlack(), deck, CardRulesPredicates.isMonoColor(MagicColor.BLACK), total);
        setLabelValue(VStatistics.SINGLETON_INSTANCE.getLblBlue(), deck, CardRulesPredicates.isMonoColor(MagicColor.BLUE), total);
        setLabelValue(VStatistics.SINGLETON_INSTANCE.getLblGreen(), deck, CardRulesPredicates.isMonoColor(MagicColor.GREEN), total);
        setLabelValue(VStatistics.SINGLETON_INSTANCE.getLblRed(), deck, CardRulesPredicates.isMonoColor(MagicColor.RED), total);
        setLabelValue(VStatistics.SINGLETON_INSTANCE.getLblWhite(), deck, CardRulesPredicates.isMonoColor(MagicColor.WHITE), total);

        setLabelValue(VStatistics.SINGLETON_INSTANCE.getLblCMC0(), deck, StatTypes.CMC_0.predicate, total);
        setLabelValue(VStatistics.SINGLETON_INSTANCE.getLblCMC1(), deck, StatTypes.CMC_1.predicate, total);
        setLabelValue(VStatistics.SINGLETON_INSTANCE.getLblCMC2(), deck, StatTypes.CMC_2.predicate, total);
        setLabelValue(VStatistics.SINGLETON_INSTANCE.getLblCMC3(), deck, StatTypes.CMC_3.predicate, total);
        setLabelValue(VStatistics.SINGLETON_INSTANCE.getLblCMC4(), deck, StatTypes.CMC_4.predicate, total);
        setLabelValue(VStatistics.SINGLETON_INSTANCE.getLblCMC5(), deck, StatTypes.CMC_5.predicate, total);
        setLabelValue(VStatistics.SINGLETON_INSTANCE.getLblCMC6(), deck, StatTypes.CMC_6.predicate, total);

        VStatistics.SINGLETON_INSTANCE.getLblTSCWhite().setText("WHITE MANA SYMBOLS IN MANA COST: " + shardCount[0]);
        VStatistics.SINGLETON_INSTANCE.getLblTSCBlue().setText("BLUE MANA SYMBOLS IN MANA COST: " + shardCount[1]);
        VStatistics.SINGLETON_INSTANCE.getLblTSCBlack().setText("BLACK MANA SYMBOLS IN MANA COST: " + shardCount[2]);
        VStatistics.SINGLETON_INSTANCE.getLblTSCRed().setText("RED MANA SYMBOLS IN MANA COST: " + shardCount[3]);
        VStatistics.SINGLETON_INSTANCE.getLblTSCGreen().setText("GREEN MANA SYMBOLS IN MANA COST: " + shardCount[4]);

        int tmc = 0;
        for (final Entry<PaperCard, Integer> e : deck) {
            tmc += e.getKey().getRules().getManaCost().getCMC() * e.getValue();
        }
        double amc = Math.round((double) tmc / (double) total * 100) / 100.0d;

        VStatistics.SINGLETON_INSTANCE.getLblTotal().setText("TOTAL CARDS: " + deck.countAll());
        VStatistics.SINGLETON_INSTANCE.getLblTMC().setText("TOTAL MANA COST: " + tmc);
        VStatistics.SINGLETON_INSTANCE.getLblAMC().setText("AVERAGE MANA COST: " + amc);
    }

    /**
     * Divides X by Y, multiplies by 100, rounds, returns.
     * 
     * @param x0 &emsp; Numerator (int)
     * @param y0 &emsp; Denominator (int)
     * @return rounded result (int)
     */
    public static int calculatePercentage(final int x0, final int y0) {
        return (int) Math.round((double) (x0 * 100) / (double) y0);
    }

    public static int[] calculateShards(ItemPool<PaperCard> deck) {
        int[] counts = new int[5]; // in WUBRG order
        for (PaperCard c : deck.toFlatList()) {
            int[] cShards = c.getRules().getManaCost().getColorShardCounts();
            for (int i = 0; i < 5; i++) {
                counts[i] += cShards[i];
            }
        }
        return counts;
    }
}
