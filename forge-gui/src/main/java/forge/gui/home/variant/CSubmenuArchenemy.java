package forge.gui.home.variant;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.SwingUtilities;

import com.google.common.base.Predicate;

import forge.Command;
import forge.FThreads;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.DeckgenUtil;
import forge.game.GameType;
import forge.game.Match;
import forge.game.RegisteredPlayer;
import forge.gui.GuiDialog;
import forge.gui.SOverlayUtils;
import forge.gui.deckchooser.FDeckChooser;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.controllers.CEditorVariant;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.gui.toolbox.FList;
import forge.item.PaperCard;
import forge.net.FServer;
import forge.net.Lobby;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.util.Aggregates;

/** 
 * Controls the constructed submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CSubmenuArchenemy implements ICDoc {
    /** */
    SINGLETON_INSTANCE;
    private final VSubmenuArchenemy view = VSubmenuArchenemy.SINGLETON_INSTANCE;
    

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void update() {
        // reinit deck list and restore last selections (if any)
        FList<Object> deckList = view.getArchenemySchemes();
        Vector<Object> listData = new Vector<Object>();
        listData.add("Random");
        listData.add("Generate");
        for (Deck schemeDeck : Singletons.getModel().getDecks().getScheme()) {
            listData.add(schemeDeck);
        }

        Object val = deckList.getSelectedValue();
        deckList.setListData(listData);
        if (null != val) {
            deckList.setSelectedValue(val, true);
        }
        
        if (-1 == deckList.getSelectedIndex()) {
            deckList.setSelectedIndex(0);
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { view.getBtnStart().requestFocusInWindow(); }
        });
    }
    
    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void initialize() {

        VSubmenuArchenemy.SINGLETON_INSTANCE.getLblEditor().setCommand(new Command() {
            private static final long serialVersionUID = -4548064747843903896L;

            @Override
            public void run() {
                
                Predicate<PaperCard> predSchemes = new Predicate<PaperCard>() {
                    @Override
                    public boolean apply(PaperCard arg0) {
                        if(arg0.getRules().getType().isScheme())
                        {
                            return true;
                        }
                        
                        return false;
                    }
                };
                
                Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_ARCHENEMY);
                CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(
                        new CEditorVariant(Singletons.getModel().getDecks().getScheme(), predSchemes, FScreen.DECK_EDITOR_ARCHENEMY));
            }
        });

        final ForgePreferences prefs = Singletons.getModel().getPreferences();
        for (FDeckChooser fdc : view.getDeckChoosers()) {
            fdc.initialize();
        }

        // Checkbox event handling
        view.getBtnStart().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                startGame();
            }
        });

        // Checkbox event handling
        view.getCbSingletons().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setPref(FPref.DECKGEN_SINGLETONS,
                        String.valueOf(view.getCbSingletons().isSelected()));
                prefs.save();
            }
        });

        view.getCbArtifacts().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setPref(
                        FPref.DECKGEN_ARTIFACTS, String.valueOf(view.getCbArtifacts().isSelected()));
                prefs.save();
            }
        });

        view.getCbRemoveSmall().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                prefs.setPref(
                        FPref.DECKGEN_NOSMALL, String.valueOf(view.getCbRemoveSmall().isSelected()));
                prefs.save();
            }
        });

        // Pre-select checkboxes
        view.getCbSingletons().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_SINGLETONS));
        view.getCbArtifacts().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_ARTIFACTS));
        view.getCbRemoveSmall().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_NOSMALL));
    }


    /** @param lists0 &emsp; {@link java.util.List}<{@link javax.swing.JList}> */
    private void startGame() {



        boolean usedDefaults = false;

        List<Deck> playerDecks = new ArrayList<Deck>();
        for (int i = 0; i < view.getNumPlayers(); i++) {
            RegisteredPlayer d = view.getDeckChoosers().get(i).getPlayer();

            if (d == null) {
                //ERROR!
                GuiDialog.message("No deck selected for player " + (i + 1));
                return;
            }
            playerDecks.add(d.getOriginalDeck());
        }

        List<PaperCard> schemes = null;
        Object obj = view.getArchenemySchemes().getSelectedValue();

        boolean useDefault = VSubmenuArchenemy.SINGLETON_INSTANCE.getCbUseDefaultSchemes().isSelected();
        useDefault &= playerDecks.get(0).has(DeckSection.Schemes);

        System.out.println(useDefault);
        if (useDefault) {
            schemes = playerDecks.get(0).get(DeckSection.Schemes).toFlatList();
            System.out.println(schemes.toString());
            usedDefaults = true;
        } else {
            if (obj instanceof String) {
                String sel = (String) obj;
                if (sel.equals("Random")) {
                    if (view.getAllSchemeDecks().isEmpty()) {
                        //Generate if no constructed scheme decks are available
                        System.out.println("Generating scheme deck - no others available");
                        schemes = DeckgenUtil.generateSchemeDeck().toFlatList();
                    } else {
                        System.out.println("Using scheme deck: " + Aggregates.random(view.getAllSchemeDecks()).getName());
                        schemes = Aggregates.random(view.getAllSchemeDecks()).get(DeckSection.Schemes).toFlatList();
                    }
                } else {
                    //Generate
                    schemes = DeckgenUtil.generateSchemeDeck().toFlatList();
                }
            } else {
                schemes = ((Deck) obj).get(DeckSection.Schemes).toFlatList();
            }
        }
        if (schemes == null) {
            //ERROR!
            GuiDialog.message("No scheme deck selected!");
            return;
        }

        if (usedDefaults) {

            GuiDialog.message("Using default scheme deck.");
        }

        // won't cancel after this point
        SOverlayUtils.startGameOverlay();
        SOverlayUtils.showOverlay();

        Lobby lobby = FServer.instance.getLobby();

        List<RegisteredPlayer> players = new ArrayList<RegisteredPlayer>();
        for (int i = 0; i < view.getNumPlayers(); i++) {
            if (i == 0) {
                
                RegisteredPlayer psc = RegisteredPlayer.forArchenemy(playerDecks.get(i), schemes);
                psc.setStartingLife(40); // 904.5: The Archenemy has 40 life.
                players.add(psc.setPlayer(lobby.getGuiPlayer()));
            } else {
                RegisteredPlayer psc = RegisteredPlayer.fromDeck(playerDecks.get(i));
                psc.setTeamNumber(0);
                players.add(psc.setPlayer(lobby.getAiPlayer()));
            }
        }

        final Match mc = new Match(GameType.Archenemy, players);
        FThreads.invokeInEdtLater(new Runnable(){
            @Override
            public void run() {
                Singletons.getControl().startGameWithUi(mc);
                SOverlayUtils.hideOverlay();
            }
        });

    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }
}