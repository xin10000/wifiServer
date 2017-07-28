package com.example.administrator.wifiserver;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;

// 为一个客户端服务的线程
public class ClientThread extends Thread {
    private Handler handler;
    private ArrayList<ClientThread> clients;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private User user;
    String TAG = "ClientThread";

    public BufferedReader getReader() {
        return reader;
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public User getUser() {
        return user;
    }

    // 客户端线程的构造方法
    public ClientThread(Socket socket, ArrayList<ClientThread> clients, Handler handler) {
        try {
            this.socket = socket;
            this.clients = clients;
            this.handler = handler;
            reader = new BufferedReader(new InputStreamReader(socket
                    .getInputStream()));
            writer = new PrintWriter(socket.getOutputStream());
            // 接收客户端的基本用户信息
            String inf = reader.readLine();
            StringTokenizer st = new StringTokenizer(inf, "@");
            user = new User(st.nextToken(), st.nextToken());

            // 反馈连接成功信息
            writer.println(user.getName() + user.getIp() + "与服务器连接成功!");
            writer.flush();
            // 反馈当前在线用户信息
//            if (clients.size() > 0) {
//                String temp = "";
//                for (int i = clients.size() - 1; i >= 0; i--) {
//                    temp += (clients.get(i).getUser().getName() + "/" + clients
//                            .get(i).getUser().getIp())
//                            + "@";
//                }
//                writer.println("USERLIST@" + clients.size() + "@" + temp);
//                writer.flush();
//            }
            // 向所有在线用户发送该用户上线命令
//            for (int i = clients.size() - 1; i >= 0; i--) {
//                clients.get(i).getWriter().println(
//                        "ADD@" + user.getName() + user.getIp());
//                clients.get(i).getWriter().flush();
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void run() {// 不断接收客户端的消息，进行处理。
        String message = null;
        while (true) {
            try {
                message = reader.readLine();// 接收客户端消息
                if (message == null) throw new Exception("客户端意外断开");
                Log.e(TAG, "接收客户端的消息: " + message);
                if (message.equals("CLOSE"))// 下线命令
                {
//                    contentArea.append(this.getUser().getName()
//                            + this.getUser().getIp() + "下线!\r\n");
                    // 断开连接释放资源
                    reader.close();
                    writer.close();
                    socket.close();

                    // 向所有在线用户发送该用户的下线命令
                    for (int i = clients.size() - 1; i >= 0; i--) {
                        clients.get(i).getWriter().println(
                                "DELETE@" + user.getName());
                        clients.get(i).getWriter().flush();
                    }

//                    listModel.removeElement(user.getName());// 更新在线列表
                    // 删除此条客户端服务线程
                    for (int i = clients.size() - 1; i >= 0; i--) {
                        if (clients.get(i).getUser() == user) {
                            ClientThread temp = clients.get(i);
                            clients.remove(i);// 删除此用户的服务线程
                            temp.interrupt();// 停止这条服务线程
                            return;
                        }
                    }
                } else if (message.equals("file")) {
                    //接受文件
                    Message msg = Message.obtain();
                    msg.what = ServerActivity.RECEIVE_FILE;
                    msg.obj = getUser();
                    handler.sendMessage(msg);
                    getWriter().println("请 " + getUser().getName() + " 准备接受文件");
                    getWriter().flush();
                } else if (message.equals("name")) {
                    String users = "";
                    for (int i = clients.size() - 1; i >= 0; i--) {
                        users = users + "  " + clients.get(i).getUser().getName();
                    }
                    getWriter().println(users);
                    getWriter().flush();
                } else {
                    getWriter().println(message);
                    getWriter().flush();
//                    dispatcherMessage(message);// 转发消息
                }
            } catch (Exception e) {
                e.printStackTrace();
                clients.remove(this);
                Log.e(TAG, "剩余用户: " + clients.size());
                break;
            }
        }
    }


    // 转发消息
    public void dispatcherMessage(String message) {
//        StringTokenizer stringTokenizer = new StringTokenizer(message, "@");
//        String source = stringTokenizer.nextToken();
//        String owner = stringTokenizer.nextToken();
//        String content = stringTokenizer.nextToken();
//        message = source + "说：" + content;
////        contentArea.append(message + "\r\n");
//        if (owner.equals("ALL")) {// 群发
//            for (int i = clients.size() - 1; i >= 0; i--) {
//                clients.get(i).getWriter().println(message + "(多人发送)");
//                clients.get(i).getWriter().flush();
//            }
//        }
    }
}