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
package forge.model;

import forge.CardStorageReader;
import forge.CardStorageReader.ProgressObserver;
import forge.FThreads;
import forge.StaticData;
import forge.achievement.AchievementCollection;
import forge.achievement.ConstructedAchievements;
import forge.achievement.DraftAchievements;
import forge.achievement.QuestAchievements;
import forge.achievement.SealedAchievements;
import forge.ai.AiProfileUtil;
import forge.card.CardPreferences;
import forge.card.CardType;
import forge.deck.io.DeckPreferences;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.game.card.CardUtil;
import forge.gauntlet.GauntletData;
import forge.interfaces.IProgressBar;
import forge.itemmanager.ItemManagerConfig;
import forge.limited.GauntletMini;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.quest.QuestController;
import forge.quest.QuestWorld;
import forge.quest.data.QuestPreferences;
import forge.util.FileUtil;
import forge.util.Localizer;
import forge.util.storage.IStorage;
import forge.util.storage.StorageBase;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The default Model implementation for Forge.
 * 
 * This used to be an interface, but it seems unlikely that we will ever use a
 * different model.
 * 
 * In case we need to convert it into an interface in the future, all fields of
 * this class must be either private or public static final.
 */
public class FModel {
    private FModel() { } //don't allow creating instance

    private static StaticData magicDb;

    private static QuestPreferences questPreferences;
    private static ForgePreferences preferences;

    private static Map<GameType, AchievementCollection> achievements;

    // Someone should take care of 2 gauntlets here
    private static GauntletData gauntletData;
    private static GauntletMini gauntlet;

    private static QuestController quest;
    private static CardCollections decks;

    private static IStorage<CardBlock> blocks;
    private static IStorage<CardBlock> fantasyBlocks;
    private static IStorage<QuestWorld> worlds;
    private static GameFormat.Collection formats;

    public static void initialize(final IProgressBar progressBar) {

		// Instantiate preferences: quest and regular
		//Preferences are initialized first so that the splash screen can be translated.
		try {
			preferences = new ForgePreferences();
		}
		catch (final Exception exn) {
			throw new RuntimeException(exn);
		}

		Localizer.getInstance().initialize(FModel.getPreferences().getPref(FPref.UI_LANGUAGE), ForgeConstants.LANG_DIR);
		
        //load card database
        final ProgressObserver progressBarBridge = (progressBar == null) ?
                ProgressObserver.emptyObserver : new ProgressObserver() {
            @Override
            public void setOperationName(final String name, final boolean usePercents) {
                FThreads.invokeInEdtLater(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setDescription(name);
                        progressBar.setPercentMode(usePercents);
                    }
                });
            }

