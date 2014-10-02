package forge.view;

import java.util.Collections;
import java.util.List;
import java.util.Observer;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.LobbyPlayer;
import forge.card.MagicColor;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameLogEntry;
import forge.game.GameLogEntryType;
import forge.game.GameOutcome;
import forge.game.GameType;
import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.combat.AttackingBand;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.ZoneType;
import forge.interfaces.IGuiBase;
import forge.match.MatchUtil;
import forge.match.input.InputProxy;
import forge.match.input.InputQueue;

public abstract class LocalGameView implements IGameView {
    protected final Game game;
    protected final IGuiBase gui;
    protected final InputQueue inputQueue;
    protected final InputProxy inputProxy;
    private PlayerView localPlayerView;

    public LocalGameView(IGuiBase gui0, Game game0) {
        game = game0;
        gui = gui0;
        inputProxy = new InputProxy(this);
        inputQueue = new InputQueue(game, inputProxy);
    }

    public final Game getGame() {
        return game;
    }

    public final IGuiBase getGui() {
        return gui;
    }

    public final InputQueue getInputQueue() {
        return inputQueue;
    }

    public InputProxy getInputProxy() {
        return inputProxy;
    }

    public PlayerView getLocalPlayerView() {
        return localPlayerView;
    }

    public void setLocalPlayer(Player localPlayer) {
        localPlayerView = getPlayerView(localPlayer);
    }

    /* (non-Javadoc)
     * @see forge.view.IGameView#isCommander()
     */
    @Override
    public boolean isCommander() {
        return game.getRules().hasAppliedVariant(GameType.Commander);
    }
    /* (non-Javadoc)
     * @see forge.view.IGameView#getGameType()
     */
    @Override
    public GameType getGameType() {
        return game.getMatch().getRules().getGameType();
    }

    @Override
    public int getTurnNumber() {
        return game.getPhaseHandler().getTurn();
    }

    @Override
    public boolean isCommandZoneNeeded() {
        return game.getMatch().getRules().getGameType().isCommandZoneNeeded();
    }

    @Override
    public boolean isWinner(final LobbyPlayer p) {
        return game.getOutcome() == null ? null : game.getOutcome().isWinner(p);
    }

    @Override
    public LobbyPlayer getWinningPlayer() {
        return game.getOutcome() == null ? null : game.getOutcome().getWinningLobbyPlayer();
    }

    @Override
    public int getWinningTeam() {
        return game.getOutcome() == null ? -1 : game.getOutcome().getWinningTeam();
    }

    /* (non-Javadoc)
     * @see forge.view.IGameView#isFirstGameInMatch()
     */
    @Override
    public boolean isFirstGameInMatch() {
        return game.getMatch().getPlayedGames().isEmpty();
    }

    @Override
    public boolean isMatchOver() {
        return game.getMatch().isMatchOver();
    }

    @Override
    public int getNumGamesInMatch() {
        return game.getMatch().getRules().getGamesPerMatch();
    }

    @Override
    public int getNumPlayedGamesInMatch() {
        return game.getMatch().getPlayedGames().size();
    }

    @Override
    public Iterable<GameOutcome> getOutcomesOfMatch() {
        return Iterables.unmodifiableIterable(game.getMatch().getPlayedGames());
    }

    @Override
    public boolean isMatchWonBy(final LobbyPlayer p) {
        return game.getMatch().isWonBy(p);
    }

    @Override
    public int getGamesWonBy(final LobbyPlayer p) {
        return game.getMatch().getGamesWonBy(p);
    }

    @Override
    public GameOutcome.AnteResult getAnteResult() {
        return null;
    }

