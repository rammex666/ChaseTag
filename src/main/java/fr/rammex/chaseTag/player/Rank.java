package fr.rammex.chaseTag.player;

public enum Rank {
    Orga("§6Organisateur","orga"),
    OwnedCeo("§cOwned CEO","ownedceo"),
    Player("§7Joueur","player");

    private final String prefix;
    private final String id;

    Rank(String prefix, String id){
        this.prefix = prefix;
        this.id = id;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getId() {
        return id;
    }

    public static Rank getRankFromID(String id){
        for (Rank rank : values()){
            if(rank.getId().equals(id)){
                return rank;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
