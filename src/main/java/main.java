import FileBrowser.FileTreeItem;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class main extends Application {
    LoginSystem loginSystem = new LoginSystem("testFiles/login.info");
    Client client = null;
    Server server = null;
    Thread srvrThread = null;
    final TextArea textArea = new TextArea(), serverLogger = new TextArea();
    final ProgressBar localProg = new ProgressBar(0), remoteProg = new ProgressBar(0);
    final Label serverStatus = new Label(), clientStatus = new Label();
    final ImageView imOn = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("on_button.png")))),
            imOff = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("off_button.png"))));

    public void append(String str) {textArea.appendText(str + "\n");}

    public void logServer(String str) { synchronized (serverLogger) {serverLogger.appendText(str + "\n");} }

    public void updateLocalProg(double percent) { synchronized (localProg) { localProg.setProgress(percent); } }

    public void updateRemoteProg(double percent) { synchronized (remoteProg) { remoteProg.setProgress(percent); } }

    public void updateStatus(boolean stat) {
        if (stat) {
            if (server != null) {
                serverStatus.setText("Server Status: Connected ");
                serverStatus.setGraphic(imOn);
            }
            if (client != null) {
                clientStatus.setText("Status: Connected ");
                clientStatus.setGraphic(imOn);
            }
        } else {
            if (server != null) {
                serverStatus.setText("Server Status: Off ");
                serverStatus.setGraphic(imOff);
            }
            if (client != null) {
                clientStatus.setText("Status: Disconnected ");
                clientStatus.setGraphic(imOff);
            }
        }
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("My Server Thing");

        //create scenes
        GridPane login = new GridPane();
        VBox home = new VBox();
        VBox serverPage = new VBox();
        VBox clientPage = new VBox();

        String address = "localhost"; //default vars
        int port = 6666;

        //set images
        imOn.setFitHeight(10);
        imOn.setFitWidth(10);
        imOff.setFitWidth(10);
        imOff.setFitHeight(10);

        //login page
        {
            ImageView logo = new ImageView(new Image("icon_dark.png")); //logo
            logo.setFitHeight(40);
            logo.setFitWidth(40);
            login.add(logo, 0, 0);

            Text scenetitle = new Text("Welcome"); //splash text
            scenetitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
            VBox myText = new VBox(scenetitle);
            myText.setAlignment(Pos.CENTER_RIGHT);
            login.add(myText, 1, 0);

            login.add(new Label("Username:"), 0, 1);
            TextField userField = new TextField();
            login.add(userField, 1, 1);

            login.add(new Label("Password:"), 0, 2);
            PasswordField passField = new PasswordField();
            login.add(passField, 1, 2);

            Button loginBtn = new Button("Sign in");
            Button regBtn = new Button("Register");
            HBox hbBtn = new HBox(10, regBtn, loginBtn);
            hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
            login.add(hbBtn, 1, 4);

            final Text errorLog = new Text();
            errorLog.setFill(Color.FIREBRICK);
            login.add(errorLog, 0, 4, 2, 1);

            regBtn.setOnAction(e -> {
                if (loginSystem.add(userField.getText(), passField.getText())) {
                    loginSystem.regUsername(userField.getText());
                    loginSystem.save();
                    primaryStage.getScene().setRoot(home);
                }
                else errorLog.setText("Username taken.");
            });

            loginBtn.setOnAction(e -> {
                if (loginSystem.check(userField.getText(), passField.getText())) {
                    loginSystem.regUsername(userField.getText());
                    primaryStage.getScene().setRoot(home);
                }
                else errorLog.setText("Invalid input.");
            });

            userField.setOnKeyPressed(keyEvent -> {if (keyEvent.getCode() == KeyCode.ENTER) passField.requestFocus();});
            passField.setOnKeyPressed(keyEvent -> {if (keyEvent.getCode() == KeyCode.ENTER) loginBtn.fire();});

            //scene
            login.setAlignment(Pos.CENTER);
            login.setHgap(10);
            login.setVgap(10);
            login.setPadding(new Insets(25));
        }

        //main menu
        Button serverBtn = new Button("Server");
        {
            serverBtn.setPrefSize(200, 10);

            Button clientBtn = new Button("Client");
            clientBtn.setPrefSize(200, 10);
            clientBtn.setOnAction(e -> {
                client = new Client(port, this, address);
                updateStatus(client.getConnection());
                primaryStage.getScene().setRoot(clientPage);
            });

            //set main scene
            home.setAlignment(Pos.CENTER);
            home.setSpacing(10);
            home.getChildren().addAll(new Label("Which node would you like to create?"), serverBtn, clientBtn);
            home.setPadding(new Insets(25));
        }

        //Server
        {
            serverPage.setPadding(new Insets(20));

            Button homeBtn = new Button("Home");
            homeBtn.setCancelButton(true);
            homeBtn.setPrefSize(50, 10);

            homeBtn.setOnAction(e -> {
                if (server != null){
                    server.end();
                    server = null;
                }
                textArea.clear();
                serverLogger.clear();
                primaryStage.getScene().setRoot(home);
            });

            TextField textField = new TextField(Integer.toString(port));
            textField.setPrefWidth(50);
            UnaryOperator<TextFormatter.Change> modifyChange = c -> {
                if (c.isContentChange() && (c.getControlNewText().length() > 5 || !Pattern.matches("^[0-9]*$", c.getControlNewText()))) {
                    c.setText(c.getControlText());
                    c.setRange(0, c.getControlText().length());
                }
                return c;
            };
            textField.setTextFormatter(new TextFormatter<>(modifyChange));

            Button nodeBtn = new Button("Set Port");
            textField.setOnKeyPressed(keyEvent -> {if (keyEvent.getCode() == KeyCode.ENTER) nodeBtn.fire();});

            //---------------------------------------------------------------

            textArea.setEditable(false);
            textArea.setMinWidth(150);
            VBox.setVgrow(textArea, Priority.SOMETIMES);
            Label serverLog = new Label("New server started.");
            serverLogger.setMinWidth(150);
            serverLogger.setPrefWidth(180);

            nodeBtn.setOnAction(action -> {
                if (server == null) serverLog.setText("Server issue. Please contact the developer.");
                else if (textField.getText().equals("")){
                    serverLog.setText("No valid port found. Please try a different port.");
                    textField.setText(Integer.toString(server.getPort()));
                } else if (Integer.parseInt(textField.getText()) == server.getPort()) serverLog.setText("Already connected to that port.");
                else if (!server.connect(Integer.parseInt(textField.getText()))) {
                    serverLog.setText("No valid port found. Please try a different port.");
                    textField.setText(Integer.toString(server.getPort()));
                } else {
                    textArea.clear();
                    serverLogger.clear();
                    serverLog.setText("Connected to a new port.");
                }
                updateStatus(server.getConnection());
            });

            serverBtn.setOnAction(e -> {
                server = new Server(port, this);
                srvrThread = new Thread(server);
                srvrThread.start();
                primaryStage.getScene().setRoot(serverPage);
                updateStatus(server.getConnection());
                if (server == null || !server.getConnection()) serverLog.setText("The server is not connected to a valid port.");
            });

            VBox temp1 = new VBox(new ToolBar(homeBtn, textField, nodeBtn), textArea, serverLog);
            HBox temp2 = new HBox(15, temp1, new VBox(new Label("Client Status"), serverLogger, serverStatus));
            VBox.setVgrow(serverLogger, Priority.SOMETIMES);
            HBox.setHgrow(temp1, Priority.SOMETIMES);
            VBox.setVgrow(temp2, Priority.SOMETIMES);

            serverPage.getChildren().addAll(temp2);
        }

        //Client
        { //Top Toolbar
            clientPage.setSpacing(10);

            Button homeBtn = new Button("Home");
            homeBtn.setCancelButton(true);
            homeBtn.setPrefSize(50, 10);

            homeBtn.setOnAction(e -> {
                if (client != null) {
                    client.end();
                    client = null;
                }
                primaryStage.getScene().setRoot(home);
            });

            TextField textField = new TextField(Integer.toString(port));
            textField.setPrefWidth(50);
            UnaryOperator<TextFormatter.Change> modifyChange = c -> {
                if (c.isContentChange() && (c.getControlNewText().length() > 5 || !Pattern.matches("^[0-9]*$", c.getControlNewText()))) {
                    c.setText(c.getControlText());
                    c.setRange(0, c.getControlText().length());
                }
                return c;
            };
            textField.setTextFormatter(new TextFormatter<>(modifyChange));

            Button nodeBtn = new Button("Set Port");
            textField.setOnKeyPressed(keyEvent -> {if (keyEvent.getCode() == KeyCode.ENTER) nodeBtn.fire();});

            Label errorLog = new Label();

            nodeBtn.setOnAction(action -> {
                if (client == null) errorLog.setText("Client issue. Please contact the developer.");
                else if (textField.getText().equals("") || textField.getText().equals("0")) errorLog.setText("No valid server found. Please try a different port.");
                else if (Integer.parseInt(textField.getText()) == client.getPort() && client.getConnection()) errorLog.setText("Already connected to that port.");
                else if (!client.connect(Integer.parseInt(textField.getText()))) errorLog.setText("No valid server found. Please try a different port.");
                else errorLog.setText("Connected to a new server.");

                updateStatus(client.getConnection());
            });

            Pane expander = new Pane();
            HBox.setHgrow(expander, Priority.ALWAYS);

            //--------------------------------------------------------------- Content

            Image compImg = new Image(Objects.requireNonNull(ClassLoader.getSystemResourceAsStream("computer.png")));

            String hostName = "Local";
            try { hostName += "- " + InetAddress.getLocalHost().getHostName(); } catch (UnknownHostException ignored) {}
            TreeItem<String> rootNode = new TreeItem<>(hostName, new ImageView(compImg));

            for (Path name : FileSystems.getDefault().getRootDirectories()) rootNode.getChildren().add(new FileTreeItem(name, true));
            rootNode.setExpanded(true);

            TreeView<String> localView = new TreeView<>(rootNode);
            HBox.setHgrow(localView, Priority.SOMETIMES);

            localView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (!new File(newValue.getValue()).isDirectory() && !newValue.equals(rootNode))
                    client.sendFiles(Collections.singletonList(new File(((FileTreeItem) newValue).getFullPath())));});

            TreeItem<String> remoteRoot = new TreeItem<>("Remote Repository", new ImageView(compImg));
            for (Path x : FileSystems.getDefault().getPath("testFiles")) remoteRoot.getChildren().add(new FileTreeItem(x, true));
            remoteRoot.setExpanded(true);

            TreeView<String> remoteView = new TreeView<>(remoteRoot);
            HBox.setHgrow(remoteView, Priority.SOMETIMES);

            // -------------------------------------

            localProg.setPrefHeight(25);

            Button detBtn = new Button("Details");
            detBtn.setPrefHeight(25);

            GridPane grid = new GridPane();
            grid.setPadding(new Insets(5));
            grid.setAlignment(Pos.BOTTOM_CENTER);

            grid.add(localProg, 0, 0);
            grid.add(detBtn, 1, 0);
            grid.setPickOnBounds(false);

            StackPane localStack = new StackPane(localView, grid);
            HBox.setHgrow(localStack, Priority.SOMETIMES);

            localProg.prefWidthProperty().bind(localStack.widthProperty().subtract(70));

            remoteProg.setPrefHeight(25);

            Button detBtn2 = new Button("Details");
            detBtn2.setPrefHeight(25);

            GridPane grid2 = new GridPane();
            grid2.setPadding(new Insets(5));
            grid2.setAlignment(Pos.BOTTOM_CENTER);

            grid2.add(remoteProg, 0, 0);
            grid2.add(detBtn2, 1, 0);
            grid2.setPickOnBounds(false);

            StackPane remoteStack = new StackPane(remoteView, grid2);
            HBox.setHgrow(remoteStack, Priority.SOMETIMES);

            remoteProg.prefWidthProperty().bind(remoteStack.widthProperty().subtract(70));

            HBox content = new HBox(localStack, remoteStack);
            VBox.setVgrow(content, Priority.SOMETIMES);

            content.setSpacing(10);
            content.setPadding(new Insets(5, 10, 0, 10));

            //--------------------------------------------------------------- Bottom Toolbar

            Button attachBtn = new Button("+");
            attachBtn.setOnAction(e-> {
                FileChooser explorer = new FileChooser();
                explorer.setInitialDirectory(new File("testFiles/"));
                explorer.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("All Files", "*.*"),
                        new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                        new FileChooser.ExtensionFilter("Comma-Separated Value Files", "*.csv"),
                        new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"),
                        new FileChooser.ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.aac"),
                        new FileChooser.ExtensionFilter("Source Code Files", "*.cpp", "*.h", "*.hpp", "*.java"),
                        new FileChooser.ExtensionFilter("Executable Files", "*.exe")
                );

                List<File> choice = explorer.showOpenMultipleDialog(primaryStage);
                if (choice != null) client.sendFiles(choice);
            });

            TextField message = new TextField();
            HBox.setHgrow(message, Priority.SOMETIMES);

            message.setOnKeyPressed(keyEvent -> { //TODO: redo commands
                if (keyEvent.getCode() == KeyCode.ENTER) {
                    if (keyEvent.isShiftDown()) message.appendText("\n");
                    else if (!client.getConnection()) {
                        nodeBtn.fire();
                        if (!client.getConnection()) {
                            errorLog.setText("Not connected to a server. Please try another port.");

                            int pos = message.getCaretPosition();
                            message.setText(message.getText().substring(0, pos - 1) + message.getText().substring(pos, message.getLength()));
                            message.positionCaret(pos - 1);
                        } else client.send(message.getText());
                    } else if (message.getLength() <= 1) {
                        errorLog.setText("Message is blank!");
                        //message.setText("");
                        message.clear();
                    } else client.send(message.getText());
                }
            });

            content.setFocusTraversable(false);
            message.requestFocus();

            Platform.runLater(message::requestFocus);

            //---------------------------------------------------------------

            clientPage.getChildren().addAll(new ToolBar(homeBtn, textField, nodeBtn, expander, clientStatus), content, new ToolBar(attachBtn, message));
        }

        //start stage
        primaryStage.setScene(new Scene(login, 450, 300));
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            loginSystem.save();
            if (server != null) server.end();
            if (client != null) client.end();
        });
    }
}

/*
Button c = new Button("Load Folder");

c.setOnAction(e-> {
    DirectoryChooser dc = new DirectoryChooser();
    dc.setInitialDirectory(new File("testFiles/"));
    File choice = dc.showDialog(primaryStage);
    if(choice == null || ! choice.isDirectory()) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Could not open directory");
        alert.setContentText("The file is invalid.");

        alert.showAndWait();
    } else {
        System.out.println(choice);
        a.setRoot(getNodesForDirectory(new File("testFiles/")));
    }
});
 */