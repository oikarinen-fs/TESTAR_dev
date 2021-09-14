package nl.ou.testar.jfx.settings.child;

import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import nl.ou.testar.jfx.utils.DisplayModeWrapper;
import nl.ou.testar.jfx.utils.GeneralSettings;
import org.fruit.monkey.ConfigTags;
import org.fruit.monkey.Settings;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class GeneralSettingsController extends ChildSettingsController {

    private DisplayMode availableDisplayModes[];
    private int displayModeSelectedIndex;

    private ComboBox sutComboBox;
    private ComboBox<DisplayModeWrapper> resolutionComboBox;
    private TextField webDriverPathField;
    private TextField locationInputField;
    private Spinner numSequencesSpinner;
    private Spinner numActionsSpinner;
    private CheckBox alwaysCompileCheckBox;

    private SpinnerValueFactory<Integer> numActionsValueFactory;
    private SpinnerValueFactory<Integer> numSequencesValueFactory;

    private GeneralSettings generalSettings;

    public GeneralSettingsController(Settings settings, String settingsPath) {
        super("General settings", settings, settingsPath);
    }

    @Override
    public void viewDidLoad(Parent view) {
        super.viewDidLoad(view);
        try {
            putSection(view, "General settings", "jfx/settings_general.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }

        GraphicsDevice dev = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();
        availableDisplayModes = dev.getDisplayModes();


        sutComboBox = (ComboBox) view.lookup("#sutConnectorSelection");
        sutComboBox.getItems().addAll(
                Settings.SUT_CONNECTOR_CMDLINE,
                Settings.SUT_CONNECTOR_PROCESS_NAME,
                Settings.SUT_CONNECTOR_WINDOW_TITLE,
                Settings.SUT_CONNECTOR_WEBDRIVER
        );

        resolutionComboBox = (ComboBox<DisplayModeWrapper>) view.lookup("#resolutionSelection");
//        SingleSelectionModel<DisplayMode> resolutionSelectionModel = new SingleSelectionModel<DisplayMode>() {
//            @Override
//            protected DisplayMode getModelItem(int index) {
//                return availableDisplayModes[index];
//            }
//
//            @Override
//            protected int getItemCount() {
//                return availableDisplayModes.length;
//            }
//        };
//        resolutionComboBox.setSelectionModel(resolutionSelectionModel);
        resolutionComboBox.setItems(FXCollections.observableArrayList(
                Arrays.stream(availableDisplayModes).map(mode -> new DisplayModeWrapper(mode, true))
                        .collect(Collectors.toList())
        ));
        DisplayMode currentDisplayMode = dev.getDisplayMode();
        System.out.println(String.format("Current dislpay mode: %dx%d+%d+%d", currentDisplayMode.getWidth(), currentDisplayMode.getHeight(), currentDisplayMode.getBitDepth(), currentDisplayMode.getRefreshRate()));
        int index = 0;
        displayModeSelectedIndex = 0;
//        for (DisplayMode displayMode : availableDisplayModes) {
////            resolutionComboBox.getItems().add(String.format("%dx%d", displayMode.getWidth(), displayMode.getHeight()));
//            if (displayMode.equals(currentDisplayMode)) {
//                resolutionComboBox.getSelectionModel().select(index);
////                resolutionComboBox.setValue("Skunk");
////                displayModeSelectedIndex = index;
//            }
//            index++;
//        }
        generalSettings = new GeneralSettings(settings.get(ConfigTags.SUTConnectorValue));

        DisplayMode selectedDisplayMode = generalSettings.getDisplayMode();
        int selectedWidth = selectedDisplayMode.getWidth();
        int selectedHeight = selectedDisplayMode.getHeight();
        int displayModeIndex = 0;
        for (DisplayMode displayMode : availableDisplayModes) {
            if (displayMode.getWidth() == selectedWidth && displayMode.getHeight() == selectedHeight) {
                break;
            }
            displayModeIndex++;
        }
        if (displayModeIndex == availableDisplayModes.length) {
            resolutionComboBox.getItems().add(new DisplayModeWrapper(selectedDisplayMode, false));
        }
        resolutionComboBox.getSelectionModel().select(displayModeIndex);
//        resolutionComboBox.setValue(new DisplayModeWrapper(generalSettings.getDisplayMode()));
//        resolutionComboBox.setId(String.format("%dx%d", currentDisplayMode.getWidth(), currentDisplayMode.getHeight()));

//        GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment()
//                .getScreenDevices();
//        for (int i = 0; i < devices.length; i++) {
////            GraphicsDevice dev = devices[i];
////            System.out.println("device " + i);
//            DisplayMode[] modes = dev.getDisplayModes();
//            for (int j = 0; j < modes.length; j++) {
//                DisplayMode m = modes[j];
//                System.out.println(" " + j + ": " + m.getWidth() + " x " + m.getHeight());
//            }
//        }

        webDriverPathField = (TextField) view.lookup("#driverPath");
        webDriverPathField.setText(generalSettings.getDriver());

        locationInputField = (TextField) view.lookup("#locationInput");
        locationInputField.setText(generalSettings.getLocation());

        FileChooser driverChooser = new FileChooser();
        FileChooser locationChooser = new FileChooser();

        Button btnSelectDriver = (Button) view.lookup("#btnSelectDriver");
        Button btnSelectLocation = (Button) view.lookup("#btnSelectLocation");

        btnSelectDriver.setOnAction(event -> {
            File driverFile = driverChooser.showOpenDialog(view.getScene().getWindow());
            webDriverPathField.setText(driverFile.getAbsolutePath());
        });

        btnSelectLocation.setOnAction(event -> {
            File locationFile = locationChooser.showOpenDialog(view.getScene().getWindow());
            locationInputField.setText(locationFile.toURI().toString());
        });

        numSequencesSpinner = (Spinner) view.lookup("#numSequences");
        numSequencesValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE);
        numSequencesSpinner.setValueFactory(numSequencesValueFactory);
        numActionsSpinner = (Spinner) view.lookup("#numActions");
        numActionsValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE);
        numActionsSpinner.setValueFactory(numActionsValueFactory);
        alwaysCompileCheckBox = (CheckBox) view.lookup("#alwaysCompile");
        alwaysCompileCheckBox.setSelected(settings.get(ConfigTags.AlwaysCompile));

        sutComboBox.setValue(settings.get(ConfigTags.SUTConnector));
        numSequencesValueFactory.setValue(settings.get(ConfigTags.Sequences));
        numActionsValueFactory.setValue(settings.get(ConfigTags.SequenceLength));
    }

    @Override
    protected void save(Settings settings) {
        generalSettings.setDisplayMode(resolutionComboBox.getValue().getMode());
        generalSettings.setDriver(webDriverPathField.getText());
        generalSettings.setLocation(locationInputField.getText());
        settings.set(ConfigTags.SUTConnectorValue, generalSettings.toString());

        settings.set(ConfigTags.SUTConnector, sutComboBox.getValue().toString());
        settings.set(ConfigTags.Sequences, numSequencesValueFactory.getValue());
        settings.set(ConfigTags.SequenceLength, numActionsValueFactory.getValue());
        settings.set(ConfigTags.AlwaysCompile, alwaysCompileCheckBox.isSelected());
    }
}
