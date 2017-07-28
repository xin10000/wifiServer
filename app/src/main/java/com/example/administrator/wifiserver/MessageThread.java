package com.example.administrator.wifiserver;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

import static com.example.administrator.wifiserver.ClientActivity.CONNECTION_EVENT;

// 不断接收消息的线程
public class MessageThread extends Thread {
    private final Socket socket;
    private final PrintWriter writer;
    private final Handler handler;
    private BufferedReader reader;
    String TAG = "MessageThread";

    // 接收消息线程的构造方法
    public MessageThread(BufferedReader reader, PrintWriter writer, Socket socket, Handler handler) {
        this.reader = reader;
        this.socket = socket;
        this.writer = writer;
        this.handler = handler;

    }

    // 被动的关闭连接
    public synchronized void closeCon() throws Exception {

        // 被动的关闭连接释放资源
        if (reader != null) {
            reader.close();
        }
        if (writer != null) {
            writer.close();
        }
        if (socket != null) {
            socket.close();
        }

    }

    public void run() {
        String message = "";
        while (true) {
            try {
//                Log.e(TAG, "run:waiting " + message);
                message = reader.readLine();

//                Log.e(TAG, "run:2 " + message);
                if (message == null) {
                   throw new Exception("异常 断开");
                }
                Message obtainMsg = Message.obtain();
                obtainMsg.obj = message;
                handler.sendMessage(obtainMsg);


//                StringTokenizer stringTokenizer = new StringTokenizer(
//                        message, "/@");
//                String command = stringTokenizer.nextToken();// 命令
                String command = message;
                Log.e(TAG, "StringTokenizer " + command);

                if (command.equals("CLOSE"))// 服务器已关闭命令
                {
                    closeCon();// 被动的关闭连接
                    return;// 结束线程
                } else if (command.equals("ADD")) {// 有用户上线更新在线列表
                    String username = "";
                    String userIp = "";
                }


            } catch (Exception e) {
                Log.e(TAG, "run:客户端异常 已断开 ");
                e.printStackTrace();
                Message obtainMsg = Message.obtain();
                obtainMsg.what=CONNECTION_EVENT;
                obtainMsg.obj = "客户端异常 已断开";
                handler.sendMessage(obtainMsg);
                try {
                    closeCon();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                break;
            }
        }
    }
}