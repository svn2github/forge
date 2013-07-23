package forge.gui.toolbox.special;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import forge.Card;
import forge.gui.CardPicturePanel;
import forge.gui.SOverlayUtils;
import forge.gui.toolbox.FOverlay;

/** 
 * Displays card image BIG.
 *
 */
public enum CardZoomer {
    SINGLETON_INSTANCE;    

    private final JPanel overlay = FOverlay.SINGLETON_INSTANCE.getPanel();
    private Card thisCard;
    private JPanel pnlMain;
    
    private CardZoomer() {        
        setupMouseListeners();
        setupKeyListeners();
    }
    
    private void setupKeyListeners() {
        
        overlay.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    closeZoomer();                   
                }                
            }        
        });

    }

    private void setupMouseListeners() {

        overlay.addMouseListener(new MouseAdapter() {            
            @Override
            public void mouseClicked(MouseEvent e) {
                closeZoomer();
            }
        });
        
        overlay.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.getWheelRotation() > 0) {
                    closeZoomer();
                } 
            }
        });
                
    }
        
    public void displayZoomedCard(Card card) {
        
        thisCard = card;
        setLayout();

        CardPicturePanel picturePanel = new CardPicturePanel(); 
        picturePanel.setCard(thisCard);        
        picturePanel.setOpaque(false);
        pnlMain.add(picturePanel, "w 40%!, h 80%!");
        
        SOverlayUtils.showOverlay();        
                      
    }
    
    private void setLayout() {
        
        overlay.removeAll();

        pnlMain = new JPanel();
        pnlMain.setOpaque(false);
               
        overlay.setLayout(new MigLayout("insets 0, w 100%!, h 100%!"));
        pnlMain.setLayout(new MigLayout("insets 0, wrap, align center"));

        overlay.add(pnlMain, "w 100%!, h 100%!");

    }
    
    private void closeZoomer() {
        SOverlayUtils.hideOverlay();
    }
              
}