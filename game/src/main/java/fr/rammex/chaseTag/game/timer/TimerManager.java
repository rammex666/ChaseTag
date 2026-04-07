package fr.rammex.chaseTag.game.timer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Gestionnaire centralisé des timers
 */
public class TimerManager {
    private final Map<String, Timer> timers;
    private final List<TimerFinishedCallback> finishCallbacks;

    /**
     * Interface pour les callbacks quand un timer se termine
     */
    @FunctionalInterface
    public interface TimerFinishedCallback {
        void onTimerFinished(String timerId, Timer timer);
    }

    public TimerManager() {
        this.timers = new ConcurrentHashMap<>();
        this.finishCallbacks = new ArrayList<>();
    }

    /**
     * Crée un nouveau timer et l'ajoute au gestionnaire
     * @param id Identifiant unique
     * @param durationMillis Durée en millisecondes
     * @return Le timer créé
     */
    public Timer createTimer(String id, long durationMillis) {
        Timer timer = new Timer(id, durationMillis);
        timers.put(id, timer);
        return timer;
    }

    /**
     * Crée un nouveau timer avec durée en secondes
     * @param id Identifiant unique
     * @param durationSeconds Durée en secondes
     * @return Le timer créé
     */
    public Timer createTimer(String id, int durationSeconds) {
        return createTimer(id, (long) durationSeconds * 1000);
    }

    /**
     * Obtient un timer par son ID
     * @param id L'ID du timer
     * @return Le timer, ou null s'il n'existe pas
     */
    public Timer getTimer(String id) {
        return timers.get(id);
    }

    /**
     * Démarre un timer
     * @param id L'ID du timer
     * @return true si le timer a été démarré, false s'il n'existe pas
     */
    public boolean startTimer(String id) {
        Timer timer = timers.get(id);
        if (timer != null) {
            timer.start();
            return true;
        }
        return false;
    }

    /**
     * Arrête un timer
     * @param id L'ID du timer
     * @return true si le timer a été arrêté, false s'il n'existe pas
     */
    public boolean stopTimer(String id) {
        Timer timer = timers.get(id);
        if (timer != null) {
            timer.stop();
            return true;
        }
        return false;
    }

    /**
     * Met en pause un timer
     * @param id L'ID du timer
     * @return true si le timer a été mis en pause, false s'il n'existe pas
     */
    public boolean pauseTimer(String id) {
        Timer timer = timers.get(id);
        if (timer != null) {
            timer.pause();
            return true;
        }
        return false;
    }

    /**
     * Reprend un timer en pause
     * @param id L'ID du timer
     * @return true si le timer a été repris, false s'il n'existe pas
     */
    public boolean resumeTimer(String id) {
        Timer timer = timers.get(id);
        if (timer != null) {
            timer.resume();
            return true;
        }
        return false;
    }

    /**
     * Supprime un timer
     * @param id L'ID du timer
     * @return true si le timer a été supprimé, false s'il n'existe pas
     */
    public boolean removeTimer(String id) {
        return timers.remove(id) != null;
    }

    /**
     * Réinitialise un timer
     * @param id L'ID du timer
     * @return true si le timer a été réinitialisé, false s'il n'existe pas
     */
    public boolean resetTimer(String id) {
        Timer timer = timers.get(id);
        if (timer != null) {
            timer.reset();
            return true;
        }
        return false;
    }

    /**
     * Vérifie si un timer existe
     * @param id L'ID du timer
     * @return true si le timer existe
     */
    public boolean exists(String id) {
        return timers.containsKey(id);
    }

    /**
     * Obtient tous les timers
     * @return Liste de tous les timers
     */
    public Collection<Timer> getAllTimers() {
        return timers.values();
    }

    /**
     * Obtient tous les timers actifs (en cours)
     * @return Liste des timers en cours d'exécution
     */
    public List<Timer> getRunningTimers() {
        return timers.values().stream()
                .filter(Timer::isRunning)
                .collect(Collectors.toList());
    }

    /**
     * Obtient tous les timers finis
     * @return Liste des timers terminés
     */
    public List<Timer> getFinishedTimers() {
        return timers.values().stream()
                .filter(Timer::isFinished)
                .collect(Collectors.toList());
    }

    /**
     * Met à jour tous les timers et vérifie ceux qui sont terminés
     * Cette méthode doit être appelée régulièrement (ex: chaque tick Minecraft)
     */
    public void update() {
        List<String> finishedTimerIds = new ArrayList<>();

        for (Timer timer : timers.values()) {
            if (timer.isRunning() && timer.isFinished()) {
                finishedTimerIds.add(timer.getId());
                // Arrêter automatiquement
                timer.stop();
            }
        }

        // Appeler les callbacks pour les timers terminés
        for (String id : finishedTimerIds) {
            Timer timer = timers.get(id);
            if (timer != null) {
                for (TimerFinishedCallback callback : finishCallbacks) {
                    callback.onTimerFinished(id, timer);
                }
            }
        }
    }

    /**
     * Enregistre un callback à appeler quand un timer se termine
     * @param callback La fonction à appeler
     */
    public void onTimerFinished(TimerFinishedCallback callback) {
        finishCallbacks.add(callback);
    }

    /**
     * Supprime tous les timers
     */
    public void clearAllTimers() {
        timers.clear();
    }

    /**
     * Arrête tous les timers
     */
    public void stopAllTimers() {
        timers.values().forEach(Timer::stop);
    }

    /**
     * Obtient le nombre de timers
     * @return Nombre total de timers
     */
    public int getTimerCount() {
        return timers.size();
    }

    /**
     * Obtient un rapport sur l'état de tous les timers
     * @return String représentant l'état
     */
    public String getStatus() {
        StringBuilder sb = new StringBuilder("=== Timer Manager Status ===\n");
        sb.append("Total timers: ").append(timers.size()).append("\n");

        for (Timer timer : timers.values()) {
            sb.append("[").append(timer.getId()).append("] ");
            sb.append("Time: ").append(timer.getFormattedTimeRemaining()).append(" | ");
            sb.append("Status: ").append(timer.isRunning() ? "RUNNING" : 
                                        timer.isPaused() ? "PAUSED" :
                                        timer.isFinished() ? "FINISHED" : "STOPPED").append(" | ");
            sb.append("Progress: ").append(String.format("%.1f%%", timer.getProgress())).append("\n");
        }

        return sb.toString();
    }
}
