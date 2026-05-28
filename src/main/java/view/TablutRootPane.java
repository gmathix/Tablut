package view;

import boardifier.view.RootPane;
import control.algos.RecurBoard;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class TablutRootPane extends RootPane {
    private static final int WINDOW_HEIGHT = 820;
    private static final int WINDOW_WIDTH  = 1180;

    public TablutRootPane() {
        super();
    }

    @Override
    public void createDefaultGroup() {
        Rectangle frame = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT, Color.web("#1d251d"));
        Rectangle boardGlow = new Rectangle(720, 720, Color.web("#2a2116"));
        boardGlow.setX(22);
        boardGlow.setY(58);

        Rectangle panel = new Rectangle(380, 760, Color.web("#233026"));
        panel.setX(760);
        panel.setY(30);

        Rectangle headerLine = new Rectangle(340, 2, Color.web("#8b6d3f"));
        headerLine.setX(780);
        headerLine.setY(150);

        Text title = new Text("TABLUT");
        title.setFont(Font.font("System", FontWeight.BOLD, 42));
        title.setFill(Color.web("#f5e7b8"));
        title.setX(790);
        title.setY(110);

        Text subtitle = new Text("A clean board, not an sub 10 looking one.");
        subtitle.setFont(Font.font("System", 18));
        subtitle.setFill(Color.web("#d9cfaf"));
        subtitle.setX(790);
        subtitle.setY(145);

        Text body1 = new Text("Use the menu to start a game.");
        body1.setFont(Font.font("System", 16));
        body1.setFill(Color.web("#e1e8dc"));
        body1.setX(790);
        body1.setY(220);

        Text body2 = new Text("The board panel appears once the match begins.");
        body2.setFont(Font.font("System", 16));
        body2.setFill(Color.web("#c8d2c3"));
        body2.setX(790);
        body2.setY(250);


        group.getChildren().clear();
        group.getChildren().addAll(frame, boardGlow, panel, headerLine, title, subtitle, body1, body2);
    }
}
