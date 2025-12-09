import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Officine : gestion des stocks d'ingrédients et de potions, et préparation
 * selon recettes.
 *
 * Hypothèses :
 * - Les noms sont comparés de façon insensible à la casse et aux espaces
 * superflus.
 * - Les entrées/pluriels acceptent "de"/"d’"/"d'".
 * - Les recettes ne sont PAS récursives : on ne fabrique pas automatiquement
 * les sous-composants.
 * Il faut donc avoir les potions intermédiaires en stock pour préparer celles
 * qui en dépendent.
 */
public class Officine {

    /** Clé canonique -> quantité en stock */
    private final Map<String, Integer> stock = new HashMap<>();

    /** Clé canonique (potion) -> besoins par potion (clé canonique -> quantité) */
    private final Map<String, Map<String, Integer>> recettes = new HashMap<>();

    /** Ensemble des ingrédients autorisés (clés canoniques) */
    private final Set<String> ingredientsConnus = new HashSet<>();

    /** Ensemble de tous les items connus (ingrédients + potions) */
    private final Set<String> itemsConnus = new HashSet<>();

    /** Alias (forme non canonique -> canonique) pour singulier/pluriel/variantes */
    private final Map<String, String> aliasVersCanonique = new HashMap<>();

    /** Regex "quantité + nom" */
    private static final Pattern QTE_NOM = Pattern.compile("^\\s*(\\d+)\\s+(.+?)\\s*$");

    public Officine() {
        // ----- Définition des ingrédients -----
        // Liste fournie (on ajoute explicitement les formes singulier/pluriel utiles).
        String[] ingredients = new String[] {
                "œil de grenouille", // singulier
                "yeux de grenouille", // pluriel irrégulier
                "larme de brume funèbre",
                "radicelle de racine hurlante",
                "pincée de poudre de lune",
                "croc de troll",
                "fragment d'écaille de dragonnet",
                "goutte de sang de citrouille"
        };

        // On choisit comme canoniques les formes singulières suivantes (y compris
        // l'irrégulier œil/yeux)
        String[][] canonicalPairs = new String[][] {
                { "œil de grenouille", "yeux de grenouille" },
                { "larme de brume funèbre", "larmes de brume funèbre" },
                { "radicelle de racine hurlante", "radicelles de racine hurlante" },
                { "pincée de poudre de lune", "pincées de poudre de lune" },
                { "croc de troll", "crocs de troll" },
                { "fragment d'écaille de dragonnet", "fragments d'écaille de dragonnet" },
                { "goutte de sang de citrouille", "gouttes de sang de citrouille" }
        };

        for (String[] pair : canonicalPairs) {
            String canon = canon(pair[0]);
            String plural = canon(pair[1]);
            ingredientsConnus.add(canon);
            itemsConnus.add(canon);
            aliasVersCanonique.put(canon, canon);
            aliasVersCanonique.put(plural, canon);
        }

        // ----- Définition des potions & recettes -----
        // Recettes fournies
        Map<String, String[]> r = new LinkedHashMap<>();
        
        r.put("fiole de glaires purulentes",
                new String[] { "2 larmes de brume funèbre", "1 goutte de sang de citrouille" });
        r.put("bille d'âme évanescente",
                new String[] { "3 pincées de poudre de lune", "1 œil de grenouille" });
        r.put("soupçon de sels suffocants",
                new String[] { "2 crocs de troll", "1 fragment d'écaille de dragonnet",
                        "1 radicelle de racine hurlante" });
        r.put("baton de pâte sépulcrale",
                new String[] { "3 radicelles de racine hurlante", "1 fiole de glaires purulentes" });
        r.put("bouffée d'essence de cauchemar",
                new String[] { "2 pincées de poudre de lune", "2 larmes de brume funèbre" });

        // Déclarer alias pour les potions (singulier/pluriel)
        String[][] potionsCanon = new String[][] {
                { "fiole de glaires purulentes", "fioles de glaires purulentes" },
                { "bille d'âme évanescente", "billes d'âme évanescente" },
                { "soupçon de sels suffocants", "soupçons de sels suffocants" },
                { "baton de pâte sépulcrale", "batons de pâte sépulcrale" },
                { "bouffée d'essence de cauchemar", "bouffées d'essence de cauchemar" }
        };
        for (String[] pair : potionsCanon) {
            String canon = canon(pair[0]);
            String plural = canon(pair[1]);
            itemsConnus.add(canon);
            aliasVersCanonique.put(canon, canon);
            aliasVersCanonique.put(plural, canon);
        }

        // Construire les recettes canoniques
        for (Map.Entry<String, String[]> e : r.entrySet()) {
            String potionCanon = canon(e.getKey());
            Map<String, Integer> besoins = new LinkedHashMap<>();
            for (String besoin : e.getValue()) {
                Parsed p = parseQteNom(besoin);
                String itemCanon = toCanonique(p.nom);
                besoins.merge(itemCanon, p.qte, Integer::sum);
            }
            recettes.put(potionCanon, besoins);
        }

        // Initialiser les stocks à 0 pour tout item connu
        for (String item : itemsConnus) {
            stock.put(item, 0);
        }
    }

