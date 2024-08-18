/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sk.apli.nb.windowactions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.util.WeakListeners;
import org.openide.util.Mutex;
import org.openide.windows.TopComponent;
import org.netbeans.core.windows.ModeImpl;
import org.netbeans.core.windows.WindowManagerImpl;
import org.netbeans.core.windows.Constants;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.netbeans.core.windows.Switches;
import org.netbeans.core.windows.actions.ActionUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.windows.Mode;
import org.openide.windows.WindowManager;

/**
 *
 * @author arsi
 */
@ActionID(id = "sk.apli.nb.windowactions.CloseAtLeft", category = "Windows/TabOperations")
@ActionRegistration(displayName = "Close tabs to the left", lazy = false)
@ActionReference(path = "Windows/TabOperations",position = 200)
public class CloseAtLeft extends AbstractAction
        implements PropertyChangeListener, Runnable {

    /**
     * TopComponent to exclude or null for global version of action
     */
    private TopComponent tc;

    /**
     * context flag - when true, close only in active mode, otherwise in whole
     * window system.
     */
    private final boolean isContext;

    private Timer updateTimer;
    private final Object LOCK = new Object();

    public CloseAtLeft() {
        this.isContext = false;
        putValue(NAME, "Close tabs to the left"); //NOI18N

        updateTimer = new Timer(300, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateEnabled();
            }
        });
        updateTimer.setRepeats(false);

        TopComponent.getRegistry().addPropertyChangeListener(
                WeakListeners.propertyChange(this, TopComponent.getRegistry()));
        updateEnabled();
    }

    public CloseAtLeft(TopComponent topComp, boolean isContext) {
        tc = topComp;
        this.isContext = isContext;
        //Include the name in the label for the popup menu - it may be clicked over
        //a component that is not selected
        putValue(Action.NAME, "Close tabs to the left"); //NOI18N

    }

    /**
     * Perform the action. Sets/unsets maximzed mode.
     */
    @Override
    public void actionPerformed(java.awt.event.ActionEvent ev) {
        TopComponent topC = obtainTC();
        if (topC != null) {
            WindowManagerImpl wmi = WindowManagerImpl.getInstance();
            ModeImpl mode = (ModeImpl) wmi.findMode(topC);
            if(mode != null && mode.getKind() == Constants.MODE_KIND_EDITOR){
                TopComponent[] openedTopComponents = wmi.getOpenedTopComponents(mode);
                int tabPosition = topC.getTabPosition();
                List<TopComponent> toDelete = new ArrayList<>();
                for (TopComponent openedTopComponent : openedTopComponents) {
                    if(!openedTopComponent.equals(topC)&&openedTopComponent.getTabPosition()<tabPosition){
                        toDelete.add(openedTopComponent);
                    }
                }
                toDelete.forEach(tC -> {
                    tC.close();
                });
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();
        if (TopComponent.Registry.PROP_ACTIVATED.equals(propName)
                || TopComponent.Registry.PROP_OPENED.equals(propName)) {
            //#216454 
            scheduleUpdate();
        }
    }

    private void scheduleUpdate() {
        synchronized (LOCK) {
            if (updateTimer.isRunning()) {
                updateTimer.restart();
            } else {
                updateTimer.start();
            }
        }
    }

    private void updateEnabled() {
        Mutex.EVENT.readAccess(this);
    }

    @Override
    public void run() {
        TopComponent tc = obtainTC();
        WindowManagerImpl wmi = WindowManagerImpl.getInstance();
        ModeImpl mode = (ModeImpl) wmi.findMode(tc);

        boolean areOtherDocs;
        if (isContext) {
            areOtherDocs = mode.getOpenedTopComponents().size() > 1;
        } else {
            areOtherDocs = wmi.getEditorTopComponents().length > 1;
        }

        setEnabled(mode != null && mode.getKind() == Constants.MODE_KIND_EDITOR
                && areOtherDocs && Switches.isEditorTopComponentClosingEnabled());
    }

    private TopComponent obtainTC() {
        TopComponent res = tc;
        if (null == res) {
            WindowManagerImpl wmi = WindowManagerImpl.getInstance();
            String[] ids = wmi.getRecentViewIDList();

            for (String tcId : ids) {
                ModeImpl mode = wmi.findModeForOpenedID(tcId);
                if (mode == null || mode.getKind() != Constants.MODE_KIND_EDITOR) {
                    continue;
                }
                res = wmi.findTopComponent(tcId);
                break;
            }
        }
        if (null == res) {
            res = TopComponent.getRegistry().getActivated();
        }
        return res;
    }

}
