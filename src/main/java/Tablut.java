import boardifier.control.StageFactory;
import boardifier.model.Model;
import boardifier.view.View;
import control.TablutController;
import javafx.application.Application;
import javafx.stage.Stage;
import view.TablutRootPane;
import view.TablutView;

public class Tablut extends Application {

    private static int mode = 0;
    private static String inputFile = "";

    public static void main(String[] args) {
        if (args.length >= 1) {
            try {
                mode = Integer.parseInt(args[0]);
                if (mode < 0 || mode > 2) {
                    mode = 0;
                }
            } catch (NumberFormatException e) {
                mode = 0;
            }
        }

        if (args.length >= 2) {
            inputFile = args[1];
        }

        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Model model = new Model();

        StageFactory.registerModelAndView("tablut", "model.TablutStageModel", "view.TablutStageView");
        TablutRootPane rootPane = new TablutRootPane();
        View tablutView = new TablutView(model, stage, rootPane);
        TablutController control = new TablutController(model, tablutView, mode, inputFile);
        control.setGameMode(mode);
        control.setInputFile(inputFile);
        control.setFirstStageName("tablut");

        stage.setTitle("Tablut : the fucking viking game");
        stage.show();
    }
}