            @Override
            public void report(final int current, final int total) {
                FThreads.invokeInEdtLater(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setMaximum(total);
                        progressBar.setValue(current);
                    }
                });
            }
        };

        final CardStorageReader reader = new CardStorageReader(ForgeConstants.CARD_DATA_DIR, progressBarBridge, null);
        magicDb = new StaticData(reader, ForgeConstants.EDITIONS_DIR, ForgeConstants.BLOCK_DATA_DIR);

        //create profile dirs if they don't already exist
        for (String dname : ForgeConstants.PROFILE_DIRS) {
            File path = new File(dname);
            if (path.isDirectory()) {
                // already exists
                continue;
            }
            if (!path.mkdirs()) {
                throw new RuntimeException("cannot create profile directory: " + dname);
            }
        }

        ForgePreferences.DEV_MODE = preferences.getPrefBoolean(FPref.DEV_MODE_ENABLED);
        ForgePreferences.UPLOAD_DRAFT = ForgePreferences.NET_CONN; // && preferences.getPrefBoolean(FPref.UI_UPLOAD_DRAFT);

        formats = new GameFormat.Collection(new GameFormat.Reader(new File(ForgeConstants.BLOCK_DATA_DIR + "formats.txt")));
        blocks = new StorageBase<CardBlock>("Block definitions", new CardBlock.Reader(ForgeConstants.BLOCK_DATA_DIR + "blocks.txt", magicDb.getEditions()));
        questPreferences = new QuestPreferences();
        fantasyBlocks = new StorageBase<CardBlock>("Custom blocks", new CardBlock.Reader(ForgeConstants.BLOCK_DATA_DIR + "fantasyblocks.txt", magicDb.getEditions()));
        worlds = new StorageBase<QuestWorld>("Quest worlds", new QuestWorld.Reader(ForgeConstants.QUEST_WORLD_DIR + "worlds.txt"));

        loadDynamicGamedata();

        if (progressBar != null) {
            FThreads.invokeInEdtLater(new Runnable() {
                @Override
                public void run() {
                    progressBar.setDescription(Localizer.getInstance().getMessage("splash.loading.decks"));
                }
            });
        }

        decks = new CardCollections();
        quest = new QuestController();

        CardPreferences.load();
        DeckPreferences.load();
        ItemManagerConfig.load();

        achievements = new HashMap<GameType, AchievementCollection>();
        achievements.put(GameType.Constructed, new ConstructedAchievements());
        achievements.put(GameType.Draft, new DraftAchievements());
        achievements.put(GameType.Sealed, new SealedAchievements());
        achievements.put(GameType.Quest, new QuestAchievements());

        //preload AI profiles
        AiProfileUtil.loadAllProfiles(ForgeConstants.AI_PROFILE_DIR);
    }

    public static QuestController getQuest() {
        return quest;
    }
    
    private static boolean keywordsLoaded = false;
    
    /**
     * Load dynamic gamedata.
     */
    public static void loadDynamicGamedata() {
        if (!CardType.Constant.LOADED[0]) {
            final List<String> typeListFile = FileUtil.readFile(ForgeConstants.TYPE_LIST_FILE);

            List<String> tList = null;

            if (typeListFile.size() > 0) {
                for (int i = 0; i < typeListFile.size(); i++) {
                    final String s = typeListFile.get(i);

                    if (s.equals("[CardTypes]")) {
                        tList = CardType.Constant.CARD_TYPES;
                    }

                    else if (s.equals("[SuperTypes]")) {
                        tList = CardType.Constant.SUPER_TYPES;
                    }

                    else if (s.equals("[BasicTypes]")) {
                        tList = CardType.Constant.BASIC_TYPES;
                    }

                    else if (s.equals("[LandTypes]")) {
                        tList = CardType.Constant.LAND_TYPES;
                    }

                    else if (s.equals("[CreatureTypes]")) {
                        tList = CardType.Constant.CREATURE_TYPES;
                    }

                    else if (s.equals("[InstantTypes]")) {
                        tList = CardType.Constant.INSTANT_TYPES;
                    }

                    else if (s.equals("[SorceryTypes]")) {
                        tList = CardType.Constant.SORCERY_TYPES;
                    }

                    else if (s.equals("[EnchantmentTypes]")) {
                        tList = CardType.Constant.ENCHANTMENT_TYPES;
                    }

                    else if (s.equals("[ArtifactTypes]")) {
                        tList = CardType.Constant.ARTIFACT_TYPES;
                    }

                    else if (s.equals("[WalkerTypes]")) {
                        tList = CardType.Constant.WALKER_TYPES;
                    }

                    else if (s.length() > 1) {
                        tList.add(s);
                    }
                }
            }
            CardType.Constant.LOADED[0] = true;
        }

        if (!keywordsLoaded) {
            final List<String> nskwListFile = FileUtil.readFile(ForgeConstants.KEYWORD_LIST_FILE);

            if (nskwListFile.size() > 1) {
                for (String s : nskwListFile) {
                    if (s.length() > 1) {
                        CardUtil.NON_STACKING_LIST.add(s);
                    }
                }
            }
            keywordsLoaded = true;
        }
    }

    public static StaticData getMagicDb() {
        return magicDb;
    }

    public static ForgePreferences getPreferences() {
        return preferences;
    }

    public static AchievementCollection getAchievements(GameType gameType) {
        return achievements.get(gameType);
    }

    public static IStorage<CardBlock> getBlocks() {
        return blocks;
    }    

    public static QuestPreferences getQuestPreferences() {
        return questPreferences;
    }

    public static GauntletData getGauntletData() {
        return gauntletData;
    }

    public static void setGauntletData(GauntletData data0) {
        gauntletData = data0;
    }

    public static GauntletMini getGauntletMini() {
        if (gauntlet == null) {
            gauntlet = new GauntletMini();
        }
        return gauntlet;
    }

    public static CardCollections getDecks() {
        return decks;
    }

    public static IStorage<QuestWorld> getWorlds() {
        return worlds;
    }
 
    public static GameFormat.Collection getFormats() {
        return formats;
    }

    public static IStorage<CardBlock> getFantasyBlocks() {
        return fantasyBlocks;
    }
}
