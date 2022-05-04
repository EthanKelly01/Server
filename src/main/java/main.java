import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class main extends Application {
    Client client = null;
    Server server = null;
    Thread srvrThread = null;
    final TextArea textArea = new TextArea(), serverLogger = new TextArea();
    final Label serverStatus = new Label();
    final ImageView imOn = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("on_button.png")))),
            imOff = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("off_button.png"))));

    public void append(String str) {
        textArea.appendText(str + "\n");
    }

    public void logServer(String str) { synchronized (serverLogger) {serverLogger.appendText(str + "\n");} }

    public void updateStatus(boolean stat) {
        if (stat) {
            serverStatus.setText("Server Status: Connected ");
            imOn.setFitHeight(10);
            imOn.setFitWidth(10);
            serverStatus.setGraphic(imOn);
        } else {
            serverStatus.setText("Server Status: Off ");
            imOff.setFitWidth(10);
            imOff.setFitHeight(10);
            serverStatus.setGraphic(imOff);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Lab10");

        //create scenes
        VBox home = new VBox();
        VBox serverPage = new VBox();
        VBox clientPage = new VBox();

        String address = "localhost"; //default vars
        int port = 6666;

        //main menu
        Button serverBtn = new Button("Server");
        {
            serverBtn.setPrefSize(200, 10);

            Button clientBtn = new Button("Client");
            clientBtn.setPrefSize(200, 10);
            clientBtn.setOnAction(e -> {
                client = new Client(address, port);
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
                else if (!server.changePort(Integer.parseInt(textField.getText()))) {
                    serverLog.setText("No valid port found. Please try a different port.");
                    textField.setText(Integer.toString(server.getPort()));
                } else {
                    textArea.clear();
                    //synchronized (serverLogger){serverLogger.clear();}
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
        {
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

            //---------------------------------------------------------------

            TextField userName = new TextField("anonymous");
            HBox hbox = new HBox(new Label("Username: "), userName);
            hbox.setAlignment(Pos.TOP_LEFT);
            TextArea message = new TextArea();
            Label errorLog = new Label();

            nodeBtn.setOnAction(action -> {
                if (client == null) errorLog.setText("Client issue. Please contact the developer.");
                else if (textField.getText().equals("") || textField.getText().equals("0")) errorLog.setText("No valid server found. Please try a different port.");
                else if (Integer.parseInt(textField.getText()) == client.getPort() && client.getConnection()) errorLog.setText("Already connected to that port.");
                else if (!client.changePort(Integer.parseInt(textField.getText()))) errorLog.setText("No valid server found. Please try a different port.");
                else errorLog.setText("Connected to a new server.");
            });

            message.setOnKeyPressed(keyEvent -> {
                if (keyEvent.getCode() == KeyCode.ENTER) {
                    if (keyEvent.isShiftDown()) message.appendText("\n");
                    else if (!client.getConnection() || userName.getText().isEmpty()) {
                        if (!client.getConnection())
                            nodeBtn.fire();
                        if (!client.getConnection()) {
                            errorLog.setText("Not connected to a server. Please try another port.");

                            int pos = message.getCaretPosition();
                            message.setText(message.getText().substring(0, pos - 1) + message.getText().substring(pos, message.getLength()));
                            message.positionCaret(pos - 1);
                        } else {
                            if (client.send(userName.getText(), message.getText())){
                                errorLog.setText("");
                                message.setText("");
                            } else {
                                errorLog.setText("The server has disconnected.");
                                message.setText(message.getText().substring(0, message.getLength()-1));
                                message.selectPositionCaret(message.getLength());
                            }
                        }
                    } else if (userName.getText().isEmpty()) {
                        errorLog.setText("Username is blank!");
                        int pos = message.getCaretPosition();
                        message.setText(message.getText().substring(0, pos - 1) + message.getText().substring(pos, message.getLength()));
                        message.positionCaret(pos - 1);
                    } else if (message.getLength() <= 1) {
                        errorLog.setText("Message is blank!");
                        message.setText("");
                    } else if (client.send(userName.getText(), message.getText())){
                        errorLog.setText("");
                        message.setText("");
                    } else {
                        errorLog.setText("The server has disconnected.");
                        message.setText(message.getText().substring(0, message.getLength()-1));
                        message.selectPositionCaret(message.getLength());
                    }
                }
            });

            VBox.setVgrow(message, Priority.SOMETIMES);
            VBox vbox = new VBox(hbox, new Label(), new Label("Message"), message, errorLog);
            VBox.setVgrow(vbox, Priority.SOMETIMES);
            vbox.setPadding(new Insets(20));

            clientPage.getChildren().addAll(new ToolBar(homeBtn, textField, nodeBtn), vbox);
        }

        //start stage
        primaryStage.setScene(new Scene(home, 450, 300));
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            if (server != null) server.end();
            if (client != null) client.end();
        });
    }

    public static void main(String[] args) {launch();} //launch the main JavaFX function
}