    @Override
    public Deck getDeck(final LobbyPlayer player) {
        for (final RegisteredPlayer rp : game.getMatch().getPlayers()) {
            if (rp.getPlayer().equals(player)) {
                return rp.getDeck();
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see forge.view.IGameView#isGameOver()
     */
    @Override
    public boolean isGameOver() {
        return game.isGameOver();
    }

    @Override
    public int getPoisonCountersToLose() {
        return game.getRules().getPoisonCountersToLose();
    }

    /* (non-Javadoc)
     * @see forge.view.IGameView#subscribeToEvents(java.lang.Object)
     */
    @Override
    public void subscribeToEvents(final Object subscriber) {
        game.subscribeToEvents(subscriber);
    }

    @Override
    public CombatView getCombat() {
        synchronized (MatchUtil.class) {
            if (MatchUtil.cachedCombatView != null) {
                return MatchUtil.cachedCombatView;
            }

            final Combat combat = game.getCombat();
            final CombatView combatView;
            if (combat == null) {
                combatView = null;
            } else {
                combatView = new CombatView();
                for (final AttackingBand b : combat.getAttackingBands()) {
                    if (b == null) continue;
                    final GameEntity defender = combat.getDefenderByAttacker(b);
                    final List<Card> blockers = combat.getBlockers(b);
                    final boolean isBlocked = b.isBlocked() == Boolean.TRUE;
                    combatView.addAttackingBand(
                            getCardViews(b.getAttackers()),
                            getGameEntityView(defender),
                            blockers == null || !isBlocked ? null : getCardViews(blockers),
                                    blockers == null ? null : getCardViews(blockers));
                }
            }
            MatchUtil.cachedCombatView = combatView;
            return combatView;
        }
    }

    public final void refreshCombat() {
        synchronized (MatchUtil.class) {
            MatchUtil.cachedCombatView = null;
            this.getCombat();
        }
    }

    @Override
    public void addLogObserver(final Observer o) {
        game.getGameLog().addObserver(o);
    }

    @Override
    public List<GameLogEntry> getLogEntries(final GameLogEntryType maxLogLevel) {
        return game.getGameLog().getLogEntries(maxLogLevel);
    }

    @Override
    public List<GameLogEntry> getLogEntriesExact(final GameLogEntryType logLevel) {
        return game.getGameLog().getLogEntriesExact(logLevel);
    }

    /* (non-Javadoc)
     * @see forge.view.IGameView#getRegisteredPlayers()
     */
    @Override
    public List<PlayerView> getPlayers() {
        return getPlayerViews(game.getRegisteredPlayers());
    }

    @Override
    public PlayerView getPlayerTurn() {
        return getPlayerView(game.getPhaseHandler().getPlayerTurn());
    }

    @Override
    public PhaseType getPhase() {
        return game.getPhaseHandler().getPhase();
    }

    /* (non-Javadoc)
     * @see forge.view.IGameView#getStack()
     */
    @Override
    public List<StackItemView> getStack() {
        final List<SpellAbilityStackInstance> stack = Lists.newArrayList(game.getStack());
        final List<StackItemView> items = Collections.unmodifiableList(getStack(stack));
        // clear the cache
        MatchUtil.stackItems.retainAllKeys(stack);
        return items;
    }

    /* (non-Javadoc)
     * @see forge.view.IGameView#peekStack()
     */
    @Override
    public StackItemView peekStack() {
        final SpellAbilityStackInstance top = game.getStack().peek();
        if (top == null) {
            return null;
        }
        return getStack(Lists.newArrayList(top)).iterator().next();
    }

    private List<StackItemView> getStack(final Iterable<SpellAbilityStackInstance> stack) {
        MatchUtil.stackItems.retainAllKeys(Lists.newArrayList(stack));
        final List<StackItemView> items = Lists.newLinkedList();
        for (final SpellAbilityStackInstance si : stack) {
            final int id = si.getId();
            if (MatchUtil.stackItems.containsKey(id)) {
                items.add(MatchUtil.stackItems.get(id));
            } else {
                items.add(getStackItemView(si));
            }
        }
        return items;
    }

    public StackItemView getStackItemView(final SpellAbilityStackInstance si) {
        if (si == null) {
            return null;
        }

        final StackItemView newItem = new StackItemView(si, this);
        MatchUtil.stackItems.put(si, newItem);
        return newItem;
    }

    public SpellAbilityStackInstance getStackItem(final StackItemView view) {
        if (view == null) {
            return null;
        }
        return MatchUtil.stackItems.getKey(view.getId());
    }

    public final GameEntityView getGameEntityView(final GameEntity e) {
        if (e instanceof Card) {
            return getCardView((Card)e);
        } else if (e instanceof Player) {
            return getPlayerView((Player)e);
        }
        return null;
    }

    private final Function<GameEntity, GameEntityView> FN_GET_GAME_ENTITY_VIEW = new Function<GameEntity, GameEntityView>() {
        @Override
        public GameEntityView apply(GameEntity input) {
            return getGameEntityView(input);
        }
    };

    public final List<GameEntityView> getGameEntityViews(final Iterable<GameEntity> entities) {
        return ViewUtil.transformIfNotNull(entities, FN_GET_GAME_ENTITY_VIEW);
    }

    public final GameEntity getGameEntity(final GameEntityView view) {
        if (view instanceof CardView) {
            return getCard((CardView) view);
        } else if (view instanceof PlayerView) {
            return getPlayer((PlayerView) view);
        }
        return null;
    }

    private final Function<Player, PlayerView> FN_GET_PLAYER_VIEW = new Function<Player, PlayerView>() {
        @Override
        public PlayerView apply(final Player input) {
            return getPlayerView(input);
        }
    };

    public final List<PlayerView> getPlayerViews(final Iterable<Player> players) {
        return ViewUtil.transformIfNotNull(players, FN_GET_PLAYER_VIEW);
    }

    public Player getPlayer(final PlayerView p) {
        if (p == null) {
            return null;
        }
        return MatchUtil.players.getKey(p.getId());
    }

    public PlayerView getPlayerView(final Player p) {
        if (p == null) {
            return null;
        }

        PlayerView view = MatchUtil.players.get(p.getId());
        if (view == null) {
            view = new PlayerView(p.getLobbyPlayer(), p.getId());
            writePlayerToView(p, view);
            MatchUtil.players.put(p, view);
        }
        else if (updateViewOnAccess) {
            updateViewOnAccess = false; //set to false temporarily to prevent stack overflow
            writePlayerToView(p, view);
            updateViewOnAccess = true;
        }
        return view;
    }

    private void writePlayerToView(final Player p, final PlayerView view) {
        view.setCommanderInfo(CardFactoryUtil.getCommanderInfo(p).trim().replace("\r\n", "; "));
        view.setKeywords(p.getKeywords());
        view.setLife(p.getLife());
        view.setMaxHandSize(p.getMaxHandSize());
        view.setNumDrawnThisTurn(p.getNumDrawnThisTurn());
        view.setPoisonCounters(p.getPoisonCounters());
        view.setPreventNextDamage(p.getPreventNextDamageTotalShields());
        view.setHasUnlimitedHandSize(p.isUnlimitedHandSize());
        view.setAnteCards(getCardViews(p.getCardsIn(ZoneType.Ante)));
        view.setBfCards(getCardViews(p.getCardsIn(ZoneType.Battlefield, false)));
        view.setCommandCards(getCardViews(p.getCardsIn(ZoneType.Command)));
        view.setExileCards(getCardViews(p.getCardsIn(ZoneType.Exile)));
        view.setFlashbackCards(getCardViews(p.getCardsActivableInExternalZones(false)));
        view.setGraveCards(getCardViews(p.getCardsIn(ZoneType.Graveyard)));
        final List<Card> handCards = p.getCardsIn(ZoneType.Hand),
                libraryCards = p.getCardsIn(ZoneType.Library);
        view.setHandCards(getCardViews(handCards));
        view.setLibraryCards(getCardViews(libraryCards));
        view.setnHandCards(handCards.size());
        view.setnLibraryCards(libraryCards.size());

        for (final byte b : MagicColor.WUBRGC) {
            view.setMana(b, p.getManaPool().getAmountOfColor(b));
        }
    }

    private boolean updateViewOnAccess;
    public void startUpdate() {
        updateViewOnAccess = true; //allow accessed card views and player views to be updated
    }
    public void endUpdate() {
        updateViewOnAccess = false;
    }

    public CardView getCardView(final Card c) {
        if (c == null || c != c.getCardForUi()) {
            return null;
        }

        CardView view = MatchUtil.cards.get(c.getId());
        if (view == null) {
            view = new CardView(c.getId());
            writeCardToView(c, view, MatchUtil.getGameView());
            MatchUtil.cards.put(c, view);
        }
        else if (updateViewOnAccess) {
            updateViewOnAccess = false; //set to false temporarily to prevent stack overflow
            writeCardToView(c, view, MatchUtil.getGameView());
            updateViewOnAccess = true;
        }
        return view;
    }

    public void updateAllCards() {
        LocalGameView gameView = MatchUtil.getGameView();
        for (Card c : MatchUtil.cards.getKeys()) {
            writeCardToView(c, getCardView(c), gameView);
        }
    }

    private final Function<Card, CardView> FN_GET_CARD_VIEW = new Function<Card, CardView>() {
        @Override
        public CardView apply(final Card input) {
            return getCardView(input);
        }
    };

    public final List<CardView> getCardViews(final Iterable<Card> cards) {
        return ViewUtil.transformIfNotNull(cards, FN_GET_CARD_VIEW);
    }

    public Card getCard(final CardView c) {
        if (c == null) {
            return null;
        }
        return MatchUtil.cards.getKey(c.getId());
    }

    private final Function<CardView, Card> FN_GET_CARD = new Function<CardView, Card>() {
        @Override
        public Card apply(final CardView input) {
            return getCard(input);
        }
    };

    public final List<Card> getCards(final List<CardView> cards) {
        return ViewUtil.transformIfNotNull(cards, FN_GET_CARD);
    }

    private void writeCardToView(final Card c, final CardView view, final LocalGameView gameView) {
        // First, write the values independent of other views.
        boolean mayShowCard = gameView.mayShowCard(c);
        ViewUtil.writeNonDependentCardViewProperties(c, view, mayShowCard, gameView.mayShowCardFace(c));

        // Next, write the values that depend on other views if card can be shown
        if (!mayShowCard) { return; }

        final Combat combat = game.getCombat();
        view.setOwner(getPlayerView(c.getOwner()));
        view.setController(getPlayerView(c.getController()));
        view.setAttacking(combat != null && combat.isAttacking(c));
        view.setBlocking(combat != null && combat.isBlocking(c));
        view.setChosenPlayer(getPlayerView(c.getChosenPlayer()));
        view.setEquipping(getCardView(Iterables.getFirst(c.getEquipping(), null)));
        view.setEquippedBy(getCardViews(c.getEquippedBy()));
        view.setEnchantingCard(getCardView(c.getEnchantingCard()));
        view.setEnchantingPlayer(getPlayerView(c.getEnchantingPlayer()));
        view.setEnchantedBy(getCardViews(c.getEnchantedBy()));
        view.setFortifiedBy(getCardViews(c.getFortifiedBy()));
        view.setGainControlTargets(getCardViews(c.getGainControlTargets()));
        view.setCloneOrigin(getCardView(c.getCloneOrigin()));
        view.setImprinted(getCardViews(c.getImprinted()));
        view.setHauntedBy(getCardViews(c.getHauntedBy()));
        view.setHaunting(getCardView(c.getHaunting()));
        view.setMustBlock(c.getMustBlockCards() == null ? Collections.<CardView>emptySet() : getCardViews(c.getMustBlockCards()));
        view.setPairedWith(getCardView(c.getPairedWith()));
    }

    public SpellAbilityView getSpellAbilityView(final SpellAbility sa) {
        if (sa == null) {
            return null;
        }

        final SpellAbilityView view;
        if (MatchUtil.spabs.containsKey(sa.getId())) {
            view = MatchUtil.spabs.get(sa.getId());
            writeSpellAbilityToView(sa, view);
        } else {
            view = new SpellAbilityView(sa.getId());
            writeSpellAbilityToView(sa, view);
            MatchUtil.spabs.put(sa, view);
        }
        return view;
    }

    private final Function<SpellAbility, SpellAbilityView> FN_GET_SPAB_VIEW = new Function<SpellAbility, SpellAbilityView>() {
        @Override
        public SpellAbilityView apply(final SpellAbility input) {
            return getSpellAbilityView(input);
        }
    };

    public final List<SpellAbilityView> getSpellAbilityViews(final List<? extends SpellAbility> cards) {
        return ViewUtil.transformIfNotNull(cards, FN_GET_SPAB_VIEW);
    }

    public SpellAbility getSpellAbility(final SpellAbilityView spabView) {
        if (spabView == null) {
            return null;
        }
        return getSpellAbility(spabView.getId());
    }
    public SpellAbility getSpellAbility(final int id) {
        return id >= 0 ? MatchUtil.spabs.getKey(id) : null;
    }

    private final Function<SpellAbilityView, SpellAbility> FN_GET_SPAB = new Function<SpellAbilityView, SpellAbility>() {
        @Override
        public SpellAbility apply(final SpellAbilityView input) {
            return getSpellAbility(input);
        }
    };

    public final List<SpellAbility> getSpellAbilities(final List<SpellAbilityView> cards) {
        return ViewUtil.transformIfNotNull(cards, FN_GET_SPAB);
    }

    private void writeSpellAbilityToView(final SpellAbility sa, final SpellAbilityView view) {
        view.setHostCard(getCardView(sa.getHostCard()));
        view.setDescription(sa.toUnsuppressedString());
        view.setCanPlay(sa.canPlay());
        view.setPromptIfOnlyPossibleAbility(sa.promptIfOnlyPossibleAbility());
    }

    @Override
    public boolean getDisableAutoYields() {
        return this.game.getDisableAutoYields();
    }
    @Override
    public void setDisableAutoYields(final boolean b) {
        this.game.setDisableAutoYields(b);
    }

    /**
     * @param c
     *            a card.
     * @return whether this player may view the specified card.
     */
    protected abstract boolean mayShowCard(Card c);

    /**
     * @param c
     *            a card.
     * @return whether this player may view the front card face of the specified
     *         card.
     */
    protected abstract boolean mayShowCardFace(Card c);
}
