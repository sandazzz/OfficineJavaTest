import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OfficineTest {

    private Officine off;

    @BeforeEach
    void setUp() {
        off = new Officine();
    }

    @Test
    void casUsuel_rentrerEtQuantite_singulierPluriel() {
        assertEquals(0, off.quantite("œil de grenouille"));
        off.rentrer("3 yeux de grenouioolle");
        assertEquals(3, off.quantite("œil de grenouille"));
        assertEquals(3, off.quantite("yeux de grenouille"));
        off.rentrer("2 œil de grenouille"); // tolère singulier après un nombre
        assertEquals(5, off.quantite("yeux de grenouille"));
    }

    @Test
    void casUsuel_preparerFiole_glairesPurulentes() {
        off.rentrer("5 larmes de brume funèbre");
        off.rentrer("2 gouttes de sang de Citrouille"); // casse différente

        int faits = off.preparer("2 fioles de glaires purulentes");
        assertEquals(2, faits);

        // Consommation : 2*2 larmes = 4 ; 2*1 goutte = 2
        assertEquals(1, off.quantite("larme de brume funèbre"));
        assertEquals(0, off.quantite("goutte de sang de citrouille"));
        assertEquals(2, off.quantite("fiole de glaires purulentes"));
    }

    @Test
    void casLimite_preparerPlusQueStockPermet() {
        off.rentrer("3 larmes de brume funèbre");
        off.rentrer("5 gouttes de sang de citrouille");

        // 1 fiole nécessite 2 larmes + 1 goutte -> avec 3 larmes, max 1 fiole
        int faits = off.preparer("3 fioles de glaires purulentes");
        assertEquals(1, faits);

        assertEquals(1, off.quantite("larme de brume funèbre")); // 3 - 2
        assertEquals(4, off.quantite("goutte de sang de citrouille")); // 5 - 1
        assertEquals(1, off.quantite("fiole de glaires purulentes"));
    }

    @Test
    void casUsuel_preparerBaton_consommeFioleEtRadicelles() {
        // Préparer les composants
        off.rentrer("3 radicelles de racine hurlante");

        off.rentrer("2 larmes de brume funèbre");
        off.rentrer("1 goutte de sang de citrouille");
        assertEquals(1, off.preparer("1 fiole de glaires purulentes"));

        // Maintenant produire 1 bâton (non accentué dans l'énoncé : "baton")
        int faits = off.preparer("1 baton de pâte sépulcrale");
        assertEquals(1, faits);

        assertEquals(0, off.quantite("fiole de glaires purulentes"));
        assertEquals(0, off.quantite("radicelle de racine hurlante"));
        assertEquals(1, off.quantite("baton de pâte sépulcrale"));
    }

    @Test
    void casLimite_zeroDemande() {
        off.rentrer("10 crocs de troll");
        int faits = off.preparer("0 soupçons de sels suffocants");
        assertEquals(0, faits);
        assertEquals(10, off.quantite("croc de troll"));
    }

    @Test
    void casErreur_itemInconnu_rentrer() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> off.rentrer("3 ailes de chauve-souris"));
        assertTrue(ex.getMessage().toLowerCase().contains("ingrédients connus"));
    }

    @Test
    void casErreur_itemInconnu_preparer() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> off.preparer("2 élixirs de lumière"));
        assertTrue(ex.getMessage().toLowerCase().contains("potion inconnue"));
    }

    @Test
    void casErreur_expressionMalFormee() {
        assertThrows(IllegalArgumentException.class, () -> off.rentrer("trois yeux de grenouille"));
        assertThrows(IllegalArgumentException.class, () -> off.preparer("fiole de glaires purulentes"));
    }

    @Test
    void casExtreme_grandesQuantites_performanceSimple() {
        off.rentrer("300000 larmes de brume funèbre");
        off.rentrer("200000 gouttes de sang de citrouille");
        int faits = off.preparer("100000 fioles de glaires purulentes");
        assertEquals(100000, faits);
        assertEquals(100000, off.quantite("fiole de glaires purulentes"));
    }

    @Test
    void plurielsEtApostrophes_normalisation() {
        off.rentrer("3 pincées de poudre de lune");
        off.rentrer("2 larmes de brume funèbre");
        int faits = off.preparer("1 bouffée d’essence de cauchemar"); // apostrophe typographique
        assertEquals(1, faits);
        assertEquals(1, off.quantite("bouffées d'essence de cauchemar") - 0); // vérifie alias pluriel
    }

    @Test
    void quantite_surPotionFonctionne() {
        off.rentrer("6 larmes de brume funèbre");
        off.rentrer("3 gouttes de sang de citrouille");
        off.preparer("3 fioles de glaires purulentes");
        assertEquals(3, off.quantite("fioles de glaires purulentes"));
    }
}
