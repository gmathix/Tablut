package view;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

public final class Constants {

    private Constants() {}

    // =========================================================
    // WINDOW
    // =========================================================

    public static final double WINDOW_WIDTH = 1180;
    public static final double WINDOW_HEIGHT = 820;

    // =========================================================
    // LAYOUT RATIOS
    // =========================================================

    public static final double BOARD_RATIO = 0.61;
    public static final double PANEL_RATIO = 1.0 - BOARD_RATIO;

    // =========================================================
    // BOARD
    // =========================================================

    public static final double BOARD_SIZE = WINDOW_WIDTH * BOARD_RATIO;

    public static final double BOARD_X = 22;
    public static final double BOARD_Y = 58;

    // =========================================================
    // RIGHT PANEL
    // =========================================================

    public static final double PANEL_WIDTH = 400;
    public static final double PANEL_HEIGHT = 760;

    public static final double PANEL_X = 760;
    public static final double PANEL_Y = 40;

    public static final double PANEL_PADDING = 30;

    public static final double CONTENT_X = PANEL_X + PANEL_PADDING;

    public static final double CONTENT_WIDTH =
            PANEL_WIDTH - PANEL_PADDING * 2;

    // =========================================================
    // TYPOGRAPHY
    // =========================================================

    public static final String FONT_FAMILY = "System";

    public static final Font TITLE_FONT =
            Font.font(FONT_FAMILY, FontWeight.BOLD, 42);

    public static final Font SUBTITLE_FONT =
            Font.font(FONT_FAMILY, FontWeight.NORMAL, 18);

    public static final Font BODY_FONT =
            Font.font(FONT_FAMILY, FontWeight.NORMAL, 16);

    public static final Font SMALL_FONT =
            Font.font(FONT_FAMILY, FontWeight.NORMAL, 14);

    public static final Font PLAYER_FONT =
            Font.font(FONT_FAMILY, FontPosture.ITALIC, 20);

    public static final Font PLAYER_ACTIVE_FONT =
            Font.font(FONT_FAMILY,
                    FontWeight.BOLD,
                    FontPosture.ITALIC,
                    27);

    // =========================================================
    // COLORS
    // =========================================================

    public static final String BACKGROUND_HEX = "#182018";
    public static final String BOARD_HEX = "#2a2116";
    public static final String PANEL_HEX = "#212d23";
    public static final String LINE_HEX = "#9a7a45";

    public static final String TITLE_HEX = "#f5e7b8";
    public static final String SUBTITLE_HEX = "#d9cfaf";
    public static final String BODY_HEX = "#e1e8dc";
    public static final String SECONDARY_TEXT_HEX = "#c8d2c3";

    public static final Color BACKGROUND_COLOR =
            Color.web(BACKGROUND_HEX);

    public static final Color BOARD_COLOR =
            Color.web(BOARD_HEX);

    public static final Color PANEL_COLOR =
            Color.web(PANEL_HEX);

    // =========================================================
    // SPACING
    // =========================================================

    public static final double TITLE_Y = 110;
    public static final double SUBTITLE_Y = 145;

    public static final double HEADER_LINE_Y = 165;

    public static final double SECTION_START_Y = 220;
    public static final double SECTION_SPACING = 85;

    public static final double HELP_Y = 300;
    public static final double LEGEND_Y = 340;
    public static final double MATERIAL_Y = 450;
    public static final double THREAT_Y = 540;

    // =========================================================
    // BOARD / PAWNS
    // =========================================================

    public static final double BOARD_RENDER_SIZE = 600;

    public static final double PAWN_SIZE = 25;
}