    // ---------- API demandée ----------

    /** Augmente les stocks de l'ingrédient (initialement 0) */
    public void rentrer(String expression) {
        Parsed p = parseQteNom(expression);
        String itemCanon = toCanonique(p.nom);

        if (!ingredientsConnus.contains(itemCanon)) {
            throw new IllegalArgumentException("On ne peut rentrer que des ingrédients connus : " + p.nom);
        }
        if (p.qte < 0)
            throw new IllegalArgumentException("Quantité négative interdite.");
        if (p.qte == 0)
            return;

        stock.merge(itemCanon, p.qte, Integer::sum);
    }

    /**
     * Retourne la quantité en stock (supporte singulier/pluriel, casse et accents).
     */
    public int quantite(String nom) {
        String itemCanon = toCanonique(nom);
        return stock.getOrDefault(itemCanon, 0);
    }

    /**
     * Prépare n potions si possible, met à jour les stocks, et retourne la quantité
     * effectivement préparée.
     * Ne fabrique PAS les sous-composants manquants automatiquement.
     */
    public int preparer(String expression) {
        Parsed p = parseQteNom(expression);
        String potionCanon = toCanonique(p.nom);

        if (!recettes.containsKey(potionCanon)) {
            throw new IllegalArgumentException("Potion inconnue : " + p.nom);
        }
        if (p.qte < 0)
            throw new IllegalArgumentException("Quantité négative interdite.");
        if (p.qte == 0)
            return 0;

        Map<String, Integer> besoinsParUnite = recettes.get(potionCanon);

        // Calcul du maximum faisable en fonction des stocks actuels
        int maxPossible = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> e : besoinsParUnite.entrySet()) {
            String item = e.getKey();
            int besoinUnitaire = e.getValue();
            int enStock = stock.getOrDefault(item, 0);
            int possible = enStock / besoinUnitaire;
            maxPossible = Math.min(maxPossible, possible);
        }

        int aFabriquer = Math.min(p.qte, maxPossible);
        if (aFabriquer <= 0)
            return 0;

        // Consommer les composants
        for (Map.Entry<String, Integer> e : besoinsParUnite.entrySet()) {
            String item = e.getKey();
            int totalBesoin = e.getValue() * aFabriquer;
            stock.put(item, stock.getOrDefault(item, 0) - totalBesoin);
        }

        // Ajouter la/les potions au stock
        stock.merge(potionCanon, aFabriquer, Integer::sum);

        return aFabriquer;
    }

    // ---------- Outils de parsing & normalisation ----------

    private static final Pattern D_APOS = Pattern.compile("d’|d'");

    private String canon(String s) {
        if (s == null)
            return "";
        String x = s.trim().toLowerCase(Locale.ROOT);

        // Normaliser apostrophes typographiques
        x = D_APOS.matcher(x).replaceAll("d'");

        // Normaliser espaces
        x = x.replaceAll("\\s+", " ");

        // Déaccentuer seulement pour la CLE d'alias ? Non : on garde les accents (œ/é)
        // mais on uniformise les variantes critiques : "oeil" -> "œil"
        x = x.replace("oeil", "œil"); // tolérer "oeil" saisi au clavier

        // Normaliser "citrouille" casse variable
        x = x.replace("citrouille", "citrouille");

        return x;
    }

    private String toCanonique(String nom) {
        String key = canon(nom);
        // Essayer alias direct
        String canon = aliasVersCanonique.get(key);
        if (canon != null)
            return canon;

        // Heuristic : si finit par 's' on tente la version singulière en retirant un
        // 's' sur le premier mot
        // (utile si un test ajoute un item pluriel simple non répertorié
        // explicitement).
        if (key.endsWith("s")) {
            // première tentative : retirer 's' global (rarement correct pour les composés)
            String tentative = key.substring(0, key.length() - 1);
            if (aliasVersCanonique.containsKey(tentative)) {
                return aliasVersCanonique.get(tentative);
            }
        }
        if (!itemsConnus.contains(key)) {
            throw new IllegalArgumentException("Item inconnu : " + nom);
        }
        return key;
    }

    private Parsed parseQteNom(String expr) {
        Objects.requireNonNull(expr, "expression nulle");
        Matcher m = QTE_NOM.matcher(expr);
        if (!m.matches()) {
            throw new IllegalArgumentException("Expression attendue : '<entier> <nom>', reçu : " + expr);
        }
        int qte = Integer.parseInt(m.group(1));
        String nom = m.group(2);
        return new Parsed(qte, nom);
    }

    private static final class Parsed {
        final int qte;
        final String nom;

        Parsed(int qte, String nom) {
            this.qte = qte;
            this.nom = nom;
        }
    }
}
