package view;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

/**
 * Size, position, color and font constants for view elements
 */
public class Constants {
    //==== SIZE CONSTANTS ====//

    // change these two and everything will change
    public static final int WINDOW_WIDTH            = 1180;
    public static final int WINDOW_HEIGHT           = 820;

    public static final int BOARD_WIDTH             = (int) (WINDOW_WIDTH * 0.6);
    public static final int BOARD_HEIGHT            = BOARD_WIDTH; // always a square board for tablut

    public static final int SIDE_PANEL_WIDTH        = WINDOW_WIDTH - BOARD_WIDTH;
    public static final int SIDE_PANEL_HEIGHT       = BOARD_HEIGHT;

    public static final int PLAYER_PANEL_WIDTH      = BOARD_WIDTH;
    public static final int PLAYER_PANEL_HEIGHT     = (int) (BOARD_HEIGHT * 0.057);

    public static final int HEADER_LINE_WIDTH       = (int) (SIDE_PANEL_WIDTH * 0.9);
    public static final int HEADER_LINE_HEIGHT      = 2;





    //==== POSITION CONSTANTS ====//

    public static final int BOARD_X             = (WINDOW_WIDTH - (BOARD_WIDTH + SIDE_PANEL_WIDTH)) / 2;
    public static final int BOARD_Y             = (WINDOW_HEIGHT - BOARD_HEIGHT) / 2;

    public static final int SIDE_PANEL_X        = BOARD_X + BOARD_WIDTH;
    public static final int SIDE_PANEL_Y        = BOARD_Y;

    public static final int MOSCOVITE_PANEL_X   = BOARD_X;
    public static final int MOSCOVITE_PANEL_Y   = (BOARD_Y - PLAYER_PANEL_HEIGHT) / 2;

    public static final int SWEDISH_PANEL_X     = BOARD_X;
    public static final int SWEDISH_PANEL_Y     = BOARD_Y + BOARD_HEIGHT + (BOARD_Y - PLAYER_PANEL_HEIGHT) / 2;

    public static final int HEADER_LINE_X       = SIDE_PANEL_X + (SIDE_PANEL_WIDTH - HEADER_LINE_WIDTH) / 2;
    public static final int HEADER_LINE_Y       = SIDE_PANEL_Y + (int) (SIDE_PANEL_HEIGHT * 0.157);

    public static final int TITLE_X             = SIDE_PANEL_X + SIDE_PANEL_WIDTH / 3;
    public static final int TITLE_Y             = SIDE_PANEL_Y + (int) (SIDE_PANEL_Y * 0.05);

    public static final int SUBTITLE_X          = SIDE_PANEL_X;
    public static final int SUBTITLE_Y          = TITLE_Y * 2;





    //==== COLOR CONSTANTS ====//

    public static final String BACKGROUND_COLOR         = "1d251d";
    public static final String MOSCOVITE_PANEL_COLOR    = "3a2116";
    public static final String SWEDISH_PANEL_COLOR      = "3a2116";
    public static final String BOARD_COLOR              = "2a2116";
    public static final String SIDE_PANEL_COLOR         = "233026";
    public static final String HEADER_LINE_COLOR        = "8b6d3f";
    public static final String TITLE_COLOR              = "f5e7b8";
    public static final String SUBTITLE_COLOR           = "d9cfaf";
    public static final String BODY_TEXT_COLOR          = "e1e8dc";
    public static final String PLAYER_TEXT_COLOR        = "d5e7b8";
    public static final String PLAYER_PLAYING_TEXT_COLOR        = "f5e7b8";




    //==== FONT CONSTANTS ====//

    public static final Font TITLE_FONT          = Font.font("System", FontWeight.BOLD, 42);
    public static final Font SUBTITLE_FONT       = Font.font("System", 18);
    public static final Font BODY_TEXT_FONT      = Font.font("System", 16);
    public static final Font PLAYER_FONT         = Font.font("System", FontPosture.ITALIC, 20);
    public static final Font PLAYER_PLAYING_FONT = Font.font("System", FontWeight.BOLD, FontPosture.ITALIC, 27);
}
