package fr.rammex.chasetag.common;

public enum ServerState {
    STARTING,   // Pterodactyl vient de créer le serveur
    WAITING,    // Serveur prêt, en attente de joueurs
    PLAYING,    // Partie en cours
    ENDING      // Partie terminée, en cours de kill
}