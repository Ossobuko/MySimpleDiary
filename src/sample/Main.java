package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import org.apache.commons.io.IOUtils;
import sun.awt.image.ImageWatched;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.print.DocFlavor;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.crypto.dsig.spec.ExcC14NParameterSpec;
import java.awt.*;
import java.awt.ScrollPane;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.Key;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main extends Application {
    // universal variables
    private static BorderPane root;
    private static HBox hBox;
    private static Button saveButton;
    private static Text infoBox;

    // diary variables
    private static String password = "";
    private static File currentFile = null;

    private static LinkedHashMap<String, String> map = new LinkedHashMap<>();
    private static String currentKey;
    private static TextArea textArea;
    private static VBox vbox;


    @Override
    public void start(Stage primaryStage) throws Exception {
        root = new BorderPane();
        root.setTop(horizontalBox());

        root.setBottom(infoBox = new Text());
        primaryStage.setTitle("My Simple Secret Diary");
        Scene scene = new Scene(root,1000,580);
        scene.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                if (db.hasFiles()) {
                    event.acceptTransferModes(TransferMode.COPY);
                } else {
                    event.consume();
                }
            }
        });
        scene.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasFiles()) {
                    success = true;
                    String filePath = null;
                    for (File file:db.getFiles()) {
                        askPasswordLoadFile(file);
                        break;
                    }
                }
                event.setDropCompleted(success);
                event.consume();
            }
        });
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private static void start (LinkedHashMap<String,String> tmp) {
        for (Node n: hBox.getChildren())
            n.setDisable(false);
        // set map and current Key
        map = tmp;
        map.remove("null");
        try {
            currentKey = new ArrayList<String>(tmp.keySet()).get(0);
        } catch (Exception e){
        }

        // create textArea
        textArea = new TextArea(map.get(currentKey));
        textArea.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                saveButton.setDisable(false);
            }
        });
        textArea.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) {
                if (!newPropertyValue) textArea.requestFocus();
            }
        });
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                textArea.requestFocus();
            }
        });
        root.setCenter(textArea);

        // create vBox
        vbox = new VBox();
        vbox.setFillWidth(true);
        for (String key : map.keySet()) {
            if (key.equals("null")) continue;
            vbox.getChildren().add(getButton(key));
        }
        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(vbox);
        sp.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.ALWAYS);
        root.setLeft(sp);

        // initiate currentKey
        setCurrentKey(currentKey);

        infoBox.setText(map.entrySet().size() + " entries are loaded.");
    }

    private static Button getButton(String key) {
        final Button b = new Button(key);
        b.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                setCurrentKey(b.getText());
            }
        });
        return b;
    }

    /**
     * set the given key as the current key, and also makes necessary UI adjustments
     */
    private static void setCurrentKey(String newKey) {
        // save the old one
        map.put(currentKey, textArea.getText());
        // refresh
        textArea.setText(map.get(currentKey = newKey));
        for (Node node : vbox.getChildren()) {
            Button b = (Button) node;
            if (b.getText().equals(newKey)) b.setStyle("-fx-background-color: aquamarine;");
            else b.setStyle("-fx-background-color: white;");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static HBox horizontalBox () {
        hBox = new HBox();

        Button newDay = new Button("Add New Entry");
        newDay.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (currentFile == null) return;
                String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm.ss").format(new Date());
                if (map.keySet().contains(timeStamp)) return;
                map.put(timeStamp, "");
                vbox.getChildren().add(getButton(timeStamp));
                setCurrentKey(timeStamp);
                infoBox.setText("New entry added. " + map.entrySet().size() + " total entries.");
            }
        });
        newDay.setDisable(true);
        hBox.getChildren().add(newDay);

        Button removeCurrentDay = new Button("Remove Selected Entry");
        removeCurrentDay.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (currentFile == null) return;
                try {
                    // delete from map
                    map.remove(currentKey);
                    // delete the button
                    Iterator<Node> iterator = vbox.getChildren().iterator();
                    while (iterator.hasNext())
                        if (((Button) iterator.next()).getText().equals(currentKey)) iterator.remove();
                    // clear textArea
                    textArea.setText("");
                    // clear current key
                    currentKey = null;
                    infoBox.setText("Entry removed. " +map.entrySet().size() + " total entries.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        removeCurrentDay.setDisable(true);
        hBox.getChildren().add(removeCurrentDay);

        hBox.getChildren().add(new Separator());

        Button newFile = new Button("NEW DIARY");
        newFile.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                createNew();
            }
        });
        newFile.setStyle("-fx-text-fill: deepskyblue ;");
        newFile.setDisable(false);
        hBox.getChildren().add(newFile);

        Button load = new Button("LOAD DIARY");
        load.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                chooseLoadFile();
            }
        });
        load.setStyle("-fx-text-fill: crimson;");
        hBox.getChildren().add(load);

        saveButton = new Button("SAVE DIARY");
        saveButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (currentFile == null) return;
                try {
                    save();
                    infoBox.setText("Diary saved. " +map.entrySet().size() + " total entries.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        saveButton.setDisable(true);
        saveButton.setStyle("-fx-text-fill: darkcyan;");
        hBox.getChildren().add(saveButton);

        return hBox;
    }


    /** save the changes to the file */
    private static void save () {
        saveButton.setDisable(true);
        try {
            map.put(currentKey,textArea.getText());
            byte byteArray[] = encrypt(map,password);
            Files.write(currentFile.toPath(), byteArray, StandardOpenOption.CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * creates a new .diary and opens it. on Desktop. Only asks for password
     */
    private static void createNew () {
        // ask for password
        PasswordDialog pd = new PasswordDialog();
        Optional<String> result = pd.showAndWait();
        result.ifPresent(password -> {
            try {
                // create the file
                File file = new File(System.getProperty("user.home") + "/Desktop/"+Math.abs(Math.random())+".diary");
                // encrypt an empty map
                byte byteArray[] = encrypt(new LinkedHashMap<String,String>(),password);
                // save it to desktop
                Files.write(file.toPath(), byteArray, StandardOpenOption.CREATE);
                // decrypt and load it
                LinkedHashMap<String,String> map = decrypt(Main.currentFile = file, Main.password = password);
                start(map);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * user chooses a file and that is loaded
     */
    private static void chooseLoadFile() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ex) {
                }
                JFileChooser chooser = new JFileChooser(System.getProperty("user.home") + "/Desktop/") {
                    @Override
                    protected JDialog createDialog(Component parent)
                            throws HeadlessException {
                        JDialog dialog = super.createDialog(parent);
                        // config here as needed - just to see a difference
                        dialog.setLocationByPlatform(true);
                        // might help - can't know because I can't reproduce the problem
                        dialog.setAlwaysOnTop(true);
                        return dialog;
                    }
                };
                chooser.setFileFilter(new FileNameExtensionFilter("Diary Files", "diary"));
                chooser.setDialogTitle("Choose the diary file");
                chooser.setMultiSelectionEnabled(false);
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    askPasswordLoadFile(chooser.getSelectedFile());
                }
            }
        });
    }

    /** ask password for the file and load it */
    private static void askPasswordLoadFile (File f) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                PasswordDialog pd = new PasswordDialog();
                Optional<String> result = pd.showAndWait();
                result.ifPresent(password -> {
                    LinkedHashMap<String,String> map = decrypt(Main.currentFile = f, Main.password = password);
                    start(map);
                });
            }
        });
    }

    private static byte[] encrypt(LinkedHashMap<String, String> map, String password) {
        try {
            Key key = new SecretKeySpec(getKey(password), "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, key);
            byte[] encValue = c.doFinal(toString(map).getBytes());
            return new BASE64Encoder().encode(encValue).getBytes();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    private static LinkedHashMap<String, String> decrypt (File file, String password) {
        try {
            Key key = new SecretKeySpec(getKey(password), "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.DECRYPT_MODE, key);
            byte[] decordedValue = new BASE64Decoder().decodeBuffer(new FileInputStream(file));
            byte[] decValue = c.doFinal(decordedValue);
            return toMap(new String(decValue));
        } catch (Exception e) {
            e.printStackTrace();
            return new LinkedHashMap<>();
        }
    }

    private static String toString(LinkedHashMap<String,String> map) {
        String s = "";
        for (String key: map.keySet()) {
            s += "$$$$$$$$$$" + key + "\n";
            s += map.get(key) + "\n";
        }
        return s;
    }

    private static LinkedHashMap<String,String> toMap (String s) {
        LinkedHashMap<String,String> map = new LinkedHashMap<>();
        Scanner scanner = new Scanner(s);
        String key = "error";
        while (scanner.hasNextLine()) {
            String tmp = scanner.nextLine();
            if (tmp.startsWith("$$$$$$$$$$")) key = tmp.replace("$$$$$$$$$$","");
            else {
                if (map.containsKey(key)) map.put(key,map.get(key) + "\n" + tmp);
                else map.put(key,tmp);
            }
        }
        scanner.close();
        return map;
    }

    private static byte[] getKey (String password) {
        byte[] bytes = new byte[16];
        byte[] bytes2 = password.getBytes();
        for (int a=0; a<16; a++) if (a<bytes2.length) bytes[a] = bytes2[a];
        return bytes;
    }


}
