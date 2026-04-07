package fr.rammex.chaseTag.game.player;

public enum Role {
    Spec("§8Spéctateur","spec"),
    Staff("§bStaff","staff"),
    Chase("§cChasseur","chase"),
    Run("§9Chassé","run"),
    None("","none");

    private final String prefix;
    private final String id;

    Role(String prefix, String id){
        this.prefix = prefix;
        this.id = id;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getId() {
        return id;
    }

    public static Role getRoleFromID(String id){
        for (Role role : values()){
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
