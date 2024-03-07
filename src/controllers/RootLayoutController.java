package controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import orad.retalk2.Retalk2ConnectionController;
import util.MonoThreadClientHandler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RootLayoutController {
    private Properties properties = new Properties();
    private Stage primaryStage;
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    private Retalk2ConnectionController controller = null;

    @FXML private AnchorPane oradControllerAnchorPane;

    @FXML private TextField reAddress, reCanvas, portField, sceneNameField, vSlotField, logField;

    @FXML private Button startServerButton, stopServerButton;

    private OradController oradConnectionController;

    private String port = "3347";

    @FXML
    private void initialize() {
        loadProperties();

        oradConnectionController = new OradController(oradControllerAnchorPane, this);
        oradConnectionController.init();


        reAddress.textProperty().addListener((observable, oldValue, newValue) ->{
            setProperties("reAddress", newValue);
        });

        reCanvas.textProperty().addListener((observable, oldValue, newValue) ->{
            setProperties("reCanvas", newValue);
        });

        sceneNameField.textProperty().addListener((observable, oldValue, newValue) ->{
            setProperties("sceneName", newValue);
        });

        vSlotField.textProperty().addListener((observable, oldValue, newValue) ->{
            setProperties("vSlot", newValue);
        });

        portField.textProperty().addListener((observable, oldValue, newValue) ->{
            setProperties("port", newValue);
            port = newValue;
        });

        startServerButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                startServer();
            }
        });

        stopServerButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                stopServer();
            }
        });


    }

    static ExecutorService executeIt = Executors.newFixedThreadPool(2);
    private ServerSocket server;
    private BufferedReader br;

    private ArrayList<Socket> clients = new ArrayList<>();



    private void startServer() {
        new Thread(() -> {
            try {
                server = new ServerSocket(Integer.parseInt(port));
                br =new BufferedReader(new InputStreamReader(System.in));
                System.out.println("Server socket created, command console reader for listen to server commands");
                while (!server.isClosed()) {

                    // проверяем поступившие комманды из консоли сервера если такие
                    // были
                    if (br.ready()) {
                        System.out.println("Main Server found any messages in channel, let's look at them.");

                        // если команда - quit то инициализируем закрытие сервера и
                        // выход из цикла раздачии нитей монопоточных серверов
                        String serverCommand = br.readLine();
                        if (serverCommand.equalsIgnoreCase("quit")) {
                            System.out.println("Main Server initiate exiting...");
                            server.close();
                            break;
                        }
                    }

                    // если комманд от сервера нет то становимся в ожидание
                    // подключения к сокету общения под именем - "clientDialog" на
                    // серверной стороне
                    Socket client = server.accept();
                    clients.add(client);

                    // после получения запроса на подключение сервер создаёт сокет
                    // для общения с клиентом и отправляет его в отдельную нить
                    // в Runnable(при необходимости можно создать Callable)
                    // монопоточную нить = сервер - MonoThreadClientHandler и тот
                    // продолжает общение от лица сервера
                    executeIt.execute(new MonoThreadClientHandler(client, controller, logField));
                    System.out.print("Connection accepted.");
                }

                // закрытие пула нитей после завершения работы всех нитей
                executeIt.shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(() -> Platform.runLater(() -> {
            startServerButton.setDisable(true);
            stopServerButton.setDisable(false);
        })).start();
    }

    public void closeApp() {
        //stopServer();
    }

    private void stopServer() {
        if (!server.isClosed()) {
            new Thread(() -> Platform.runLater(() -> {
                startServerButton.setDisable(false);
                stopServerButton.setDisable(true);
            })).start();
            try {
                server.close();
                for (Socket client : clients) {
                    client.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setOradController(Retalk2ConnectionController controller) {
        this.controller = controller;
    }

    public Retalk2ConnectionController getController() {
        return controller;
    }

    private void loadProperties() {
        try (InputStream input = new FileInputStream("config.properties")) {

            properties.load(input);

            reAddress.setText(properties.getProperty("reAddress"));
            reCanvas.setText(properties.getProperty("reCanvas"));
            sceneNameField.setText(properties.getProperty("sceneName"));
            vSlotField.setText(properties.getProperty("vSlot"));
            portField.setText(properties.getProperty("port"));
            port = properties.getProperty("port");


        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void setProperties(String key, String value) {
        properties.setProperty(key, value);
        writeProperties();
    }

    private void writeProperties() {
        try (OutputStream output = new FileOutputStream("config.properties")) {

            properties.store(output, null);

        } catch (IOException io) {
            io.printStackTrace();
        }
    }

}
