package net.lxy520.javafx;

import com.alibaba.fastjson.JSON;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.lxy520.config.Setting;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

public class Controller {

    @FXML
    private TextField txtUserName;
    @FXML
    private PasswordField txtPasswd;
    @FXML
    private TextField txtStart;
    @FXML
    private TextField txtInterval;
    @FXML
    private WebView webView;
    @FXML
    private TextArea logBox;

    private WebEngine webEngine;

    private Setting setting = new Setting();

    public Controller() {
        System.out.println("程序启动中......");
    }

    private static final String restart = "function doaction(){\n" +
            "    KD.enableLogs();\n" +
            "    console.info(\"开始检测....\");\n" +
            "    if(!KD.isLoggedIn()){\n" +
            "        console.info(\"帐号未登陆,检测当前是否在登陆页面....\");\n" +
            "        if(!(location.href==\"https://koding.com/Login\")&&!(location.href==\"https://koding.com/#!/Login\")){\n" +
            "            console.info(\"不在登陆页面,正在打开登陆页面....\");\n" +
            "            location.href=\"https://koding.com/Login\"\n" +
            "        }else{\n" +
            "            console.info(\"当前正在登陆界面,登陆中.....\");\n" +
            "            $(\"input[name=username][testpath=login-form-username]\").val(\"{{ username }}\");\n" +
            "            $(\"input[name=password][testpath=login-form-password]\").val(\"{{ userpasswd }}\");\n" +
            "            $(\"button[testpath=login-button]\").trigger(\"submit\");\n" +
            "        }\n" +
            "    }else{\n" +
            "        console.info(\"帐号已在线,检测是否需要重启VM\");\n" +
            "        if($(\"button.kdbutton.turn-on.state-button.solid.green.medium.with-icon\").prop(\"outerHTML\")){\n" +
            "            console.info(\"VM已经关闭,正在重启VM....\");\n" +
            "            $(\"button.kdbutton.turn-on.state-button.solid.green.medium.with-icon\").trigger(\"click\");\n" +
            "        }else{\n" +
            "            console.info(\"VM已生动,不需要重启.\");\n" +
            "        }\n" +
            "    }\n" +
            "}\n" +
            "var restart=self.setInterval(\"doaction()\", {{ time }});";

            private static final String worker = "function worker(){\n" +
            "    KD.enableLogs();\n" +
            "    location.href=\"https://koding.com/IDE\";\n" +
            "}\n" +
            "var workerright = self.setTimeout(\"worker()\", {{ time }});";
    /**
     * 读取用户信息
     */
    public void readSetting() {
        try {
            File file = new File("setting.json");
            if (file.exists()) {
               List<String> strings = Files.readAllLines(file.toPath());
                if (strings!=null && !strings.isEmpty()) {
                    Setting set = JSON.parseObject(strings.get(0), Setting.class);//反序列化
                    if (set != null) {
                        setSetting(set);//设置
                        loadSetting();//加载
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载用户设置
     */
    public void loadSetting() {
        if (setting != null) {
            try {
                logBox.setText("加载用户设置......\n\n");
                txtUserName.setText(setting.getUserName());
                txtPasswd.setText(setting.getPasswd());
                txtStart.setText(setting.getStart());
                txtInterval.setText(setting.getInterval());
                logBox.appendText("加载用户设置成功\n\n");
            } catch (Exception e1) {
                logBox.appendText("加载用户设置失败\n\n");
            }
        }
    }

    /**
     * 保存软件设置
     */
    private void saveSetting() {
        try {//保存程序设置信息
            setting.setUserName(txtUserName.getText());
            setting.setPasswd(txtPasswd.getText());
            setting.setStart(txtStart.getText());
            setting.setInterval(txtInterval.getText());
            String json = JSON.toJSONString(setting);
            ByteBuffer buffer = ByteBuffer.allocate(json.getBytes().length);
            buffer.put(json.getBytes());
            FileOutputStream fout = new FileOutputStream("setting.json");
            FileChannel fc = fout.getChannel();
            buffer.flip();
            fc.write(buffer);
            fc.close();
            fout.close();
            logBox.appendText("保存用户设置成功\n\n");
        } catch (Exception e) {
            logBox.appendText("保存用户设置失败\n\n");
        }
    }

    @FXML
    public void start(ActionEvent e) {
        if (txtUserName == null || "".equals(txtUserName.getText())) {
            logBox.appendText("用户名不能为空\n");
            showMessage("提示", "用户名不能为空");
            return;
        }
        if (txtPasswd == null || "".equals(txtPasswd.getText())) {
            logBox.appendText("密码不能为空\n");
            showMessage("提示", "密码不能为空");
            return;
        }
        saveSetting();
        getWebEngine().load("https://koding.com/IDE");
        getWebEngine().reload();
        logBox.appendText("系统启动中......\n");
        getWebEngine().getLoadWorker().stateProperty().addListener(new ChangeAction());
        getWebEngine().getLoadWorker().messageProperty().addListener(new ChangeAction());
    }

    /**
     * 临听网页加载
     */
    private class ChangeAction implements ChangeListener {
        @Override
        public void changed(ObservableValue observable, Object oldValue, Object newValue) {
            logBox.appendText("页面有变动,刷新中.\n");
            if (newValue == Worker.State.SUCCEEDED) {
                String location = getWebEngine().executeScript("location.href").toString();
                String interval = "500";
                if (location.indexOf("IDE")>0){
                    interval = txtInterval.getText()+"*1000*60";
                }
                logBox.appendText("页面加载成功,执行JS代码......\n");
                String reStart = restart.replaceAll("\\{\\{ username }\\}",txtUserName.getText()).replaceAll("\\{\\{ userpasswd \\}\\}",txtPasswd.getText()).replaceAll("\\{\\{ time \\}\\}",interval);
                getWebEngine().executeScript(reStart);
            }else {
                String location = getWebEngine().executeScript("location.href").toString();
                if (location.indexOf("IDE")>0){
                    String iworker = worker.replaceAll("\\{\\{ time \\}\\}",txtInterval.getText()+"*1000*60");
                    getWebEngine().executeScript(iworker);
                }
                logBox.appendText(ZonedDateTime.now().toLocalTime()+":"+newValue.toString()+"\n");
            }
        }
    }

    /**
     * 配置信息
     *
     * @param setting
     */
    public void setSetting(Setting setting) {
        this.setting = setting;
    }

    public WebEngine getWebEngine() {
        webView.getEngine().setUserAgent("Mozilla/5.0 (ArchLinux Linux 3.16) AppleWebKit/537.36 (KHTML, like Gecko) Maxthon/4.0 Chrome/30.0.1599.101 Safari/537.36");
        return webView.getEngine();
    }

    /**
     * 消息框
     *
     * @param message 消息
     */
    private void showMessage(String title, String message) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        VBox v = new VBox();
        v.setAlignment(Pos.CENTER);
        Label l = new Label(message);
        l.setPadding(new Insets(0, 0, 10, 0));
        Button btn = new Button("确定");
        btn.setOnAction(e -> stage.close());
        v.getChildren().addAll(l, btn);
        Scene s = new Scene(v, 300, 100);
        stage.setScene(s);
        stage.setTitle(title);
        stage.show();
    }
}
