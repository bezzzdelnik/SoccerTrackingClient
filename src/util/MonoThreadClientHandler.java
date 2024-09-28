package util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import orad.retalk2.Retalk2ConnectionController;

import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MonoThreadClientHandler implements Runnable {

    private String[] participants0 = new String[100];
    private String[] participants1 = new String[100];
    private String[] participants2 = new String[100];

    private static Socket clientDialog;
    private Retalk2ConnectionController controller;

    FileWriter fw;

    {
        try {
            fw = new FileWriter("./log.txt", true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    BufferedWriter bw = new BufferedWriter(fw);
    PrintWriter fileout = new PrintWriter(bw);

    private TextField textField;
    private String sceneName;
    private TextArea logArea;
    private Label clientAdress;

    public MonoThreadClientHandler(Socket client, Retalk2ConnectionController controller, TextArea logArea, String sceneName, Label clientAdress) {
        MonoThreadClientHandler.clientDialog = client;
        this.controller = controller;
        this.logArea = logArea;
        this.sceneName = sceneName;
        this.clientAdress = clientAdress;
        initParticipants();
    }

    private DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    public boolean isValid(String json) {
        try {
            JsonParser.parseString(json).getAsJsonObject();
        } catch (IllegalStateException e) {
            return false;
        } catch (JsonSyntaxException e) {
            return false;
        }
        return true;
    }
    public String[] getData(String json) {
        JsonObject parser = JsonParser.parseString(json).getAsJsonObject();
        String[] data = new String[9];
        data[0] = "0";
        data[1] = "";
        data[2] = "";
        data[3] = "";
        data[4] = "";
        data[5] = "";
        data[6] = "";
        data[7] = "";
        data[8] = "";
        try {
            data[0] = parser.get("index").getAsString();
        } catch (NullPointerException e) {
            System.out.println(e);
        }
        try {
            data[1] = parser.get("team").getAsString();
        } catch (NullPointerException e) {
            System.out.println(e);
        }
        try {
            data[2] = parser.get("number").getAsString();
        } catch (NullPointerException e) {
            System.out.println(e);
        }
        try {
            data[3] = parser.get("name").getAsString();
        } catch (NullPointerException e) {
            System.out.println(e);
        }
        try {
            data[4] = parser.get("x").getAsString();
        } catch (NullPointerException e) {
            System.out.println(e);
        }
        try {
            data[5] = parser.get("y").getAsString();
        } catch (NullPointerException e) {
            System.out.println(e);
        }
        try {
            data[6] = parser.get("z").getAsString();
        } catch (NullPointerException e) {
            System.out.println(e);
        }
        try {
            data[7] = parser.get("visible").getAsString();
        } catch (NullPointerException e) {
            System.out.println(e);
        }
        try {
            data[8] = parser.get("user").getAsString();
        } catch (NullPointerException e) {
            System.out.println(e);
        }
        return data;
    }


    @Override
    public void run() {


        try {
            new Thread(() -> Platform.runLater(() -> clientAdress.setText("Установлено соединение - " + clientDialog.getInetAddress().getHostAddress()))).start();
            // инициируем каналы общения в сокете, для сервера

            // канал записи в сокет следует инициализировать сначала канал чтения для избежания блокировки выполнения программы на ожидании заголовка в сокете
            OutputStream out = clientDialog.getOutputStream();

            // канал чтения из сокета
            InputStream in = clientDialog.getInputStream();
            System.out.println("DataInputStream created");

            System.out.println("DataOutputStream  created");
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // основная рабочая часть //
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            // начинаем диалог с подключенным клиентом в цикле, пока сокет не
            // закрыт клиентом
            while (!clientDialog.isClosed()) {
                //System.out.println("Server reading from channel");
                // серверная нить ждёт в канале чтения (inputstream) получения
                // данных клиента после получения данных считывает их
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
                try {
                    String entry = reader.readLine();    // reads a line of text
                    if (!entry.isEmpty()) {
                        fileout.println(dateFormat.format(new Date()) + " " + entry);
                        String[] data;
                        if (entry.contains(";")) {
                            data = entry.split(";");
                        } else {
                            data = new String[1];
                            data[0] = entry;
                        }
                        StringBuilder log = new StringBuilder();
                        for (String splitData : data) {
                            String d = splitData.replace(";", "");
                            log.append(d).append("\r\n");
                            //writer.flush();
                            if (isValid(d) && controller.isConnected()) {
                                sendCoordinates(getData(d));
                            }
                        }
                        new Thread(() -> Platform.runLater(() -> logArea.setText(dateFormat.format(new Date()) + "\r\n" + log))).start();
                        // и выводит в консоль
                    }

                    // инициализация проверки условия продолжения работы с клиентом
                    // по этому сокету по кодовому слову - quit в любом регистре
                    if (entry.equalsIgnoreCase("quit") || entry.equalsIgnoreCase("exit")) {
                        // если кодовое слово получено то инициализируется закрытие
                        // серверной нити
                        System.out.println("Client initialize connections suicide ...");
                        writer.write("Server reply - " + entry + " - OK");
                        break;
                    }

                    // если условие окончания работы не верно - продолжаем работу -
                    // отправляем эхо обратно клиенту
                    //System.out.println("Server try writing to channel");
                    writer.write("Server reply - " + entry + " - OK");
                    //System.out.println("Server Wrote message to clientDialog.");
                    // освобождаем буфер сетевых сообщений
                    writer.flush();

                    // возвращаемся в началло для считывания нового сообщения
                } catch (NullPointerException ex) {
                    System.out.println();
                    break;
                }
            }
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // основная рабочая часть //
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // если условие выхода - верно выключаем соединения
            System.out.println("Client disconnected");
            System.out.println("Closing connections & channels.");
            // закрываем сначала каналы сокета !
            in.close();
            out.close();
            // потом закрываем сокет общения с клиентом в нити моносервера
            clientDialog.close();
            new Thread(() -> Platform.runLater(() -> clientAdress.setText("Клиент " + clientDialog.getInetAddress().getHostAddress() + " отключился"))).start();
            System.out.println("Closing connections & channels - DONE.");
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendParticipants(String data) {
        JsonObject parser = JsonParser.parseString(data).getAsJsonObject();
        Integer _index = parser.get("index").getAsInt();
        String index;
        if ( (_index + 1) < 10) {
            index = "0" + (_index + 1);
        } else index = String.valueOf(_index + 1);
        String team = parser.get("team").getAsString();
        String number =parser.get("number").getAsString();
        String name = parser.get("name").getAsString();
        controller.sendSetExport(sceneName, String.format("Geometry_T-0%s-N-0%s_Input_String", team, index), number);
        controller.sendSetExport(sceneName, String.format("Geometry_T-0%s-P-0%s_Input_String", team, index), name);
        System.out.println(String.format("index = %s, team = %s, number = %s, name = %s", index, team, number, name));
    }

    private void initParticipants() {
        for (int i=0; i< 100; i++) {
            participants0[i] = "0";
            participants1[i] = "0";
            participants2[i] = "0";
        }
    }

    private void sendCoordinates(String[] data) {
        int _index = Integer.parseInt(data[0]);
        String index;
        if ( (_index + 1) < 10) {
            index = "0" + (_index + 1);
        } else index = String.valueOf(_index + 1);
        String team = data[1];
        String number = data[2];
        String name = data[3];
        String x = data[4];
        String y = data[5];
        String z = data[6];
        String visible = data[7];
        String user = data[8];
        controller.sendSetExport(sceneName, String.format("Geometry_T-0%s-N-0%s_Input_String", team, index), number);
        controller.sendSetExport(sceneName, String.format("Geometry_T-0%s-P-0%s_Input_String", team, index), name);
        controller.sendSetExport(sceneName, String.format("Transformation_TEAM-0%s-Player-0%s_Position_X", team, index), x);
        //controller.sendSetExport(sceneName, String.format("Transformation_TEAM-0%s-Player-0%s_Position_Y", team, index), y);
        controller.sendSetExport(sceneName, String.format("Transformation_TEAM-0%s-Player-0%s_Position_Z", team, index), z);
        if (team.equals("0")) {
            if (!visible.equals(participants0[_index])) {
                controller.sendSetExport(sceneName, String.format("Object_TEAM-0%s-Player-0%s_Object_Visible", team, index), visible);
                participants0[_index] = visible;
            }
        }
        if (team.equals("1")) {
            if (!visible.equals(participants1[_index])) {
                controller.sendSetExport(sceneName, String.format("Object_TEAM-0%s-Player-0%s_Object_Visible", team, index), visible);
                participants1[_index] = visible;
            }
        }
        if (team.equals("2")) {
            if (!visible.equals(participants2[_index])) {
                controller.sendSetExport(sceneName, String.format("Object_TEAM-0%s-Player-0%s_Object_Visible", team, index), visible);
                participants2[_index] = visible;
            }
        }
        System.out.println(String.format("index = %s, name = %s, number = %s, team = %s, x = %s, y = %s, z = %s, visible = %s, user = %s", index, name, number, team, x, y, z, visible, user));
    }
}