package view;

import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;

public final class Constants {

    private Constants() {}

    // =========================================================
    // WINDOW  — capped at the available screen area
    // =========================================================

    private static final double RAW_WIDTH;
    private static final double RAW_HEIGHT;

    static {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        // Desired aspect ratio is ~1180×820 ≈ 1.44 : 1
        double maxW = screen.getWidth()  * 0.96;
        double maxH = screen.getHeight() * 0.90;

        double byWidth  = maxW;
        double byHeight = maxH * (1180.0 / 820.0);

        double chosen = Math.min(byWidth, byHeight);
        RAW_WIDTH  = chosen;
        RAW_HEIGHT = chosen * (820.0 / 1180.0);
    }

    public static final double WINDOW_WIDTH  = RAW_WIDTH;
    public static final double WINDOW_HEIGHT = RAW_HEIGHT;

    /** Scale factor relative to the original 1180-wide design. */
    private static final double S = WINDOW_WIDTH / 1180.0;

    private static double s(double original) { return original * S; }

    // =========================================================
    // LAYOUT RATIOS
    // =========================================================

    public static final double BOARD_RATIO = 0.61;
    public static final double PANEL_RATIO = 1.0 - BOARD_RATIO;

    // =========================================================
    // BOARD
    // =========================================================

    public static final double BOARD_SIZE = WINDOW_WIDTH * BOARD_RATIO;

    public static final double BOARD_X = s(22);
    public static final double BOARD_Y = s(58);

    // =========================================================
    // RIGHT PANEL
    // =========================================================

    public static final double PANEL_WIDTH  = s(400);
    public static final double PANEL_HEIGHT = s(760);

    public static final double PANEL_X = s(760);
    public static final double PANEL_Y = s(40);

    public static final double PANEL_PADDING = s(30);

    public static final double CONTENT_X = PANEL_X + PANEL_PADDING;

    public static final double CONTENT_WIDTH = PANEL_WIDTH - PANEL_PADDING * 2;

    // =========================================================
    // TYPOGRAPHY  — font sizes scale with S
    // =========================================================

    public static final String FONT_FAMILY = "System";

    public static final Font TITLE_FONT =
            Font.font(FONT_FAMILY, FontWeight.BOLD, s(42));

    public static final Font SUBTITLE_FONT =
            Font.font(FONT_FAMILY, FontWeight.NORMAL, s(18));

    public static final Font BODY_FONT =
            Font.font(FONT_FAMILY, FontWeight.NORMAL, s(16));

    public static final Font SMALL_FONT =
            Font.font(FONT_FAMILY, FontWeight.NORMAL, s(14));

    public static final Font PLAYER_FONT =
            Font.font(FONT_FAMILY, FontPosture.ITALIC, s(20));

    public static final Font PLAYER_ACTIVE_FONT =
            Font.font(FONT_FAMILY, FontWeight.BOLD, FontPosture.ITALIC, s(27));

    // =========================================================
    // COLORS
    // =========================================================

    public static final String BACKGROUND_HEX    = "#182018";
    public static final String BOARD_HEX         = "#2a2116";
    public static final String PANEL_HEX         = "#212d23";
    public static final String LINE_HEX          = "#9a7a45";

    public static final String TITLE_HEX         = "#f5e7b8";
    public static final String SUBTITLE_HEX      = "#d9cfaf";
    public static final String BODY_HEX          = "#e1e8dc";
    public static final String SECONDARY_TEXT_HEX = "#c8d2c3";

    public static final Color BACKGROUND_COLOR = Color.web(BACKGROUND_HEX);
    public static final Color BOARD_COLOR      = Color.web(BOARD_HEX);
    public static final Color PANEL_COLOR      = Color.web(PANEL_HEX);

    // =========================================================
    // SPACING  — all scaled
    // =========================================================

    public static final double TITLE_Y        = s(110);
    public static final double SUBTITLE_Y     = s(145);

    public static final double HEADER_LINE_Y  = s(165);

    public static final double SECTION_START_Y = s(220);
    public static final double SECTION_SPACING = s(85);

    public static final double HELP_Y     = s(300);
    public static final double LEGEND_Y   = s(340);
    public static final double MATERIAL_Y = s(450);
    public static final double THREAT_Y   = s(540);

    // =========================================================
    // BOARD / PAWNS  — scaled
    // =========================================================

    public static final double BOARD_RENDER_SIZE = s(600);

    public static final double PAWN_SIZE = s(25);
}
