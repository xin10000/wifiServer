package com.example.administrator.wifiserver;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;


// 服务器线程
public class ServerThread extends Thread {
    private ServerSocket serverSocket;
    private int max;// 人数上限
    String TAG="ServerThread";
    private ArrayList<ClientThread> clients;
    private Handler handler;

    // 服务器线程的构造方法
    public ServerThread(ServerSocket serverSocket, int max, ArrayList<ClientThread> clients,Handler handler) {
        this.serverSocket = serverSocket;
        this.max = max;
        this.clients = clients;
        this.handler = handler;
    }

    public void run() {
        while (true) {// 不停的等待客户端的链接
            try {
                Socket socket = serverSocket.accept();
                if (clients.size() == max) {// 如果已达人数上限
                    BufferedReader r = new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
                    PrintWriter w = new PrintWriter(socket
                            .getOutputStream());
                    // 接收客户端的基本用户信息
                    String inf = r.readLine();
                    StringTokenizer st = new StringTokenizer(inf, "@");
                    User user = new User(st.nextToken(), st.nextToken());
                    // 反馈连接成功信息
                    w.println("MAX@服务器：对不起，" + user.getName()
                            + user.getIp() + "，服务器在线人数已达上限，请稍后尝试连接！");
                    w.flush();
                    // 释放资源
                    r.close();
                    w.close();
                    socket.close();
                    continue;
                }
                ClientThread client = new ClientThread(socket,clients,handler);
                client.start();// 开启对此客户端服务的线程
                clients.add(client);



                Message message = Message.obtain();
                message.what=ServerActivity.ADD_CLIENT;
                message.obj=client.getUser().getName()
                        + client.getUser().getIp() + "上线!\r\n";
                handler.sendMessage(message);
                Log.e(TAG, "run: "+client.getUser().getName()
                        + client.getUser().getIp() + "上线!\r\n" );
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}