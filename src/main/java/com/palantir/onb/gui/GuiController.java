/*
 * (c) Copyright 2025 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.onb.gui;
// CHECKSTYLE.OFF: IllegalImport
// CHECKSTYLE.OFF: AvoidStarImport
// There are too many packages with JavaFX to do this

import com.palantir.onb.Core;
import com.palantir.onb.GeneralTools;
import com.palantir.onb.LogStandard;
import com.palantir.onb.Manager;
import com.palantir.onb.types.BootRule;
import java.awt.AWTException;
import java.awt.Image;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.File;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Callback;
import javax.imageio.ImageIO;
// CHECKSTYLE.ON: IllegalImport

/**
 * Controller for the JavaFX GUI.
 */
public class GuiController {
    private static final String BLANK_MAC = "00:00:00:00:00:00";
    private static final String GENERAL_IP = "0.0.0.0";

    @SuppressWarnings("for-rollout:NullAway")
    private Manager manager;

    private boolean closing = false;
    private boolean editingTable = false;
    private boolean loadingPanel = false;
    private final Map<String, PcTracker> generalSystems = new HashMap<>();
    private final Map<String, String> ipToMac = new HashMap<>();

    @SuppressWarnings("for-rollout:NullAway")
    private TrayIcon trayIcon;

    private final GuiLog pxeLogger = new GuiLog();
    private final GuiLog tftpLogger = new GuiLog();
    private final GuiLog httpLogger = new GuiLog();

    @SuppressWarnings("for-rollout:NullAway")
    @FXML
    private TabPane mainTabPanel;

    @SuppressWarnings("for-rollout:NullAway")
    @FXML
    private Pane topTabControlPanel;

    @SuppressWarnings("for-rollout:NullAway")
    @FXML
    private TabPane logsTabPane;

    /**
     * Start the gui and check for some basic stuff.
     */
    @FXML
    protected void initialize() {
        manager = Core.getManager();
        pxeLogger.setChildLogger(manager.getLogger());
        tftpLogger.setChildLogger(manager.getLogger());
        httpLogger.setChildLogger(manager.getLogger());

        if (!manager.getRunningRules().getTftpSettingSet().getRootFs().exists()) {
            pxeLogger.simpleReport("TFTP folder does not exist", true);
        }

        manager.initDhcpThreads("0.0.0.0", pxeLogger);
        if (manager.getRunningRules().getEnablePxe() == 2) {
            manager.startPxeWithLogger(pxeLogger);
        } else {
            pxeLogger.simpleReport("Settings have PxeService disabled", false);
        }

        if (manager.getRunningRules().getEnableTftp() == 2) {
            manager.startTftpWithLogger(tftpLogger);
        } else {
            tftpLogger.simpleReport("Settings have TftpService disabled", false);
        }

        if (manager.getRunningRules().getEnableHttp() == 2) {
            manager.startHttpWithLogger(httpLogger);
        } else {
            httpLogger.simpleReport("Settings have HTTP disabled", false);
        }

        initGeneralTable();

        Task<Void> pxeTask = new Task<>() {
            @Override
            public Void call() {
                AnchorPane anchorPane =
                        (AnchorPane) logsTabPane.getTabs().getFirst().getContent();
                ObservableList<Node> children = anchorPane.getChildren();
                TextArea textArea = (TextArea) children.getFirst();
                AnchorPane dhcpAnchor =
                        (AnchorPane) logsTabPane.getTabs().get(1).getContent();
                ObservableList<Node> dhcpChildren = dhcpAnchor.getChildren();
                TextArea dhcpTextArea = (TextArea) dhcpChildren.getFirst();
                AnchorPane tftpAnchor =
                        (AnchorPane) logsTabPane.getTabs().get(2).getContent();
                ObservableList<Node> tftpChildren = tftpAnchor.getChildren();
                TextArea tftpTextArea = (TextArea) tftpChildren.getFirst();
                AnchorPane httpAnchor =
                        (AnchorPane) logsTabPane.getTabs().get(3).getContent();
                ObservableList<Node> httpChildren = httpAnchor.getChildren();
                TextArea httpTextArea = (TextArea) httpChildren.getFirst();
                while (!closing) {
                    List<LogStandard.TimeStampedLog> logs = pxeLogger.getLogNDump();
                    addToTextArea(textArea, dhcpTextArea, logs);
                    logs = pxeLogger.getLogNDumpE();
                    addToTextArea(textArea, dhcpTextArea, logs);
                    logs = tftpLogger.getLogNDump();
                    addToTextArea(textArea, tftpTextArea, logs);
                    logs = tftpLogger.getLogNDumpE();
                    addToTextArea(textArea, tftpTextArea, logs);
                    logs = httpLogger.getLogNDump();
                    addToTextArea(textArea, httpTextArea, logs);
                    logs = httpLogger.getLogNDumpE();
                    addToTextArea(textArea, httpTextArea, logs);
                    drawGeneralTable();
                    try {
                        Thread.sleep(100);
                        // TODO(#1): This needs to be updated to use event handlers
                    } catch (InterruptedException e) {
                        pxeLogger.simpleReport("Error getting logs: " + e, true);
                        closing = true;
                    }
                }
                return null;
            }
        };
        new Thread(pxeTask).start();

        Node versionId = getSettingsItem("versionLabel");
        if (versionId != null) {
            ((Label) versionId).setText("Version " + Core.ONB_VERSION);
        }

        javax.swing.SwingUtilities.invokeLater(this::initTray);
    }

    /**
     * Function to append the log text we want to a specific log textArea panel, and the general one.
     * @param textArea General text area
     * @param specificTextArea Specific text area
     * @param logs Log lines to add
     */
    private void addToTextArea(TextArea textArea, TextArea specificTextArea, List<LogStandard.TimeStampedLog> logs) {
        for (LogStandard.TimeStampedLog singleLogLine : logs) {
            Platform.runLater(() -> textArea.appendText(singleLogLine.getLog() + System.lineSeparator()));
            Platform.runLater(() -> specificTextArea.appendText(singleLogLine.getLog() + System.lineSeparator()));
            addToHashMaps(singleLogLine.getLog(), singleLogLine.getTimestamp());
        }
    }

