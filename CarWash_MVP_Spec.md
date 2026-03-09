# 🚗 CarWash Manager — Spécification MVP

> **Principe directeur :** Chaque fonctionnalité doit répondre à un problème quotidien réel du spot.  
> Si tu ne peux pas dire *"sans ça, mon employé ou moi on galère chaque jour"* — ça sort du MVP.

---

## 👥 Les 3 Utilisateurs du Système

| Utilisateur | Support | Besoin principal |
|---|---|---|
| **Employé (Caissier)** | Tablette fixe au spot | Enregistrer une transaction en < 10 secondes |
| **Toi (Manager)** | Smartphone, n'importe où | Voir les chiffres en temps réel, gérer les abonnements, recevoir les alertes |
| **Associé (Co-gérant)** | Smartphone | Accès lecture seule sur les chiffres |

---

## 📦 Les 4 Modules du MVP

---

### MODULE 1 — La Caisse 🧾
> *Priorité absolue — sans ça, rien d'autre ne sert.*

#### Ce que l'employé fait
- Sélectionne le type de service parmi une liste visuelle (icônes + noms + prix)
- Sélectionne le mode de paiement : **Cash / Orange Money / Moov / Abonnement**
- Valide → transaction enregistrée instantanément
- Peut annuler une transaction dans les 2 minutes (avec raison obligatoire)

#### Ce que le système fait automatiquement
- Horodate chaque transaction
- Calcule le total journalier en temps réel
- Si paiement par abonnement → décrémente le compteur de passages du client
- Génère un reçu simple (numéro + service + montant + heure)

#### ❌ Hors périmètre MVP
- Gestion de monnaie rendue
- Impression de ticket (optionnel si imprimante Bluetooth disponible)
- Caisse multi-employé avec tiroir caisse

---

### MODULE 2 — Abonnements & Fidélité 🎟️
> *Le module qui justifie le plus l'app par rapport à Excel.*

#### Types de comptes clients

| Type | Description |
|---|---|
| **Carte passages** | 5 ou 10 lavages prépayés, décomptés à chaque passage |
| **Abonnement mensuel Bouclier** | 8 000 FCFA/mois — 2 lavages + protections inclus |
| **Client VIP** | Tarif négocié, suivi personnalisé (flottes entreprises, ONG) |

#### Ce que le système gère
- Création d'un client en 30 secondes (nom + téléphone — rien d'autre)
- Recherche client rapide par nom ou numéro de téléphone
- Affichage immédiat du solde/statut à la caisse quand l'employé sélectionne "Abonnement"
- Alerte automatique quand un abonnement expire dans **5 jours**
- Alerte quand il reste **1 seul passage** sur une carte

#### ❌ Hors périmètre MVP
- Portail client (le client ne se connecte pas lui-même)
- Paiement automatique de renouvellement
- Points de fidélité avec calcul complexe

---

### MODULE 3 — Dashboard Manager 📊
> *Ta fenêtre sur le spot quand tu es ailleurs. Accessible sur mobile, chargement rapide.*

#### Vue Journalière
- Nombre de véhicules lavés aujourd'hui
- CA brut du jour par mode de paiement (Cash / Mobile Money / Abonnements)
- Breakdown par type de service
- Comparaison avec la même journée la semaine passée

#### Vue Mensuelle
- Courbe CA sur 30 jours
- Top 3 services les plus vendus
- Nombre d'abonnements actifs vs expirés
- Bénéfice estimé (CA − charges fixes paramétrées)

#### Alertes temps réel (notifications push)
- 🔴 Transaction annulée par l'employé → notification immédiate
- 🟢 CA journalier atteint l'objectif → notification positive
- 🟡 Stock produit sous le seuil → alerte

#### ❌ Hors périmètre MVP
- Comptabilité formelle (bilan, TVA)
- Prévisions automatiques
- Export PDF de rapports

---

### MODULE 4 — Gestion Stock Basique 📦
> *Simple mais vital pour ne jamais tomber en rupture.*

#### Ce que tu paramètres une fois
- Liste des produits avec stock initial et seuil d'alerte
- Exemple : *Shampoing 20L → stock initial : 2 bidons → alerte à 0,5 bidon*

#### Ce que le système fait automatiquement
- Décrémentation du stock à chaque transaction selon des ratios définis
- Exemple : *1 lavage complet = 0,05L shampoing consommé*

#### Ce que tu fais
- Mise à jour manuelle du stock à chaque livraison (2 clics)
- Réception des alertes quand un produit approche du seuil

#### ❌ Hors périmètre MVP
- Gestion fournisseurs
- Bons de commande automatiques
- Valorisation du stock

---

## 🏗️ Architecture Technique

### Frontend — Vue.js 3

| Technologie | Rôle | Pourquoi |
|---|---|---|
| **Vite** | Bundler | Beaucoup plus rapide que Vue CLI |
| **Pinia** | State management | Plus simple que Vuex pour ce périmètre |
| **Vue Router** | Navigation | Standard Vue |
| **TailwindCSS** | UI | Rapide à écrire, responsive natif |
| **PWA** | Mode offline | Fonctionne sans internet, installable sur tablette |

