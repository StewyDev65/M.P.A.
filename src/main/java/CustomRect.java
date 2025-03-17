public class CustomRect extends javafx.scene.shape.Rectangle {

    public CustomRect(double width, double height) {
        super(width, height);
        setArcWidth(2);
        setArcHeight(2);
    }
}