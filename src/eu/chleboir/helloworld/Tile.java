/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.chleboir.helloworld;

import java.awt.image.BufferedImage;


public class Tile {
    private boolean passable;
    private BufferedImage img;
    private final char identifier;
    
    public Tile(boolean passable, BufferedImage img, char identifier) {
        this.passable = passable;
        this.img = img;
        this.identifier = identifier;
    } 
    
    public boolean isPassable() {
        return passable;
    }

    public void setPassable(boolean passable) {
        this.passable = passable;
    }

    public BufferedImage getImg() {
        return img;
    }

    public void setImg(BufferedImage img) {
        this.img = img;
    }

    public char getIdentifier() {
        return identifier;
    }
}
