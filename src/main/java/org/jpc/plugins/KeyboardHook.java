package org.jpc.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

public class KeyboardHook implements NativeKeyListener{
    
    private VirtualKeyboard virtualKeyboard = null;
    
    private List<Integer> keyPressed;
    
    public KeyboardHook(VirtualKeyboard virtualKeyboard) {
        this.virtualKeyboard=virtualKeyboard;
        this.keyPressed=new ArrayList<Integer>();
    }


    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        int scan = e.getKeyCode();
        if(!this.keyPressed.contains(scan)){
            this.virtualKeyboard.sendKey(16, null, scan);
            this.keyPressed.add(new Integer(scan));
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        int scan = e.getKeyCode();
        this.virtualKeyboard.sendKey(16, null, scan);
        
        if(this.keyPressed.contains(scan))
            this.keyPressed.remove(new Integer(scan));
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
    }
    
    public void sendMouseButton(int mouseButton){
        try {
            this.virtualKeyboard.getKeyboard().sendMouseButton(mouseButton);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }  
    
    public void sendMouseMotion(int xMotion, int yMotion){
        try {
            this.virtualKeyboard.getKeyboard().sendYMouseMotion(yMotion);
            this.virtualKeyboard.getKeyboard().sendXMouseMotion(xMotion);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }  
    
}
