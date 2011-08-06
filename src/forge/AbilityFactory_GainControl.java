package forge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;

//AB:GainControl|ValidTgts$Creature|TgtPrompt$Select target legendary creature|LoseControl$Untap,LoseControl|SpellDescription$Gain control of target xxxxxxx

//GainControl specific params:
//	LoseControl - the lose control conditions (as a comma separated list)
//			-Untap - source card becomes untapped
//			-LoseControl - you lose control of source card
//			-LeavesPlay - source card leaves the battlefield
//			-PowerGT - (not implemented yet for Old Man of the Sea)
//	AddKWs	- Keywords to add to the controlled card (as a "&"-separated list; like Haste, Sacrifice CARDNAME at EOT, any standard keyword)
//  OppChoice - set to True if opponent chooses creature (for Preacher) - not implemented yet
//	Untap	- set to True if target card should untap when control is taken
//	DestroyTgt - actions upon which the tgt should be destroyed.  same list as LoseControl
//	NoRegen - set if destroyed creature can't be regenerated.  used only with DestroyTgt

public class AbilityFactory_GainControl {
	
	private final Card movedCards[] = new Card[1];

	private AbilityFactory AF = null;
	private HashMap<String,String> params = null;
	private Card hostCard = null;
	private ArrayList<String> lose = null;
	private ArrayList<String> destroyOn = null;
	private boolean bNoRegen = false;
	private boolean bUntap = false;
	private boolean bTapOnLose = false;
	private ArrayList<String> kws = null;
	
	public AbilityFactory_GainControl(AbilityFactory newAF) {
		AF = newAF;
		params = AF.getMapParams();
		hostCard = AF.getHostCard();
		if (params.containsKey("LoseControl")) {
			lose = new ArrayList<String>(Arrays.asList(params.get("LoseControl").split(",")));
		}
		if(params.containsKey("Untap")) {
				bUntap = true;
		}
		if(params.containsKey("TapOnLose")) {
				bTapOnLose = true;
		}
		if(params.containsKey("AddKWs")) {
			kws = new ArrayList<String>(Arrays.asList(params.get("AddKWs").split(" & ")));
		}
		if (params.containsKey("DestroyTgt")) {
			destroyOn = new ArrayList<String>(Arrays.asList(params.get("DestroyTgt").split(",")));
		}
		if(params.containsKey("NoRegen")) {
			bNoRegen = true;
		}
	}
	
	public SpellAbility getSpell() {
        SpellAbility spControl = new Spell(hostCard, AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = 3125489644424832311L;
			
			@Override
			public boolean canPlayAI() {
            	return doTgtAI(this);
            }
            
			@Override
            public void resolve() {
            	doResolve(this);
            }//resolve
            
            @Override
			public String getStackDescription(){
				 StringBuilder sb = new StringBuilder();
				 String name = AF.getHostCard().getName();
				 sb.append(name).append(" - gain control of ");
				 Card tgt = getTargetCard();
				 if (tgt != null) {
					 if(tgt.isFaceDown()) sb.append("Morph");
					 else sb.append(tgt.getName());
				 }
				 return sb.toString();
			}
        };//SpellAbility
        
        return spControl;
	}

	public SpellAbility getAbility() {

		final SpellAbility abControl = new Ability_Activated(hostCard, AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = -4384705198674678831L;

			@Override
			public boolean canPlayAI() {
				return doTgtAI(this);
			}

			@Override
			public void resolve() {
				doResolve(this);
				hostCard.setAbilityUsed(hostCard.getAbilityUsed() + 1);
			}

			@Override
			public String getStackDescription(){
				StringBuilder sb = new StringBuilder();
				String name = AF.getHostCard().getName();
				sb.append(name).append(" - gain control of ");
				Card tgt = getTargetCard();
				if (tgt != null) {
					if(tgt.isFaceDown()) sb.append("Morph");
					else sb.append(tgt.getName());
				}
				return sb.toString();
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return doTgtAI(this);
			}
		};//Ability_Activated

		return abControl;
	}


