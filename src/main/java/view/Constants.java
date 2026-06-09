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
    // SCREEN
    // =========================================================

    private static final Rectangle2D SCREEN =
            Screen.getPrimary().getVisualBounds();

    // =========================================================
    // WINDOW
    // =========================================================

    public static final double WINDOW_WIDTH =
            SCREEN.getWidth();

    public static final double WINDOW_HEIGHT =
            SCREEN.getHeight();

    // =========================================================
    // GLOBAL SPACING
    // =========================================================

    public static final double MARGIN = WINDOW_WIDTH * 0.02;

    // =========================================================
    // BOARD
    // =========================================================

    /*
     * Le plateau prend environ 75% de la hauteur
     * pour rester compatible avec les petits écrans.
     */
    public static final double BOARD_RENDER_SIZE =
            WINDOW_HEIGHT * 0.78;

    /*
     * Taille logique du plateau.
     */
    public static final double BOARD_SIZE =
            BOARD_RENDER_SIZE;

    /*
     * Position du plateau.
     */
    public static final double BOARD_X =
            MARGIN;

    public static final double BOARD_Y =
            (WINDOW_HEIGHT - BOARD_RENDER_SIZE) / 2;

    // =========================================================
    // RIGHT PANEL
    // =========================================================

    public static final double PANEL_WIDTH =
            WINDOW_WIDTH * 0.24;

    public static final double PANEL_HEIGHT =
            WINDOW_HEIGHT * 0.85;

    public static final double PANEL_X =
            BOARD_X + BOARD_RENDER_SIZE + MARGIN;

    public static final double PANEL_Y =
            (WINDOW_HEIGHT - PANEL_HEIGHT) / 2;

    public static final double PANEL_PADDING =
            WINDOW_WIDTH * 0.015;

    // =========================================================
    // CONTENT
    // =========================================================

    public static final double CONTENT_X =
            PANEL_X + PANEL_PADDING;

    public static final double CONTENT_WIDTH =
            PANEL_WIDTH - PANEL_PADDING * 2;

    // =========================================================
    // TYPOGRAPHY
    // =========================================================

    public static final String FONT_FAMILY = "System";

    /*
     * Les tailles de texte s'adaptent aussi à la résolution.
     */
    public static final Font TITLE_FONT =
            Font.font(
                    FONT_FAMILY,
                    FontWeight.BOLD,
                    WINDOW_WIDTH * 0.022
            );

    public static final Font SUBTITLE_FONT =
            Font.font(
                    FONT_FAMILY,
                    FontWeight.NORMAL,
                    WINDOW_WIDTH * 0.010
            );

    public static final Font BODY_FONT =
            Font.font(
                    FONT_FAMILY,
                    FontWeight.NORMAL,
                    WINDOW_WIDTH * 0.009
            );

    public static final Font SMALL_FONT =
            Font.font(
                    FONT_FAMILY,
                    FontWeight.NORMAL,
                    WINDOW_WIDTH * 0.008
            );

    public static final Font PLAYER_FONT =
            Font.font(
                    FONT_FAMILY,
                    FontPosture.ITALIC,
                    WINDOW_WIDTH * 0.011
            );

    public static final Font PLAYER_ACTIVE_FONT =
            Font.font(
                    FONT_FAMILY,
                    FontWeight.BOLD,
                    FontPosture.ITALIC,
                    WINDOW_WIDTH * 0.015
            );

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
    // SPACING / POSITIONS
    // =========================================================

    public static final double TITLE_Y =
            PANEL_Y + 60;

    public static final double SUBTITLE_Y =
            TITLE_Y + 40;

    public static final double HEADER_LINE_Y =
            SUBTITLE_Y + 30;

    public static final double SECTION_START_Y =
            HEADER_LINE_Y + 60;

    public static final double SECTION_SPACING =
            WINDOW_HEIGHT * 0.11;

    public static final double HELP_Y =
            SECTION_START_Y + SECTION_SPACING;

    public static final double LEGEND_Y =
            HELP_Y + SECTION_SPACING;

    public static final double MATERIAL_Y =
            LEGEND_Y + SECTION_SPACING;

    public static final double THREAT_Y =
            MATERIAL_Y + SECTION_SPACING;

    // =========================================================
    // PAWNS
    // =========================================================

    public static final double PAWN_SIZE =
            BOARD_RENDER_SIZE / 24.0;
}