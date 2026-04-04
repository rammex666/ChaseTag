package fr.rammex.chasetag.common.timer;

/**
 * Exemple d'utilisation du gestionnaire de timers
 */
public class TimerUsageExample {

    public static void main(String[] args) throws InterruptedException {
        // Créer un gestionnaire de timers
        TimerManager timerManager = new TimerManager();

        // === EXEMPLE 1: Créer et démarrer un timer avec callback ===
        Timer matchTimer = timerManager.createTimer("match", 60) // 60 secondes
                .onFinished(() -> System.out.println("✅ La partie est terminée!"));
        matchTimer.start();
        System.out.println("Timer lancé: " + matchTimer.getFormattedTimeRemaining());

        // === EXEMPLE 2: Créer plusieurs timers ===
        timerManager.createTimer("countdown", 30).start();
        timerManager.createTimer("cooldown", 10).start();

        // === EXEMPLE 3: Vérifier le temps restant ===
        Thread.sleep(5000); // Attendre 5 secondes
        Timer timer = timerManager.getTimer("match");
        System.out.println("Temps restant: " + timer.getFormattedTimeRemaining() + " (" + timer.getTimeRemainingSeconds() + "s)");
        System.out.println("Temps écoulé: " + timer.getElapsedSeconds() + "s");
        System.out.println("Progress: " + String.format("%.1f%%", timer.getProgress()));

        // === EXEMPLE 4: Vérifier si un timer est fini ===
        if (timer.isFinished()) {
            System.out.println("Le timer est terminé!");
        } else {
            System.out.println("Le timer est toujours en cours...");
        }

        // === EXEMPLE 5: Mettre en pause et reprendre ===
        Timer pausedTimer = timerManager.getTimer("countdown");
        pausedTimer.pause();
        System.out.println("Timer en pause: " + pausedTimer.getFormattedTimeRemaining());
        Thread.sleep(2000);
        System.out.println("Après 2s de pause: " + pausedTimer.getFormattedTimeRemaining());
        pausedTimer.resume();
        System.out.println("Timer repris");

        // === EXEMPLE 6: Enregistrer un callback pour quand le timer finit ===
        timerManager.onTimerFinished((id, finishedTimer) -> {
            System.out.println("⏱️  Timer '" + id + "' est terminé!");
        });

        // === EXEMPLE 6b: Utiliser onFinished() directement sur un timer ===
        Timer cooldownTimer = timerManager.getTimer("cooldown");
        if (cooldownTimer != null) {
            cooldownTimer.onFinished(() -> {
                System.out.println("🔄 Le cooldown a expiré!");
                // Vous pouvez ajouter n'importe quel code ici
                System.out.println("Cooldown duration was: " + cooldownTimer.getDurationMillis() + "ms");
            });
        }

        // === EXEMPLE 6c: Code personnalisé quand le timer finit ===
        Timer specialTimer = timerManager.createTimer("special_event", 10)
                .onFinished(() -> {
                    System.out.println("🎉 Événement spécial déclenché!");
                    System.out.println("Exécution du code personnalisé...");
                    // Ici vous pouvez exécuter vos actions:
                    // - Changer l'état du jeu
                    // - Envoyer des messages aux joueurs
                    // - Déclencher des événements
                    // - etc.
                });
        specialTimer.start();

        // === EXEMPLE 7: Mettre à jour les timers (appeler chaque tick) ===
        for (int i = 0; i < 5; i++) {
            timerManager.update(); // Cette méthode appelle les callbacks si des timers sont finis
            Thread.sleep(1000);
        }

        // === EXEMPLE 8: Obtenir l'état de tous les timers ===
        System.out.println(timerManager.getStatus());

        // === EXEMPLE 9: Obtenir des listes de timers ===
        System.out.println("Timers en cours: " + timerManager.getRunningTimers().size());
        System.out.println("Timers terminés: " + timerManager.getFinishedTimers().size());

        // === EXEMPLE 10: Éditer un timer existant ===
        timerManager.pauseTimer("match");
        timerManager.resetTimer("match");
        timerManager.startTimer("match");

        // === EXEMPLE 11: Supprimer un timer ===
        timerManager.removeTimer("cooldown");
        System.out.println("Nombre de timers restants: " + timerManager.getTimerCount());

        // === EXEMPLE 12: Utiliser une durée en millisecondes ===
        Timer msTimer = timerManager.createTimer("ms_timer", 5000); // 5000ms = 5s
        msTimer.start();
        System.out.println("Timer en ms: " + msTimer.getTimeRemainingMillis() + "ms");
    }
}
