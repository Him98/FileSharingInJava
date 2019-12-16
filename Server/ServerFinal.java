import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import java.math.*;

public class ServerFinal {
    public static int max_clients;
	public static Vector<Socket> ClientSockets;
    public static Vector<Integer> Ports;
    public static String notif = " *** ";
    public static int portUDP = 7010;
    public static Vector<String> LoginNames;
    public static Vector<Group> Groups;
    public static Map<String,Group> ConnectedGroups;
    public static int portTCP = 8080;
    public static DatagramSocket SocUDP;
    ServerFinal() {
    	try
        {
            Ports = new Vector<Integer>();
    		System.out.println(notif+"Server running on localhost Port-8080(TCP), 7010(UDP)"+notif);
            ClientSockets = new Vector<Socket>();
            ServerSocket socketTCP = new ServerSocket(portTCP) ;
            DatagramSocket socketUDP = new DatagramSocket(portUDP);
            LoginNames = new Vector<String>();
            Groups = new Vector<Group>();
            ConnectedGroups = new HashMap<String,Group>();
            // Maximum clients fixed to 10.
            max_clients = 10;
            while(true)
            {
                Socket skt = socketTCP.accept();
                AcceptClient clientConn = new AcceptClient(socketUDP,skt);
            }
    	}
    	catch(Exception e)
        {
    		System.out.println("Error initializing sockets: " + e);
    		System.exit(0);
    	}
    }
    public static void main(String args[]) throws Exception
    {
        ServerFinal server = new ServerFinal();
    }
}

class AcceptClient extends Thread {
	Socket ClientSocket;
	DataInputStream din ;
	DataOutputStream dout ;
	String LoginName;
    public static String notif = " *** ";
	DatagramSocket SocUDP;
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
    public String getUsername()
    {
        return LoginName;
    }

    public void setUsername(String username)
    {
        LoginName = username;
    }
    private void initialize_add(int port)
    {
        ServerFinal.Ports.add(port);
        ServerFinal.LoginNames.add(LoginName);
        ServerFinal.ClientSockets.add(ClientSocket);
    }
	AcceptClient (DatagramSocket socketUDP, Socket skt) throws Exception {
        byte[] intial = new byte[1000];
        din = new DataInputStream(skt.getInputStream());
        dout = new DataOutputStream(skt.getOutputStream());
        SocUDP = socketUDP;
        DatagramPacket recieve_inital = new DatagramPacket(intial, intial.length);
        SocUDP.receive(recieve_inital);
        setUsername(din.readUTF());
        ClientSocket = skt;
        if(ServerFinal.LoginNames.size() == ServerFinal.max_clients)
        {
            dout.writeUTF("Server's maximum limit of 10 reached");
            System.out.println("Server's maximum limit of 10 reached");
            close();
            return;
        }
        System.out.println("User "+getUsername()+" logged in");
        int port = recieve_inital.getPort();
        initialize_add(port);
        ServerFinal.ConnectedGroups.put(getUsername(),null);
        start();
    }

    private void close() {
            try 
            {
                if(ClientSocket != null) 
                    ClientSocket.close();
            }
            catch(Exception e) {}
            try 
            {
                if(din != null)
                    din.close();
            }
            catch(Exception e) {};
            try
            {
                if(dout != null)
                    dout.close();
            }
            catch (Exception e) {}
    }

    public Group getGroup()
    {
        return ServerFinal.ConnectedGroups.get(getUsername());
    }

