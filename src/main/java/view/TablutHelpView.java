package view;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class TablutHelpView {

    public static void show(Stage owner) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Tablut – Rules & Help");

        VBox content = new VBox(14);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #1a2318;");


        content.getChildren().add(makeTitle("TABLUT – RULES"));


        content.getChildren().add(makeHeading("Classic Rules"));
        content.getChildren().add(makeBody(
                "Tablut is an asymmetric Viking board game played on a 9×9 grid.\n" +
                        "One side plays the Swedes (defenders, green), the other the Muscovites (attackers, gold).\n\n" +
                        "• The Swedes win if the King reaches any edge square.\n" +
                        "• The Muscovites win if they encircle the King so he has no escape.\n\n" +
                        "Pieces move like rooks in chess: any number of squares horizontally or vertically, " +
                        "but cannot jump over other pieces.\n\n" +
                        "A piece is captured when it is sandwiched between two enemy pieces on the same row or column."
        ));


        content.getChildren().add(makeHeading("The King"));
        content.getChildren().add(makeBody(
                "The King starts at the centre (E5, the throne).\n" +
                        "To capture the King, he must be surrounded on all four sides by attackers " +
                        "(or by attackers + the throne if he is adjacent to it).\n" +
                        "The King can pass through the throne square but no other piece can stop there."
        ));


        content.getChildren().add(makeHeading("Special Squares"));
        content.getChildren().add(makeBody(
                "• Throne (E5, centre) : only the King may occupy it.\n" +
                        "• Corner squares : the King's escape goals. No other piece may enter them.\n" +
                        "• Restricted squares (corners + throne) act as hostile pieces for capture purposes."
        ));


        content.getChildren().add(makeHeading("Rulesets"));
        content.getChildren().add(makeBody(
                "Normal rules are always active. The following optional rulesets can be toggled before a game:\n\n" +

                        "• Ashton rules (official, tournament) :\n" +
                        "The standard ruleset used in official Tablut tournaments, based on the work of historian " +
                        "Sten Helmfrid and later refined by Tim Ashton. These rules clarify all ambiguities of the " +
                        "original 18th-century rules written by Linnaeus. The throne is hostile to all pieces at all times, " +
                        "meaning it counts as an enemy piece for capture purposes even when empty. " +
                        "The four corner squares are also permanently hostile. " +
                        "The King must reach a corner to win (this overrides the edge-escape rule). " +
                        "Attackers may not pass through the throne, and the King requires all four sides to be " +
                        "surrounded for capture, even when adjacent to the throne.\n\n" +

                        " King cannot land on starting Moscovite squares :\n" +
                        "At the start of the game, the 16 Moscovite (attacker) pieces are placed on specific squares " +
                        "around the edges of the board. With this rule active, the King is permanently forbidden from " +
                        "moving onto any of those squares, even if they are empty. " +
                        "This restricts the King's escape routes and forces the defenders to find alternative paths, " +
                        "making the game significantly harder for the Swedish side.\n\n" +

                        " King cannot move more than 4 squares :\n" +
                        "Normally, pieces in Tablut can move any number of free squares in a straight line, like a rook in chess. " +
                        "This rule caps the King's movement at a maximum of 4 squares per turn. " +
                        "It prevents the King from making long diagonal escapes in a single move and gives the " +
                        "Moscovite side more time to react and block escape lanes. " +
                        "Regular defender and attacker pieces are not affected by this restriction.\n\n" +

                        " King must reach a corner to win :\n" +
                        "In the base rules of this implementation, the King wins by reaching any square on the edge of the board. " +
                        "With this rule enabled, only the four corner squares (A1, I1, A9, I9) count as valid escape points. " +
                        "This makes winning considerably harder for the Swedish side, as the corners are the most " +
                        "heavily guarded squares and require a coordinated multi-piece strategy to reach safely. " +
                        "This is also the rule used in the official Ashton tournament format."
        ));


        content.getChildren().add(makeHeading("Game Modes"));
        content.getChildren().add(makeBody(
                "Human vs Human\n" +
                        " Human vs Bot (choose side, algorithm and difficulty level)\n" +
                        " Bot vs Bot\n\n" +
                        "Available algorithms : Negamax, Monte-Carlo, Nega-Monte-Carlo.\n" +
                        "Difficulty levels go from level 1 (easy) to level 10 (hard)."
        ));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1a2318; -fx-background-color: #1a2318;");

        Scene scene = new Scene(scroll, 640, 520);
        dialog.setScene(scene);
        dialog.show();
    }



    private static Text makeTitle(String text) {
        Text t = new Text(text);
        t.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        t.setFill(Color.web("#c8a76a"));
        return t;
    }

    private static Text makeHeading(String text) {
        Text t = new Text(text);
        t.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        t.setFill(Color.web("#c8a76a"));
        return t;
    }

    private static Text makeBody(String text) {
        Text t = new Text(text);
        t.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        t.setFill(Color.web("#dde8d8"));
        t.setWrappingWidth(580);
        return t;
    }
}

