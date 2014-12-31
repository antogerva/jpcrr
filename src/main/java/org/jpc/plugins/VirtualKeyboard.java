/*
    JPC-RR: A x86 PC Hardware Emulator
    Release 1

    Copyright (C) 2007-2009 Isis Innovation Limited
    Copyright (C) 2009-2010 H. Ilari Liusvaara
    Copyright (C) 2010 Foone Debonte

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License version 2 as published by
    the Free Software Foundation.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

    Based on JPC x86 PC Hardware emulator,
    A project from the Physics Dept, The University of Oxford

    Details about original JPC can be found at:

    www-jpc.physics.ox.ac.uk

*/

package org.jpc.plugins;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jpc.Misc;
import org.jpc.emulator.peripheral.Keyboard;
import org.jpc.emulator.KeyboardStatusListener;
import org.jpc.jrsr.UTFInputLineStream;
import org.jpc.pluginsbase.Plugins;
import org.jpc.pluginsbase.Plugin;
import org.jpc.pluginsaux.ConstantTableLayout;
import org.jpc.Misc;

import static org.jpc.Misc.errorDialog;
import static org.jpc.Misc.moveWindow;
import static org.jpc.Misc.parseStringToComponents;
import static org.jpc.Misc.openStream;

import java.io.*;

import javax.swing.*;
import javax.swing.border.*;

import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.awt.event.*;
import java.awt.*;

import javax.swing.plaf.basic.*;
import javax.swing.plaf.*;

public class VirtualKeyboard implements ActionListener, Plugin, KeyboardStatusListener
{
    public static boolean isKeyboardHookHasBeenActivated = false;
    
    private JFrame window;
    private JPanel panel;
    private HashMap<String, Integer> commandToKey;
    private HashMap<Integer, String> keyToCommand;
    private HashMap<String, JToggleButton> commandToButton;
    private JToggleButton capsLock;
    private JToggleButton numLock;
    private JToggleButton scrollLock;
    private Font keyFont;
    private Font smallKeyFont;
    private Border keyBorder, smallKeyBorder, classicBorder;
    private boolean nativeButtons;
    private static String DEFAULT_KEYBOARD_FILENAME = "datafiles/keyboards/default";

    private org.jpc.emulator.peripheral.Keyboard keyboard;
    private int keyNo;
    private boolean[] cachedState;
    private Plugins pluginManager;
    private int nativeWidth, nativeHeight;
    
    public org.jpc.emulator.peripheral.Keyboard getKeyboard() {
        return keyboard;
    }

    public JToggleButton addKey(String name, String topKey, int scanCode, int x, int y, int w, int h, char sizeCode,
        boolean special)
    {
        String cmdName = name + "-" + (keyNo++);
        String label = name;
        if(topKey != null) {
            label = "<html>" + topKey + "<br>" + name + "</html>";
        } else if(label.indexOf('&') >= 0) {
            label = "<html>" + name + "</html>";
        }
        JToggleButton button = new JToggleButton(label, false);
        if(sizeCode == 'N') {
            button.setFont(keyFont);
            button.setBorder(keyBorder);
        } else if(sizeCode == 'S') {
            button.setFont(smallKeyFont);
            button.setBorder(smallKeyBorder);
        } else if(sizeCode == 'C' && !nativeButtons) {
            button.setBorder(classicBorder);
        }

        button.setRolloverEnabled(false);
        if(special) {
            button.setEnabled(false);
            button.setVisible(false);
        } else {
            commandToKey.put(cmdName, new Integer(scanCode));
            keyToCommand.put(new Integer(scanCode),cmdName);
            commandToButton.put(cmdName, button);
            button.setActionCommand(cmdName);
            button.addActionListener(this);
        }

        if(!nativeButtons) button.setUI(new KeyboardButtonUI());

        panel.add(button, new ConstantTableLayout.Placement(x, y, w, h));
        return button;
    }

    public void eci_virtualkeyboard_setwinpos(Integer x, Integer y)
    {
        moveWindow(window, x.intValue(), y.intValue(), nativeWidth, nativeHeight);
    }

    public VirtualKeyboard(Plugins _pluginManager) throws IOException
    {
        this(_pluginManager, "");
    }
    
    public final class LogFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            StringBuilder line = new StringBuilder();

            line.append(new Date(record.getMillis()))
                .append(" ")
                .append(record.getLevel().getLocalizedName())
                .append(":\t")
                .append(formatMessage(record));

            if (record.getThrown() != null) {
                try {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    record.getThrown().printStackTrace(pw);
                    pw.close();
                    line.append(sw.toString());
                    sw.close();
                }
                catch (Exception ex) { /* Do Nothing */ }
            }

