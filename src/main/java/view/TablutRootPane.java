package view;

import boardifier.view.RootPane;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import model.Pawn;
import model.RuleSets;
import model.TablutBoard;

import java.util.ArrayList;
import java.util.List;


public class TablutRootPane extends RootPane {

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
                "The king escapes by reaching a legal exit square : an edge square that is not brown" +
                        "(depends on current ruleset)",
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
                panel,
                headerLine,
                title,
                subtitle,
                body1,
                body2,
                body3,
                body4
        );

        createStaticBoard();
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


    /**
     * renders a static and unclickable board on the intro menu
     */
    private void createStaticBoard() {
        Rectangle board = new Rectangle(
                Constants.BOARD_SIZE,
                Constants.BOARD_SIZE,
                Constants.BOARD_COLOR
        );
        board.setX(Constants.BOARD_X);
        board.setY(Constants.BOARD_Y);


        int startX = (int) (Constants.BOARD_X + 4 + (Constants.BOARD_SIZE - Constants.BOARD_RENDER_SIZE) / 2);
        int startY = (int) (Constants.BOARD_Y + 17 + (Constants.BOARD_SIZE - Constants.BOARD_RENDER_SIZE) / 2);
        int squareSize = (int) (Constants.BOARD_RENDER_SIZE / 9);
        List<Rectangle> squares = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                Rectangle square = new Rectangle(squareSize, squareSize);
                square.setX(startX + j*squareSize);
                square.setY(startY + i*squareSize);
                square.setFill(((i+j)%2 == 0) ? TablutBoardLook.LIGHT_SQUARE : TablutBoardLook.DARK_SQUARE);

                if (RuleSets.isCampSquare(i*9+j)) {
                    square.setFill(TablutBoardLook.SPECIAL_SQUARE);
                }
                if (i*9 + j == 40) {
                    square.setFill(Color.web("aa5528"));
                }

                square.setStrokeWidth(2);
                square.setStrokeMiterLimit(10);
                square.setStrokeType(StrokeType.CENTERED);
                square.setStroke(TablutBoardLook.BORDER_COLOR);

                squares.add(square);
            }
        }


        List<Circle> circles = new ArrayList<>();
        List<Circle> innerCircles = new ArrayList<>();
        List<Circle> highlights = new ArrayList<>();
        int pawnRadius = (int) Constants.PAWN_SIZE;
        int pawnCenterOffset = squareSize / 2;

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                int type = TablutBoard.startingBoard[i][j];
                if (type == 0) continue;


                Circle circle = new Circle(pawnRadius);
                circle.setCenterX(startX + j*squareSize + pawnCenterOffset);
                circle.setCenterY(startY + i*squareSize + pawnCenterOffset);
                circle.setFill(PawnLook.getGradient(type));
                circle.setStroke(PawnLook.getEdgeColor(type));
                circle.setStrokeWidth(1.5);

                Circle innerCircle = new Circle(pawnRadius * 0.58);
                innerCircle.setCenterX(startX + j*squareSize + pawnCenterOffset);
                innerCircle.setCenterY(startY + i*squareSize + pawnCenterOffset);
                if (type == Pawn.PAWN_KING) {
                    innerCircle.setFill(Color.web("#2a2417"));
                    innerCircle.setStroke(Color.web("#f7dd77"));
                    innerCircle.setStrokeWidth(2);
                } else if (type == Pawn.PAWN_MOSCOVITE) {
                    innerCircle.setFill(Color.web("#f9e8a7", 0.22));
                    innerCircle.setStroke(Color.web("#fff8df", 0.45));
                    innerCircle.setStrokeWidth(1);
                } else {
                    innerCircle.setFill(Color.web("#e1f3e6", 0.18));
                    innerCircle.setStroke(Color.web("#f3fff5", 0.42));
                    innerCircle.setStrokeWidth(1);
                }

                Circle highlight = new Circle(pawnRadius * 0.24);
                highlight.setCenterX(startX + j*squareSize + pawnCenterOffset);
                highlight.setCenterY(startY + i*squareSize + pawnCenterOffset);
                highlight.setTranslateX(-pawnRadius * 0.28);
                highlight.setTranslateY(-pawnRadius * 0.34);
                highlight.setFill(Color.web("#ffffff", 0.28));
                highlight.setStroke(Color.TRANSPARENT);

                circles.add(circle);
                innerCircles.add(innerCircle);
                highlights.add(highlight);
            }
        }


        List<Text> coords = new ArrayList<>();
        char[] row = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};
        char[] col = new char[]{'1', '2', '3', '4', '5', '6', '7', '8', '9'};

        double coordFontSize = Constants.SMALL_FONT.getSize();
        double coordOffsetH  = squareSize * 0.37;   // ~25 / 66 of a square
        double coordAboveY   = squareSize * 0.42;   // ~28 / 66
        double coordLeftX    = squareSize * 0.60;   // ~40 / 66
        double coordBelowY   = squareSize * 0.64;   // ~42 / 66

        for (int i = 0; i < 9; i++) {
            Text text = new Text(Character.toString(row[i]));
            text.setFont(new Font(coordFontSize));
            text.setX(startX + i * squareSize + coordOffsetH);
            text.setY(startY - coordAboveY);
            coords.add(text);
        }
        for (int i = 0; i < 9; i++) {
            Text text = new Text(Character.toString(col[i]));
            text.setFont(new Font(coordFontSize));
            text.setX(startX - coordLeftX);
            text.setY(startY + i * squareSize + coordBelowY);
            coords.add(text);
        }


        group.getChildren().add(board);
        group.getChildren().addAll(squares);
        group.getChildren().addAll(circles);
        group.getChildren().addAll(innerCircles);
        group.getChildren().addAll(highlights);
        group.getChildren().addAll(coords);
    }
}
