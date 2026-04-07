package fr.rammex.chaseTag.game.arena;

import org.bukkit.Location;

public class Arena {
    private final String id;
    private final String name;
    private String worldName;
    private double x1;
    private double x2;
    private double y1;
    private double y2;
    private double z1;
    private double z2;
    private Location blueSpawn;
    private Location redSpawn;
    private Location specSpawn;

    public Arena(String id, String name, String worldName, double x1, double x2, double y1, double y2, double z1, double z2){
        this.id = id;
        this.name = name;
        this.worldName = worldName;
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
        this.z1 = z1;
        this.z2 = z2;
    }

    public String getId() {
        return id;
    }

    public double getZ2() {
        return z2;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getY2() {
        return y2;
    }

    public double getY1() {
        return y1;
    }

    public double getX2() {
        return x2;
    }

    public double getX1() {
        return x1;
    }

    public String getName() {
        return name;
    }

    public double getZ1() {
        return z1;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public void setX1(double x1) {
        this.x1 = x1;
    }


    public void setX2(double x2) {
        this.x2 = x2;
    }

    public void setY1(double y1) {
        this.y1 = y1;
    }

    public void setY2(double y2) {
        this.y2 = y2;
    }

    public void setZ1(double z1) {
        this.z1 = z1;
    }

    public void setZ2(double z2) {
        this.z2 = z2;
    }

    public void setBlueSpawn(Location blueSpawn) {
        this.blueSpawn = blueSpawn;
    }

    public void setRedSpawn(Location redSpawn) {
        this.redSpawn = redSpawn;
    }

    public void setSpecSpawn(Location specSpawn) {
        this.specSpawn = specSpawn;
    }

    public boolean isInside(Location loc) {
        if (loc == null || loc.getWorld() == null || !loc.getWorld().getName().equals(worldName)) {
            return false;
        }
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();

        return x >= Math.min(x1, x2) && x <= Math.max(x1, x2) &&
               y >= Math.min(y1, y2) && y <= Math.max(y1, y2) &&
               z >= Math.min(z1, z2) && z <= Math.max(z1, z2);
    }

    public Location getBlueSpawn() {
        return blueSpawn;
    }

    public Location getRedSpawn() {
        return redSpawn;
    }

    public Location getSpecSpawn() {
        return specSpawn;
    }
}