    /**
     * Logs are stored in hash maps and timed to make sure they come in in order, this function does that processing.
     * @param logline Log text
     * @param timestamp long of a time stamp to track exact add time
     */
    private void addToHashMaps(String logline, long timestamp) {
        if (logline.split("-", -1).length < 3) {
            return;
        }
        String[] logparts = logline.split("-", -1);
        if (!logparts[1].trim().startsWith("/")) {
            return;
        }
        String[] ipParts = logparts[1].trim().split("/", -1);
        switch (ipParts.length) {
            case 2 -> twoPartLog(ipParts, logparts, timestamp);
            case 3 -> {
                // ip/mac , [0]"" [1]xxx.xxx.xxx.xxx [2]xx:xx:xx:xx:xx:xx
                PcTracker singlePc;
                if (generalSystems.containsKey(ipParts[2])) {
                    singlePc = generalSystems.get(ipParts[2]);
                    singlePc.update(logparts[2], true, logparts[0], ipParts[1]);
                } else {
                    singlePc = new PcTracker(ipParts[1], logparts[2], logparts[0], true, timestamp);
                    generalSystems.put(ipParts[2], singlePc);
                }
                if (!GENERAL_IP.equals(ipParts[1]) && !ipToMac.containsKey(ipParts[1])) {
                    ipToMac.put(ipParts[1], ipParts[2]);
                }
            }
        }
    }

    /**
     * Process a log that comes in with 2 parts.
     * just IP, [0]"" [1]xxx.xxx.xxx.xxx
     * @param ipParts IP data
     * @param logparts split up log
     * @param timestamp time stamp of the event
     */
    @SuppressWarnings("for-rollout:NullAway")
    private void twoPartLog(String[] ipParts, String[] logparts, long timestamp) {
        if (ipToMac.containsKey(ipParts[1])) {
            String mac = ipToMac.get(ipParts[1]);
            PcTracker singlePc = generalSystems.get(mac);
            if (singlePc.getTimestamp() > timestamp) {
                return;
            }
            singlePc.update(logparts[2], true, logparts[0], ipParts[1]);
            singlePc.setTimestamp(timestamp);
        } else {
            PcTracker singlePc;
            if (generalSystems.containsKey(BLANK_MAC)) {
                singlePc = generalSystems.get(BLANK_MAC);
                singlePc.update(logparts[2], true, logparts[0], ipParts[1]);
            } else {
                singlePc = new PcTracker(ipParts[1], logparts[2], logparts[0], true, timestamp);
                generalSystems.put(BLANK_MAC, singlePc);
            }
            if (!GENERAL_IP.equals(ipParts[1]) && !ipToMac.containsKey(ipParts[1])) {
                ipToMac.put(ipParts[1], BLANK_MAC);
            }
        }
    }

    /**
     * Init the general services table.
     */
    @SuppressWarnings("unchecked")
    private void initGeneralTable() {
        AnchorPane mainPageAnchor = (AnchorPane) mainTabPanel.getTabs().get(0).getContent();
        TableView<GeneralSystemRow> generalTable =
                (TableView<GeneralSystemRow>) mainPageAnchor.getChildren().get(0);
        generalTable.setEditable(false);
        TableColumn<GeneralSystemRow, String> macCol = new TableColumn<>("MAC");
        macCol.setCellValueFactory(new PropertyValueFactory<>("mac"));

        TableColumn<GeneralSystemRow, String> lastIpCol = new TableColumn<>("Last IP");
        lastIpCol.setCellValueFactory(new PropertyValueFactory<>("lastIp"));

        TableColumn<GeneralSystemRow, String> service = new TableColumn<>("Service");
        service.setCellValueFactory(new PropertyValueFactory<>("service"));

        TableColumn<GeneralSystemRow, String> lastDateTime = new TableColumn<>("Date/Time");
        lastDateTime.setCellValueFactory(new PropertyValueFactory<>("time"));

        generalTable.getColumns().clear();
        generalTable.getColumns().add(macCol);
        generalTable.getColumns().add(lastIpCol);
        generalTable.getColumns().add(service);
        generalTable.getColumns().add(lastDateTime);
    }

    /**
     * Draw data into the general table.
     */
    @SuppressWarnings("unchecked")
    private void drawGeneralTable() {
        AnchorPane mainPageAnchor = (AnchorPane) mainTabPanel.getTabs().get(0).getContent();
        TableView<GeneralSystemRow> generalTable =
                (TableView<GeneralSystemRow>) mainPageAnchor.getChildren().get(0);
        generalTable.setEditable(false);

        for (Map.Entry<String, PcTracker> item : generalSystems.entrySet()) {
            if (!item.getValue().isUpdated()) {
                continue;
            }
            Iterator<GeneralSystemRow> rowIterable = generalTable.getItems().iterator();
            boolean found = false;
            while (rowIterable.hasNext()) {
                GeneralSystemRow singleRow = rowIterable.next();
                if (singleRow.mac.get().equals(item.getKey())) {
                    singleRow.setService(item.getValue().getService());
                    if (!GENERAL_IP.equals(item.getValue().getIp()) && GENERAL_IP.equals(singleRow.getLastIp())) {
                        singleRow.setLastIp(item.getValue().getIp());
                    }
                    singleRow.setTime(item.getValue().getDateTime());
                    found = true;
                    break;
                }
            }
            if (!found) {
                generalTable
                        .getItems()
                        .add(new GeneralSystemRow(
                                item.getKey(),
                                item.getValue().getIp(),
                                item.getValue().getService(),
                                item.getValue().getDateTime()));
            }
            item.getValue().setUpdated(false);
        }
    }

    /**
     * Change view to general.
     */
    @FXML
    protected void changeToGeneralView() {
        changeView(0, "generalButton");
    }

    /**
     * Change view to logs.
     */
    @FXML
    protected void changeToLogsView() {
        changeView(1, "logsButton");
    }