> 💡 **PWA = clé du projet.** Pas besoin d'app mobile native. Une Progressive Web App installée sur la tablette de l'employé se comporte comme une app native, fonctionne hors ligne, et tu développes une seule codebase.

### Backend — Spring Boot

| Technologie | Rôle |
|---|---|
| **Spring Boot 3** + Java 17 | Framework principal |
| **Spring Security** + JWT | Auth — 3 rôles : `EMPLOYEE` / `MANAGER` / `PARTNER` |
| **Spring Data JPA** + **PostgreSQL** | Persistance (plus robuste que MySQL pour ce cas) |
| **Spring WebSocket** | Mises à jour temps réel du dashboard |
| **Firebase Cloud Messaging** | Notifications push |

### Base de Données — Schéma simplifié

```sql
User          (id, name, phone, role, createdAt)
Client        (id, name, phone, type, balance, expiresAt)
Service       (id, name, price, category, productConsumption)
Transaction   (id, serviceId, clientId, userId, amount, paymentMethod, createdAt)
Product       (id, name, stock, alertThreshold, unit)
StockMovement (id, productId, quantity, type, transactionId, createdAt)
```

> **6 tables seulement.** Pas besoin de 20 tables pour le MVP.

### Hébergement

| Composant | Service | Coût |
|---|---|---|
| **Backend** | Railway ou Render | Gratuit → ~5$/mois si besoin |
| **Base de données** | Supabase (PostgreSQL managé) | Gratuit (plan généreux) |
| **Frontend** | Vercel | Gratuit, déploiement auto depuis GitHub |

> 💰 **Coût total hébergement : 0 à 5$/mois** au démarrage.

---

## 📅 Planning de Développement

> Rythme : soirs + weekends en parallèle du job CMP

| Semaine | Livrable |
|---|---|
| **S1 – S2** | Setup projet + Auth + Base de données + API Services/Produits |
| **S3 – S4** | Module Caisse complet (frontend + backend) |
| **S5 – S6** | Module Abonnements (CRUD clients + logique décompte) |
| **S7 – S8** | Dashboard Manager (graphes + temps réel WebSocket) |
| **S9** | Module Stock (décrémentation auto + alertes) |
| **S10** | Tests, corrections, déploiement, formation employé |

> ⏱️ **Total : 10 semaines** — soit ~2 mois et demi, ce qui correspond exactement à ta période de préparation avant l'ouverture du Spot 1.

---

## ⚠️ Risques à Anticiper

### Risque 1 — Feature Creep
Tu vas avoir envie d'ajouter des fonctionnalités en cours de route.  
→ **Solution :** Crée un fichier `BACKLOG_V2.md` où tu notes tout ce qui sort du périmètre. Ne touche pas au MVP.

### Risque 2 — Connexion Internet instable
À Ouaga, le réseau peut être capricieux.  
→ **Solution :** La caisse doit fonctionner **offline** et synchroniser quand la connexion revient. Prévoir le service worker PWA dès **S1**, pas en rattrapage.

### Risque 3 — Résistance de l'employé
Si l'interface n'est pas ultra intuitive, il va "oublier" d'enregistrer des transactions.  
→ **Solution :** Prévoir une session de test avec une vraie personne non-tech avant l'ouverture. L'interface caisse doit être validée par quelqu'un qui n'a jamais vu l'app.

---

## 🗺️ Vue d'Ensemble

```
CarWash Manager MVP
│
├── 👨‍💼 Employé (Tablette — PWA)
│   ├── Caisse
│   │   ├── Sélectionner service
│   │   ├── Choisir mode de paiement
│   │   └── Valider → Transaction enregistrée
│   └── Client abonné
│       ├── Rechercher par nom/téléphone
│       ├── Vérifier solde/statut
│       └── Décompter passage → Valider
│
├── 📱 Manager (Mobile — PWA)
│   ├── Dashboard
│   │   ├── CA jour / semaine / mois
│   │   ├── Breakdown par service
│   │   └── Comparaison semaine passée
│   ├── Abonnements
│   │   ├── Liste clients + statuts
│   │   ├── Alertes expiration
│   │   └── Alertes solde faible
│   └── Stock
│       ├── Niveaux produits
│       └── Alertes rupture
│
└── 🔧 Stack Technique
    ├── Vue 3 + Pinia + TailwindCSS (PWA)
    ├── Spring Boot 3 + PostgreSQL
    └── Hébergement ~0$/mois au démarrage
```

---

## 🚀 Prochaines Étapes

Une fois le MVP validé, voici ce qui peut venir en **V2** (après 6 mois d'exploitation) :

- [ ] Export rapports PDF / Excel
- [ ] Portail client (historique lavages, statut abonnement)
- [ ] Intégration Orange Money API (paiement automatique renouvellement)
- [ ] Gestion multi-spots (Spot 1 + Spot 2 dashboard consolidé)
- [ ] Notifications WhatsApp clients (rappel abonnement, promo)
- [ ] Module paie employé (heures + commissions)

---

*Document généré — Mars 2026 | Spot 1 Ouagadougou*
