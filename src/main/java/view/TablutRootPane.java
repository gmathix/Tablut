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
        Rectangle frame = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT, Color.web("#" + Constants.BACKGROUND_COLOR));

        Rectangle moscovitePlayerPanel = new Rectangle(Constants.PLAYER_PANEL_WIDTH, Constants.PLAYER_PANEL_HEIGHT, Color.web("#" + Constants.MOSCOVITE_PANEL_COLOR));
        moscovitePlayerPanel.setX(Constants.MOSCOVITE_PANEL_X);
        moscovitePlayerPanel.setY(Constants.MOSCOVITE_PANEL_Y);

        Rectangle swedishPlayerPanel = new Rectangle(Constants.PLAYER_PANEL_WIDTH, Constants.PLAYER_PANEL_HEIGHT, Color.web("#" + Constants.SWEDISH_PANEL_COLOR));
        swedishPlayerPanel.setX(Constants.SWEDISH_PANEL_X);
        swedishPlayerPanel.setY(Constants.SWEDISH_PANEL_Y);

        Rectangle boardGlow = new Rectangle(Constants.BOARD_WIDTH, Constants.BOARD_HEIGHT, Color.web("#" + Constants.BOARD_COLOR));
        boardGlow.setX(Constants.BOARD_X);
        boardGlow.setY(Constants.BOARD_Y);

        Rectangle panel = new Rectangle(Constants.SIDE_PANEL_WIDTH, Constants.SIDE_PANEL_HEIGHT, Color.web("#" + Constants.SIDE_PANEL_COLOR));
        panel.setX(Constants.SIDE_PANEL_X);
        panel.setY(Constants.SIDE_PANEL_Y);

        Rectangle headerLine = new Rectangle(Constants.HEADER_LINE_WIDTH, Constants.HEADER_LINE_HEIGHT, Color.web("#" + Constants.HEADER_LINE_COLOR));
        headerLine.setX(Constants.HEADER_LINE_X);
        headerLine.setY(Constants.HEADER_LINE_Y);

        Text moscovitePlayerText = new Text("Player 2");
        moscovitePlayerText.setFont(Constants.PLAYER_FONT);
        moscovitePlayerText.setFill(Color.web( "#" + Constants.PLAYER_TEXT_COLOR));
        moscovitePlayerText.setX(Constants.MOSCOVITE_PANEL_X + 5);
        moscovitePlayerText.setY(Constants.MOSCOVITE_PANEL_Y + (Constants.PLAYER_PANEL_HEIGHT * 0.1));

        Text swedishPlayerText = new Text("Player 1");
        swedishPlayerText.setFont(Constants.PLAYER_FONT);
        swedishPlayerText.setFill(Color.web( "#" + Constants.PLAYER_TEXT_COLOR));
        swedishPlayerText.setX(Constants.SWEDISH_PANEL_X + 5);
        swedishPlayerText.setY(Constants.SWEDISH_PANEL_Y + (Constants.PLAYER_PANEL_HEIGHT * 0.1));

        Text title = new Text("TABLUT");
        title.setFont(Constants.TITLE_FONT);
        title.setFill(Color.web("#" + Constants.TITLE_COLOR));
        title.setX(790);
        title.setY(110);

        Text subtitle = new Text("A clean board, not an sub 10 looking one.");
        subtitle.setFont(Constants.SUBTITLE_FONT);
        subtitle.setFill(Color.web("#" + Constants.SUBTITLE_COLOR));
        subtitle.setX(790);
        subtitle.setY(145);

        Text body1 = new Text("Use the menu to start a game.");
        body1.setFont(Constants.BODY_TEXT_FONT);
        body1.setFill(Color.web("#" + Constants.BODY_TEXT_COLOR));
        body1.setX(790);
        body1.setY(220);

        Text body2 = new Text("The board panel appears once the match begins.");
        body2.setFont(Constants.BODY_TEXT_FONT);
        body2.setFill(Color.web("#" + Constants.BODY_TEXT_COLOR));
        body2.setX(790);
        body2.setY(250);


        group.getChildren().clear();
        group.getChildren().addAll(frame, moscovitePlayerPanel, swedishPlayerPanel,
                boardGlow, panel, headerLine, title, subtitle, body1, body2);
    }
}