    /**
     * Change view to options.
     */
    @FXML
    protected void changeToOptionsView() {
        changeView(2, "optionsButton");
        loadOptionsStatus();
    }

    /**
     * Change if DHCP should start with app startup.
     */
    @FXML
    protected void changeDhcpStartSetting() {
        Node setting = getSettingsItem("dhcpAutostart");
        CheckBox settings = (CheckBox) setting;
        if (settings == null) {
            pxeLogger.simpleReport("Failed to load dhcp settings", true);
            return;
        }
        manager.getRunningRules().setEnablePxe(settings.isSelected() ? (byte) 2 : (byte) 0);
        manager.saveSettingsFile();
    }

    /**
     * Change if Tftp should start at app startup.
     */
    @FXML
    protected void changeTftpStartSetting() {
        Node setting = getSettingsItem("tftpAutostart");
        CheckBox settings = (CheckBox) setting;
        if (settings == null) {
            tftpLogger.simpleReport("Failed to load tftp settings", true);
            return;
        }
        manager.getRunningRules().setEnableTftp(settings.isSelected() ? (byte) 2 : (byte) 0);
        manager.saveSettingsFile();
    }

    /**
     * Change if Http should start with app.
     */
    @FXML
    protected void changeHttpStartSetting() {
        Node setting = getSettingsItem("httpAutostart");
        CheckBox settings = (CheckBox) setting;
        if (settings == null) {
            httpLogger.simpleReport("Failed to load http settings", true);
            return;
        }
        manager.getRunningRules().setEnableHttp(settings.isSelected() ? (byte) 2 : (byte) 0);
        manager.saveSettingsFile();
    }

    /**
     * Change if ISO/Zip files are allowed to be opened and processed.
     */
    @FXML
    protected void changeIsoExtractingSetting() {
        Node setting = getSettingsItem("allowIsoExtracting");
        CheckBox settings = (CheckBox) setting;
        if (settings == null) {
            pxeLogger.simpleReport("Failed to load ISO/ZIP setting", true);
            return;
        }
        manager.getRunningRules().setAllowIsoExtracting(settings.isSelected());
        manager.saveSettingsFile();
        if (manager.flipHttp()) {
            manager.flipHttp();
        }
    }

    /**
     * Add a rule to the current configuration, then save it to the startup rules.onr.
     */
    @FXML
    protected void addRule() {
        ListView<String> list = getRulesView();
        int itemIndex = -1;
        if (!loadingPanel
                || !editingTable
                || (list != null
                        && (list.getSelectionModel().getSelectedIndices().size() > 0))) {
            itemIndex = list.getSelectionModel().getSelectedIndices().get(0);
        }

        BootRule bootRule = new BootRule();
        if (itemIndex == -1) {
            // No item selected, just add to end of the rules
            manager.getRunningRules().getRuleSet().add(bootRule);
            manager.saveSettingsFile();
            loadOptionsListDraw(getRulesView());
            if (list == null) {
                pxeLogger.simpleReport("Error parsing settings to save", true);
                return;
            }
            list.getSelectionModel()
                    .selectIndices(manager.getRunningRules().getRuleSet().size());
        } else {
            manager.getRunningRules().getRuleSet().add(itemIndex + 1, bootRule);
            manager.saveSettingsFile();
            loadOptionsListDraw(getRulesView());
            list.getSelectionModel().selectIndices(itemIndex + 1);
        }
    }

    /**
     * Move a rule up in the processing order.
     */
    @FXML
    protected void moveRuleUp() {
        ListView<String> list = getRulesView();
        if (loadingPanel
                || editingTable
                || list == null
                || list.getSelectionModel().getSelectedIndices().get(0) == null
                || list.getSelectionModel().getSelectedIndices().get(0) == -1) {
            return;
        }
        int itemIndex = list.getSelectionModel().getSelectedIndices().get(0);
        if (itemIndex == 0) {
            return;
        }
        Collections.swap(manager.getRunningRules().getRuleSet(), itemIndex, itemIndex - 1);
        manager.saveSettingsFile();
        loadOptionsListDraw(getRulesView());
        list.getSelectionModel().selectIndices(itemIndex - 1);
    }

    /**
     * Move a rule down in the processing order.
     */
    @FXML
    protected void moveRuleDown() {
        ListView<String> list = getRulesView();
        if (loadingPanel
                || editingTable
                || list == null
                || list.getSelectionModel().getSelectedIndices().get(0) == null
                || list.getSelectionModel().getSelectedIndices().get(0) == -1) {
            return;
        }
        int itemIndex = list.getSelectionModel().getSelectedIndices().get(0);
        if (itemIndex == (manager.getRunningRules().getRuleSet().size() - 1)) {
            return;
        }
        Collections.swap(manager.getRunningRules().getRuleSet(), itemIndex, itemIndex + 1);
        manager.saveSettingsFile();
        loadOptionsListDraw(getRulesView());
        list.getSelectionModel().selectIndices(itemIndex + 1);
    }

