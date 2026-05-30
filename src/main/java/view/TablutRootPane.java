package view;

import boardifier.view.RootPane;
import control.algos.RecurBoard;
import javafx.scene.control.Label;
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

        Rectangle frame = new Rectangle(
                Constants.WINDOW_WIDTH,
                Constants.WINDOW_HEIGHT,
                Constants.BACKGROUND_COLOR
        );

        Rectangle boardGlow = new Rectangle(
                Constants.BOARD_SIZE,
                Constants.BOARD_SIZE,
                Constants.BOARD_COLOR
        );

        boardGlow.setX(Constants.BOARD_X);
        boardGlow.setY(Constants.BOARD_Y);

        Rectangle panel = new Rectangle(
                Constants.PANEL_WIDTH,
                Constants.PANEL_HEIGHT,
                Constants.PANEL_COLOR
        );

        panel.setX(Constants.PANEL_X);
        panel.setY(Constants.PANEL_Y);

        Rectangle headerLine = new Rectangle(
                Constants.CONTENT_WIDTH,
                2,
                Color.web(Constants.LINE_HEX)
        );

        headerLine.setX(Constants.CONTENT_X);
        headerLine.setY(Constants.HEADER_LINE_Y);

        Text title = new Text("TABLUT");
        title.setFont(Constants.TITLE_FONT);
        title.setFill(Color.web(Constants.TITLE_HEX));
        title.setX(Constants.CONTENT_X);
        title.setY(Constants.TITLE_Y);


        Text subtitle = new Text(
                "A king, eight defenders, sixteen attackers."
        );

        subtitle.setFont(Constants.SUBTITLE_FONT);
        subtitle.setFill(Color.web(Constants.SUBTITLE_HEX));
        subtitle.setX(Constants.CONTENT_X);
        subtitle.setY(Constants.SUBTITLE_Y);

        Label body1 = createBodyLabel(
                "New Game opens the board and the right-hand panel.",
                Constants.SECTION_START_Y
        );

        Label body2 = createBodyLabel(
                "The king escapes by reaching a legal exit square.",
                Constants.SECTION_START_Y + Constants.SECTION_SPACING
        );

        Label body3 = createBodyLabel(
                "Raichi means one escape lane. Tuichi means two or more.",
                Constants.SECTION_START_Y + Constants.SECTION_SPACING * 2
        );

        Label body4 = createBodyLabel(
                "The material line counts remaining defenders and attackers.",
                Constants.SECTION_START_Y + Constants.SECTION_SPACING * 3
        );

        group.getChildren().clear();

        group.getChildren().addAll(
                frame,
                boardGlow,
                panel,
                headerLine,
                title,
                subtitle,
                body1,
                body2,
                body3,
                body4
        );
    }

    private Label createBodyLabel(String text, double y) {

        Label label = new Label(text);

        label.setWrapText(true);
        label.setMaxWidth(Constants.CONTENT_WIDTH);
        label.setPrefWidth(Constants.CONTENT_WIDTH);

        label.setLayoutX(Constants.CONTENT_X);
        label.setLayoutY(y);

        label.setFont(Constants.BODY_FONT);

        label.setTextFill(Color.web(Constants.BODY_HEX));

        return label;
    }
}
