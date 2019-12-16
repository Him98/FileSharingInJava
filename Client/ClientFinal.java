import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import java.math.*;

public class ClientFinal {

    public static int port_UDP = 7010;
    public static int port_TCP = 8080;
    public static DatagramSocket clientUDP;
    public static String LoginName;
    public static String notif = " *** ";
    public static String serverAddr = "localhost";
    public static String bargenerate(long bar)
    {
        long i, j;
        String result = "[";
        for(i=0;i<bar;i++)
        {
            result = result+"=";
        }
        result = result+=">";
        for(j=bar;j<10;j++)
        {
            result = result+" ";
        }

        result = result + "]";
        return result;
    }
    public static void main(String args[])   {
        try
        {
            Socket clientskt = new Socket(serverAddr,port_TCP);
            DataInputStream din;
            din = new DataInputStream(clientskt.getInputStream());
            DataOutputStream dout;
            dout = new DataOutputStream(clientskt.getOutputStream());
            clientUDP = new DatagramSocket();
            String ini = "Initial UDP Packet";
            byte[] file_contents = new byte[1000];
            file_contents = ini.getBytes();
            DatagramPacket initial = new DatagramPacket(file_contents,file_contents.length,InetAddress.getByName(serverAddr),port_UDP);
            //System.out.println(initial);
            clientUDP.send(initial);
            System.out.println("Enter the username: ");
            Scanner scan = new Scanner(System.in);
            LoginName = scan.nextLine();
            dout.writeUTF(LoginName);
            System.out.println(notif + "Connected to Server at " + serverAddr + " Port-8080(TCP) and Port-7010(UDP)"+notif);
            // Thread which receives message from Server
            new Thread(new IncomingMessages (LoginName,din,port_UDP,port_TCP)).start();

            // For sending messages to server.
            BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
            String inputLine=null;
            while(true)
            {
                try
                {
                    String comm,fl;
                    // Getting command entered by the user.
                    inputLine=buffer.readLine();
                    dout.writeUTF(inputLine);
                    if(inputLine.equalsIgnoreCase("LOGOUT"))
                    {
                        System.out.println(notif+"Logged Out"+notif);
                        clientskt.close();
                        din.close();
                        dout.close();
                        System.exit(0);
                    }
                    StringTokenizer tokenedcommand = new StringTokenizer(inputLine," ");
                    //check file transfer
                    comm = tokenedcommand.nextToken();
                    if(comm.contains("upload"))
                    {
                        if(tokenedcommand.hasMoreTokens())
                        {
                            fl=tokenedcommand.nextToken();
                            // TCP file upload
                            if(comm.equals("upload"))
                            {
                                File file = new File(fl);
                                long fileLength =  file.length();
                                FileInputStream fpin = new FileInputStream(file);
                                dout.writeUTF("LENGTH " + fileLength);
                                BufferedInputStream bpin = new BufferedInputStream(fpin);
                                for(long current = 0;current != fileLength;)
                                {
                                    int size=1000;
                                    long avail = size + current;
                                    if(fileLength >= avail)
                                        current = avail;
                                    else
                                    {
                                        size = (int)(fileLength-current);
                                        current = fileLength;
                                    }
                                    file_contents = new byte[size];
                                    bpin.read(file_contents,0,size);
                                    dout.write(file_contents);
                                    long bar = (current*100/fileLength)/10;
                                    System.out.println("Sending file ..."+" "+bargenerate(bar)+" "+(current*100/fileLength)+"% complete");
                                }
                                System.out.println(notif+"File Sent using TCP"+notif);
                            }
                            // UDP file upload
                            else if(comm.equals("upload_udp"))
                            {
                                int size=1024;
                                File file = new File(fl);
                                long fileLength = file.length();
                                System.out.println(fileLength);
                                FileInputStream fpin = new FileInputStream(file);
                                dout.writeUTF("LENGTH " + fileLength);
                                BufferedInputStream bpin = new BufferedInputStream(fpin);
                                for(long current = 0;current != fileLength;)
                                {
                                    long avail = size + current;
                                    if(fileLength  >= avail)
                                        current = avail;
                                    else
                                    {
                                        size = (int)(fileLength-current);
                                        current = fileLength;
                                    }
                                    byte[] file_contents1 = new byte[size];
                                    bpin.read(file_contents1,0,size);
                                    DatagramPacket sendPacket = new DatagramPacket(file_contents1,size,InetAddress.getByName(serverAddr),port_UDP);
                                    clientUDP.send(sendPacket);
                                    long bar = (current*100/fileLength)/10;
                                    System.out.println("Sending file ..."+" "+bargenerate(bar)+" "+(current*100/fileLength)+"% complete");
                                }
                                System.out.println(notif+"File Sent using UDP"+notif);
                            }
                        }
                    }
                }
                catch(IOException ioe)
                {
                    System.out.println("Error occured in Input Output streams: "+ioe);
                    // break;
                }
                catch(Exception e)
                {
                    System.out.println("Error occured: "+e);
                    System.exit(0);
                }
            }
        }
        catch(Exception e)
        {
            System.out.println("Error initializing sockets or streams: " + e);
            System.exit(0);
        }
    }
}
// Class for receiving messages from server
class IncomingMessages implements Runnable {
	private DataInputStream fromServer;
	private String LoginName;
    private Integer udpPort;
    private Integer tcpPort;
    public static String notif = " *** ";
    public static String inputLine = null;
    public IncomingMessages(String LoginName, DataInputStream fromServer, Integer udpPort, Integer tcpPort) {
        this.LoginName = LoginName;
        this.udpPort = udpPort;
        this.tcpPort = tcpPort;
        this.fromServer = fromServer;
    }
    public void run() {
        while(true)
        {
            try {
                int bytesRead, size;
                inputLine = fromServer.readUTF();
                StringTokenizer st = new StringTokenizer(inputLine," ");
                if(st.nextToken().equals("FILE"))
                {
                    String fileName = st.nextToken();
                    String tcp_udp = st.nextToken();
                    st.nextToken();
                    System.out.println("Recieving file "+fileName);
                    byte[] file_contents = new byte[1000];
                    String fn = fileName.split("/",0)[1];
                    File folder = new File(LoginName);
                    int fileLength = Integer.parseInt(st.nextToken());
                    try
                    {
                        folder.mkdir();
                    }
                    catch(Exception e)
                    {
                        System.out.println("File doesn't exist"+e);
                    }
                    FileOutputStream fpout = new FileOutputStream(LoginName+"/"+fn);
                    BufferedOutputStream bpout = new BufferedOutputStream(fpout);
                    if(tcp_udp.equals("UDP"))
                    {
                        size = 1024;
                        file_contents = new byte[size];
                        if(size > fileLength)
                            size = fileLength;
                        System.out.println(fileLength);
                        while(fileLength>0)
                        {
                            DatagramPacket receivePacket  = new DatagramPacket(file_contents, size);
                            ClientFinal.clientUDP.receive(receivePacket);
                            bpout.write(file_contents,0,size);
                            fileLength -= size;
                            if(size > fileLength)
                                size = fileLength;
                        }
                    }
                    else
                    {
                        bytesRead = 0;
                        size = 1000;
                        if(size>fileLength)
                            size = fileLength;
                        while((bytesRead = fromServer.read(file_contents,0,size)) != -1 && fileLength > 0)
                        {
                            bpout.write(file_contents,0,size);
                            fileLength -= size;
                            if(size > fileLength)
                                size = fileLength;
                        }
                    }
                    bpout.flush();
                    System.out.println(notif+"File Recieved"+notif);
                }
                else
                    System.out.println(inputLine);
            }
            catch(Exception e)
            {
                e.printStackTrace(System.out);
                break;
            }
        }
    }
}
