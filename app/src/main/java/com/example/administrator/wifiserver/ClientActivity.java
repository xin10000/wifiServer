package com.example.administrator.wifiserver;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.folderselector.FileChooserDialog;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;


public class ClientActivity extends AppCompatActivity implements View.OnClickListener, FileChooserDialog.FileCallback {
    String TAG = "客户端";
    public static final int UPDATE_PROGRESS = 2;
    public static final int CONNECTION_EVENT = 1;
    Button btnSendMsg;
    EditText etSend;

    private MessageThread messageThread;
    private Button btnConnect;
    private Button btnClose;
    private Button btnSendFile;
    boolean connSuccess;
    boolean closeSuccess;
    private String hostAddress = "192.168.43.1";
    Handler sendFileHandler;
    Socket socket;
    PrintWriter writer;
    BufferedReader reader;
    ProgressDialog progressDialog;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == UPDATE_PROGRESS) {
                progressDialog.setProgress(msg.arg1);
                progressDialog.show();
                Log.e(TAG, "handleMessage:setProgress " + msg.arg1);
                if (msg.arg1 >= 100) {
                    progressDialog.setProgress(0);
                    progressDialog.dismiss();
                }
            } else if (msg.what == CONNECTION_EVENT) {
                showMessage((String) msg.obj);
                connSuccess = false;
            } else
                showMessage((String) msg.obj);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);


        btnSendMsg = (Button) findViewById(R.id.btn_send_msg);
        btnConnect = (Button) findViewById(R.id.btn_connect);
        btnClose = (Button) findViewById(R.id.btn_close);
        btnSendFile = (Button) findViewById(R.id.btn_send_file);

        etSend = (EditText) findViewById(R.id.editText);
        setTitle("客户端");
        String ipAddress = getIPAddress();
        Log.e(TAG, "onCreate: " + ipAddress);

        btnSendMsg.setOnClickListener(this);
        btnConnect.setOnClickListener(this);
        btnClose.setOnClickListener(this);
        btnSendFile.setOnClickListener(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMax(100);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCanceledOnTouchOutside(false);
        sendFileQueue();

    }

    private void sendFileQueue() {
        HandlerThread handlerThread = new HandlerThread("send file");
        handlerThread.start();
        sendFileHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Log.e(TAG, "handleMessage: " + Thread.currentThread().getName());
                sendFile((File) msg.obj);
            }
        };
    }


    /**
     * 连接服务器
     *
     * @param port
     * @param hostIp
     * @param name
     */
    public boolean connectServer(int port, String hostIp, String name) {
        // 连接服务器
        try {
            socket = new Socket(hostIp, port);// 根据端口号和服务器ip建立连接

            writer = new PrintWriter(socket.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(socket
                    .getInputStream()));
            // 发送客户端用户基本信息(用户名和ip地址)
            sendMessage(name + "@" + socket.getLocalAddress().toString());
            // 开启接收消息的线程
            messageThread = new MessageThread(reader, writer, socket, handler);
            messageThread.start();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "connectServer: " + "与端口号为：" + port + "    IP地址为：" + hostIp
                    + "   的服务器连接失败!" + "\r\n");
            return false;
        }
    }


    /**
     * 发送消息
     *
     * @param message
     */
    public void sendMessage(final String message) {
        new Thread() {
            @Override
            public void run() {
                if (writer == null) return;
                writer.println(message);
                writer.flush();
            }
        }.start();

    }

    /**
     * 客户端主动关闭连接
     */

    public synchronized boolean closeConnection() {
        try {
            sendMessage("CLOSE");// 发送断开连接命令给服务器

            if (messageThread == null) return false;
//            messageThread.stop();// 停止接受消息线程
            // 释放资源
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null) {
                socket.close();
            }

            return true;
        } catch (IOException e1) {
            e1.printStackTrace();

            return false;
        }
    }


    public String getIPAddress() {
        @SuppressLint("WifiManagerLeak")
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        DhcpInfo info = wifiManager.getDhcpInfo();
        Log.e(TAG, "getIPAddress: " + intIP2StringIP(wifiInfo.getIpAddress()));
        String ipAddress = intIP2StringIP(info.serverAddress);//得到IPV4地址

        System.out.println(info.serverAddress);
        return ipAddress;


    }

    /**
     * 将得到的int类型的IP转换为String类型
     *
     * @param ip
     * @return
     */
    public static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_send_msg) {
            sendMessage(etSend.getText() + "");
        } else if (view.getId() == R.id.btn_connect) {
            if (connSuccess) {
                showMessage("用户已连接！");
                return;
            }
            new Thread() {
                @Override
                public void run() {
                    connSuccess = connectServer(1992, "192.168.43.1", "mj" + new Random().nextInt(100));
                    showMessage(connSuccess ? "连接成功" : "连接失败");

                }
            }.start();


        } else if (view.getId() == R.id.btn_close) {
            closeSuccess = closeConnection();
            showMessage(closeSuccess ? "关闭成功" : "关闭失败");
        } else if (view.getId() == R.id.btn_send_file) {
            new FileChooserDialog.Builder(this)
                    .initialPath(Environment.getExternalStorageDirectory().getPath())  // changes initial path, defaults to external storage directory
                    .mimeType("*/*") // Optional MIME type filter
                    .goUpLabel("返回上一级目录<<") // custom go up label, default label is "..."
                    .show(); // an AppCompatActivity which implements FileCallback

        }
    }


    private void sendFile(final File file) {
        DataOutputStream dataOutputStream = null;
        InputStream fileIS = null;
        ServerSocket fileServerSocket = null;
        Socket fileSocket = null;
        try {
            //发送指令，让服务端准备接收文件
            writer.println("file");
            writer.flush();

            fileServerSocket = new ServerSocket(10087);
            Log.e(TAG, "客户端开启服务: 端口10087 waiting for connection ");
            //2、调用accept()方法开始监听，等待客户端的连接
            fileSocket = fileServerSocket.accept();
            long startTime = System.currentTimeMillis();

//                    String fileName = "lantern-installer-beta.apk";
//                    InputStream fileIS = getAssets().open(fileName);
            String fileName = file.getName();
            long fileLength = file.length();
            fileIS = new FileInputStream(file);


            dataOutputStream = new DataOutputStream(fileSocket.getOutputStream());
            dataOutputStream.writeUTF(fileName);
            dataOutputStream.writeLong(fileLength);
            Log.e(TAG, "已连接，开始发送文件");


            byte[] bytes = new byte[1024 * 1024];
            int length = 0;
            int sendFileLength = 0;
            while ((length = fileIS.read(bytes)) != -1) {
                dataOutputStream.write(bytes, 0, length);
                dataOutputStream.flush();
                sendFileLength += length;
                Log.e(TAG, "run: " + length + " 已发送 " + sendFileLength * 1.0f / fileLength);
                Message message = Message.obtain();
                message.what = UPDATE_PROGRESS;
                message.arg1 = (int) ((sendFileLength * 1.0f / fileLength) * 100);
                handler.sendMessage(message);
            }
            Log.e(TAG, "发送文件完毕 " + (System.currentTimeMillis() - startTime) + " 毫秒");
            showMessage("发送文件完毕,发送用了 " + (System.currentTimeMillis() - startTime) + " 毫秒");
            writer.println("发送文件完毕,发送用了 " + (System.currentTimeMillis() - startTime) + " 毫秒");
            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileIS != null)
                    fileIS.close();
                if (dataOutputStream != null)
                    dataOutputStream.close();
                if (fileSocket != null)
                    fileSocket.close();
                if (fileServerSocket != null)
                    fileServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }


    public void showMessage(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ClientActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onFileSelection(@NonNull FileChooserDialog dialog, @NonNull File file) {
        showMessage("开始发送文件");
        Message msg = Message.obtain();
        msg.obj = file;
        sendFileHandler.sendMessage(msg);


    }

    @Override
    public void onFileChooserDismissed(@NonNull FileChooserDialog dialog) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socket != null) {
            try {
                writer.close();
                reader.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