    /**
     * Delete a rule from rule list.
     */
    @FXML
    protected void removeRule() {
        ListView<String> list = getRulesView();
        if (loadingPanel
                || editingTable
                || list == null
                || list.getSelectionModel().getSelectedIndices().get(0) == null
                || list.getSelectionModel().getSelectedIndices().get(0) == -1) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Dialog");
        alert.setContentText("Are you sure you want to delete PxeService Rule?");
        ButtonType buttonYes = new ButtonType("Yes");
        ButtonType buttonNo = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(buttonYes, buttonNo);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == buttonNo) {
            return;
        }
        int itemIndex = list.getSelectionModel().getSelectedIndices().get(0);
        manager.getRunningRules().getRuleSet().remove(itemIndex);
        manager.saveSettingsFile();
        loadOptionsListDraw(getRulesView());
        if (itemIndex == manager.getRunningRules().getRuleSet().size()) {
            list.getSelectionModel().selectIndices(itemIndex - 1);
        } else {
            if (!manager.getRunningRules().getRuleSet().isEmpty()) {
                list.getSelectionModel().selectIndices(itemIndex);
            }
        }
    }

    /**
     * Change the panel to the selected view.
     * @param tabIndex the different views are just tabs, switch to that tab
     * @param buttonId which button chould be underlined
     */
    private void changeView(int tabIndex, String buttonId) {
        SingleSelectionModel<Tab> selectionModel = mainTabPanel.getSelectionModel();
        selectionModel.select(tabIndex);
        ObservableList<Node> list = topTabControlPanel.getChildren();
        for (Node temp : list) {
            if ("Label".equals(temp.getTypeSelector())) {
                Label tempLabel = (Label) temp;
                tempLabel.setUnderline(buttonId.equals(temp.getId()));
            }
        }
    }

    /**
     * When "Services" are clicked, show there status.
     */
    @SuppressWarnings("unchecked")
    private void loadOptionsStatus() {
        loadingPanel = true;
        AnchorPane settingsOuterAnchor =
                (AnchorPane) mainTabPanel.getTabs().get(2).getContent();
        AnchorPane settingsInnerAnchor =
                (AnchorPane) ((ScrollPane) settingsOuterAnchor.getChildren().get(0)).getContent();
        ObservableList<Node> list = settingsInnerAnchor.getChildren();
        for (Node temp : list) {
            switch (temp.getTypeSelector()) {
                case "CheckBox" -> loadOptionsCheckBoxes((CheckBox) temp);
                case "TableView" -> loadOptionsTableView((TableView<InterfaceDevice>) temp);
                case "TextField" -> loadOptionsTextField((TextField) temp);
                case "ComboBox" -> loadOptionsComboBox((ComboBox<String>) temp);
                case "ListView" -> loadOptionsRulesList((ListView<String>) temp);
            }
        }
        loadingPanel = false;
    }

    private void loadOptionsCheckBoxes(CheckBox passedCheck) {
        switch (passedCheck.getId()) {
            case "dhcpAutostart" ->
                passedCheck.setSelected(manager.getRunningRules().getEnablePxe() == 2);
            case "tftpAutostart" ->
                passedCheck.setSelected(manager.getRunningRules().getEnableTftp() == 2);
            case "httpAutostart" ->
                passedCheck.setSelected(manager.getRunningRules().getEnableHttp() == 2);
            case "allowIsoExtracting" ->
                passedCheck.setSelected(manager.getRunningRules().isAllowIsoExtracting());
        }
    }

    /**
     * Loads tables for the options view, this is the interface table.
     * @param passedTable which table object to load into
     */
    @SuppressWarnings("unchecked")
    private void loadOptionsTableView(TableView<InterfaceDevice> passedTable) {
        if (!passedTable.getId().equals("interfaceTable")) {
            return;
        }

        passedTable.getItems().clear();
        passedTable.setEditable(true);
        Callback<TableColumn<InterfaceDevice, Boolean>, TableCell<InterfaceDevice, Boolean>> booleanCellFactory =
                _p -> new BooleanCell();
        TableColumn<InterfaceDevice, Boolean> enabledCol = new TableColumn<>("Enabled");
        enabledCol.setCellValueFactory(new PropertyValueFactory<>("enabled"));
        enabledCol.setCellFactory(booleanCellFactory);
        enabledCol.setEditable(true);

        TableColumn<InterfaceDevice, String> nicName = new TableColumn<>("Interface Name");
        nicName.setCellValueFactory(new PropertyValueFactory<>("deviceName"));

        TableColumn<InterfaceDevice, String> ipAddrs = new TableColumn<>("Interface IPs");
        ipAddrs.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));

        passedTable.getColumns().setAll(enabledCol, nicName, ipAddrs);

        Enumeration<NetworkInterface> interfaces;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            pxeLogger.simpleReport(e.toString(), true);
            return;
        }
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            String name = networkInterface.getDisplayName() + " (" + networkInterface.getName() + ")";
            StringBuilder ips = new StringBuilder();
            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress inetAddress = addresses.nextElement();
                if (inetAddress instanceof Inet6Address) {
                    continue;
                }
                ips.append(GeneralTools.arrayToString(inetAddress.getAddress()));
                ips.append(", ");
            }
            passedTable
                    .getItems()
                    .add(new InterfaceDevice(activeInterface(networkInterface.getName()), name, ips.toString()));
        }
    }

    /**
     * Get if the interface is set to be used.
     * @param interfaceName interface short name
     * @return was able to find the interface
     */
    private boolean activeInterface(String interfaceName) {
        for (String singleInterface : manager.getRunningRules().getLastInterfaces()) {
            if (interfaceName.equalsIgnoreCase(singleInterface)) {
                return true;
            }
        }
        return false;
    }

    // CHECKSTYLE.OFF: CyclomaticComplexity
    /**
     * Load the text boxes on the options screen.
     * @param passedTextField which text field to set
     */
    private void loadOptionsTextField(TextField passedTextField) {
        switch (passedTextField.getId()) {
            case "locationSetable" -> loadOptionsTextFieldLocation(passedTextField);
            case "optionsHttpPort" -> loadOptionsTextFieldHttp(passedTextField);
            case "optionsTftpHome" -> loadOptionsTextFieldTftp(passedTextField);
            case "ruleClientIdSetable" -> loadOptionsTextFieldClientId(passedTextField);
            case "ruleMacSetable" -> loadOptionsTextFieldMac(passedTextField);
            case "ruleArchSelectable" -> loadOptionsTextFieldArch(passedTextField);
            case "ruleFileSetable" ->
                passedTextField.textProperty().addListener((_observable, _oldValue, newValue) -> {
                    ListView<String> list = getRulesView();
                    if (loadingPanel
                            || editingTable
                            || list == null
                            || newValue == null
                            || list.getSelectionModel().getSelectedIndices().isEmpty()
                            || list.getSelectionModel().getSelectedIndices().get(0) == -1) {
                        return;
                    }
                    manager.getRunningRules()
                            .getRuleSet()
                            .get(list.getSelectionModel().getSelectedIndices().get(0))
                            .setBootFile(newValue);
                    manager.saveSettingsFile();
                    loadOptionsListDraw(getRulesView());
                });
            case "ruleServerSetable" ->
                passedTextField.textProperty().addListener((_observable, _oldValue, newValue) -> {
                    ListView<String> list = getRulesView();
                    if (loadingPanel
                            || editingTable
                            || list == null
                            || newValue == null
                            || list.getSelectionModel().getSelectedIndices().isEmpty()
                            || list.getSelectionModel().getSelectedIndices().get(0) == -1) {
                        return;
                    }
                    manager.getRunningRules()
                            .getRuleSet()
                            .get(list.getSelectionModel().getSelectedIndices().get(0))
                            .setServerIp(newValue);
                    manager.saveSettingsFile();
                    loadOptionsListDraw(getRulesView());
                });
        }
    }
    // CHECKSTYLE.ON

    private void loadOptionsTextFieldLocation(TextField passedTextField) {
        passedTextField.textProperty().setValue(manager.getRunningRules().getLastFileLogLoc());
        passedTextField.textProperty().addListener((_observable, _oldValue, newValue) -> {
            if (newValue == null || loadingPanel) {
                return;
            }
            manager.getRunningRules().setLastFileLogLoc(newValue);
            manager.saveSettingsFile();
            manager.overrideLogLocation(newValue);
        });
    }

    private void loadOptionsTextFieldHttp(TextField passedTextField) {
        passedTextField
                .textProperty()
                .setValue(Integer.toString(manager.getRunningRules().getHttpPort()));
        passedTextField.textProperty().addListener((_observable, _oldValue, newValue) -> {
            ListView<String> list = getRulesView();
            if (loadingPanel
                    || list == null
                    || newValue == null
                    || newValue.isEmpty()
                    || list.getSelectionModel().getSelectedIndices().get(0) == null) {
                return;
            }
            manager.getRunningRules().setHttpPort(Integer.parseInt(newValue));
            manager.saveSettingsFile();
            if (manager.flipHttp()) {
                manager.flipHttp();
            }
        });
    }

    private void loadOptionsTextFieldTftp(TextField passedTextField) {
        passedTextField
                .textProperty()
                .setValue(manager.getRunningRules()
                        .getTftpSettingSet()
                        .getRootFs()
                        .getPath());
        passedTextField.textProperty().addListener((_observable, _oldValue, newValue) -> {
            ListView<String> list = getRulesView();
            if (loadingPanel
                    || list == null
                    || newValue == null
                    || list.getSelectionModel().getSelectedIndices().get(0) == null) {
                return;
            }
            manager.getRunningRules().getTftpSettingSet().setRootFs(new File(newValue));
            manager.saveSettingsFile();
            if (manager.flipHttp()) {
                manager.flipHttp();
            }
            if (manager.flipTftp()) {
                manager.flipTftp();
            }
        });
    }

    private void loadOptionsTextFieldClientId(TextField passedTextField) {
        passedTextField.textProperty().addListener((_observable, _oldValue, newValue) -> {
            ListView<String> list = getRulesView();
            if (loadingPanel
                    || editingTable
                    || list == null
                    || newValue == null
                    || list.getSelectionModel().getSelectedIndices().isEmpty()
                    || list.getSelectionModel().getSelectedIndices().get(0) == -1) {
                return;
            }
            manager.getRunningRules()
                    .getRuleSet()
                    .get(list.getSelectionModel().getSelectedIndices().get(0))
                    .setClient(newValue);
            manager.saveSettingsFile();
            loadOptionsListDraw(getRulesView());
        });
    }

    private void loadOptionsTextFieldMac(TextField passedTextField) {
        passedTextField.textProperty().addListener((_observable, _oldValue, newValue) -> {
            ListView<String> list = getRulesView();
            if (loadingPanel
                    || editingTable
                    || list == null
                    || newValue == null
                    || list.getSelectionModel().getSelectedIndices().isEmpty()
                    || list.getSelectionModel().getSelectedIndices().get(0) == -1) {
                return;
            }
            manager.getRunningRules()
                    .getRuleSet()
                    .get(list.getSelectionModel().getSelectedIndices().get(0))
                    .setHardwareAddress(newValue);
            manager.saveSettingsFile();
            loadOptionsListDraw(getRulesView());
        });
    }

    private void loadOptionsTextFieldArch(TextField passedTextField) {
        passedTextField.textProperty().addListener((_observable, oldValue, newValue) -> {
            String replacementValue = newValue;
            ListView<String> list = getRulesView();
            if (loadingPanel
                    || editingTable
                    || list == null
                    || replacementValue == null
                    || list.getSelectionModel().getSelectedIndices().isEmpty()
                    || list.getSelectionModel().getSelectedIndices().get(0) == -1) {
                return;
            }

            if (oldValue.length() == (replacementValue.length() + 1) && oldValue.startsWith(replacementValue)) {
                // User has deleted 1 character, its probably a ',', and user didn't delete a character in the mid
                replacementValue = replacementValue.substring(0, replacementValue.lastIndexOf(','));
            }

            List<Integer> newArchs = new ArrayList<>();
            if (!replacementValue.isEmpty()) {
                for (int i = 0; i < replacementValue.split(",", -1).length; i++) {
                    try {
                        newArchs.add(Integer.parseInt(replacementValue.split(",", -1)[i]));
                    } catch (NumberFormatException e) {
                        // This doesnt matter at all
                    }
                }
            }

            manager.getRunningRules()
                    .getRuleSet()
                    .get(list.getSelectionModel().getSelectedIndices().get(0))
                    .setHardwareTypes(newArchs.stream().mapToInt(i -> i).toArray());
            manager.saveSettingsFile();
            loadOptionsListDraw(getRulesView());
        });
    }

    /**
     * Load the combo boxes on the options page.
     * @param passedCombo which combo box
     */
    private void loadOptionsComboBox(ComboBox<String> passedCombo) {
        if (passedCombo.getItems() != null) {
            passedCombo.getItems().clear();
        }

        switch (passedCombo.getId()) {
            case "logLevel" -> {
                String[] logStringOptions = {
                    "0 - Say when the system starts and stops, NO FILE LOGGING",
                    "1 - Say when ports come up, no other info",
                    "2 - Echo out when a packet is received, and simple response",
                    "3 - More in depth info about packet and response",
                    "4 - Most verbose level of output"
                };

                ObservableList<String> logOptions = FXCollections.observableArrayList(logStringOptions);
                passedCombo.setItems(logOptions);
                passedCombo.setValue(logStringOptions[manager.getRunningRules().getLastConsoleLogLvl()]);
                passedCombo.valueProperty().addListener((_observable, _oldValue, newValue) -> {
                    if (loadingPanel || newValue == null) {
                        return;
                    }
                    manager.getRunningRules().setLastFileLogLvl(Integer.parseInt(newValue.substring(0, 1)));
                    manager.getRunningRules().setLastConsoleLogLvl(Integer.parseInt(newValue.substring(0, 1)));
                    manager.overrideLogLevelConsole(Integer.parseInt(newValue.substring(0, 1)));
                    manager.overrideLogLevelFile(Integer.parseInt(newValue.substring(0, 1)));
                    manager.saveSettingsFile();
                });
            }
            case "broadcastTypeSelectable" -> {
                String[] broadcastStringOptions = {
                    "0 - Send to network broadcast",
                    "1 - Send to global broadcast",
                    "2 - Send to network and global broadcast"
                };

                ObservableList<String> broadcastOptions = FXCollections.observableArrayList(broadcastStringOptions);
                passedCombo.setItems(broadcastOptions);
                passedCombo.setValue(
                        broadcastStringOptions[manager.getRunningRules().getBroadcastSetting()]);
                passedCombo.valueProperty().addListener((_observable, _oldValue, newValue) -> {
                    if (loadingPanel || newValue == null) {
                        return;
                    }
                    manager.getRunningRules().setBroadcastSetting(Integer.parseInt(newValue.substring(0, 1)));
                    manager.saveSettingsFile();
                    manager.bounceThreadsWithInterfaceLimits();
                });
            }
        }
    }

    /**
     * Load the rules view listView, and set it up blank.
     * @param passedList Which list view to load
     */
    private void loadOptionsRulesList(ListView<String> passedList) {
        if (!"rulesView".equals(passedList.getId())) {
            return;
        }
        editingTable = true;

        loadOptionsListDraw(passedList);
        passedList.getSelectionModel().selectedItemProperty().addListener((_observable, _oldValue, _newValue) -> {
            if (loadingPanel
                    || editingTable
                    || passedList.getSelectionModel().getSelectedIndices().isEmpty()) {
                return;
            }
            if (passedList.getSelectionModel().getSelectedIndices().isEmpty()) {
                drawRuleInfo(-1);
            } else {
                int rowTracker =
                        passedList.getSelectionModel().getSelectedIndices().get(0);
                if (rowTracker < 0) {
                    rowTracker = 0;
                }
                drawRuleInfo(rowTracker);
            }
        });

        editingTable = false;
    }

    /**
     * Draw the rules area, with the text internally.
     * @param passedList the rules list box
     */
    private void loadOptionsListDraw(ListView<String> passedList) {
        int selected = 0;

        if (passedList.getItems() != null && passedList.getItems().size() > 0) {
            selected = passedList.getSelectionModel().getSelectedIndices().get(0);
            passedList.getItems().clear();
        }
        for (int k = 0; k < manager.getRunningRules().getRuleSet().size(); k++) {
            BootRule bootRule = manager.getRunningRules().getRuleSet().get(k);
            StringBuilder tempString = new StringBuilder();
            if (!bootRule.getClient().isEmpty()) {
                tempString.append("Client ID: ");
                tempString.append(bootRule.getClient());
            }

            if (!bootRule.getHardwareAddress().isEmpty()) {
                if (tempString.length() != 0) {
                    tempString.append(", ");
                }
                tempString.append("MAC: ");
                tempString.append(bootRule.getHardwareAddress());
            }

            if (bootRule.getHardwareTypes().length > 0) {
                if (tempString.length() != 0) {
                    tempString.append(", ");
                }
                tempString.append("Arch: ");
                for (int i : bootRule.getHardwareTypes()) {
                    tempString.append(i);
                    tempString.append(',');
                }
                if (tempString.substring(tempString.length() - 1).equals(",")) {
                    tempString.deleteCharAt(tempString.length() - 1);
                }
            }

            if (!bootRule.getBootFile().isEmpty()) {
                if (tempString.length() != 0) {
                    tempString.append(", ");
                }
                tempString.append("Boot File: ");
                tempString.append(bootRule.getBootFile());
            }

            if (!bootRule.getServerIp().isEmpty()) {
                if (tempString.length() != 0) {
                    tempString.append(", ");
                }
                tempString.append("Server: ");
                tempString.append(bootRule.getServerIp());
            }
            passedList.getItems().add(k + ": " + tempString);
        }
        if (selected == -1) {
            selected = 0;
        }
        passedList.getSelectionModel().select(selected);
    }

    /**
     * Draw the text boxes when a rule is selected.
     * @param index Index of the rule to select
     */
    private void drawRuleInfo(int index) {
        if (index == -1) {
            wipeTable();
        }
        if (loadingPanel || editingTable || index == -1) {
            return;
        }

        editingTable = true;
        // We are only ever going to draw 1
        BootRule operatingRule = manager.getRunningRules().getRuleSet().get(index);
        AnchorPane settingsOuterAnchor =
                (AnchorPane) mainTabPanel.getTabs().get(2).getContent();
        AnchorPane settingsInnerAnchor =
                (AnchorPane) ((ScrollPane) settingsOuterAnchor.getChildren().get(0)).getContent();
        ObservableList<Node> list = settingsInnerAnchor.getChildren();
        for (Node temp : list) {
            if (!"TextField".equals(temp.getTypeSelector())) {
                continue;
            }
            switch (temp.getId()) {
                case "ruleArchSelectable" -> {
                    StringBuilder tempString = new StringBuilder();
                    for (int type : operatingRule.getHardwareTypes()) {
                        tempString.append(type).append(',');
                    }
                    ((TextField) temp).textProperty().setValue(tempString.toString());
                }
                case "ruleFileSetable" -> ((TextField) temp).textProperty().setValue(operatingRule.getBootFile());
                case "ruleMacSetable" -> ((TextField) temp).textProperty().setValue(operatingRule.getHardwareAddress());
                case "ruleServerSetable" -> ((TextField) temp).textProperty().setValue(operatingRule.getServerIp());
                case "ruleClientIdSetable" -> ((TextField) temp).textProperty().setValue(operatingRule.getClient());
            }
        }
        editingTable = false;
    }

    private void wipeTable() {
        AnchorPane settingsOuterAnchor =
                (AnchorPane) mainTabPanel.getTabs().get(2).getContent();
        AnchorPane settingsInnerAnchor =
                (AnchorPane) ((ScrollPane) settingsOuterAnchor.getChildren().get(0)).getContent();
        ObservableList<Node> list = settingsInnerAnchor.getChildren();
        for (Node temp : list) {
            if (!"TextField".equals(temp.getTypeSelector())) {
                continue;
            }
            switch (temp.getId()) {
                case "ruleArchSelectable",
                        "ruleFileSetable",
                        "ruleMacSetable",
                        "ruleServerSetable",
                        "ruleClientIdSetable" ->
                    ((TextField) temp).textProperty().setValue("");
            }
        }
        editingTable = false;
    }

    /**
     * Draw the service status in the drop down.
     */
    @FXML
    protected void showServicesStatus() {
        ObservableList<Node> list = topTabControlPanel.getChildren();
        for (Node temp : list) {
            if ("MenuButton".equals(temp.getTypeSelector())) {
                MenuButton button = (MenuButton) temp;
                button.getItems().clear();
                String tempStatus = manager.dhcp67Up() ? "UP" : "DOWN";
                MenuItem dhcpOneUp = new MenuItem("DHCP(67) - " + tempStatus);
                dhcpOneUp.setOnAction(_event -> manager.flip67());
                button.getItems().add(dhcpOneUp);
                tempStatus = manager.dhcp4011Up() ? "UP" : "DOWN";
                MenuItem dhcpOneTwo = new MenuItem("DHCP(4011) - " + tempStatus);
                dhcpOneTwo.setOnAction(_event -> manager.flip4011());
                button.getItems().add(dhcpOneTwo);
                tempStatus = manager.tftpUp() ? "UP" : "DOWN";
                MenuItem tftpUp = new MenuItem("TftpService(69) - " + tempStatus);
                tftpUp.setOnAction(_event -> manager.flipTftp());
                button.getItems().add(tftpUp);
                tempStatus = manager.httpUp() ? "UP" : "DOWN";
                MenuItem httpUp = new MenuItem("HTTP(" + manager.httpPort() + ") - " + tempStatus);
                httpUp.setOnAction(_event -> manager.flipHttp());
                button.getItems().add(httpUp);
                button.show();
                break;
            }
        }
    }

    /**
     * If the system supports tray icons, draw a tray icon and init that mode.
     */
    private void initTray() {
        // Toolkit.getDefaultToolkit().getImage(imageUrl)
        try {
            URL iconUrl = getClass().getResource("/icon.gif");
            if (iconUrl == null) {
                pxeLogger.simpleReport("Could not load tray icon", true);
                return;
            }
            Image icon = ImageIO.read(iconUrl);
            if (icon != null) {
                trayIcon = new TrayIcon(icon, "ONB - Classic");
            } else {
                // no tray icon
                return;
            }
        } catch (IOException e) {
            pxeLogger.simpleReport(e.toString(), true);
            return;
        }

        final SystemTray tray = SystemTray.getSystemTray();
        trayIcon.addActionListener(_event -> Platform.runLater(this::showStage));
        // Create a pop-up menu components
        java.awt.MenuItem openItem = new java.awt.MenuItem("Open Application");
        Platform.setImplicitExit(false);
        openItem.addActionListener(_event -> Platform.runLater(this::showStage));

        java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit");
        exitItem.addActionListener(_e -> System.exit(0));

        // Add components to pop-up menu
        final PopupMenu popup = new PopupMenu();
        popup.add(openItem);
        popup.addSeparator();
        popup.add(exitItem);

        trayIcon.setPopupMenu(popup);

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            pxeLogger.simpleReport("TrayIcon could not be added.", true);
        }

        if (SystemTray.isSupported()) {
            Stage stage = (Stage) mainTabPanel.getScene().getWindow();
            stage.setOnCloseRequest(_event -> {
                javax.swing.SwingUtilities.invokeLater(this::goingToTrayMessage);
                stage.hide();
            });
        } else {
            Stage stage = (Stage) mainTabPanel.getScene().getWindow();
            stage.setOnCloseRequest(_event -> stage.close());
        }
    }

    /**
     * Show stage when showing and hiding.
     */
    private void showStage() {
        Stage stage = (Stage) mainTabPanel.getScene().getWindow();
        stage.show();
    }

    /**
     * Message the user that the app will go to the tray.
     */
    private void goingToTrayMessage() {
        if (SystemTray.isSupported() && trayIcon != null) {
            trayIcon.displayMessage(
                    "Closed to tray", "ONB will continue to run in the tray.", TrayIcon.MessageType.INFO);
        }
    }

    /**
     * Class for the interface used on the options panel.
     *
     * Note: When Intellij says delete extra functions, that breaks everything
     */
        public static final class InterfaceDevice {
        private final SimpleBooleanProperty enabled;
        private final SimpleStringProperty deviceName;
        private final SimpleStringProperty ipAddress;

        private InterfaceDevice(boolean enabled, String device, String ipAddr) {
            this.enabled = new SimpleBooleanProperty(enabled);
            this.deviceName = new SimpleStringProperty(device);
            this.ipAddress = new SimpleStringProperty(ipAddr);
        }

        public String getDeviceName() {
            return deviceName.get();
        }

        public boolean isEnabled() {
            return enabled.get();
        }

        public SimpleBooleanProperty enabledProperty() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled.set(enabled);
        }

        public SimpleStringProperty deviceNameProperty() {
            return deviceName;
        }

        public void setDeviceName(String deviceName) {
            this.deviceName.set(deviceName);
        }

        public String getIpAddress() {
            return ipAddress.get();
        }

        public SimpleStringProperty ipAddressProperty() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress.set(ipAddress);
        }
    }

    /**
     * A custom boolean box, so the interface table can have checkboxes.
     */
    public class BooleanCell extends TableCell<InterfaceDevice, Boolean> {
        private final CheckBox checkBox;

        BooleanCell() {
            checkBox = new CheckBox();
            checkBox.setDisable(true);
            checkBox.selectedProperty().addListener((_observable, _oldValue, newValue) -> {
                if (isEditing()) {
                    commitEdit(newValue != null && newValue);
                }
            });
            this.setGraphic(checkBox);
            this.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            this.setEditable(true);
        }

        /**
         * Add a checkbox with a cover to search setting here.
         */
        @Override
        public void startEdit() {
            super.startEdit();
            if (isEmpty()) {
                return;
            }
            checkBox.setDisable(false);
            checkBox.requestFocus();
        }

        /**
         * User cancels their edit.
         */
        @Override
        public void cancelEdit() {
            super.cancelEdit();
            checkBox.setDisable(true);
        }

        /**
         * Save this edit to the interfaces dataset.
         * @param value - Value to commit
         */
        @SuppressWarnings("unchecked")
        @Override
        public void commitEdit(Boolean value) {
            super.commitEdit(value);
            checkBox.setDisable(true);
            AnchorPane settingsOuterAnchor =
                    (AnchorPane) mainTabPanel.getTabs().get(2).getContent();
            AnchorPane settingsInnerAnchor =
                    (AnchorPane) ((ScrollPane) settingsOuterAnchor.getChildren().get(0)).getContent();
            ObservableList<Node> list = settingsInnerAnchor.getChildren();
            for (Node temp : list) {
                if (temp.getId() != null
                        && temp.getTypeSelector().equals("TableView")
                        && "interfaceTable".equals(temp.getId())) {
                    updateManager((TableView<InterfaceDevice>) temp);
                    break;
                }
            }
        }

        /**
         * Update one of the settings.
         * @param item - Settings state
         * @param empty - Setting
         */
        @Override
        public void updateItem(Boolean item, boolean empty) {
            super.updateItem(item, empty);
            if (!isEmpty()) {
                checkBox.setSelected(item);
            }
        }

        /**
         * Replace interface data in the settings, replace active interface data.
         * @param passedTable Table with interface data being passed in to reead.
         */
        private void updateManager(TableView<InterfaceDevice> passedTable) {
            ObservableList<InterfaceDevice> rows = passedTable.getItems();
            List<String> newInterfaces = new ArrayList<>();
            for (InterfaceDevice singleDevice : rows) {
                if (singleDevice.enabled.getValue()) {
                    int lastPos = singleDevice.getDeviceName().lastIndexOf('(');
                    newInterfaces.add(singleDevice
                            .getDeviceName()
                            .substring(lastPos + 1, singleDevice.getDeviceName().length() - 1));
                }
            }
            manager.getRunningRules().setLastInterfaces(newInterfaces.toArray(new String[0]));
            manager.saveSettingsFile();
            manager.bounceThreadsWithInterfaceLimits();
        }
    }

    /**
     * General panel system row, allow for quick glance.
     *
     * Note: When Intellij says delete extra functions, that breaks everything
     */
        public static final class GeneralSystemRow {
        private final SimpleStringProperty mac;
        private final SimpleStringProperty lastIp;
        private final SimpleStringProperty service;
        private final SimpleStringProperty time;

        private GeneralSystemRow(String mac, String lastIp, String service, String time) {
            this.mac = new SimpleStringProperty(mac);
            this.lastIp = new SimpleStringProperty(lastIp);
            this.service = new SimpleStringProperty(service);
            this.time = new SimpleStringProperty(time);
        }

        private String getLastIp() {
            return lastIp.get();
        }

        private void setLastIp(String lastIp) {
            this.lastIp.set(lastIp);
        }

        private void setService(String service) {
            this.service.set(service);
        }

        private String getTime() {
            return time.get();
        }

        private void setTime(String time) {
            this.time.set(time);
        }

        public String getMac() {
            return mac.get();
        }

        public SimpleStringProperty macProperty() {
            return mac;
        }

        public void setMac(String mac) {
            this.mac.set(mac);
        }

        public SimpleStringProperty lastIpProperty() {
            return lastIp;
        }

        public String getService() {
            return service.get();
        }

        public SimpleStringProperty serviceProperty() {
            return service;
        }

        public SimpleStringProperty timeProperty() {
            return time;
        }
    }

    @SuppressWarnings("unchecked")
    private ListView<String> getRulesView() {
        return (ListView<String>) getSettingsItem("rulesView");
    }

    /**
     * Connect rules selection to item.
     * @param id item selected
     * @return node of that item
     */
    @SuppressWarnings("for-rollout:NullAway")
    private Node getSettingsItem(String id) {
        AnchorPane settingsOuterAnchor =
                (AnchorPane) mainTabPanel.getTabs().get(2).getContent();
        AnchorPane settingsInnerAnchor =
                (AnchorPane) ((ScrollPane) settingsOuterAnchor.getChildren().get(0)).getContent();
        ObservableList<Node> list = settingsInnerAnchor.getChildren();
        for (Node temp : list) {
            if (temp.getId() == null) {
                continue;
            }
            if (temp.getId().equals(id)) {
                return temp;
            }
        }
        return null;
    }
}