            return line.toString();
        }
    }

    

    private static final Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());

    public VirtualKeyboard(Plugins _pluginManager, String args) throws IOException
    {
        // Disable parent logger and set the desired level.
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);

        // Add our custom formatter to a console handler.
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new LogFormatter());
        handler.setLevel(Level.WARNING);
        logger.addHandler(handler);
        
        
        
        pluginManager = _pluginManager;
        Map<String, String> params = parseStringToComponents(args);
        String keyboardPath = params.get("keyboard");

        nativeButtons = "native".equalsIgnoreCase(params.get("style")) || (keyboardPath == null);

        keyNo = 0;
        keyboard = null;
        commandToKey = new HashMap<String, Integer>();
        keyToCommand = new HashMap<Integer, String>();
        keyToCommand.put(57419, "&#8592;-99");//left-arrow
        keyToCommand.put(57421, "&#8594;-101");//right-arrow
        keyToCommand.put(57424, "&#8595;-100");//down-arrow
        keyToCommand.put(57416, "&#8593;-89"); //up-arrow
        commandToButton = new HashMap<String, JToggleButton>();
        window = new JFrame("Virtual Keyboard" + Misc.emuname);
        ConstantTableLayout layout = new ConstantTableLayout();
        cachedState = new boolean[256];
        panel = new JPanel(layout);
        keyFont = new Font("SanSerif", Font.PLAIN, 11);
        smallKeyFont = keyFont.deriveFont(9.0f);

        if(nativeButtons) {
            keyBorder = new EmptyBorder(0, 5, 0, 5);
            smallKeyBorder = new EmptyBorder(0, 1, 0, 1);
            // classicBorder isn't used with native buttons
        } else {
            Border outerBorder = new CompoundBorder(new EmptyBorder(1, 1, 0, 0), new SimpleButtonBorder(false));
            keyBorder = new CompoundBorder(outerBorder, new EmptyBorder(0, 3, 0, 3));
            smallKeyBorder = new CompoundBorder(outerBorder, new EmptyBorder(0, 1, 0, 1));
            classicBorder = new SimpleButtonBorder(true);
        }
        
        final KeyboardHook hook = new KeyboardHook(this);
        final JLabel lblBindKeyboard = new JLabel();
        lblBindKeyboard.setBounds(25, 0, 100, 50);
        lblBindKeyboard.setText("Bind keyboard");
        window.add(lblBindKeyboard);
        
        final JCheckBox chkBindKeyBoard = new JCheckBox();
        chkBindKeyBoard.setBounds(0, 0, 25, 50);
        chkBindKeyBoard.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if(chkBindKeyBoard.isSelected()){
                    if(!VirtualKeyboard.isKeyboardHookHasBeenActivated){
                        try {
                            GlobalScreen.registerNativeHook();
                            VirtualKeyboard.isKeyboardHookHasBeenActivated=true;
                        } catch (NativeHookException e) {
                            e.printStackTrace();
                        }
                    }
                    GlobalScreen.getInstance().addNativeKeyListener(hook);
                } else {
                    GlobalScreen.getInstance().removeNativeKeyListener(hook);
                    try {
                        GlobalScreen.unregisterNativeHook();
                        VirtualKeyboard.isKeyboardHookHasBeenActivated=false;
                    } catch (NativeHookException e) {
                        e.printStackTrace();
                    }                    
                }
                
            }
        });
        window.add(chkBindKeyBoard);

        final JLabel lblMoveMouse = new JLabel();
        lblMoveMouse.setBounds(150, 0, 100, 50);
        lblMoveMouse.setText("Move Mouse :");
        window.add(lblMoveMouse);
        
        final JTextField txtMoveLeft = new JTextField();
        txtMoveLeft.setBounds(250, 15, 50, 20);  
        txtMoveLeft.setText("0");
        window.add(txtMoveLeft);

        final JTextField txtMoveUp = new JTextField();
        txtMoveUp.setBounds(302, 5, 50, 20);  
        txtMoveUp.setText("0");
        window.add(txtMoveUp);
        
        final JTextField txtMoveDown = new JTextField();
        txtMoveDown.setBounds(302, 28, 50, 20);  
        txtMoveDown.setText("0");
        window.add(txtMoveDown);

        final JTextField txtMoveRight = new JTextField();
        txtMoveRight.setBounds(354, 15, 50, 20);  
        txtMoveRight.setText("0");
        window.add(txtMoveRight);
        
        final JButton btnMoveMouse = new JButton();
        btnMoveMouse.setBounds(408,15,100,20);
        btnMoveMouse.setText("Move");
        btnMoveMouse.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                int left, right, up, down;
                try{
                    left=Integer.parseInt(txtMoveRight.getText());
                    right=Integer.parseInt(txtMoveLeft.getText());
                    up=Integer.parseInt(txtMoveUp.getText());
                    down=Integer.parseInt(txtMoveDown.getText());
                }catch(Exception e){
                    left=0; right=0; up=0; down=0;
                    System.err.println("Couldn't read input for mouse motion");
                }
                hook.sendMouseMotion(left-right,up-down);
                txtMoveLeft.setText("0");
                txtMoveRight.setText("0");
                txtMoveUp.setText("0");
                txtMoveDown.setText("0");
            }
        });
        window.add(btnMoveMouse);
        
        final JToggleButton btnMouse0 = new JToggleButton("<html>" + "MOUSEBUTTON 0" + "</html>");
        btnMouse0.setBounds(512,15,50,20);
        btnMouse0.setText("ML0");
        btnMouse0.setFont(keyFont);
        btnMouse0.setBorder(keyBorder);
        //btnMouse0.addActionListener(this);
        btnMouse0.addActionListener(new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) {
                hook.sendMouseButton(0);
            }
        });
        btnMouse0.setActionCommand("MOUSEBUTTON 0");
        window.add(btnMouse0);
        

        final JToggleButton btnMouse1 = new JToggleButton("<html>" + "MOUSEBUTTON 1" + "</html>");
        btnMouse1.setBounds(562,15,50,20);
        btnMouse1.setText("MR1");
        btnMouse1.setFont(keyFont);
        btnMouse1.setBorder(keyBorder);
        //btnMouse1.addActionListener(this);
        btnMouse1.addActionListener(new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) {
                hook.sendMouseButton(1);
            }
        });
        btnMouse1.setActionCommand("MOUSEBUTTON 1");
        window.add(btnMouse1);
        

        final JToggleButton btnMouse2 = new JToggleButton("<html>" + "MOUSEBUTTON 2" + "</html>");
        btnMouse2.setBounds(612,15,50,20);
        btnMouse2.setText("MM2");
        btnMouse2.setFont(keyFont);
        btnMouse2.setBorder(keyBorder);
        //btnMouse2.addActionListener(this);
        btnMouse2.addActionListener(new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) {
                hook.sendMouseButton(2);
            }
        });
        btnMouse2.setActionCommand("MOUSEBUTTON 2");
        window.add(btnMouse2);
                
        window.add(panel);
        parseKeyboardFile(keyboardPath);

        window.pack();
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        Dimension d = window.getSize();
        window.setSize(new Dimension((int) d.getWidth(),d.height+50));
        d= window.getSize();
        nativeWidth = d.width;
        nativeHeight = d.height;
        window.setVisible(true);
    }

    private static int parseCoord(String value, int next)
    {
        if("-".equals(value))
            return next;
        else
            return Integer.valueOf(value);
    }

    private void parseKeyboardFile(String filename) throws IOException
    {
        InputStream in = openStream(filename, DEFAULT_KEYBOARD_FILENAME);
        if((in = openStream(filename, DEFAULT_KEYBOARD_FILENAME)) == null)
            throw new IOException("Neither primary keyboard file nor fallback file exists.");

        UTFInputLineStream keyboardFile = new UTFInputLineStream(in);
        String[] line;
        int nextX = 0, nextY = 0;

        while((line = Misc.nextParseLine(keyboardFile)) != null)
            if(line.length <= 1)
                continue;
            else if(line.length == 7 || line.length == 8) {
                int x = parseCoord(line[1], nextX), y = parseCoord(line[2], nextY);
                int w = Integer.parseInt(line[3]), h = Integer.parseInt(line[4]);
                char sizeCode = line[5].charAt(0);
                String name = line[6], shifted = null;
                if(line.length == 8)
                    shifted = line[7];

                try {
                    int scanCode = Integer.parseInt(line[0]);
                    addKey(name, shifted, scanCode, x, y, w, h, sizeCode, false);
                } catch(NumberFormatException nfe) { // The scanCode wasn't a number, so this must be a special key
                    String scanName = line[0];
                    JToggleButton specialButton = addKey(name, null, 0, x, y, w, h, sizeCode, true);
                    if(scanName.equalsIgnoreCase("numlock"))
                        numLock=specialButton;
                    else if(scanName.equalsIgnoreCase("capslock"))
                        capsLock=specialButton;
                    else if(scanName.equalsIgnoreCase("scrolllock"))
                        scrollLock=specialButton;

                }
                nextX = x + w;
                nextY = y;
            } else
                throw new IOException("Invalid line in keyboard layout.");
    }

    //-1 if unknown, bit 2 is capslock, bit 1 is numlock, bit 0 is scrollock.
    private void updateLEDs(int status)
    {
        if(status < 0) {
            numLock.setVisible(false);
            numLock.setSelected(false);
            capsLock.setVisible(false);
            capsLock.setSelected(false);
            scrollLock.setVisible(false);
            scrollLock.setSelected(false);
        } else {
            numLock.setVisible((status & 2) != 0);
            capsLock.setVisible((status & 4) != 0);
            scrollLock.setVisible((status & 1) != 0);
        }
    }

    public void resetButtons()
    {
        for(Map.Entry<String, Integer> entry : commandToKey.entrySet()) {
            int scan = entry.getValue().intValue();
            JToggleButton button = commandToButton.get(entry.getKey());
            if(keyboard.getKeyStatus((byte)scan) != cachedState[scan]) {
                cachedState[scan] = keyboard.getKeyStatus((byte)scan);
                button.setSelected(cachedState[scan]);
            }
        }
        updateLEDs(keyboard.getLEDStatus());
    }

    private void keyStatusChangeEventThread(int scancode, boolean pressed)
    {
/*      THIS IS JUST PLAIN BROKEN.
        for(Map.Entry<String, Integer> entry : commandToKey.entrySet()) {
            int scan = entry.getValue().intValue();
            if(scan != scancode)
                continue;
            JToggleButton button = commandToButton.get(entry.getKey());
            if(pressed != cachedState[scan]) {
                cachedState[scan] = pressed;
                button.setSelected(pressed);
            }
        }
*/
    }

    public void keyExecStatusChange(int scancode, boolean pressed)
    {
        //These aren't currently shown.
    }

    public void keyStatusChange(int scancode, boolean pressed)
    {
        if(!SwingUtilities.isEventDispatchThread())
            try {
                final int _scancode = scancode;
                final boolean _pressed = pressed;
                SwingUtilities.invokeLater(new Thread() { public void run() {
                    VirtualKeyboard.this.keyStatusChangeEventThread(_scancode, _pressed); }});
            } catch(Exception e) {
            }
        else
            keyStatusChangeEventThread(scancode, pressed);
    }

    public void keyStatusReload()
    {
        if(!SwingUtilities.isEventDispatchThread())
            try {
                SwingUtilities.invokeLater(new Thread() { public void run() { VirtualKeyboard.this.resetButtons(); }});
            } catch(Exception e) {
            }
        else
            resetButtons();
    }

    public void ledStatusChange(int newstatus)
    {
        if(!SwingUtilities.isEventDispatchThread())
            try {
                final int _newstatus = newstatus;
                SwingUtilities.invokeLater(new Thread() { public void run() {
                    VirtualKeyboard.this.updateLEDs(_newstatus); }});
            } catch(Exception e) {
            }
        else
            updateLEDs(newstatus);
    }

    public void mouseButtonsChange(int newstatus)
    {
        //Not interesting.
    }

    public void mouseExecButtonsChange(int newstatus)
    {
        //Not interesting.
    }

    public void main()
    {
        //This runs entierely in UI thread.
    }

    public boolean systemShutdown()
    {
        //OK to proceed with JVM shutdown.
        return true;
    }

    public void pcStarting()
    {
        //Not interested.
    }

    public void pcStopping()
    {
        if(pluginManager.isShuttingDown())
            return;  //Too much of deadlock risk.

        if(!SwingUtilities.isEventDispatchThread())
            try {
                SwingUtilities.invokeAndWait(new Thread() { public void run() { VirtualKeyboard.this.resetButtons(); }});
            } catch(Exception e) {
            }
        else
            resetButtons();
    }

    public void reconnect(org.jpc.emulator.PC pc)
    {
        if(keyboard != null)
            keyboard.removeStatusListener(this);
        if(pc != null) {
            Keyboard keys = (Keyboard)pc.getComponent(Keyboard.class);
            keyboard = keys;
            keyboard.addStatusListener(this);
            keyStatusReload();
        } else {
            keyboard = null;
            Iterator<Map.Entry<String, Integer> > itt = commandToKey.entrySet().iterator();
            while (itt.hasNext())
            {
                Map.Entry<String, Integer> entry = itt.next();
                String n = entry.getKey();
                Integer s = entry.getValue();
                cachedState[s.intValue()] = false;
                commandToButton.get(n).setSelected(false);
                ledStatusChange(-1);
            }
        }
    }

    public void actionPerformed(ActionEvent evt)
    {
        if(keyboard == null)
            return;
        String command = evt.getActionCommand();
        JToggleButton button = commandToButton.get(command);
        int scan = commandToKey.get(command).intValue();
        sendKey(evt.getModifiers(), button, scan);
    }
    
    public void sendKey(int modifier, JToggleButton button, int scan){
        if(button==null){
            scan=commandToKey.get((keyToCommand.get(scan)));
            button=commandToButton.get(keyToCommand.get(scan));
            if(button==null)
                return;                
        }
        
        boolean doubleEdge = (scan != 255) && ((modifier & ActionEvent.SHIFT_MASK) != 0);
        if(button.isSelected())
            if(doubleEdge)
                System.err.println("Informational: Keyhit on key " + scan + ".");
            else
                System.err.println("Informational: Keydown on key " + scan + ".");
        else
            if(doubleEdge)
                System.err.println("Informational: Keyupdown on key " + scan + ".");
            else
                System.err.println("Informational: Keyup on key " + scan + ".");
        try {
            keyboard.sendEdge(scan);
            if(doubleEdge)
                keyboard.sendEdge(scan);
        } catch(Exception e) {
            System.err.println("Error: Sending command failed: " + e);
            errorDialog(e, "Failed to send keyboard key edge", null, "Dismiss");
        }
        if(!doubleEdge)
            cachedState[scan] = !cachedState[scan];
        button.setSelected(cachedState[scan]);        
    }

    protected static class KeyboardButtonUI extends BasicToggleButtonUI
    {
        protected static Color highlightColor = new Color(200, 200, 200);
        protected static Color backgroundColor = new Color(220, 220, 220);

        protected void simplePaint(Graphics g, JComponent c, Color color)
        {
            Rectangle viewRect = new Rectangle(c.getSize());
            Insets margin;
            try {
                /* We want to ignore the inner margins while calculating the background size, so we have to
                 * pull the outside border out of the compound border */
                CompoundBorder border = (CompoundBorder)c.getBorder();
                margin = border.getOutsideBorder().getBorderInsets(c);
            } catch(ClassCastException cce) {
                // We were called on a button without our elaborate triple-border, so default to the whole inset
                margin = c.getBorder().getBorderInsets(c);
            }

            g.setColor(color);

            g.fillRect(viewRect.x + margin.left, viewRect.y + margin.top,
                    viewRect.width - (margin.left + margin.right),
                    viewRect.height - (margin.top + margin.bottom));
        }

        protected void paintButtonPressed(Graphics g, AbstractButton b)
        {
            simplePaint(g, b, highlightColor);
        }

        public void paint(Graphics g, JComponent c)
        {
            simplePaint(g, c, backgroundColor);
            super.paint(g, c);
        }
    }

    protected static class SimpleButtonBorder extends LineBorder
    {
        private static final long serialVersionUID = 1L;

        protected static Color nwColor=new Color(240, 240, 240);
        protected static Color seColor=new Color(130, 130, 130);
        protected static Color pressedColor=new Color(160, 160, 160);
        protected boolean thin;
        public SimpleButtonBorder(boolean thin)
        {
            super(Color.BLACK, 1, true);
            this.thin = thin;
        }

        public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width,
            final int height)
        {
            Color oldColor = g.getColor();
            JToggleButton button = (JToggleButton)c;
            ButtonModel model = button.getModel();
            int adjust = thin ? 0 : 1;

            // Draw inner highlights
            if(model.isSelected() || model.isPressed()) {
                // Draw the north-west highlight, but in the south-east color
                g.setColor(seColor);
                g.drawRect(x + 1, y + 1, width - 2, 0);
                g.drawRect(x + 1, y + 1, 0, height - 2);
            } else {
                // Draw the north-west highlight
                g.setColor(nwColor);
                g.drawRect(x + 1, y + 1, width - 2, adjust);
                g.drawRect(x + 1, y + 1, adjust, height - 2);
                // Draw the south-east highlight
                g.setColor(seColor);
                g.drawRect(x + 1, y + height - 2, width - 2, 0);
                g.drawRect(x + width - 2, y + 1, 0, height - 2);
                if(!thin) { // Draw inner line of shadow
                    g.drawRect(x + 2, y + height - 3, width - 3, 0);
                    g.drawRect(x + width - 3, y + 2, 0, height - 3);
                }
            }

            // Draw actual border
            g.setColor(model.isPressed() ? pressedColor : lineColor);
            g.drawRoundRect(x, y, width - 1, height - 1, 2, 2);

            // Restore color state
            g.setColor(oldColor);
        }
    }
}
