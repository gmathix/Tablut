package boardifier.view;

import boardifier.model.GameElement;
import boardifier.model.TextElement;
import javafx.geometry.Bounds;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class TextLook extends ElementLook {

    protected double wrappingWidth;
    protected Text text;
    protected int fontSize;
    protected String color; // must be prodive as a string containing an hex value like "0x123456"

    public TextLook(int fontSize, String color, GameElement element) {
        this(fontSize, color, "System", FontWeight.NORMAL, element);
    }

    public TextLook(int fontSize, String color, String fontFamily, FontWeight fontWeight, GameElement element) {
        super(element);
        this.fontSize = fontSize;
        this.color = color;
        TextElement te = (TextElement) element;
        text = new Text(te.getText());
        text.setFont(Font.font(fontFamily, fontWeight, fontSize));
        text.setFill(Color.valueOf(color));
        addShape(text);
    }

    public void setWrappingWidth(double width) {
        wrappingWidth = width;
        text.setWrappingWidth(width);
    }

    public void setFont(Font font) {
        text.setFont(font);
    }

    public void setColor(String color) {
        this.color = color;
        text.setFill(Color.valueOf(color));
    }

    public double getTextWidth() {
        Bounds b = text.getLayoutBounds();
        return b.getWidth();
    }

    public double getTextHeight() {
        Bounds b = text.getLayoutBounds();
        return b.getHeight();
    }

    public void render() {
        TextElement te = (TextElement) getElement();
        text.setText(te.getText());
    }

    public void onFaceChange() {
        render();
    }
}
