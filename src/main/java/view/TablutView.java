package view;

import boardifier.model.Model;
import boardifier.view.RootPane;
import boardifier.view.View;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;


public class TablutView extends View {
    private MenuItem menuStart;
    private MenuItem menuIntro;
    private MenuItem menuQuit;
    private MenuItem menuRules;


    private MenuItem menuImportGame;
    private MenuItem menuExportGame;


    public TablutView(Model model, Stage stage, RootPane rootPane) {
        super(model, stage, rootPane);
    }

    @Override

    protected void createMenuBar() {
        menuBar = new MenuBar();

        Menu menuGame = new Menu("Game");
        menuStart = new MenuItem("New game");
        menuIntro = new MenuItem("Intro");
        menuQuit = new MenuItem("Quit");
        menuGame.getItems().add(menuStart);
        menuGame.getItems().add(menuIntro);
        menuGame.getItems().add(menuQuit);

        Menu menuFile = new Menu("File");
        menuImportGame = new MenuItem("Import Game");
        menuExportGame = new MenuItem("Export Game");
        menuFile.getItems().add(menuImportGame);
        menuFile.getItems().add(menuExportGame);

        Menu menuHelp = new Menu("Help");
        menuRules = new MenuItem("Rules");
        menuHelp.getItems().add(menuRules);

        menuBar.getMenus().add(menuFile);
        menuBar.getMenus().add(menuGame);
        menuBar.getMenus().add(menuHelp);
    }



    public MenuItem getMenuStart() {
        return menuStart;
    }

    public MenuItem getMenuIntro() {
        return menuIntro;
    }

    public MenuItem getMenuQuit() {
        return menuQuit;
    }

    public MenuItem getMenuImportGame() { return menuImportGame; }

    public MenuItem getMenuExportGame() { return menuExportGame; }

    public MenuItem getMenuRules() { return menuRules; }


}
