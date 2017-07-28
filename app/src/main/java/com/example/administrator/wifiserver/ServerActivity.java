package com.example.administrator.wifiserver;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerActivity extends AppCompatActivity {

    private static final String TAG = "服务端";
    public static final int ADD_CLIENT = 0;
    public static final int RECEIVE_FILE = 1;
    TextView tvRoom;
    private ArrayList<ClientThread> clients;
    private ServerSocket serverSocket;
    ServerThread serverThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        setTitle("服务端");
        tvRoom = (TextView) findViewById(R.id.tv_room);
        serverStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serverSocket != null) {
            try {
                serverSocket.close();
//                serverThread.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case ADD_CLIENT:
                    Toast.makeText(ServerActivity.this, "" + (String) msg.obj, Toast.LENGTH_SHORT).show();
                    tvRoom.setText(tvRoom.getText() + (String) msg.obj + "***");
                    break;

                case RECEIVE_FILE:
                    Toast.makeText(ServerActivity.this, "准备连接对方，接受文件", Toast.LENGTH_SHORT).show();
                    createFileRecClient((User) msg.obj);
                    break;
            }
        }
    };

    private void createFileRecClient(final User user) {
        new Thread() {
            @Override
            public void run() {
                DataInputStream dataInputStream = null;
                FileOutputStream fos= null;
                Socket fileSocket= null;
                try {
                    String userIp = user.getIp().replace("/", "");
                    Log.e(TAG, "接收文件线程，连接服务器ip： " + userIp);
                    fileSocket = new Socket(userIp, 10087);
                    long startTime = System.currentTimeMillis();

                    dataInputStream = new DataInputStream(fileSocket.getInputStream());
                    String fileName = dataInputStream.readUTF();
                    long fileLength = dataInputStream.readLong();

                    Log.e(TAG, "开始接受:文件名： " + fileName + " 大小： " + fileLength);

                    File dir = new File(Environment.getExternalStorageDirectory(), "wifiServer");
                    if (!dir.exists())
                        dir.mkdir();

                     fos = new FileOutputStream(new File(dir, fileName));
                    byte[] bytes = new byte[1024 * 1024];
                    int length = 0;
                    int readFileLength = 0;
                    while ((length = dataInputStream.read(bytes)) != -1) {
                        fos.write(bytes, 0, length);
                        fos.flush();
                        readFileLength += length;
                        Log.e(TAG, "run: " + length + " 已接收 " + readFileLength/fileLength*1.0f);
                    }
                    fos.close();
                    Log.e(TAG, "接收文件完毕，时间 "+(System.currentTimeMillis()-startTime)+" 毫秒");
                    showMessage("接收文件完毕，时间 "+(System.currentTimeMillis()-startTime)+" 毫秒");

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (fos != null)
                            fos.close();
                        if (dataInputStream != null)
                            dataInputStream.close();
                        if (fileSocket != null)
                            fileSocket.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }


            }
        }.start();
    }

    // 启动服务器
    public void serverStart() {
        try {
            int port = 1992;
            int max = 5;

            clients = new ArrayList<>();
            serverSocket = new ServerSocket(port);
            serverThread = new ServerThread(serverSocket, max, clients, handler);
            serverThread.start();
            Log.e(TAG, "serverStart: 成功");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvRoom.setText("开启服务成功！" + tvRoom.getText());
                }
            });
        } catch (BindException e) {
            Log.e(TAG, "serverStart:端口号已被占用，请换一个！ ");
        } catch (Exception e1) {
            e1.printStackTrace();
            Log.e(TAG, "serverStart:启动服务器异常 ");
        }
    }


    private void sendFile(final String hostAddress) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    Socket socket = new Socket(hostAddress, 10087);
//2、调用accept()方法开始监听，等待客户端的连接
                    Log.e(TAG, "发送文件: " + hostAddress);
                    OutputStream outputStream = socket.getOutputStream();
                    InputStream inputStream = getAssets().open("app-debug.apk");
                    byte[] bytes = new byte[1024 * 1024];
                    int length = 0;
                    while ((length = inputStream.read(bytes)) != -1) {
                        outputStream.write(bytes, 0, length);
                        outputStream.flush();
                    }
                    outputStream.close();

                    Log.e(TAG, "发送文件完毕 ");


                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }.start();
    }


    public void showMessage(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ServerActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
