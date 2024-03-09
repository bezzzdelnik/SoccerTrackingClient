package util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import javafx.application.Platform;
import javafx.scene.control.TextField;
import orad.retalk2.Retalk2ConnectionController;

import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MonoThreadClientHandler implements Runnable {

    private String[] participants0 = new String[30];
    private String[] participants1 = new String[30];
    private String[] participants2 = new String[30];

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

    public MonoThreadClientHandler(Socket client, Retalk2ConnectionController controller, TextField textField, String sceneName) {
        MonoThreadClientHandler.clientDialog = client;
        this.controller = controller;
        this.textField = textField;
        this.sceneName = sceneName;
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
    public boolean isData(String json) {
        try {
            int number = JsonParser.parseString(json).getAsJsonObject().get("number").getAsInt();
            int team = JsonParser.parseString(json).getAsJsonObject().get("number").getAsInt();
            double x = JsonParser.parseString(json).getAsJsonObject().get("x").getAsDouble();
            double y = JsonParser.parseString(json).getAsJsonObject().get("y").getAsDouble();
            double z = JsonParser.parseString(json).getAsJsonObject().get("z").getAsDouble();
            int visible = JsonParser.parseString(json).getAsJsonObject().get("visible").getAsInt();
            String user = JsonParser.parseString(json).getAsJsonObject().get("user").getAsString();
        } catch (NullPointerException e) {
            return false;
        }
        return true;
    }


    @Override
    public void run() {


        try {
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
                String entry = reader.readLine();    // reads a line of text
                fileout.println(dateFormat.format(new Date()) + " " + entry);
                String[] data = entry.split(";");

                for (int i=0; i < data.length; i++) {
                    String d = data[i].replace(";", "");
                    System.out.println(dateFormat.format(new Date()) + " " + d);
                    new Thread(() -> Platform.runLater(() -> textField.setText(dateFormat.format(new Date()) + " " + d))).start();
                    writer.flush();
                    if (isValid(d) && controller.isConnected()) {
                        if (d.contains("number")) {
                            sendParticipants(d);
                        }
                        if (d.contains("visible")) {
                            sendCoordinates(d);
                        }
                    }
                }



                // и выводит в консоль

                // инициализация проверки условия продолжения работы с клиентом
                // по этому сокету по кодовому слову - quit в любом регистре
                if (entry.equalsIgnoreCase("quit")) {

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
        //System.out.println(String.format("index = %s, team = %s, number = %s, name = %s", index, team, number, name));
    }

    private void initParticipants() {
        for (int i=0; i< 30; i++) {
            participants0[i] = "0";
            participants1[i] = "0";
            participants2[i] = "0";
        }
    }

    private void sendCoordinates(String data) {
        JsonObject parser = JsonParser.parseString(data).getAsJsonObject();
        Integer _index = parser.get("index").getAsInt();
        String index;
        if ( (_index + 1) < 10) {
            index = "0" + (_index + 1);
        } else index = String.valueOf(_index + 1);
        String team = parser.get("team").getAsString();
        String x = parser.get("x").getAsString();
        String y = parser.get("y").getAsString();
        String z = parser.get("z").getAsString();
        String visible = parser.get("visible").getAsString();
        String user = parser.get("user").getAsString();
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
        //System.out.println(String.format("index = %s, team = %s, x = %s, y = %s, z = %s, visible = %d, user = %s", index, team, x, y, z, visible, user));
    }
}