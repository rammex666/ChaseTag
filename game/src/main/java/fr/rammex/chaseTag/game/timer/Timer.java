package fr.rammex.chasetag.common.timer;

/**
 * Classe représentant un timer individuel
 */
public class Timer {
    private final String id;
    private final long durationMillis;
    private long startTimeMillis;
    private long pausedTimeMillis;
    private boolean running;
    private boolean paused;
    private Runnable finishedCallback;

    /**
     * Crée un nouveau timer
     * @param id Identifiant unique du timer
     * @param durationMillis Durée du timer en millisecondes
     */
    public Timer(String id, long durationMillis) {
        this.id = id;
        this.durationMillis = durationMillis;
        this.running = false;
        this.paused = false;
        this.pausedTimeMillis = 0;
        this.finishedCallback = null;
    }

    /**
     * Crée un nouveau timer avec une durée en secondes
     * @param id Identifiant unique du timer
     * @param durationSeconds Durée du timer en secondes
     */
    public Timer(String id, int durationSeconds) {
        this(id, (long) durationSeconds * 1000);
    }

    /**
     * Démarre le timer
     */
    public void start() {
        if (!running) {
            this.startTimeMillis = System.currentTimeMillis() - pausedTimeMillis;
            this.running = true;
            this.paused = false;
        }
    }

    /**
     * Arrête le timer
     */
    public void stop() {
        this.running = false;
        this.paused = false;
        this.pausedTimeMillis = 0;
        // Exécuter le callback si le timer est fini
        if (isFinished() && finishedCallback != null) {
            finishedCallback.run();
        }
    }

    /**
     * Met en pause le timer
     */
    public void pause() {
        if (running && !paused) {
            this.pausedTimeMillis = System.currentTimeMillis() - startTimeMillis;
            this.paused = true;
            this.running = false;
        }
    }

    /**
     * Reprend le timer après une pause
     */
    public void resume() {
        if (paused) {
            this.startTimeMillis = System.currentTimeMillis() - pausedTimeMillis;
            this.running = true;
            this.paused = false;
        }
    }

    /**
     * Réinitialise le timer
     */
    public void reset() {
        this.running = false;
        this.paused = false;
        this.pausedTimeMillis = 0;
    }

    /**
     * Définit l'action à exécuter quand le timer se termine
     * @param callback Le code à exécuter (Runnable)
     * @return this pour permettre le chaînage
     */
    public Timer onFinished(Runnable callback) {
        this.finishedCallback = callback;
        return this;
    }

    /**
     * Exécute manuellement le callback si le timer est fini
     * Cette méthode est utile pour forcer l'exécution du callback
     */
    public void executeFinished() {
        if (isFinished() && finishedCallback != null) {
            finishedCallback.run();
        }
    }

    /**
     * Obtient le temps restant en millisecondes
     * @return Temps restant en ms, 0 si le timer est fini
     */
    public long getTimeRemainingMillis() {
        if (!running && !paused) {
            return durationMillis;
        }

        long elapsedTime = paused ? pausedTimeMillis : (System.currentTimeMillis() - startTimeMillis);
        long remaining = durationMillis - elapsedTime;

        return Math.max(0, remaining);
    }

    /**
     * Obtient le temps restant en secondes
     * @return Temps restant en secondes
     */
    public long getTimeRemainingSeconds() {
        return getTimeRemainingMillis() / 1000;
    }

    /**
     * Obtient le temps écoulé en millisecondes
     * @return Temps écoulé
     */
    public long getElapsedMillis() {
        if (!running && !paused) {
            return 0;
        }

        return paused ? pausedTimeMillis : (System.currentTimeMillis() - startTimeMillis);
    }

    /**
     * Obtient le temps écoulé en secondes
     * @return Temps écoulé en secondes
     */
    public long getElapsedSeconds() {
        return getElapsedMillis() / 1000;
    }

    /**
     * Vérifie si le timer est fini
     * @return true si le timer a terminé son décompte
     */
    public boolean isFinished() {
        return getTimeRemainingMillis() <= 0;
    }

    /**
     * Vérifie si le timer est en cours d'exécution
     * @return true si le timer tourne
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Vérifie si le timer est en pause
     * @return true si le timer est en pause
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Obtient l'identifiant du timer
     * @return L'ID du timer
     */
    public String getId() {
        return id;
    }

    /**
     * Obtient la durée totale du timer en millisecondes
     * @return Durée totale
     */
    public long getDurationMillis() {
        return durationMillis;
    }

    /**
     * Obtient la progression du timer en pourcentage
     * @return Pourcentage d'avancement (0-100)
     */
    public double getProgress() {
        long elapsed = getElapsedMillis();
        return Math.min(100.0, (elapsed * 100.0) / durationMillis);
    }

    /**
     * Formate le temps restant en format lisible (mm:ss)
     * @return Chaîne formatée du temps restant
     */
    public String getFormattedTimeRemaining() {
        long seconds = getTimeRemainingSeconds();
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    @Override
    public String toString() {
        return "Timer{" +
                "id='" + id + '\'' +
                ", timeRemaining=" + getFormattedTimeRemaining() +
                ", running=" + running +
                ", paused=" + paused +
                '}';
    }
}
