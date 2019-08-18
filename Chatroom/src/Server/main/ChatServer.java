package Server.main;


import Client.function.ChatBean;
import Client.function.ClientBean;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class ChatServer {
    private static ServerSocket ss;
    public static HashMap<String, ClientBean> onlines;
    static {
        try {
            ss = new ServerSocket(8520);
            onlines = new HashMap<String, ClientBean>();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    class CatClientThread extends Thread {
        private Socket client;
        private ChatBean bean;
        private ObjectInputStream ois;
        private ObjectOutputStream oos;

        public CatClientThread(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                // 不停的从客户端接收消息
                while (true) {
                    // 获取输入流 读取从客户端接收到额ChatBean消息
                    ois = new ObjectInputStream(client.getInputStream());
                    bean = (ChatBean)ois.readObject();

                    // 对ChatBean类型做出判断，并一一回应
                    switch (bean.getType()) {
                        // 上下线更新
                        case 0: { // 上线
                            // 记录上线客户用户名以及端口在clientbean中
                            ClientBean cbean = new ClientBean();
                            cbean.setName(bean.getName());
                            cbean.setSocket(client);
                            // 在线列表中添加用户
                            onlines.put(bean.getName(), cbean);
                            // 创建服务器的chatBean，并发送给客户端
                            ChatBean serverBean = new ChatBean();
                            serverBean.setType(0);
                            serverBean.setInfo(bean.getTimer() + "  "
                                    + bean.getName() + "上线了");
                            // 给所有在线用户发出有人上线的消息
                            HashSet<String> set = new HashSet<String>();
                            // 刷新用户列表
                            set.addAll(onlines.keySet());
                            serverBean.setClients(set);
                            sendAll(serverBean);
                            break;
                        }
                        case -1: { // 下线
                            // 创建服务器的chatBean，并发送给客户端
                            ChatBean serverBean = new ChatBean();
                            serverBean.setType(-1);

                            try {
                                oos = new ObjectOutputStream(
                                        client.getOutputStream());
                                oos.writeObject(serverBean);
                                oos.flush();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }

                            onlines.remove(bean.getName());

                            // 向剩下的在线用户发送有人离开的通知
                            ChatBean serverBean2 = new ChatBean();
                            serverBean2.setInfo(bean.getTimer() + "  "
                                    + bean.getName() + " " + " 下线了");
                            serverBean2.setType(0);
                            HashSet<String> set = new HashSet<String>();
                            set.addAll(onlines.keySet());
                            serverBean2.setClients(set);

                            sendAll(serverBean2);
                            return;
                        }
                        case 1: { // 聊天

						 // 创建服务器的chatBean，并发送给客户端
                            ChatBean serverBean = new ChatBean();

                            serverBean.setType(1);
                            serverBean.setClients(bean.getClients());
                            serverBean.setInfo(bean.getInfo());
                            serverBean.setName(bean.getName());
                            serverBean.setTimer(bean.getTimer());
                            // 向选中的客户发送数据
                            sendMessage(serverBean);
                            break;
                        }
                        case 2: { // 请求接受文件

                            ChatBean serverBean = new ChatBean();
                            String info = bean.getTimer() + "  " + bean.getName()
                                    + "向你传送文件,是否需要接受";

                            serverBean.setType(2);
                            serverBean.setClients(bean.getClients()); // 这是发送的目的地
                            serverBean.setFileName(bean.getFileName()); //文件名称
                            serverBean.setSize(bean.getSize()); // 文件大小
                            serverBean.setInfo(info);//提示信息内容
                            serverBean.setName(bean.getName()); // 来源
                            serverBean.setTimer(bean.getTimer());
                            //  向选中的客户发送数据
                            sendMessage(serverBean);

                            break;
                        }
                        case 3: { // 确定接收文件
                            ChatBean serverBean = new ChatBean();

                            serverBean.setType(3);
                            serverBean.setClients(bean.getClients()); //文件来源
                            serverBean.setTo(bean.getTo()); // 文件目的地
                            serverBean.setFileName(bean.getFileName()); // 文件名称
                            serverBean.setIp(bean.getIp());
                            serverBean.setPort(bean.getPort());
                            serverBean.setName(bean.getName()); // 接收的客户名称
                            serverBean.setTimer(bean.getTimer());
                            // 通知文件来源的客户，对方确定接收文件
                            sendMessage(serverBean);
                            break;
                        }
                        case 4: {
                            ChatBean serverBean = new ChatBean();

                            serverBean.setType(4);
                            serverBean.setClients(bean.getClients());
                            serverBean.setTo(bean.getTo());
                            serverBean.setFileName(bean.getFileName());
                            serverBean.setInfo(bean.getInfo());
                            serverBean.setName(bean.getName());
                            serverBean.setTimer(bean.getTimer());
                            sendMessage(serverBean);

                            break;
                        }
                        default: {
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                close();
            }
        }

        //向用户发送数据
        private void sendMessage(ChatBean serverBean) {
            //首先取得所有的values
            Set<String> cbs = onlines.keySet();
            Iterator<String> it = cbs.iterator();
            // 选中客户
            HashSet<String> clients = serverBean.getClients();
            while (it.hasNext()) {
                //在线客户
                String client = it.next();
                // 选中的客户中若是在线的，就发送serverbean
                if (clients.contains(client)) {
                    Socket c = onlines.get(client).getSocket();
                    ObjectOutputStream oos;
                    try {
                        oos = new ObjectOutputStream(c.getOutputStream());
                        oos.writeObject(serverBean);
                        oos.flush();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
            }
        }

        // 向所有用户发送数据
        public void sendAll(ChatBean serverBean) {
            Collection<ClientBean> clients = onlines.values();
            Iterator<ClientBean> it = clients.iterator();
            ObjectOutputStream oos;
            while (it.hasNext()) {
                Socket c = it.next().getSocket();
                try {
                    //进行数据传送
                    oos = new ObjectOutputStream(c.getOutputStream());
                    oos.writeObject(serverBean);
                    oos.flush();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        //关闭各种流
        private void close() {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public void start() {
        try {
            while (true) {
                Socket client = ss.accept();
                new CatClientThread(client).start();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ChatServer().start();
    }

}