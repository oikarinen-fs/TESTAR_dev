package nl.ou.testar.jfx;

import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import nl.ou.testar.jfx.core.NavigationController;
import nl.ou.testar.jfx.core.NavigationDelegate;
import nl.ou.testar.jfx.core.ViewController;
import nl.ou.testar.jfx.dashboard.DashboardController;
import nl.ou.testar.jfx.settings.SettingsController;

import javafx.scene.control.*;
import org.fruit.monkey.Settings;

import java.io.File;

public class MainController extends ViewController {

    enum Mode {
        HOME, SETTINGS
    }

    private Mode mode;
    private String settingsPath;

    private DashboardController dashboardController;
    private SettingsController settingsController;

    public DashboardController getDashboardController() {
        return dashboardController;
    }

    public MainController(Settings settings, String settingsPath) {
        super("Testar", "jfx/main.fxml", settings);
        this.settingsPath = settingsPath;
        dashboardController = new DashboardController(settings, settingsPath);
        settingsController = new SettingsController(settings, settingsPath);
    }

    private void setupMode(Parent view, Mode mode) {
        if (mode != this.mode) {
//            final Label titleLabel = (Label) view.lookup("#titleLabel");
//            final Button btnBack = (Button) view.lookup("#btnBack");

            final BorderPane contentPane = (BorderPane) view.lookup("#contentPane");
            ViewController targetController;
            switch (mode) {
                case SETTINGS:
                    targetController = settingsController;
                    break;
                default: //HOME
                    targetController = dashboardController;
                    break;
            }
            final NavigationController navigationController = new NavigationController(targetController);
            navigationController.startWithDelegate(new NavigationDelegate() {
                @Override
                public void onViewControllerActivated(ViewController viewController, Parent view) {
//                    titleLabel.setText(viewController.getTitle());
                    contentPane.setCenter(view);
//                    btnBack.setVisible(navigationController.isBackAvailable());
//                    contentPane.getChildren().removeAll();
//                    contentPane.getChildren().add(view);
                }
            });

//            btnBack.setOnAction(event -> {
//                navigationController.navigateBack();
//            });

            this.mode = mode;
        }
    }

    @Override
    public void viewDidLoad(Parent view) {
        Button btnHome = (Button) view.lookup("#btnHome");
        Button btnSettings = (Button) view.lookup("#btnSettings");

        btnHome.setOnAction(event -> {
            setupMode(view, Mode.HOME);
        });

        btnSettings.setOnAction(event -> {
            setupMode(view, Mode.SETTINGS);
        });

        setupMode(view, Mode.HOME);
    }
}