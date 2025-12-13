# Guide Utilisateur - FoodNow Mobile

Ce guide décrit comment utiliser l'application mobile FoodNow pour chaque rôle : Client, Restaurant, Livreur et Administrateur.

## Prérequis
- Backend Spring Boot démarré (Port 8080).
- Base de données PostgreSQL configurée.
- Application Mobile installée sur appareil/émulateur Android.
- Clé API Google Maps configurée dans `AndroidManifest.xml`.

## 1. Client
**Fonctionnalités :** Inscription, Connexion, Consultation Menu, Passage de commande, Suivi de commande.

1.  **Inscription/Connexion** :
    *   Lancez l'app. Cliquez sur "Register" pour créer un compte Client.
    *   Connectez-vous avec email/mot de passe.
2.  **Commander** :
    *   Sur l'écran d'accueil, sélectionnez "Order Now".
    *   Choisissez un restaurant.
    *   Ajoutez des articles au panier (bouton "+").
    *   Validez la commande (Fonctionnalité simulée si pas d'écran panier complet).
3.  **Suivi** :
    *   Allez dans l'onglet "Orders" (Barre de navigation).
    *   Si une commande est marquée "IN_DELIVERY", cliquez dessus pour ouvrir la carte de suivi en temps réel.
    *   Vous verrez la position du livreur se mettre à jour.

## 2. Administrateur
**Fonctionnalités :** Gestion des Restaurants, Livreurs et Utilisateurs.

1.  **Connexion** : Connectez-vous avec un compte ayant le rôle `ADMIN`.
2.  **Dashboard** : Vue des statistiques système (nombre d'utilisateurs, commandes, etc.).
3.  **Restaurants** :
    *   Onglet "Restaurants".
    *   **Créer** : Bouton "+" (FAB) pour ajouter un restaurant et son compte propriétaire.
    *   **Gérer** : Cliquez sur "Disable/Enable" pour activer/désactiver un restaurant.
    *   **Commandes** : Cliquez sur un restaurant pour voir l'historique de ses commandes.
4.  **Livreurs** :
    *   Onglet "Livreurs".
    *   **Créer** : Bouton "+" pour ajouter un livreur.
    *   **Gérer** : Activez/Désactivez les comptes livreurs.

## 3. Restaurant
**Fonctionnalités :** Gestion du Menu, Gestion des Commandes.

1.  **Connexion** : Utilisez le compte créé par l'Administrateur.
2.  **Dashboard** : Infos du restaurant.
3.  **Commandes** :
    *   Onglet "Orders".
    *   Acceptez (`Accept`), Préparez (`Prepare`), et marquez Prêt (`Ready`) les commandes entrantes.
    *   Rejetez les commandes si nécessaire.
4.  **Menu** :
    *   Onglet "Menu".
    *   Ajoutez ("+"), Nom, Prix, Description des plats.

## 4. Livreur
**Fonctionnalités :** Gestion des Livraisons, Localisation.

1.  **Connexion** : Utilisez le compte créé par l'Administrateur.
2.  **Profil** : Activez votre statut "Available" pour être visible.
3.  **Livraisons** :
    *   Onglet "Deliveries".
    *   Consultez les livraisons assignées.
    *   Mettez à jour le statut : `Pick Up` (Récupéré) -> `Delivered` (Livré).
4.  **Tracking** :
    *   L'application envoie votre position GPS en arrière-plan au serveur pour que le client puisse vous suivre.
    *   Assurez-vous d'accepter les permissions de localisation.
