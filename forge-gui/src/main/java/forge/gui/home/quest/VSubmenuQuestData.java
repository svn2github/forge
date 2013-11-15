package forge.gui.home.quest;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.text.WordUtils;

import forge.Constant;
import forge.Singletons;
import forge.card.MagicColor;
import forge.deck.CardCollections;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.game.GameFormat;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.gui.home.VHomeUI;
import forge.gui.toolbox.FCheckBox;
import forge.gui.toolbox.FComboBoxWrapper;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FRadioButton;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.JXButtonPanel;
import forge.item.PreconDeck;
import forge.quest.QuestController;
import forge.quest.QuestWorld;
import forge.quest.StartingPoolType;
import forge.util.storage.IStorage;

/**
 * Assembles Swing components of quest data submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuQuestData implements IVSubmenu<CSubmenuQuestData> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Quest Data");

    /** */
    private final FLabel lblTitle = new FLabel.Builder()
        .text("Load Quest Data").fontAlign(SwingConstants.CENTER)
        .opaque(true).fontSize(16).build();

    private final JLabel lblTitleNew = new FLabel.Builder().text("Start a new Quest")
            .opaque(true).fontSize(16).build();

    private final JLabel lblOldQuests = new FLabel.Builder().text("Old quest data? Put into "
            + "res/quest/data, and restart Forge.")
            .fontAlign(SwingConstants.CENTER).fontSize(12).build();

    private final QuestFileLister lstQuests = new QuestFileLister();
    private final FScrollPane scrQuests = new FScrollPane(lstQuests);
    private final JPanel pnlOptions = new JPanel();

    /* Fist column */
    private final JRadioButton radEasy = new FRadioButton("Easy");
    private final JRadioButton radMedium = new FRadioButton("Medium");
    private final JRadioButton radHard = new FRadioButton("Hard");
    private final JRadioButton radExpert = new FRadioButton("Expert");
    private final JCheckBox boxFantasy = new FCheckBox("Fantasy Mode");

    private final JLabel lblStartingWorld = new FLabel.Builder().text("Starting world:").build();
    private final FComboBoxWrapper<QuestWorld> cbxStartingWorld = new FComboBoxWrapper<QuestWorld>();

    private final JLabel lblPreferredColor = new FLabel.Builder().text("Starting pool colors:").build();
    private final FComboBoxWrapper<String> cbxPreferredColor = new FComboBoxWrapper<String>();
    private final String stringBalancedDistribution = new String("balanced distribution");
    private final String stringRandomizedDistribution = new String("randomized distribution");
    private final String stringBias = new String(" bias");

    /* Second column */

    private final JLabel lblStartingPool = new FLabel.Builder().text("Starting pool:").build();
    private final FComboBoxWrapper<StartingPoolType> cbxStartingPool = new FComboBoxWrapper<StartingPoolType>();

    private final JLabel lblUnrestricted = new FLabel.Builder().text("All cards will be available to play.").build();

    private final JLabel lblPreconDeck = new FLabel.Builder().text("Starter/Event deck:").build();
    private final FComboBoxWrapper<String> cbxPreconDeck = new FComboBoxWrapper<String>();

    private final JLabel lblFormat = new FLabel.Builder().text("Sanctioned format:").build();
    private final FComboBoxWrapper<GameFormat> cbxFormat = new FComboBoxWrapper<GameFormat>();

    private final JLabel lblCustomDeck = new FLabel.Builder().text("Custom deck:").build();
    private final FComboBoxWrapper<Deck> cbxCustomDeck = new FComboBoxWrapper<Deck>();

    private final FLabel btnDefineCustomFormat = new FLabel.Builder().opaque(true).hoverable(true).text("Define custom format").build();
    private final FLabel btnPrizeDefineCustomFormat = new FLabel.Builder().opaque(true).hoverable(true).text("Define custom format").build();

    private final JLabel lblPrizedCards = new FLabel.Builder().text("Prized cards:").build();
    private final FComboBoxWrapper<Object> cbxPrizedCards = new FComboBoxWrapper<Object>();

    private final JLabel lblPrizeFormat = new FLabel.Builder().text("Sanctioned format:").build();
    private final FComboBoxWrapper<GameFormat> cbxPrizeFormat = new FComboBoxWrapper<GameFormat>();

    private final JLabel lblPrizeUnrestricted = new FLabel.Builder().text("All cards will be available to win.").build();
    private final JLabel lblPrizeSameAsStarting = new FLabel.Builder().text("Only sets found in starting pool will be available.").build();

    private final JCheckBox cboAllowUnlocks = new FCheckBox("Allow unlock of additional editions");

    private final FLabel btnEmbark = new FLabel.Builder().opaque(true)
            .fontSize(16).hoverable(true).text("Embark!").build();

    /* Listeners */
    private final ActionListener alStartingPool = new ActionListener() {
        @SuppressWarnings("incomplete-switch")
        @Override
        public void actionPerformed(ActionEvent e) {
            StartingPoolType newVal = getStartingPoolType();
            lblUnrestricted.setVisible(newVal == StartingPoolType.Complete);

            lblPreconDeck.setVisible(newVal == StartingPoolType.Precon);
            cbxPreconDeck.setVisible(newVal == StartingPoolType.Precon);

            lblFormat.setVisible(newVal == StartingPoolType.Rotating);
            cbxFormat.setVisible(newVal == StartingPoolType.Rotating);

            btnDefineCustomFormat.setVisible(newVal == StartingPoolType.CustomFormat);


            boolean usesDeckList = newVal == StartingPoolType.SealedDeck || newVal == StartingPoolType.DraftDeck || newVal == StartingPoolType.Cube;
            lblCustomDeck.setVisible(usesDeckList);
            cbxCustomDeck.setVisible(usesDeckList);

            if (usesDeckList) {
                cbxCustomDeck.removeAllItems();
                CardCollections decks = Singletons.getModel().getDecks();
                switch (newVal) {
                    case SealedDeck:
                        for (DeckGroup d : decks.getSealed()) { cbxCustomDeck.addItem(d.getHumanDeck()); }
                        break;
                    case DraftDeck:
                        for (DeckGroup d : decks.getDraft()) { cbxCustomDeck.addItem(d.getHumanDeck()); }
                        break;
                    case Cube:
                        for (Deck d : decks.getCubes()) { cbxCustomDeck.addItem(d); }
                        break;
                }
            }
        }
    };

    /* Listeners */
    private final ActionListener alPrizesPool = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            StartingPoolType newVal = getPrizedPoolType();
            lblPrizeUnrestricted.setVisible(newVal == StartingPoolType.Complete);
            cboAllowUnlocks.setVisible(newVal != StartingPoolType.Complete);

            lblPrizeFormat.setVisible(newVal == StartingPoolType.Rotating);
            cbxPrizeFormat.setVisible(newVal == StartingPoolType.Rotating);
            btnPrizeDefineCustomFormat.setVisible(newVal == StartingPoolType.CustomFormat);
            lblPrizeSameAsStarting.setVisible(newVal == null);
        }
    };

    /* Listeners */
    private final ActionListener alStartingWorld = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            updateEnableFormats();
        }
     };


    /**
     * Aux function for enabling or disabling the format selection according to world selection.
     */
     private void updateEnableFormats() {
         final QuestWorld qw = Singletons.getModel().getWorlds().get(getStartingWorldName());
         if (qw != null) {
             cbxStartingPool.setEnabled(qw.getFormat() == null);
             cbxFormat.setEnabled(qw.getFormat() == null);
             cbxCustomDeck.setEnabled(qw.getFormat() == null);
             // Do NOT disable the following...
             // cbxPrizeFormat.setEnabled(qw.getFormat() == null);
             // cboAllowUnlocks.setEnabled(qw.getFormat() == null);
             // cbxPrizedCards.setEnabled(qw.getFormat() == null);

         }
     }

    /**
     * Constructor.
     */
    @SuppressWarnings("unchecked")
    private VSubmenuQuestData() {
        FSkin.get(lblTitle).setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        FSkin.get(lblTitleNew).setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        scrQuests.setBorder(null);

        final JXButtonPanel difficultyPanel = new JXButtonPanel();
        final String difficulty_constraints = "h 27px!, gapbottom 5";
        difficultyPanel.add(radEasy, difficulty_constraints);
        difficultyPanel.add(radMedium, difficulty_constraints);
        difficultyPanel.add(radHard, difficulty_constraints);
        difficultyPanel.add(radExpert, difficulty_constraints);
        radEasy.setSelected(true);

        cbxStartingPool.addItem(StartingPoolType.Complete);
        cbxStartingPool.addItem(StartingPoolType.Rotating);
        cbxStartingPool.addItem(StartingPoolType.CustomFormat);
        cbxStartingPool.addItem(StartingPoolType.Precon);
        cbxStartingPool.addItem(StartingPoolType.DraftDeck);
        cbxStartingPool.addItem(StartingPoolType.SealedDeck);
        cbxStartingPool.addItem(StartingPoolType.Cube);
        cbxStartingPool.addActionListener(alStartingPool);

        // initial adjustment
        alStartingPool.actionPerformed(null);
        alPrizesPool.actionPerformed(null);

        cbxPrizedCards.addItem("Same as starting pool");
        cbxPrizedCards.addItem(StartingPoolType.Complete);
        cbxPrizedCards.addItem(StartingPoolType.Rotating);
        cbxPrizedCards.addItem(StartingPoolType.CustomFormat);
        cbxPrizedCards.addActionListener(alPrizesPool);

        for (GameFormat gf : Singletons.getModel().getFormats()) {
            cbxFormat.addItem(gf);
            cbxPrizeFormat.addItem(gf);
        }

        // Initialize color balance selection
        cbxPreferredColor.addItem(stringBalancedDistribution);
        cbxPreferredColor.addItem(stringRandomizedDistribution);
        cbxPreferredColor.addItem(Constant.Color.WHITE + stringBias);
        cbxPreferredColor.addItem(Constant.Color.BLUE + stringBias);
        cbxPreferredColor.addItem(Constant.Color.BLACK + stringBias);
        cbxPreferredColor.addItem(Constant.Color.RED + stringBias);
        cbxPreferredColor.addItem(Constant.Color.GREEN + stringBias);
        cbxPreferredColor.addItem(Constant.Color.COLORLESS + stringBias);

        for (QuestWorld qw : Singletons.getModel().getWorlds()) {
            cbxStartingWorld.addItem(qw);
        }
        // Default to 'Main world'
        cbxStartingWorld.setSelectedItem(Singletons.getModel().getWorlds().get("Main world"));

        cbxStartingWorld.addActionListener(alStartingWorld);
        updateEnableFormats();

        cboAllowUnlocks.setSelected(true);

        final Map<String, String> preconDescriptions = new HashMap<String, String>();
        IStorage<PreconDeck> preconDecks = QuestController.getPrecons();

        for (PreconDeck preconDeck : preconDecks) {
            if (preconDeck.getRecommendedDeals().getMinWins() > 0) {
                continue;
            }
            String name = preconDeck.getName();
            cbxPreconDeck.addItem(name);
            String description = preconDeck.getDescription();
            description = "<html>" + WordUtils.wrap(description, 40, "<br>", false) + "</html>";
            preconDescriptions.put(name, description);
        }

        // The cbx needs strictly typed renderer
        cbxPreconDeck.setRenderer(new BasicComboBoxRenderer() {
            private static final long serialVersionUID = 3477357932538947199L;

            @SuppressWarnings("rawtypes")
            @Override
            public Component getListCellRendererComponent(
                    JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component defaultComponent =
                        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (-1 < index && null != value) {
                    String val = (String) value;
                    list.setToolTipText(preconDescriptions.get(val));
                }
                return defaultComponent;
            }
        });

        // Fantasy box enabled by Default
        boxFantasy.setSelected(true);
        boxFantasy.setEnabled(true);

        cbxPreferredColor.setEnabled(true);

        pnlOptions.setOpaque(false);
        pnlOptions.setLayout(new MigLayout("insets 0, gap 10px, fillx, wrap 2"));

        JPanel pnlDifficultyMode = new JPanel(new MigLayout("insets 0, gap 1%, flowy"));
        pnlDifficultyMode.add(difficultyPanel, "gapright 4%");
        pnlDifficultyMode.add(boxFantasy, difficulty_constraints + ", gapright 4%");
        pnlDifficultyMode.setOpaque(false);
        pnlOptions.add(pnlDifficultyMode, "w 40%");


        JPanel pnlRestrictions = new JPanel();
        final String constraints = "h 27px!, ";
        final String lblWidth = "w 40%, ";
        final String hidemode = "hidemode 3, ";
        final String lblWidthStart = lblWidth + hidemode;
        final String cboWidth = "pushx, ";
        final String cboWidthStart = cboWidth + hidemode;

        pnlRestrictions.setLayout(new MigLayout("insets 0, gap 0, wrap 2", "[120, al right][240, fill]", "[|]12[|]6[]"));


        pnlRestrictions.add(lblStartingPool, constraints + lblWidthStart);
        cbxStartingPool.addTo(pnlRestrictions, constraints + cboWidthStart);

        /* out of these 3 groups only one will be visible */
        pnlRestrictions.add(lblUnrestricted, constraints + hidemode + "spanx 2");

        pnlRestrictions.add(lblPreconDeck, constraints + lblWidthStart);
        cbxPreconDeck.addTo(pnlRestrictions, constraints + cboWidthStart);

        pnlRestrictions.add(lblCustomDeck, constraints + lblWidthStart);
        cbxCustomDeck.addTo(pnlRestrictions, constraints + cboWidthStart); // , skip 1

        pnlRestrictions.add(lblFormat, constraints + lblWidthStart);
        cbxFormat.addTo(pnlRestrictions, constraints + cboWidthStart); // , skip 1

        pnlRestrictions.add(btnDefineCustomFormat, constraints + hidemode + "spanx 2, w 240px");

        // Prized cards options
        pnlRestrictions.add(lblPrizedCards, constraints + lblWidth);
        cbxPrizedCards.addTo(pnlRestrictions, constraints + cboWidth);

        pnlRestrictions.add(lblPrizeFormat, constraints + lblWidthStart);
        cbxPrizeFormat.addTo(pnlRestrictions, constraints + cboWidthStart); // , skip 1
        pnlRestrictions.add(btnPrizeDefineCustomFormat, constraints + hidemode + "spanx 2, w 240px");
        pnlRestrictions.add(lblPrizeSameAsStarting,  constraints + hidemode + "spanx 2");
        pnlRestrictions.add(lblPrizeUnrestricted, constraints + hidemode + "spanx 2");

        pnlRestrictions.add(cboAllowUnlocks, constraints + "spanx 2, ax right");


        pnlRestrictions.add(lblPreferredColor,  constraints + lblWidthStart);
        cbxPreferredColor.addTo(pnlRestrictions, constraints + cboWidthStart + ", wrap");

        pnlRestrictions.add(lblStartingWorld, constraints + lblWidthStart);
        cbxStartingWorld.addTo(pnlRestrictions, constraints + cboWidthStart);

//        cboAllowUnlocks.setOpaque(false);
        pnlRestrictions.setOpaque(false);
        pnlOptions.add(pnlRestrictions, "pushx, ay top");

        pnlOptions.add(btnEmbark, "w 300px!, h 30px!, ax center, span 2, gap 0 0 15px 30px");
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap"));

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 98%!, h 30px!, gap 1% 0 15px 15px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblOldQuests, "w 98%, h 30px!, gap 1% 0 0 5px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(scrQuests, "w 98%!, growy, pushy, gap 1% 0 0 20px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitleNew, "w 98%, h 30px!, gap 1% 0 0 10px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlOptions, "w 98%!, gap 1% 0 0 0");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.QUEST;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "New / Load Quest";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuName()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_QUESTDATA;
    }

    /**
     * @return {@link forge.gui.home.quest.QuestFileLister}
     */
    public QuestFileLister getLstQuests() {
        return this.lstQuests;
    }

    /**
     * @return {@link forge.gui.toolbox.FLabel}
     */
    public FLabel getBtnEmbark() {
        return btnEmbark;
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_QUESTDATA;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CSubmenuQuestData getLayoutControl() {
        return CSubmenuQuestData.SINGLETON_INSTANCE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell(forge.gui.framework.DragCell)
     */
    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return parentCell;
    }

    public int getSelectedDifficulty() {
        if (radEasy.isSelected()) {
            return 0;
        } else if (radMedium.isSelected()) {
            return 1;
        } else if (radHard.isSelected()) {
            return 2;
        } else if (radExpert.isSelected()) {
            return 3;
        }
        return 0;
    }

    public String getSelectedPrecon() {
        return cbxPreconDeck.getSelectedItem().toString();
    }

    public Deck getSelectedDeck() {
        Object sel = cbxCustomDeck.getSelectedItem();
        return sel instanceof Deck ? (Deck) sel : null;
    }

    public boolean isUnlockSetsAllowed() {
        return cboAllowUnlocks.isSelected();
    }

    public StartingPoolType getStartingPoolType() {
        return (StartingPoolType) cbxStartingPool.getSelectedItem();
    }

    public StartingPoolType getPrizedPoolType() {
         Object v = cbxPrizedCards.getSelectedItem();
         return v instanceof StartingPoolType ? (StartingPoolType) v : null;
    }

    public String getStartingWorldName() {
        return cbxStartingWorld.getSelectedItem().toString();
    }

    public boolean isFantasy() {
        return boxFantasy.isSelected();
    }

    public boolean randomizeColorDistribution() {
        return stringRandomizedDistribution.equals(cbxPreferredColor.getSelectedItem());
    }

    public byte getPreferredColor() {
        if (stringBalancedDistribution.equals(cbxPreferredColor.getSelectedItem())
                || stringRandomizedDistribution.equals(cbxPreferredColor.getSelectedItem())) {
            return MagicColor.ALL_COLORS;
        }
        return MagicColor.fromName("" + cbxPreferredColor.getSelectedItem().charAt(0));
    }

    public GameFormat getRotatingFormat() {
        return (GameFormat) cbxFormat.getSelectedItem();
    }

    public GameFormat getPrizedRotatingFormat() {
        return (GameFormat) cbxPrizeFormat.getSelectedItem();
    }

    public FLabel getBtnCustomFormat() {
        return btnDefineCustomFormat;
    }
    public FLabel getBtnPrizeCustomFormat() {
        return btnPrizeDefineCustomFormat;
    }

}