//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        Officine o = new Officine();

        o.rentrer("5 larmes de brume funèbre");
        o.rentrer("2 gouttes de sang de citrouille");

        System.out.println("Avant :");
        System.out.println("larmes = " + o.quantite("larme de brume funèbre"));
        System.out.println("sang = " + o.quantite("goutte de sang de citrouille"));

        int faites = o.preparer("2 fioles de glaires purulentes");
        System.out.println("Préparé : " + faites);

        System.out.println("Après :");
        System.out.println("larmes = " + o.quantite("larme de brume funèbre"));
        System.out.println("sang = " + o.quantite("goutte de sang de citrouille"));
        System.out.println("fioles = " + o.quantite("fiole de glaires purulentes"));
    }

}