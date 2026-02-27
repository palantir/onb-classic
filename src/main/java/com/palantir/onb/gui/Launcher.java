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
import com.palantir.onb.Manager;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.net.URL;
import java.util.Optional;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
// CHECKSTYLE.ON: IllegalImport

/**
 * GUI launcher.
 */
public class Launcher extends Application {

    /**
     * Start the Gui.
     * @param stage - stage to use
     * @throws Exception - Error starting stage
     */
    @Override
    public void start(Stage stage) throws Exception {
        if (Taskbar.isTaskbarSupported()) {
            try {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    java.awt.Image image =
                            Toolkit.getDefaultToolkit().getImage(getClass().getResource("/logo.png"));
                    taskbar.setIconImage(image);
                }
            } catch (Exception e) {
                Manager.getManagerLogger().simpleReport("Error setting tray icon. " + e.getMessage(), true);
            }
        }

        Optional<URL> guiObject = Optional.ofNullable(getClass().getResource("/gui.fxml"));
        if (guiObject.isPresent()) {
            Parent root = FXMLLoader.load(guiObject.get());
            stage.setTitle("ONB-Classic");
            stage.setScene(new Scene(root, 800, 600));
            stage.setResizable(true);
            stage.show();
        } else {
            Manager.getManagerLogger().simpleReport("Failure to load GUI. Object not found.", true);
        }
    }

    /**
     * Call to open the scene.
     * @param args arguments for app
     */
    public void open(String[] args) {
        Application.launch(args);
    }
}