    public void run() {
    	while(true) {
    		try {
    			String commandfromClient = new String() ;
                commandfromClient = din.readUTF() ;
                StringTokenizer tokenedcommand = new StringTokenizer(commandfromClient," ");
                String command=tokenedcommand.nextToken();
                if(command.equalsIgnoreCase("LOGOUT"))
                {
                    Group C = getGroup();
                    if(C != null)
                    {
                        String outp = C.Leave(LoginName);
                        if(outp.equals("DEL"))
                        {
                            ServerFinal.Groups.remove(C);
                        }
                        else
                        	dout.writeUTF(outp);
                        close();
                        if(ServerFinal.Groups.contains(C))
                            C.Notify(notif+LoginName+" left the group"+notif,LoginName);
                        C=null;
                    }
                    ServerFinal.LoginNames.remove(LoginName);
                    ServerFinal.ClientSockets.remove(ClientSocket) ;
                }
                else if(command.equals("create_folder"))
                {
                    String folder_name = tokenedcommand.nextToken();
                    File fl = new File(folder_name);
                    if(!fl.exists())
                    {
                    	if(fl.mkdir())
                    		dout.writeUTF("Folder created");
	                    else
	                    	dout.writeUTF("Error creating folder");
                	}
                	else
                		System.out.println("Already exists");
                }
                else if(command.equals("move_file"))
                {
                    String src_path = tokenedcommand.nextToken();
                    String dest_path = tokenedcommand.nextToken();
                    File file = new File(src_path);
                    String[] st = src_path.split("/",0);
                    String file_name = st[st.length-1];
                    System.out.println(file_name);
                    if(file.renameTo(new File(dest_path+file_name)))
                    {
                    	file.delete();
                    	dout.writeUTF("File moved successfully");
                    }
                    else
                    	dout.writeUTF("Error moving file");
                }
                else if(command.equals("create_group"))
                {
                    Group C = getGroup();
                    if(C!=null)
                    	dout.writeUTF("You are already in group "+C.name);
                    else
                    {
                        String grpName = tokenedcommand.nextToken();
                        ServerFinal.Groups.add(new Group(grpName, LoginName));
                        // System.out.println(ServerFinal.Groups.size());
                        dout.writeUTF("Group with name as " + grpName + " created.");
                    }
                }
                else if(command.equals("list_groups"))
                {
                    String outp = "";
                    int i,sz = ServerFinal.Groups.size();
                    if(sz == 0)
                    	dout.writeUTF("No Groups exist till now");
                    else
                    {
                        for(i=0;i<sz;i++)
                        	outp=outp+ServerFinal.Groups.elementAt(i).name+"\n";
                        dout.writeUTF(outp);
                    }
                }
                else if(command.equals("list_detail")) {
                    Group C = getGroup();
                    if(C == null)
                    	dout.writeUTF("You are not part of any group yet");
                    else
                    {
                        Vector<String> outpl = C.Members;
                        String outp="";
                        for(int i=0;i<outpl.size();i++)
                        {
                        	outp=outp+"User Name: "+outpl.elementAt(i)+"\n";
                            String user = outpl.elementAt(i);
                            File dir = new File(user);
                            if(!dir.exists())
                                dir.mkdir();
                            File folder = new File(outpl.elementAt(i)+"/");
                            File[] listOfFiles = folder.listFiles();
                            outp += "File Names: ";
                            for(int j = 0; j < listOfFiles.length;j++)
                                outp = outp + user + "/" + listOfFiles[j].getName() + " ";
                            outp += "\n\n";
                        }
                        dout.writeUTF(outp);
                    }
                }
                else if(command.equals("join_group"))
                {
                    Group C = getGroup();
                    if(C != null)
                    	dout.writeUTF("You are already part of group named " + C.name);
                    else
                    {
                        int i, sz = ServerFinal.Groups.size();
                        String grpName=tokenedcommand.nextToken();
                        for(i=0;i<sz;i++)
                        	if(ServerFinal.Groups.elementAt(i).name.equals(grpName))
	                      	{
	                            String outp=ServerFinal.Groups.elementAt(i).Join(LoginName);
	                            dout.writeUTF(outp);
	                            ServerFinal.Groups.elementAt(i).Notify(LoginName+" joined the group",LoginName);
                                break;
	                        }
                        if(i == sz)
                        	dout.writeUTF("Group with name as : "+grpName+" doesn't exist");
                    }
                }
                else if(command.equals("leave_group"))
                {
                    Group C = getGroup();
                    if(C == null)
                    	dout.writeUTF("You are not part of any group yet");
                    else
                    {
                        String grpName = C.name;
                        String outp = C.Leave(LoginName);
                        C.Notify(LoginName+" left the group",LoginName);
                        if(outp.equals("DEL"))
                        {
                            ServerFinal.Groups.remove(C);
                            // c = null;
                            String msg = "You left the group "+grpName+'\n'+grpName+" deleted";
                            dout.writeUTF(msg);
                        }
                        else
                            dout.writeUTF(outp);
                    }
                }
                else if(command.contains("upload"))
                {
                    StringTokenizer st = new StringTokenizer(commandfromClient," ");
                    String cmd=st.nextToken(),fl;
                    if(st.hasMoreTokens())
                    {
                        fl=st.nextToken();
                        if(command.equals("upload"))
                        {
                            // TCP Transfer
                            String st_ = din.readUTF();
                            StringTokenizer stt = new StringTokenizer(st_," ");
                            stt.nextToken();
                            int fileLength = Integer.parseInt(stt.nextToken());
                            StringTokenizer fileName = new StringTokenizer(fl,"/");
                            boolean val = fileName.hasMoreTokens();
                            while(val)
                            {
                            	fl=fileName.nextToken();
                                val = fileName.hasMoreTokens();
                            }
                            byte[] file_contents = new byte[1000];
                            int bytesRead=0,size=1000;
                            if(size>fileLength)
                            	size=fileLength;
                            System.out.println(fl);
                            File folder = new File(LoginName);
                            try{folder.mkdir();}
                            catch(Exception e){}
                            FileOutputStream fpout = new FileOutputStream(folder+"/"+fl);
                            BufferedOutputStream bpout = new BufferedOutputStream(fpout);
                            while((bytesRead=din.read(file_contents,0,size))!=-1 && fileLength>0)
                            {
                                bpout.write(file_contents,0,size);
                                fileLength-=size; if(size>fileLength) size=fileLength;
                            }
                            bpout.flush();
                            System.out.println("File received using TCP.");
                        }
                        else if(command.equals("upload_udp"))
                        {
                            //UDP Transfer
                            String st_ = din.readUTF();
                            StringTokenizer stt = new StringTokenizer(st_," ");
                            stt.nextToken();
                            int fileLength = Integer.parseInt(stt.nextToken());
                            StringTokenizer fileName = new StringTokenizer(fl,"/");
                            boolean val = fileName.hasMoreTokens();
                            while(val)
                            {
                                fl=fileName.nextToken();
                                val = fileName.hasMoreTokens();
                            }
                            int size = 1024;
                            byte[] file_contents = new byte[size];
                            if(size>fileLength)size=fileLength;
                            System.out.println(fileLength);

                            //System.out.println(fileLength);
                            File folder = new File(LoginName);
                            try{folder.mkdir();}
                            catch(Exception e){}
                            FileOutputStream fpout = new FileOutputStream(folder+"/"+fl);
                            BufferedOutputStream bpout = new BufferedOutputStream(fpout);
                            DatagramPacket packetUDP;
                            while(fileLength>0)
                            {
                                packetUDP = new DatagramPacket(file_contents,size);
                                SocUDP.receive(packetUDP);
                                bpout.write(file_contents,0,size);
                                fileLength-=size; 
                                if(size>fileLength)
                                	size=fileLength;
                                // System.out.println(fileLength);
                            }
                            bpout.flush();
                            System.out.println("File received using UDP.");
                        }
                    }
	            }
                else if(command.equals("share_msg"))
                {
                    String msgfromClient=LoginName+":";
                    Group C = getGroup();
                    while(tokenedcommand.hasMoreTokens())
                        msgfromClient=msgfromClient+" "+tokenedcommand.nextToken();
                    if(C==null)
                        dout.writeUTF("You are not part of any group yet");
                    else C.Notify(msgfromClient,LoginName);
                }
                else if(command.equals("get_file"))
                {
                    int size = 1000;
                    byte[] file_contents = new byte[size];
                    String fl = tokenedcommand.nextToken();
                    int ind = fl.indexOf('/');
                    fl = fl.substring(ind+1);
                    System.out.println(fl);
                    File file = new File(fl);
                    FileInputStream fpin = new FileInputStream(file);
                    BufferedInputStream bpin = new BufferedInputStream(fpin);
                    long fileLength =  file.length(), current=0, start = System.nanoTime();
                    dout.writeUTF("FILE "+fl+" TCP  LENGTH "+fileLength);
                    while(current!=fileLength)
                    {
                        size=1000;
                        if(fileLength - current >= size)
                            current+=size;
                        else {
                            size = (int)(fileLength-current);
                            current=fileLength;
                        }
                        file_contents = new byte[size];
                        bpin.read(file_contents,0,size); dout.write(file_contents);
                        long bar = (current*100/fileLength)/10;
                        System.out.println("Sending file ..."+" "+bargenerate(bar)+" "+(current*100/fileLength)+"% complete");
                    }
                    System.out.println("File Sent to user");
                }
	            else
                {
                    dout.writeUTF("Unrecognised command");
                }
    		}
    		catch(Exception e) {
                System.out.println("Connection to user " + LoginName + " interrupted.");
                // System.out.println("hi");
                // e.printStackTrace(System.out) ; 
                break;
            }
    	}
    }
}
// Class Group has all the functions related forming different groups
class Group {
    String name;
	Vector<String> Members = new Vector<String>();
    Group (String LoginName,String member)
    {
        this.name = LoginName;
        ServerFinal.ConnectedGroups.put(member,this);
        this.Members.add(member);
    }
    public String Join (String member)
    {
        ServerFinal.ConnectedGroups.put(member,this);
        this.Members.add(member);
        return ("Joined the group with name: "+this.name);
    }
    public String Leave (String member)
    {
        this.Members.remove(member);
        ServerFinal.ConnectedGroups.put(member,null);
        if(!(this.Members.isEmpty()))
        	return("You left the group with name: "+this.name);
        else
        	return ("DEL");
    }
    public void Notify(String msg,String no_notif)
    {
        int i=0;
        for(;i<this.Members.size();i++)
        {
            Socket sendSoc;
            DataOutputStream senddout;
            if(!this.Members.elementAt(i).equals(no_notif))
            {
                try {
                    sendSoc = ServerFinal.ClientSockets.elementAt(ServerFinal.LoginNames.indexOf(this.Members.elementAt(i)));
                    senddout = new DataOutputStream(sendSoc.getOutputStream());
                    senddout.writeUTF(msg);
                }
                catch(Exception e)
                {
                    e.printStackTrace(System.out);
                }
            }
        }
    }
}