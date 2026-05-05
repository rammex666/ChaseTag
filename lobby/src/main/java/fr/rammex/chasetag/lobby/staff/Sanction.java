package fr.rammex.chasetag.lobby.staff;

import java.util.UUID;

public class Sanction {
    public enum Type { MUTE, BAN }

    private final String id;
    private final Type type;
    private final String targetName;
    private final UUID targetUuid;
    private final String reason;
    private final String staffName;
    private final long timestamp;

    public Sanction(String id, Type type, String targetName, UUID targetUuid, String reason, String staffName) {
        this.id = id;
        this.type = type;
        this.targetName = targetName;
        this.targetUuid = targetUuid;
        this.reason = reason;
        this.staffName = staffName;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public Type getType() { return type; }
    public String getTargetName() { return targetName; }
    public UUID getTargetUuid() { return targetUuid; }
    public String getReason() { return reason; }
    public String getStaffName() { return staffName; }
    public long getTimestamp() { return timestamp; }
}
