package fr.rammex.chasetag.lobby.player.rank;

public enum Rank {
    Orga("Organisateur","orga","#38B6FF","#FF26B9", ""),
    Star("Star","star","#DAA544","#DAA544", ""),
    Staff("Staff","staff","#78DA44","#78DA44", ""),
    RespStaff("Résp-Staff","respstaff","#E75D36","#E75D36", ""),
    Joueur("Joueur","joueur","#3E9AE0","#3E9AE0", ""),
    Dev("Développeur","dev","#8D00B0","#FFFFFF", "#8D00B0"),
    Graphiste("Graphiste","graphiste","#843EE0","#843EE0", "");

    private final String prefix;
    private final String id;
    private final String hex1;
    private final String hex2;
    private final String hex3;

    Rank(String prefix, String id, String hex1, String hex2, String hex3){
        this.prefix = prefix;
        this.id = id;
        this.hex1 = hex1;
        this.hex2 = hex2;
        this.hex3 = hex3;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getId() {
        return id;
    }

    public String getHex1() {
        return hex1;
    }

    public String getHex2() {
        return hex2;
    }

    public String getHex3() {
        return hex3;
    }

    public static Rank getRoleFromID(String id){
        for (Rank role : values()){
            if(role.getId().equals(id)){
                return role;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