    private boolean doTgtAI(SpellAbility sa) {
        boolean hasCreature = false;
        boolean hasArtifact = false;
        boolean hasEnchantment = false;
        boolean hasLand = false;
        
		Target tgt = AF.getAbTgt();
		CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
		list = list.getValidCards(tgt.getValidTgts(), hostCard.getController(), hostCard);
		//AI won't try to grab cards that are filtered out of AI decks on purpose
		list = list.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				Hashtable<String, String> vars = c.getSVars();
				return !vars.containsKey("RemAIDeck");
			}
		});
		//filter in only cards human controls
		list = list.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				return c.getController().isHuman();
			}
		});
		
        if (list.isEmpty())
        	return false;
        
        // Don't steal something if I can't Attack without, or prevent it from blocking at least
        if (lose != null && lose.contains("EOT") && AllZone.Phase.isAfter(Constant.Phase.Combat_Declare_Blockers))
        	return false;
        
		while(tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)) { 
			Card t = null;
			for(Card c:list) {
	        	if(c.isCreature()) hasCreature = true;
	        	if(c.isArtifact()) hasArtifact = true;
	        	if(c.isLand()) hasLand = true;
	        	if(c.isEnchantment()) hasEnchantment = true;
	        }
			
			if (list.isEmpty()){
				if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0){
					tgt.resetTargets();
					return false;
				}
				else{
					// todo is this good enough? for up to amounts?
					break;
				}
			}
			
			if(hasCreature) t = CardFactoryUtil.AI_getBestCreature(list);
			else if(hasArtifact) t = CardFactoryUtil.AI_getBestArtifact(list);
			else if(hasLand) t = CardFactoryUtil.AI_getBestLand(list);
			else if(hasEnchantment) t = CardFactoryUtil.AI_getBestEnchantment(list, sa.getSourceCard(), true);
			else t = CardFactoryUtil.AI_getMostExpensivePermanent(list, sa.getSourceCard(), true);
			
			tgt.addTarget(t);
			list.remove(t);
			
			hasCreature = false;
			hasArtifact = false;
			hasLand = false;
			hasEnchantment = false;
		}
        
        return true;

    }   
    
    private void doResolve(SpellAbility sa) {
		ArrayList<Card> tgtCards;
		Target tgt = AF.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else{
			tgtCards = new ArrayList<Card>();
			tgtCards.add(hostCard);
		}

		int size = tgtCards.size();
		for(int j = 0; j < size; j++){
			final Card tgtC = tgtCards.get(j);
			
			// copied from CardFactory_Creatures for Rubinia Soulsinger
			
			//Card c = getTargetCard();
            movedCards[j] = tgtC;
            hostCard.addGainControlTarget(tgtC);
            
            if(AllZone.GameAction.isCardInPlay(tgtC) && CardFactoryUtil.canTarget(hostCard, tgtC)) {
                //set summoning sickness
                if(tgtC.getKeyword().contains("Haste")) {
                    tgtC.setSickness(false);
                } else {
                    tgtC.setSickness(true);
                }
                
                AllZone.GameAction.changeController(new CardList(tgtC), tgtC.getController(), sa.getActivatingPlayer());
                
                if(bUntap) tgtC.untap();
                
                if(null != kws) {
					for(String kw:kws) {
						tgtC.addExtrinsicKeyword(kw);
					}
				}
            }
			
			//end copied
            
            if (lose != null){
	            if(lose.contains("LeavesPlay")) {
	            	hostCard.addLeavesPlayCommand(getLoseControlCommand(j));
	            }
	            if(lose.contains("Untap")) {
	            	hostCard.addUntapCommand(getLoseControlCommand(j));
	            }
	            if(lose.contains("LoseControl")) {
	            	hostCard.addChangeControllerCommand(getLoseControlCommand(j));
	            }
	            if(lose.contains("EOT")) {
	            	AllZone.EndOfTurn.addAt(getLoseControlCommand(j));
	            }
            }
            
            if (destroyOn != null){
	            if(destroyOn.contains("LeavesPlay")) {
	            	hostCard.addLeavesPlayCommand(getDestroyCommand(j));
	            }
	            if(destroyOn.contains("Untap")) {
	            	hostCard.addUntapCommand(getDestroyCommand(j));
	            }
	            if(destroyOn.contains("LoseControl")) {
	            	hostCard.addChangeControllerCommand(getDestroyCommand(j));
	            }
            }
            
            //for Old Man of the Sea - 0 is hardcoded since it only allows 1 target
            hostCard.clearGainControlReleaseCommands();
            hostCard.addGainControlReleaseCommand(getLoseControlCommand(0));
        
		}//end foreach target
		
		if (AF.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
			   abSub.resolve();
			}
			// doesn't support old style drawbacks
		}
    }
    
    private Command getDestroyCommand(final int i) {
    	final Command destroy = new Command() {
			private static final long serialVersionUID = 878543373519872418L;

			public void execute() {
				final Card c = movedCards[i];
				Ability ability = new Ability(hostCard, "0") {
					public void resolve() {
						
		    			if(bNoRegen) {
		    				AllZone.GameAction.destroyNoRegeneration(c);
		    			}
		    			else {
		    				AllZone.GameAction.destroy(c);
		    			}
					}
				};
				StringBuilder sb = new StringBuilder();
            	sb.append(hostCard).append(" - destroy ").append(c.getName()).append(".");
            	if(bNoRegen) sb.append("  It can't be regenerated.");
            	ability.setStackDescription(sb.toString());
                
                AllZone.Stack.add(ability);
    		}
			
    	};
    	return destroy;
    }
    
    private Command getLoseControlCommand(final int i) {
    	final Command loseControl = new Command() {
    		private static final long serialVersionUID = 878543373519872418L;

    		public void execute() {
    			Card c = movedCards[i];
    			//ArrayList<Card> c = hostCard.getGainControlTargets();
    			if(null == c) return;

    			if(AllZone.GameAction.isCardInPlay(c)) {
    				AllZone.GameAction.changeController(new CardList(c), c.getController(), c.getController().getOpponent());

    				c.setSickness(true);

    				if(bTapOnLose) c.tap();
    				
    				if(null != kws) {
    					for(String kw:kws) {
    						c.removeExtrinsicKeyword(kw);
    					}
    				}
    			}//if
    			hostCard.clearGainControlTargets();
    			hostCard.clearGainControlReleaseCommands();
    			movedCards[i] = null;
    		}//execute()
    	};
    	
    	return loseControl;
    }
}
