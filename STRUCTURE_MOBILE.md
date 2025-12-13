# Architecture du Projet - FoodNow Mobile

Ce document détaille la structure technique de l'application Android native FoodNow.

## Technologies
- **Langage** : Kotlin
- **Architecture** : MVVM (Model-View-ViewModel)
- **Réseau** : Retrofit 2 + OkHttp + Gson
- **Asynchrone** : Coroutines + Flow/LiveData
- **UI** : XML Layouts + Material Design 3
- **Navigation** : Android Navigation Component
- **Maps** : Google Maps SDK
- **Real-time** : WebSocket (StompProtocolAndroid)

## Structure des Dossiers (`com.example.foodnow`)

### `data`
Contient la couche de données.
- `Models.kt` : Data Classes (DTOs) pour l'API (User, Restaurant, Order, etc.).
- `ApiService.kt` : Interface Retrofit définissant les endpoints HTTP.
- `Repository.kt` : Abstraction pour l'accès aux données, gère les appels API.
- `TokenManager.kt` : Gestion du stockage local du Token JWT (SharedPreferences).

### `ui`
Contient l'interface utilisateur, organisée par fonctionnalité/rôle.

#### `login` / `register` / `splash`
- Fragments et ViewModels pour l'authentification.
- Routage basé sur le rôle après connexion (`LoginFragment` -> `MainActivity` ou RoleActivity).

#### `home` / `menu` / `orders` (Client)
- `HomeFragment` : Liste des restaurants.
- `MenuFragment` : Liste des plats d'un restaurant.
- `OrdersFragment` : Historique des commandes client.
- `TrackOrderFragment` : Suivi temps réel sur carte (Google Maps + WebSocket).

#### `restaurant` (Role Restaurant)
- `RestaurantActivity` : Container principal avec BottomNavigation.
- `RestaurantDashboardFragment` : Infos restaurant.
- `RestaurantOrdersFragment` : Gestion des commandes (Accept/Reject/Prepare/Ready).
- `RestaurantMenuFragment` : CRUD des plats.

#### `livreur` (Role Livreur)
- `LivreurActivity` : Container principal.
- `LivreurDashboardFragment` : Liste des livraisons assignées.
- `LivreurProfileFragment` : Gestion disponibilité.
- `DeliveryAdapter` : Liste et actions sur les livraisons.

#### `admin` (Role Admin)
- `AdminActivity` : Container principal.
- `AdminDashboardFragment` : Statistiques.
- `AdminRestaurantsFragment` : CRUD Restaurants (Liste, Toggle, Create).
- `AdminLivreursFragment` : CRUD Livreurs (Liste, Toggle, Create).
- `AdminOrdersFragment` : Vue des commandes par restaurant.

### `service`
- `LocationService.kt` : Service de premier plan (Foreground Service) pour envoyer la position GPS du livreur au serveur en temps réel.

## Navigation
- `nav_graph.xml` : Flux principal Client (Login -> Home -> Menu -> Order).
- `nav_graph_restaurant.xml` : Flux Restaurant.
- `nav_graph_livreur.xml` : Flux Livreur.
- `nav_graph_admin.xml` : Flux Administrateur.

## Configuration
- `AndroidManifest.xml` : Permissions (Internet, Location), Déclaration des Activités et Services.
- `build.gradle.kts` : Dépendances (Retrofit, Maps, Stomp, etc.).
